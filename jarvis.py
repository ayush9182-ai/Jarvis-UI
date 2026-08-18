from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
import pyttsx3, os, datetime
import google.generativeai as genai

# Configure Gemini API
genai.configure(api_key="AQ.Ab8RN6I5r39xOZNRLh9mU2YprqrFPyIHsjrmf9XHioRxjitOUw")

# Initialize FastAPI app
app = FastAPI()

# Enable CORS (important for GitHub UI connection)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],   # तुम चाहो तो specific domain डाल सकते हो
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize TTS engine
engine = pyttsx3.init()

def speak(text):
    engine.setProperty('rate', 180)
    engine.setProperty('voice', engine.getProperty('voices')[1].id)
    engine.say(text)
    engine.runAndWait()

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
        # Gemini 3.6 Flash model call
        model = genai.GenerativeModel("gemini-3.6-flash")
        response = model.generate_content(cmd)
        reply_text = response.text
        speak(reply_text)
        return {"reply": reply_text}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5000)
