package com.ayush9182.jarvis

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JarvisAgent(context: Context) {
    private val memory = MemoryStore(context)
    private val router = IntentRouter()
    private val llm = LlmClient(context)
    private val tools = AndroidTools(context, object : ConfirmationPolicy {
        override fun confirm(title: String, message: String, action: () -> Unit) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Continue") { _, _ -> action() }
                    .show()
            }
        }
    })

    fun apiKey() = llm.apiKey()
    fun saveApiKey(key: String) = llm.saveApiKey(key)
    fun clearHistory() = memory.clear()

    fun execute(command: String, callback: (String) -> Unit) {
        memory.remember("user", command)
        val route = router.route(command)

        when (route.type) {
            IntentRouter.IntentType.GREETING -> finish("Hello Boss! Jarvis online hai. Aap kya karna chahte ho?", callback)
            IntentRouter.IntentType.CHAT -> CoroutineScope(Dispatchers.IO).launch {
                val reply = llm.answer(command, memory.contextText())
                withContext(Dispatchers.Main) {
                    finish(reply, callback)
                }
            }
            else -> tools.run(route, callback = { result -> finish(result, callback) })
        }
    }

    private fun finish(reply: String, callback: (String) -> Unit) {
        memory.remember("assistant", reply)
        callback(reply)
    }
}

class JarvisBrain(context: Context) {
    private val agent = JarvisAgent(context)
    fun apiKey() = agent.apiKey()
    fun saveApiKey(key: String) = agent.saveApiKey(key)
    fun clearHistory() = agent.clearHistory()
    fun execute(command: String, callback: (String) -> Unit) = agent.execute(command, callback)
}
