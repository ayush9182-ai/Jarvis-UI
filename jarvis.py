# jarvis.py
from fastapi import FastAPI, Request
import pyttsx3, os, speech_recognition as sr
from openai import OpenAI
import asyncio, datetime, json

app = FastAPI()
engine = pyttsx3.init()
r = sr.Recognizer()
client = OpenAI(api_key="YOUR_API_KEY")

def speak(text):
    engine.setProperty('rate', 180)
    engine.setProperty('voice', engine.getProperty('voices')[1].id)
    engine.say(text)
    engine.runAndWait()

def listen():
    with sr.Microphone() as source:
        print("🎧 Listening...")
        audio = r.listen(source)
        try:
            command = r.recognize_google(audio).lower()
            print("🗣️ You said:", command)
            return command
        except:
            speak("Sorry Ayush, I didn’t catch that.")
            return ""

@app.post("/command")
async def command(request: Request):
    data = await request.json()
    cmd = data.get("command", "").lower()
    if "open browser" in cmd:
        os.system("start chrome")
        speak("Opening browser.")
        return {"reply": "Browser opened."}
    elif "time" in cmd:
        now = datetime.datetime.now().strftime("%H:%M:%S")
        speak(f"The time is {now}")
        return {"reply": f"Current time: {now}"}
    else:
        ai_reply = client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[{"role": "user", "content": cmd}]
        )
        response = ai_reply.choices[0].message.content
        speak(response)
        return {"reply": response}

@app.get("/status")
def status():
    return {"status": "Jarvis online"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5000)
