import speech_recognition as sr
import pyttsx3
import os

engine = pyttsx3.init()
r = sr.Recognizer()

def speak(text):
    engine.say(text)
    engine.runAndWait()

def listen():
    with sr.Microphone() as source:
        print("Listening...")
        audio = r.listen(source)
        try:
            command = r.recognize_google(audio).lower()
            print("You said:", command)
            return command
        except:
            speak("Sorry, I didn't catch that.")
            return ""

while True:
    command = listen()
    if "open browser" in command:
        os.system("start chrome")
        speak("Opening browser.")
    elif "stop jarvis" in command:
        speak("Goodbye Ayush.")
        break
