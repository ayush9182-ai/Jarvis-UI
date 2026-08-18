// script.js
const btn = document.getElementById("activate");
const output = document.getElementById("output");

btn.addEventListener("click", async () => {
  output.innerText = "Activating Jarvis...";
  const res = await fetch("http://localhost:5000/command", {
    method: "POST",
    headers: {"Content-Type": "application/json"},
    body: JSON.stringify({command: "Hey Jarvis"})
  });
  const data = await res.json();
  output.innerText = data.reply;
});

