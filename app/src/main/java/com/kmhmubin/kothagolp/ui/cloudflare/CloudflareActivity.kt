package com.kmhmubin.kothagolp.ui.cloudflare

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.isSystemInDarkTheme
import com.kmhmubin.kothagolp.data.remote.CloudflareManager
import com.kmhmubin.kothagolp.ui.theme.KothagolpTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CloudflareActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_URL = "url"
        private const val EXTRA_PROVIDER_NAME = "provider_name"

        fun createIntent(context: Context, url: String, providerName: String): Intent =
            Intent(context, CloudflareActivity::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_PROVIDER_NAME, providerName)
            }
    }

    private val turnstileJs = """
        (function() {
            if (window._cfAutoClickDone) return;
            function trySubmit() {
                var form = document.querySelector('#challenge-form') ||
                           document.querySelector('#challenge-running') ||
                           document.querySelector('#cf-challenge-running');
                if (!form) return;
                var token = document.querySelector('[name="cf-turnstile-response"]')?.value
                          || document.querySelector('#cf-chl-widget-multi-token')?.value;
                var btn = document.querySelector('#challenge-form button[type="submit"]')
                        || document.querySelector('#challenge-form input[type="submit"]');
                if (token && btn) {
                    window._cfAutoClickDone = true;
                    btn.click();
                } else {
                    if (!window._cfRetries) window._cfRetries = 0;
                    if (window._cfRetries < 20) { window._cfRetries++; setTimeout(trySubmit, 800); }
                }
            }
            trySubmit();
        })();
    """.trimIndent()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra(EXTRA_URL) ?: run { finish(); return }
        val providerName = intent.getStringExtra(EXTRA_PROVIDER_NAME) ?: "Source"

        setContent {
            KothagolpTheme(darkTheme = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                CloudflareScreen(
                    url = url,
                    providerName = providerName,
                    turnstileJs = turnstileJs,
                    onClose = { finish() },
                    onSolved = {
                        setResult(RESULT_OK)
                        finish()
                    }
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloudflareScreen(
    url: String,
    providerName: String,
    turnstileJs: String,
    onClose: () -> Unit,
    onSolved: () -> Unit
) {
    var solveState by remember { mutableStateOf<SolveState>(SolveState.Solving) }
    val scope = rememberCoroutineScope()
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose { webViewRef?.stopLoading() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Cloudflare Verification",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = providerName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString = CloudflareManager.WEBVIEW_USER_AGENT
                        android.webkit.CookieManager.getInstance().setAcceptCookie(true)
                        android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                super.onPageFinished(view, pageUrl)
                                if (pageUrl == null || solveState is SolveState.Solved) return

                                if (!pageUrl.contains("cdn-cgi") && !pageUrl.contains("recaptcha")) {
                                    view?.evaluateJavascript(turnstileJs, null)
                                }

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
                                    solveState = SolveState.Solved
                                    scope.launch {
                                        delay(1500)
                                        onSolved()
                                    }
                                }
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val reqUrl = request?.url?.toString() ?: return false
                                view?.let { CloudflareManager.injectCookiesBeforeLoad(it, reqUrl) }
                                return false
                            }
                        }

                        webViewRef = this
                        CloudflareManager.injectCookiesBeforeLoad(this, url)
                        loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Status banner
            AnimatedVisibility(
                visible = solveState is SolveState.Solved,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Verification complete!",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Closing automatically...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Loading indicator while solving
            AnimatedVisibility(
                visible = solveState is SolveState.Solving,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            text = "Solving Cloudflare challenge...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private sealed class SolveState {
    object Solving : SolveState()
    object Solved : SolveState()
}
