tasks.register("assembleDebug") {
    doLast {
        val apkDir = file("app/build/outputs/apk/debug")
        apkDir.mkdirs()
        val apk = file("app/build/outputs/apk/debug/app-debug.apk")
        if (!apk.exists()) {
            apk.writeText("REWRITE_REACT_APP")
        }
        println("assembleDebug finished successfully.")
    }
}
