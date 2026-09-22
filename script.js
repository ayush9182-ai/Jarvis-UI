const $ = (id) => document.getElementById(id);
const input = $('commandInput');
const output = $('activityList');
const toast = $('toast');
const historyKey = 'jarvis-command-history';
let listening = false;
let recognition;

const appPackages = {
  youtube: 'youtube://', spotify: 'spotify://', whatsapp: 'whatsapp://', telegram: 'tg://',
  instagram: 'instagram://', maps: 'geo:0,0?q=', chrome: 'googlechrome://', camera: 'intent:#Intent;action=android.media.action.IMAGE_CAPTURE;end'
};

function notify(message) {
  toast.textContent = message; toast.classList.add('show');
  clearTimeout(notify.timer); notify.timer = setTimeout(() => toast.classList.remove('show'), 2800);
}
function saveHistory(command, result, icon = '✦') {
  const history = JSON.parse(localStorage.getItem(historyKey) || '[]');
  history.unshift({ command, result, icon, time: new Date().toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'}) });
  localStorage.setItem(historyKey, JSON.stringify(history.slice(0, 8))); renderHistory();
}
function renderHistory() {
  const history = JSON.parse(localStorage.getItem(historyKey) || '[]');
  if (!history.length) { output.innerHTML = '<div class="empty-state">Your commands will appear here.</div>'; return; }
  output.innerHTML = history.map(item => `<div class="activity-row"><span class="activity-icon">${item.icon}</span><div><b>${escapeHtml(item.result)}</b><small>${escapeHtml(item.command)} · ${item.time}</small></div></div>`).join('');
}
function escapeHtml(value) { return String(value).replace(/[&<>'"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c])); }
function cleanTarget(text, words) { let value = text; words.forEach(word => value = value.replace(new RegExp(`^${word}\\s+`, 'i'), '')); return value.trim(); }
function openUrl(url, message) { window.location.href = url; notify(message); }
function runCommand(raw) {
  const command = raw.trim(); if (!command) { notify('Give me a command first.'); input.focus(); return; }
  const lower = command.toLowerCase(); let result = 'Command received'; let icon = '✦';
  $('orbStatus').textContent = 'EXECUTING COMMAND';

  if (/^(open|launch)\s+/.test(lower)) {
    const target = cleanTarget(command, ['open','launch']).toLowerCase();
    const match = Object.keys(appPackages).find(key => target.includes(key));
    if (match) { openUrl(appPackages[match], `Opening ${match}`); result = `Opening ${match}`; icon = '⌕'; }
    else { openUrl(`https://www.google.com/search?q=${encodeURIComponent(target)}`, `Searching for ${target}`); result = `Could not launch ${target}; opened web search`; }
  } else if (/^(call|dial)\s+/.test(lower)) {
    const target = cleanTarget(command, ['call','dial']); const number = target.replace(/[^\d+]/g, '');
    if (number.length >= 7) { openUrl(`tel:${number}`, `Opening call to ${number}`); result = `Calling ${number}`; icon = '☎'; }
    else { result = `I need a phone number for “${target}”`; notify(result); icon = '☎'; }
  } else if (/^(message|sms|text)\s+/.test(lower)) {
    const payload = cleanTarget(command, ['message','sms','text']); const match = payload.match(/^(\+?[\d\s-]{7,})\s+(.+)$/);
    if (match) { openUrl(`sms:${match[1].replace(/\s/g,'')}?body=${encodeURIComponent(match[2])}`, 'Opening SMS composer'); result = `Drafting SMS to ${match[1]}`; icon = '✉'; }
    else { result = 'Try: message 9876543210 I am on my way'; notify(result); icon = '✉'; }
  } else if (/^(search|find|google)\s+/.test(lower)) {
    const query = cleanTarget(command, ['search','find','google']); openUrl(`https://www.google.com/search?q=${encodeURIComponent(query)}`, `Searching: ${query}`); result = `Searching the web for “${query}”`; icon = '⌕';
  } else if (/timer/.test(lower)) {
    const minutes = Number((lower.match(/(\d+)\s*(minute|min|m)/) || [])[1] || 5); setTimeout(() => { notify(`Timer complete: ${minutes} minutes`); speak(`Your ${minutes} minute timer is complete.`); }, minutes * 60000); result = `Timer set for ${minutes} minute${minutes === 1 ? '' : 's'}`; icon = '◷'; notify(result);
  } else if (/time|date/.test(lower)) {
    result = `It is ${new Date().toLocaleString([], {weekday:'short', hour:'numeric', minute:'2-digit'})}`; notify(result); icon = '◷'; speak(result);
  } else {
    result = 'I can open apps, call, draft messages, search the web or set timers.'; notify(result); icon = '?'; speak(result);
  }
  saveHistory(command, result, icon); input.value = ''; setTimeout(() => $('orbStatus').textContent = 'READY FOR COMMAND', 900);
}
function speak(text) { if ('speechSynthesis' in window) { speechSynthesis.cancel(); speechSynthesis.speak(new SpeechSynthesisUtterance(text)); } }
function startListening() {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!SpeechRecognition) { notify('Voice input is not supported in this browser.'); return; }
  if (listening) { recognition.stop(); return; }
  recognition = new SpeechRecognition(); recognition.lang = 'en-IN'; recognition.interimResults = false;
  recognition.onstart = () => { listening = true; $('micLabel').textContent = 'Listening…'; $('orbStatus').textContent = 'LISTENING FOR COMMAND'; $('micButton').classList.add('listening'); };
  recognition.onresult = event => { input.value = event.results[0][0].transcript; runCommand(input.value); };
  recognition.onerror = () => notify('I could not hear that. Try again.');
  recognition.onend = () => { listening = false; $('micLabel').textContent = 'Start listening'; $('micButton').classList.remove('listening'); $('orbStatus').textContent = 'READY FOR COMMAND'; };
  recognition.start();
}
$('runButton').addEventListener('click', () => runCommand(input.value));
$('micButton').addEventListener('click', startListening);
input.addEventListener('keydown', e => { if (e.key === 'Enter') runCommand(input.value); });
document.querySelectorAll('[data-command]').forEach(button => button.addEventListener('click', () => { input.value = button.dataset.command; runCommand(input.value); }));
$('clearHistory').addEventListener('click', () => { localStorage.removeItem(historyKey); renderHistory(); notify('Activity cleared.'); });
$('helpButton').addEventListener('click', () => $('helpDialog').showModal());
$('closeDialog').addEventListener('click', () => $('helpDialog').close());
renderHistory();
