package com.ayush9182.jarvis

import android.content.Context

/** Backwards-compatible facade used by the existing activity. */
class JarvisBrain(context: Context) {
    private val agent = JarvisAgent(context, object : ConfirmationPolicy {
        override fun confirm(title: String, message: String, action: () -> Unit) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.app.AlertDialog.Builder(context).setTitle(title).setMessage(message)
                    .setNegativeButton("Cancel", null).setPositiveButton("Continue") { _, _ -> action() }.show()
            }
        }
    })
    fun apiKey() = agent.apiKey()
    fun saveApiKey(key: String) = agent.saveApiKey(key)
    fun clearHistory() = agent.clearHistory()
    fun execute(command: String, callback: (String) -> Unit) = agent.execute(command, callback)
}
