package com.ayush9182.jarvis

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Agent orchestration: route -> tool or LLM -> memory -> user response. */
class JarvisAgent(context: Context, private val confirmation: ConfirmationPolicy) {
    private val memory = MemoryStore(context)
    private val router = IntentRouter()
    private val llm = LlmClient(context)
    private val tools = AndroidTools(context, confirmation)

    fun apiKey() = llm.apiKey()
    fun saveApiKey(key: String) = llm.saveApiKey(key)
    fun clearHistory() = memory.clear()

    fun execute(command: String, callback: (String) -> Unit) {
        val route = router.route(command)
        memory.remember("user", command)
        when (route.type) {
            IntentRouter.IntentType.GREETING -> finish("Hello Boss! Jarvis online hai. Aap kya karna chahte ho?", callback)
            IntentRouter.IntentType.CHAT -> CoroutineScope(Dispatchers.IO).launch {
                val reply = llm.answer(command, memory.contextText())
                withContext(Dispatchers.Main) { finish(reply, callback) }
            }
            else -> tools.run(route) { finish(it, callback) }
        }
    }

    private fun finish(reply: String, callback: (String) -> Unit) {
        memory.remember("assistant", reply)
        callback(reply)
    }
}
