package com.kmhmubin.kothagolp.data.remote

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Silently resolves Cloudflare managed challenges using a hidden WebView.
 *
 * Works by: loading the blocked URL in a headless WebView → injecting JS that
 * auto-clicks the Turnstile form once the token is ready → monitoring for
 * cf_clearance cookie → saving to CloudflareManager → signalling done.
 *
 * Only handles managed challenges ("Just a moment..."). Interactive CAPTCHA
 * challenges (human-click required) will time out and fall through gracefully.
 */
object CloudflareWebViewSolver {

    private val mutex = Mutex()

    private const val TURNSTILE_AUTO_CLICK_JS = """
        (function() {
            if (window._cfAutoClickDone) return;

            function trySubmit() {
                var challengeForm = document.querySelector('#challenge-form') ||
                                    document.querySelector('#challenge-running') ||
                                    document.querySelector('#cf-challenge-running');
                if (!challengeForm) return;

                var cfToken = document.querySelector('[name="cf-turnstile-response"]')?.value
                              || document.querySelector('#cf-chl-widget-multi-token')?.value;

                var submitBtn = document.querySelector('#challenge-form button[type="submit"]')
                                || document.querySelector('#challenge-form input[type="submit"]');

                if (cfToken && submitBtn) {
                    window._cfAutoClickDone = true;
                    submitBtn.click();
                } else {
                    if (!window._cfRetries) window._cfRetries = 0;
                    if (window._cfRetries < 15) {
                        window._cfRetries++;
                        setTimeout(trySubmit, 1000);
                    }
                }
            }
            trySubmit();
        })();
    """

    /**
     * Attempt to silently bypass Cloudflare for [url].
     * Returns true if cf_clearance was obtained and saved; false on timeout or error.
     */
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun solve(url: String, timeoutMs: Long = 30_000L): Boolean {
        val ctx = CloudflareManager.getAppContext() ?: return false

        return mutex.withLock {
            // Another coroutine may have solved it while we waited for the lock.
            if (CloudflareManager.hasClearanceCookie(url)) return@withLock true

            val deferred = CompletableDeferred<Boolean>()
            var webViewRef: WebView? = null

            withContext(Dispatchers.Main) {
                try {
                    val wv = WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString = CloudflareManager.WEBVIEW_USER_AGENT
                        android.webkit.CookieManager.getInstance().setAcceptCookie(true)
                        android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                super.onPageFinished(view, pageUrl)
                                if (pageUrl == null || deferred.isCompleted) return

                                // Attempt Turnstile auto-click for managed challenge pages.
                                if (!pageUrl.contains("cdn-cgi") && !pageUrl.contains("recaptcha")) {
                                    view?.evaluateJavascript(TURNSTILE_AUTO_CLICK_JS, null)
                                }

                                // Check whether the cookie appeared after this page load.
                                android.webkit.CookieManager.getInstance().flush()
                                val cookies = CloudflareManager.extractCookiesFromWebView(pageUrl)
                                if (!cookies.isNullOrBlank() &&
                                    cookies.contains("cf_clearance") &&
                                    CloudflareManager.isValidCloudflareCookie(cookies)
                                ) {
                                    CloudflareManager.saveCookiesForDomain(
                                        domain = CloudflareManager.getDomain(url),
                                        cookies = cookies,
                                        userAgent = CloudflareManager.WEBVIEW_USER_AGENT
                                    )
                                    deferred.complete(true)
                                }
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean = false
                        }
                    }

                    webViewRef = wv
                    CloudflareManager.injectCookiesBeforeLoad(wv, url)
                    wv.loadUrl(url)
                } catch (e: Exception) {
                    android.util.Log.e("CloudflareWebViewSolver", "WebView setup failed: $url", e)
                    deferred.complete(false)
                }
            }

            val solved = withTimeoutOrNull(timeoutMs) { deferred.await() } ?: false

            withContext(Dispatchers.Main) {
                webViewRef?.stopLoading()
                webViewRef?.destroy()
                webViewRef = null
            }

            android.util.Log.i(
                "CloudflareWebViewSolver",
                if (solved) "Bypassed CF for $url" else "CF bypass timed out / failed for $url"
            )

            solved
        }
    }
}
