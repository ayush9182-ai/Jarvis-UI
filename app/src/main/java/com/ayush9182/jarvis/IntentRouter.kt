package com.ayush9182.jarvis

class IntentRouter {
    enum class IntentType { GREETING, OPEN_APP, SEARCH, CALL, MESSAGE, SETTINGS, TIMER, CHAT }
    data class Route(val type: IntentType, val value: String = "")

    fun route(command: String): Route {
        val c = command.trim()
        val l = c.lowercase()
        return when {
            l.matches(Regex("^(hello|hi|hlo|helo|hey|namaste|yo|sup)(\\s+jarvis)?[.!?]*$")) -> Route(IntentType.GREETING)
            l.startsWith("open ") || l.startsWith("launch ") -> Route(IntentType.OPEN_APP, c.substringAfter(' '))
            l.startsWith("search ") || l.startsWith("find ") || l.startsWith("google ") -> Route(IntentType.SEARCH, c.substringAfter(' '))
            l.startsWith("call ") || l.startsWith("dial ") -> Route(IntentType.CALL, c.substringAfter(' '))
            l.startsWith("message ") || l.startsWith("sms ") || l.startsWith("text ") -> Route(IntentType.MESSAGE, c.substringAfter(' '))
            l.contains("settings") -> Route(IntentType.SETTINGS)
            l.contains("timer") || l.matches(Regex(".*\\d+\\s*(minute|min|m|second|sec|s).*")) -> Route(IntentType.TIMER, c)
            else -> Route(IntentType.CHAT, c)
        }
    }
}
