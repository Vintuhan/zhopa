package com.nodep.project

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

class CasinoBlockerService : AccessibilityService() {

    companion object {
        private val BLOCKED_PACKAGES = setOf(
            "com.casino.slots", "com.poker.stars", "com.pokerstars", 
            "com.vavada", "com.winline.ru", "com.fonbet.android", "com.betboom"
        )

        private val GAMBLING_KEYWORDS = listOf(
            "casino", "казино", "poker", "покер", "slots", "слоты",
            "roulette", "рулетка", "1xbet", "vavada", "stake.com",
            "fon.bet", "winline", "betboom", "parimatch", "mostbet"
        )

        private val BROWSER_URL_RES_IDS = listOf(
            ":id/url_bar", ":id/search_box_text", ":id/mozac_browser_toolbar_edit_url_view",
            ":id/location_bar_edit_text", ":id/url_field"
        )

        private val BROWSER_PACKAGES = setOf(
            "com.android.chrome", "com.yandex.browser", "org.mozilla.firefox",
            "com.sec.android.app.sbrowser", "com.opera.browser", "com.brave.browser"
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        if (BLOCKED_PACKAGES.contains(packageName)) {
            triggerBlockScreen(packageName)
            return
        }

        if (BROWSER_PACKAGES.contains(packageName)) {
            val rootNode = rootInActiveWindow ?: return
            val detectedUrl = extractUrlFromNode(rootNode)
            if (detectedUrl != null && isGamblingUrl(detectedUrl)) {
                performGlobalAction(GLOBAL_ACTION_BACK)
                triggerBlockScreen(detectedUrl)
            }
        }
    }

    private fun extractUrlFromNode(node: AccessibilityNodeInfo): String? {
        val viewId = node.viewIdResourceName?.lowercase(Locale.ROOT)
        if (viewId != null && BROWSER_URL_RES_IDS.any { viewId.endsWith(it) }) {
            val text = node.text?.toString()
            if (!text.isNullOrBlank()) return text
        }
        val text = node.text?.toString()
        if (node.isEditable && !text.isNullOrBlank()) {
            if (text.contains(".") && !text.contains(" ") && text.length > 3) return text
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = extractUrlFromNode(child)
            if (result != null) return result
        }
        return null
    }

    private fun isGamblingUrl(url: String): Boolean {
        val lower = url.lowercase(Locale.ROOT)
        return GAMBLING_KEYWORDS.any { lower.contains(it) }
    }

    private fun triggerBlockScreen(blockedItem: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("EXTRA_IS_BLOCKED", true)
            putExtra("EXTRA_BLOCKED_HOST", blockedItem)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {}
}
