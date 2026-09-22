package com.ayush9182.jarvis

interface JarvisTool {
    val name: String
    fun canHandle(route: IntentRouter.Route): Boolean
    fun run(route: IntentRouter.Route, callback: (String) -> Unit)
}

/** Actions which can affect the user always expose confirmation before execution. */
interface ConfirmationPolicy {
    fun confirm(title: String, message: String, action: () -> Unit)
}
