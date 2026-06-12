package com.kmhmubin.kothagolp.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.webkit.WebView
import com.kmhmubin.kothagolp.data.remote.CloudflareManager.injectCookiesBeforeLoad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URI
import android.webkit.CookieManager as WebViewCookieManager

/**
 * Manages Cloudflare cookies for bypassing protection.
 *
 * Key design decisions:
 * - cf_clearance cookies are valid for 24 hours per Cloudflare's spec.
 *   We treat them as expired at 23 hours to give a 1-hour safety buffer.
 * - Cookies must be injected into the WebView CookieManager BEFORE any
 *   page load, not just saved after. This is the critical path for bypass.
 * - Domains are normalised (www-stripped, lowercase) before storage so
 *   lookups are consistent regardless of how the URL was typed.
 */
object CloudflareManager {

    private const val PREFS_NAME = "cloudflare_cookies"

    // CF clearance cookies are valid for 24 hours. We refresh at 23 h.
    private const val COOKIE_MAX_AGE_HOURS = 23L
    private val COOKIE_MAX_AGE_MS = COOKIE_MAX_AGE_HOURS * 60 * 60 * 1000L

    // Warn the user before the cookie actually expires (1 hour buffer).
    private const val COOKIE_EXPIRY_WARN_HOURS = 1L
    private val COOKIE_WARN_THRESHOLD_MS =
        (COOKIE_MAX_AGE_HOURS - COOKIE_EXPIRY_WARN_HOURS) * 60 * 60 * 1000L

    // This MUST match exactly what is used in the WebView.
    const val WEBVIEW_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    private var prefs: SharedPreferences? = null

    private val _cookieStateChanged = MutableStateFlow(0L)
    val cookieStateChanged: StateFlow<Long> = _cookieStateChanged.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            cleanupExpiredCookies()
        }
    }

    // ========================================================================
    // Domain Normalisation
    // ========================================================================

    /**
     * Normalise a URL or hostname to a bare domain for use as a storage key.
     *
     * Examples:
     *   "https://www.novelfire.net/novel/foo" → "novelfire.net"
     *   "cdn.novelfire.net"                  → "novelfire.net" (via parent lookup)
     *   "novelfire.net"                      → "novelfire.net"
     */
    fun getDomain(url: String): String {
        return try {
            val cleanUrl = url.trim()
            val uri = URI(
                if (cleanUrl.startsWith("http")) cleanUrl else "https://$cleanUrl"
            )
            uri.host
                ?.removePrefix("www.")
                ?.lowercase()
                ?: fallbackDomainParse(cleanUrl)
        } catch (e: Exception) {
            fallbackDomainParse(url)
        }
    }

    private fun fallbackDomainParse(url: String): String =
        url.removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .substringBefore("/")
            .substringBefore("?")
            .substringBefore("#")
            .lowercase()

    /**
     * Return the parent domain if this is a subdomain, otherwise return the
     * domain itself. E.g. "cdn.novelfire.net" → "novelfire.net".
     * Returns null when the domain is already a top-level registrable domain.
     */
    private fun parentDomain(domain: String): String? {
        val parts = domain.split(".")
        return if (parts.size > 2) parts.takeLast(2).joinToString(".") else null
    }

    // ========================================================================
    // Cookie Storage
    // ========================================================================

    fun getCookiesForDomain(domain: String): String {
        val key = getDomain(domain)
        // Try exact domain first, then parent domain for CDN subdomains.
        return prefs?.getString("cookies_$key", "")?.takeIf { it.isNotBlank() }
            ?: parentDomain(key)?.let { prefs?.getString("cookies_$it", "") }
            ?: ""
    }

    fun getUserAgent(domain: String): String? {
        val key = getDomain(domain)
        return prefs?.getString("ua_$key", null)
            ?: parentDomain(key)?.let { prefs?.getString("ua_$it", null) }
    }

    fun saveCookiesForDomain(domain: String, cookies: String, userAgent: String) {
        if (!cookies.contains("cf_clearance")) {
            android.util.Log.d("CloudflareManager", "No cf_clearance — skipping save for $domain")
            return
        }

        val key = getDomain(domain)
        android.util.Log.d("CloudflareManager", "Saving CF cookies for $key")

        val now = System.currentTimeMillis()
        prefs?.edit()
            ?.putString("cookies_$key", cookies)
            ?.putString("ua_$key", userAgent)
            ?.putLong("time_$key", now)
            ?.apply()

        // Mirror to parent domain so CDN subdomains pick up the same cookie.
        parentDomain(key)?.let { parent ->
            prefs?.edit()
                ?.putString("cookies_$parent", cookies)
                ?.putString("ua_$parent", userAgent)
                ?.putLong("time_$parent", now)
                ?.apply()
            android.util.Log.d("CloudflareManager", "Mirrored CF cookies to parent $parent")
        }

        _cookieStateChanged.value = now
    }

    fun clearCookiesForDomain(domain: String) {
        val key = getDomain(domain)
        prefs?.edit()
            ?.remove("cookies_$key")
            ?.remove("ua_$key")
            ?.remove("time_$key")
            ?.apply()
        _cookieStateChanged.value = System.currentTimeMillis()
    }

    fun clearAllCookies() {
        prefs?.edit()?.clear()?.apply()
        _cookieStateChanged.value = System.currentTimeMillis()
    }

    fun getAllStoredDomains(): List<String> =
        prefs?.all?.keys
            ?.filter { it.startsWith("cookies_") }
            ?.map { it.removePrefix("cookies_") }
            ?: emptyList()

    // ========================================================================
    // Cookie Status
    // ========================================================================

    /**
     * Returns true when we have a non-expired cf_clearance for this URL.
     */
    fun hasClearanceCookie(url: String): Boolean {
        val domain = getDomain(url)
        val cookies = getCookiesForDomain(domain)
        val hasCookie = cookies.contains("cf_clearance=")
        val valid = !areCookiesExpired(domain)
        android.util.Log.d(
            "CloudflareManager",
            "hasClearanceCookie($domain): present=$hasCookie valid=$valid"
        )
        return hasCookie && valid
    }

    /**
     * Returns true when the stored cookies are older than COOKIE_MAX_AGE_HOURS.
     * Fixed bug: the previous implementation used minutes instead of hours,
     * causing cookies to be discarded after only 25 minutes.
     */
    fun areCookiesExpired(domain: String): Boolean {
        val key = getDomain(domain)
        val savedTime = prefs?.getLong("time_$key", 0L) ?: 0L
        if (savedTime == 0L) return true
        return (System.currentTimeMillis() - savedTime) > COOKIE_MAX_AGE_MS
    }

    /**
     * Returns true when the cookie is close to expiry (within COOKIE_EXPIRY_WARN_HOURS).
     */
    fun areCookiesExpiringSoon(domain: String): Boolean {
        val key = getDomain(domain)
        val savedTime = prefs?.getLong("time_$key", 0L) ?: 0L
        if (savedTime == 0L) return true
        return (System.currentTimeMillis() - savedTime) > COOKIE_WARN_THRESHOLD_MS
    }

    fun getCookieAgeMinutes(domain: String): Int? {
        val key = getDomain(domain)
        val savedTime = prefs?.getLong("time_$key", 0L) ?: 0L
        if (savedTime == 0L) return null
        return ((System.currentTimeMillis() - savedTime) / 60_000L).toInt()
    }

    fun getRemainingMinutes(domain: String): Int? {
        val ageMin = getCookieAgeMinutes(domain) ?: return null
        val maxMin = (COOKIE_MAX_AGE_HOURS * 60).toInt()
        val remaining = maxMin - ageMin
        return remaining.coerceAtLeast(0)
    }

    fun getCookieSavedTime(domain: String): Long {
        val key = getDomain(domain)
        return prefs?.getLong("time_$key", 0L) ?: 0L
    }

    fun getCookieStatus(url: String): CookieStatus {
        val domain = getDomain(url)
        val cookies = getCookiesForDomain(domain)
        return when {
            cookies.isBlank() || !cookies.contains("cf_clearance") -> CookieStatus.NONE
            areCookiesExpired(domain) -> CookieStatus.EXPIRED
            else -> CookieStatus.VALID
        }
    }

    enum class CookieStatus { NONE, VALID, EXPIRED }

    private fun cleanupExpiredCookies() {
        // Use a generous 48-hour window for cleanup; the validity check (23h)
        // is separate so we don't delete cookies that might still be useful
        // on sites where CF accepts older tokens.
        val cutoffMs = 48L * 60 * 60 * 1000L
        getAllStoredDomains().forEach { domain ->
            val saved = getCookieSavedTime(domain)
            if (saved == 0L || (System.currentTimeMillis() - saved) > cutoffMs) {
                clearCookiesForDomain(domain)
            }
        }
    }

    // ========================================================================
    // WebView Cookie Bridge
    // ========================================================================

    /**
     * Extract whatever cookies the WebView currently holds for [url].
     * Tries multiple URL variants to ensure a hit.
     */
    fun extractCookiesFromWebView(url: String): String? {
        return try {
            val cm = WebViewCookieManager.getInstance()
            val domain = getDomain(url)

            val candidates = listOf(
                url,
                "https://$domain",
                "https://www.$domain",
                "http://$domain"
            )
            candidates
                .mapNotNull { cm.getCookie(it) }
                .firstOrNull { it.contains("cf_clearance") }
                .also { cookies ->
                    android.util.Log.d(
                        "CloudflareManager",
                        "Extracted WebView cookies for $url: ${cookies?.take(80)}…"
                    )
                }
        } catch (e: Exception) {
            android.util.Log.e("CloudflareManager", "Failed to extract WebView cookies", e)
            null
        }
    }

    fun flushWebViewCookies() {
        try {
            WebViewCookieManager.getInstance().flush()
        } catch (e: Exception) {
            android.util.Log.e("CloudflareManager", "Failed to flush WebView cookies", e)
        }
    }

    /**
     * THE critical fix: inject stored CF cookies into the WebView's
     * CookieManager BEFORE the WebView loads a URL.
     *
     * Call this:
     *  1. In the WebView factory, right before `loadUrl(startUrl)`
     *  2. Inside `shouldOverrideUrlLoading`, before returning false
     *
     * Without this, saved cookies never reach the WebView on cold start
     * or when navigating to a new domain, so Cloudflare always challenges.
     */
    fun injectCookiesBeforeLoad(webView: WebView, url: String) {
        try {
            val domain = getDomain(url)
            val cookies = getCookiesForDomain(domain)

            if (cookies.isBlank()) {
                android.util.Log.d("CloudflareManager", "No stored cookies to inject for $domain")
                return
            }
            if (areCookiesExpired(domain)) {
                android.util.Log.d("CloudflareManager", "Stored cookies for $domain are expired, skipping inject")
                return
            }

            val cm = WebViewCookieManager.getInstance()
            cm.setAcceptCookie(true)
            cm.setAcceptThirdPartyCookies(webView, true)

            val domainsToSet = buildList {
                add(domain)
                add("www.$domain")
                parentDomain(domain)?.let { parent ->
                    add(parent)
                    add("www.$parent")
                }
            }

            cookies.split(";")
                .map { it.trim() }
                .filter { it.isNotBlank() && it.contains("=") }
                .forEach { cookie ->
                    domainsToSet.forEach { d ->
                        cm.setCookie("https://$d", cookie)
                        cm.setCookie("http://$d", cookie)
                    }
                }

            cm.flush()
            android.util.Log.d(
                "CloudflareManager",
                "Injected CF cookies for ${domainsToSet.size} domain variants of $domain"
            )
        } catch (e: Exception) {
            android.util.Log.e("CloudflareManager", "Failed to inject cookies", e)
        }
    }

    /**
     * Legacy alias kept for call sites that use the old name.
     * Prefer [injectCookiesBeforeLoad] when a WebView reference is available.
     */
    fun injectCookiesIntoWebView(url: String) {
        android.util.Log.w(
            "CloudflareManager",
            "injectCookiesIntoWebView(url) called without WebView — cookies may not be injected correctly. " +
                    "Use injectCookiesBeforeLoad(webView, url) instead."
        )
        try {
            val domain = getDomain(url)
            val cookies = getCookiesForDomain(domain)
            if (cookies.isBlank() || areCookiesExpired(domain)) return

            val cm = WebViewCookieManager.getInstance()
            val domainsToSet = buildList {
                add(domain)
                add("www.$domain")
                parentDomain(domain)?.let { add(it); add("www.$it") }
            }
            cookies.split(";").map { it.trim() }.filter { it.isNotBlank() }.forEach { cookie ->
                domainsToSet.forEach { d ->
                    cm.setCookie("https://$d", cookie)
                    cm.setCookie("http://$d", cookie)
                }
            }
            cm.flush()
        } catch (e: Exception) {
            android.util.Log.e("CloudflareManager", "Legacy inject failed", e)
        }
    }

    /**
     * Returns true when a page body looks like a Cloudflare challenge page.
     * Useful for detecting mid-session challenges in the WebViewClient.
     */
    fun isCloudflareChallengeHtml(html: String): Boolean {
        val lower = html.lowercase()
        return listOf(
            "cf-browser-verification",
            "cf_chl_opt",
            "challenge-platform",
            "checking your browser",
            "just a moment",
            "verify you are human",
            "cf-turnstile",
            "challenges.cloudflare.com",
            "cf-spinner"
        ).any { lower.contains(it) }
    }

    /**
     * Validate that a cookie string contains a well-formed cf_clearance value.
     */
    fun isValidCloudflareCookie(cookies: String): Boolean {
        val match = Regex("cf_clearance=([^;\\s]+)").find(cookies) ?: return false
        val value = match.groupValues.getOrNull(1) ?: return false
        // cf_clearance is a long alphanumeric + hyphens/underscores token.
        return value.length > 20 && value.all { it.isLetterOrDigit() || it == '-' || it == '_' }
    }
}