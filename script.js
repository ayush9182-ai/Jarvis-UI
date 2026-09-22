const $ = (id) => document.getElementById(id);
const input = $('commandInput');
const output = $('activityList');
const toast = $('toast');
const historyKey = 'jarvis-command-history';
let listening = false;
let wakeMode = false;
let recognition;
let restartTimer;

const appPackages = {
  youtube: 'youtube://', spotify: 'spotify://', whatsapp: 'whatsapp://', telegram: 'tg://',
  instagram: 'instagram://', maps: 'geo:0,0?q=', chrome: 'googlechrome://',
  camera: 'intent:#Intent;action=android.media.action.IMAGE_CAPTURE;end'
};

function notify(message) {
  toast.textContent = message;
  toast.classList.add('show');
  clearTimeout(notify.timer);
  notify.timer = setTimeout(() => toast.classList.remove('show'), 2800);
}

function saveHistory(command, result, icon = '✦') {
  const history = JSON.parse(localStorage.getItem(historyKey) || '[]');
  history.unshift({ command, result, icon, time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) });
  localStorage.setItem(historyKey, JSON.stringify(history.slice(0, 8)));
  renderHistory();
}

function renderHistory() {
  const history = JSON.parse(localStorage.getItem(historyKey) || '[]');
  if (!history.length) {
    output.innerHTML = '<div class="empty-state">Your commands will appear here.</div>';
    return;
  }
  output.innerHTML = history.map(item => `<div class="activity-row"><span class="activity-icon">${item.icon}</span><div><b>${escapeHtml(item.result)}</b><small>${escapeHtml(item.command)} · ${item.time}</small></div></div>`).join('');
}

function escapeHtml(value) {
  return String(value).replace(/[&<>'"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[c]));
}

function cleanTarget(text, words) {
  let value = text;
  words.forEach(word => { value = value.replace(new RegExp(`^${word}\\s+`, 'i'), ''); });
  return value.trim();
}

function removeWakeWord(text) {
  return text.trim().replace(/^(hey\s+)?jarvis[\s,;:!-]*/i, '').trim();
}

function openUrl(url, message) {
  notify(message);
  window.location.href = url;
}

function runCommand(raw) {
  const original = raw.trim();
  if (!original) { notify('Haan, bolo — main sun raha hoon.'); speak('Haan, bolo. Main sun raha hoon.'); return; }

  const command = removeWakeWord(original);
  const lower = command.toLowerCase();
  let result = 'Command received';
  let icon = '✦';
  $('orbStatus').textContent = 'EXECUTING COMMAND';

  if (/^(hello|hi|hey|hlo|helo|namaste|नमस्ते|सलाम)(\s+jarvis)?[.!?]*$/i.test(command)) {
    result = 'Hello! Jarvis online hai. Aap kya karna chahte ho?';
    icon = '◉';
    notify(result);
    speak('Hello! Jarvis online hai. Aap kya karna chahte ho?');
  } else if (/^(open|launch)\s+/.test(lower)) {
    const target = cleanTarget(command, ['open', 'launch']).toLowerCase();
    const match = Object.keys(appPackages).find(key => target.includes(key));
    if (match) { openUrl(appPackages[match], `Opening ${match}`); result = `Opening ${match}`; icon = '⌕'; }
    else { openUrl(`https://www.google.com/search?q=${encodeURIComponent(target)}`, `Searching for ${target}`); result = `Could not launch ${target}; opened web search`; }
  } else if (/^(call|dial)\s+/.test(lower)) {
    const target = cleanTarget(command, ['call', 'dial']);
    const number = target.replace(/[^\d+]/g, '');
    if (number.length >= 7) { openUrl(`tel:${number}`, `Opening call to ${number}`); result = `Calling ${number}`; icon = '☎'; }
    else { result = `Contact “${target}” needs native Android contacts access`; notify(result); speak(result); icon = '☎'; }
  } else if (/^(message|sms|text)\s+/.test(lower)) {
    const payload = cleanTarget(command, ['message', 'sms', 'text']);
    const match = payload.match(/^(\+?[\d\s-]{7,})\s+(.+)$/);
    if (match) { openUrl(`sms:${match[1].replace(/\s/g, '')}?body=${encodeURIComponent(match[2])}`, 'Opening SMS composer'); result = `Drafting SMS to ${match[1]}`; icon = '✉'; }
    else { result = 'Try: message 9876543210 I am on my way'; notify(result); speak(result); icon = '✉'; }
  } else if (/^(search|find|google)\s+/.test(lower)) {
    const query = cleanTarget(command, ['search', 'find', 'google']);
    openUrl(`https://www.google.com/search?q=${encodeURIComponent(query)}`, `Searching: ${query}`);
    result = `Searching the web for “${query}”`; icon = '⌕';
  } else if (/timer/.test(lower)) {
    const minutes = Number((lower.match(/(\d+)\s*(minute|min|m)/) || [])[1] || 5);
    setTimeout(() => { notify(`Timer complete: ${minutes} minutes`); speak(`Your ${minutes} minute timer is complete.`); }, minutes * 60000);
    result = `Timer set for ${minutes} minute${minutes === 1 ? '' : 's'}`; icon = '◷'; notify(result); speak(result);
  } else if (/time|date/.test(lower)) {
    result = `It is ${new Date().toLocaleString([], { weekday: 'short', hour: 'numeric', minute: '2-digit' })}`;
    notify(result); speak(result); icon = '◷';
  } else {
    result = 'Main samajh gaya: main apps open, call, SMS draft, web search aur timer kar sakta hoon.';
    notify(result); speak(result); icon = '?';
  }

  saveHistory(original, result, icon);
  input.value = '';
  setTimeout(() => { $('orbStatus').textContent = wakeMode ? 'SAY “HEY JARVIS”' : 'READY FOR COMMAND'; }, 900);
}

function speak(text) {
  if ('speechSynthesis' in window) {
    speechSynthesis.cancel();
    speechSynthesis.speak(new SpeechSynthesisUtterance(text));
  }
}

function startListening() {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!SpeechRecognition) { notify('Is browser me voice input supported nahi hai. Chrome Android use karo.'); return; }
  if (listening) { wakeMode = false; recognition.stop(); return; }

  recognition = new SpeechRecognition();
  recognition.lang = 'en-IN';
  recognition.continuous = true;
  recognition.interimResults = false;
  recognition.onstart = () => {
    listening = true; wakeMode = true;
    $('micLabel').textContent = 'Hey Jarvis enabled';
    $('orbStatus').textContent = 'SAY “HEY JARVIS”';
    $('micButton').classList.add('listening');
    notify('Hey Jarvis mode on. Page open rakho aur bolo.');
  };
  recognition.onresult = event => {
    const transcript = event.results[event.results.length - 1][0].transcript.trim();
    const match = transcript.match(/(?:hey\s+)?jarvis[\s,;:!-]*(.*)/i);
    if (match) {
      const command = match[1].trim();
      if (command) runCommand(command);
      else { notify('Haan, bolo.'); speak('Haan, bolo.'); }
    }
  };
  recognition.onerror = event => {
    if (event.error === 'not-allowed') notify('Microphone permission allow karo.');
    else if (event.error !== 'aborted') notify('Voice connection ruk gayi. Dobara tap karke try karo.');
  };
  recognition.onend = () => {
    listening = false;
    $('micLabel').textContent = 'Enable Hey Jarvis';
    $('micButton').classList.remove('listening');
    if (wakeMode) {
      clearTimeout(restartTimer);
      restartTimer = setTimeout(() => { try { recognition.start(); } catch (_) {} }, 500);
    } else $('orbStatus').textContent = 'READY FOR COMMAND';
  };
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
