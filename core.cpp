// core.cpp
#include <iostream>
#include <cstdlib>
#include <windows.h>
using namespace std;

void openApp(string app) {
    if (app == "chrome") system("start chrome");
    else if (app == "notepad") system("start notepad");
    else cout << "Unknown app\n";
}

void speak(const string& text) {
    string cmd = "PowerShell -Command \"Add-Type –AssemblyName System.Speech; "
                 "($speak = New-Object System.Speech.Synthesis.SpeechSynthesizer).Speak('" + text + "');\"";
    system(cmd.c_str());
}

int main() {
    speak("Jarvis core online.");
    openApp("chrome");
    speak("Chrome launched successfully.");
    return 0;
}

