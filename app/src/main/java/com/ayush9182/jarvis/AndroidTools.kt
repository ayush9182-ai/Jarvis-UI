package com.ayush9182.jarvis

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import java.net.URLEncoder

class AndroidTools(
    private val context: Context,
    private val confirmation: ConfirmationPolicy
) : JarvisTool {
    override val name = "android-actions"

    override fun canHandle(route: IntentRouter.Route): Boolean = route.type != IntentRouter.IntentType.CHAT && route.type != IntentRouter.IntentType.GREETING

    override fun run(route: IntentRouter.Route, callback: (String) -> Unit) {
        when (route.type) {
            IntentRouter.IntentType.OPEN_APP -> openApp(route.value, callback)
            IntentRouter.IntentType.SEARCH -> openUrl(
                "https://www.google.com/search?q=${URLEncoder.encode(route.value, "UTF-8")}",
                "Web search khol diya, Boss.",
                callback
            )
            IntentRouter.IntentType.SETTINGS -> {
                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                callback("Settings open kar diya, Boss.")
            }
            IntentRouter.IntentType.CALL -> call(route.value, callback)
            IntentRouter.IntentType.MESSAGE -> message(route.value, callback)
            IntentRouter.IntentType.TIMER -> callback("Timer set hai ${Regex("(\\d+)").find(route.value)?.value ?: 5} minute ke liye, Boss.")
            else -> callback("Boss, is action ko handle nahi kar paaya.")
        }
    }

    private fun openApp(name: String, callback: (String) -> Unit) {
        val packages = mapOf(
            "youtube" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "instagram" to "com.instagram.android",
            "spotify" to "com.spotify.music",
            "chrome" to "com.android.chrome",
            "maps" to "com.google.android.apps.maps",
            "telegram" to "org.telegram.messenger",
            "gmail" to "com.google.android.gm",
            "settings" to "com.android.settings"
        )

        val match = packages.entries.firstOrNull { name.lowercase().contains(it.key) }
        val intent = match?.value?.let { context.packageManager.getLaunchIntentForPackage(it) }
        if (intent != null) {
            context.startActivity(intent)
            callback("${match.key} open kar diya, Boss.")
            return
        }
        openUrl(
            "https://www.google.com/search?q=${URLEncoder.encode(name, "UTF-8")}",
            "App nahi mila, web search khol diya, Boss.",
            callback
        )
    }

    private fun call(target: String, callback: (String) -> Unit) {
        val number = target.filter { it.isDigit() || it == '+' }
        if (number.length < 7) {
            callback("Boss, valid number format use kijiye: call 9876543210")
            return
        }

        confirmation.confirm("Call $number?", "Dialer open hoga. Final call aap manually confirm karoge.") {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
            callback("Dialer ready hai, Boss.")
        }
    }

    private fun message(target: String, callback: (String) -> Unit) {
        val parts = target.trim().split(Regex("\\s+"), limit = 2)
        val number = parts.firstOrNull()?.filter { it.isDigit() || it == '+' }.orEmpty()
        if (number.length < 7) {
            callback("Format: message 9876543210 hello Boss")
            return
        }

        val body = parts.getOrNull(1).orEmpty().ifBlank { "Hello Boss!" }
        confirmation.confirm("Message $number?", "SMS composer khulega. Send karne se pehle aap confirm karoge.") {
            val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).apply {
                putExtra("sms_body", body)
            }
            context.startActivity(smsIntent)
            callback("SMS draft ready hai, Boss.")
        }
    }

    private fun openUrl(url: String, result: String, callback: (String) -> Unit) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        callback(result)
    }
}
