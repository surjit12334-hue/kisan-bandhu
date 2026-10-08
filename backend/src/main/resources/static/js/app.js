const API = '';
let state = { token: localStorage.getItem('kb_token') || null, user: JSON.parse(localStorage.getItem('kb_user') || 'null'), lang: localStorage.getItem('kb_lang') || 'hi' };
let selectedMandi = null, selectedSlot = null, adminMandi = null;
let ivr = { node: null, active: false, phone: '9876543210' };

const I18N = {
  hi: { brand:'किसान बंधु · Kisan Bandhu', tagline:'APMC वीघब्रिज स्लॉट · Toll-Free IVR', nav_home:'होम', nav_book:'स्लॉट बुक', nav_tokens:'मेरे टोकन', nav_prices:'मंडी भाव', nav_ivr:'IVR डेमो', nav_admin:'डैशबोर्ड', login:'लॉगिन', logout:'लॉगआउट', register:'पंजीकरण',
    hero_title:'गेट की भीड़ से सीधे निर्बाध खरीदी तक', hero_sub:'4–8 घंटे की लाइन खत्म। डिजिटल टोकन, ज़ीरो-आइडल री-शेड्यूलिंग और टोल-फ्री आवाज़ सहायक — हर किसान के लिए, स्मार्टफ़ोन हो या फ़ीचर फ़ोन।', cta_book:'अभी स्लॉट बुक करें', cta_ivr:'IVR आवाज़ डेमो',
    art_token:'डिजिटल टोकन', art_idle:'ज़ीरो-आइडल', art_idle_meta:'वेटब्रिज खाली समय', art_wait:'पहले इंतज़ार', art_wait_meta:'अब ~0 मिनट',
    stat_mandis:'APMC मंडियाँ (लक्ष्य)', stat_channels:'चैनल: PWA + IVR', stat_engine:'ज़ीरो-आइडल इंजन टिक', stat_training:'ट्रेनिंग ज़रूरी नहीं',
    features:'विशेषताएँ', f1t:'स्वचालित गेट स्लॉट बुकिंग', f1p:'वीघब्रिज के लिए 15-मिनट के स्लॉट, तुरंत टोकन और SMS/OTP अलर्ट।', f2t:'ज़ीरो-आइडल ट्रैप इंजन', f2p:'नो-शो को अपने आप री-शेड्यूल, खाली स्लॉट में प्रतीक्षा-सूची को तुरंत भरना।', f3t:'दोहरी पहुँच: PWA + IVR', f3p:'स्मार्टफ़ोन के लिए वेब PWA, फ़ीचर फ़ोन के लिए टोल-फ्री आवाज़ सहायक।', f4t:'पूर्ण पारदर्शिता', f4p:'लाइव एडमिन डैशबोर्ड, घंटेवार थ्रूपुट, नो-शो दर और SMS आउटबॉक्स।',
    phone:'मोबाइल नंबर', password:'पासवर्ड', name:'नाम', feature_phone:'मेरे पास फ़ीचर फ़ोन है (केवल IVR)', send_otp:'OTP भेजें', verify_otp:'सत्यापित',
    book_title:'वीघब्रिज स्लॉट बुक करें', mandi:'मंडी', date:'तारीख़', vehicle:'वाहन नंबर', crop:'फ़सल', qty:'मात्रा (टन)', confirm_book:'टोकन कन्फ़र्म करें',
    full_title:'स्लॉट भर गए?', full_sub:'प्रतीक्षा सूची में जुड़ें — ज़ीरो-आइडल इंजन खाली होते ही स्लॉट देगा और SMS भेजेगा।', join_waitlist:'प्रतीक्षा सूची में जुड़ें',
    tokens_title:'मेरे टोकन एवं प्रतीक्षा सूची', prices_title:'आज के मंडी भाव', commodity:'फ़सल', price:'भाव (₹)', unit:'इकाई', market:'मंडी', pdate:'तारीख़',
    ivr_title:'टोल-फ्री IVR आवाज़ सहायक — इंटरैक्टिव डेमो', ivr_sub:'फ़ीचर फ़ोन वाला किसान 1800 नंबर पर कॉल करता है। नीचे कीपैड से वही अनुभव आज़माएँ — हर जवाब वास्तविक IVR API से आता है।', call:'कॉल करें', hangup:'रोकें', ivr_menu:'मेन्यू', ivr_1:'स्लॉट बुक करें', ivr_2:'टोकन स्थिति', ivr_3:'आज के भाव', ivr_4:'प्रतीक्षा सूची', ivr_0:'बाहर',
    ivr_note:'प्रदर्शन हेतु कॉलर नंबर 9876543210 (डेमो किसान) माना जाता है। वास्तविक एकीकरण: Exotel / Twilio / Knowlarity वेबहुक इसी एंडपॉइंट पर पॉइंट करते हैं।',
    admin_title:'लाइव प्रशासन डैशबोर्ड', throughput:'घंटेवार थ्रूपुट (पूर्ण वीघब्रिजिंग)', slots_today:'आज के स्लॉट', time:'समय', bridge:'वीघब्रिज', farmer:'किसान', status:'स्थिति', sms_log:'SMS आउटबॉक्स (मॉक गेटवे)', events:'लाइव इवेंट लॉग' },
  en: { brand:'Kisan Bandhu', tagline:'APMC weighbridge slots · Toll-Free IVR', nav_home:'Home', nav_book:'Book Slot', nav_tokens:'My Tokens', nav_prices:'Prices', nav_ivr:'IVR Demo', nav_admin:'Dashboard', login:'Login', logout:'Logout', register:'Register',
    hero_title:'From gate congestion to seamless procurement', hero_sub:'End the 4–8 hour queue. Digital tokens, Zero-Idle rescheduling and a toll-free voice assistant — for every farmer, smartphone or feature phone.', cta_book:'Book a slot now', cta_ivr:'Try the IVR voice demo',
    art_token:'Digital token', art_idle:'Zero-Idle', art_idle_meta:'weighbridge idle time', art_wait:'Earlier wait', art_wait_meta:'now ~0 min',
    stat_mandis:'APMC mandis (target)', stat_channels:'Channels: PWA + IVR', stat_engine:'Zero-Idle engine tick', stat_training:'Zero training needed',
    features:'Features', f1t:'Automated gate slot booking', f1p:'15-minute weighbridge slots with instant tokens and SMS/OTP alerts.', f2t:'Zero-Idle Trap Engine', f2p:'Auto-reschedules no-shows and instantly fills freed slots from the waitlist.', f3t:'Dual access: PWA + IVR', f3p:'Web PWA for smartphones, toll-free voice assistant for feature phones.', f4t:'End-to-end transparency', f4p:'Live admin dashboard, hourly throughput, no-show rate and SMS outbox.',
    phone:'Mobile number', password:'Password', name:'Name', feature_phone:'I use a feature phone (IVR only)', send_hot:'Send OTP', send_otp:'Send OTP', verify_otp:'Verify',
    book_title:'Book a weighbridge slot', mandi:'Mandi', date:'Date', vehicle:'Vehicle number', crop:'Crop', qty:'Quantity (tons)', confirm_book:'Confirm token',
    full_title:'Slots full?', full_sub:'Join the waitlist — the Zero-Idle engine assigns a slot the moment one frees and sends you an SMS.', join_waitlist:'Join waitlist',
    tokens_title:'My tokens & waitlist', prices_title:"Today's mandi prices", commodity:'Crop', price:'Price (₹)', unit:'Unit', market:'Mandi', pdate:'Date',
    ivr_title:'Toll-Free IVR voice assistant — interactive demo', ivr_sub:'A feature-phone farmer calls the 1800 number. Try the same flow on the keypad — every reply comes from the real IVR API.', call:'Call', hangup:'Hang up', ivr_menu:'Menu', ivr_1:'Book slot', ivr_2:'Token status', ivr_3:"Today's prices", ivr_4:'Join waitlist', ivr_0:'Exit',
    ivr_note:'For this demo the caller is treated as 9876543210 (demo farmer). In production, Exotel / Twilio / Knowlarity webhooks point at this same endpoint.',
    admin_title:'Live administration dashboard', throughput:'Hourly throughput (completed weighings)', slots_today:"Today's slots", time:'Time', bridge:'Weighbridge', farmer:'Farmer', status:'Status', sms_log:'SMS outbox (mock gateway)', events:'Live event log' }
};

function t(k) { return (I18N[state.lang] && I18N[state.lang][k]) || I18N.hi[k] || k; }
function applyI18n() {
  document.querySelectorAll('[data-i18n]').forEach(el => { el.textContent = t(el.dataset.i18n); });
  document.getElementById('langBtn').textContent = state.lang === 'hi' ? 'EN' : 'हि';
  renderAuthChip();
}
function toggleLang() { state.lang = state.lang === 'hi' ? 'en' : 'hi'; localStorage.setItem('kb_lang', state.lang); applyI18n(); route(); }

function toast(msg, err) {
  const el = document.getElementById('toast');
  el.textContent = msg; el.className = 'toast show' + (err ? ' err' : '');
  setTimeout(() => el.className = 'toast', 3200);
}

async function api(path, opts = {}) {
  opts.headers = Object.assign({ 'Content-Type': 'application/json' }, opts.headers || {});
  if (state.token) opts.headers['Authorization'] = 'Bearer ' + state.token;
  const res = await fetch(API + path, opts);
  const body = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(body.error || ('HTTP ' + res.status));
  return body;
}

function renderAuthChip() {
  const chip = document.getElementById('userChip'), btn = document.getElementById('authBtn');
  const adminLink = document.getElementById('adminLink');
  if (state.user) {
    chip.style.display = ''; chip.textContent = state.user.name + ' · ' + state.user.role;
    btn.textContent = t('logout'); btn.onclick = logout;
    adminLink.style.display = state.user.role === 'ADMIN' ? '' : 'none';
  } else {
    chip.style.display = 'none'; btn.textContent = t('login'); btn.onclick = () => nav('auth');
    adminLink.style.display = 'none';
  }
}
function logout() { state.token = null; state.user = null; localStorage.removeItem('kb_token'); localStorage.removeItem('kb_user'); renderAuthChip(); nav('home'); }

function nav(view) { location.hash = view; }
function route() {
  const view = (location.hash || '#home').slice(1);
  document.querySelectorAll('.view').forEach(v => v.style.display = 'none');
  const target = document.getElementById('view-' + view) || document.getElementById('view-home');
  target.style.display = '';
  document.querySelectorAll('#mainnav a').forEach(a => a.classList.toggle('active', a.getAttribute('href') === '#' + view));
  if (view === 'book') initBook();
  if (view === 'tokens') loadTokens();
  if (view === 'prices') loadPrices();
  if (view === 'admin') initAdmin();
  if (view === 'auth' && state.user) nav('home');
}

async function initBook() {
  const sel = document.getElementById('mandiSel');
  if (!sel.options.length) {
    const mandis = await api('/api/public/mandis');
    sel.innerHTML = mandis.map(m => `<option value="${m.id}">${m.name} · ${m.district}</option>`).join('');
    document.getElementById('tollFreeHome').textContent = '☎ Toll-Free: ' + (mandis[0]?.tollFree || '');
  }
  const dateEl = document.getElementById('bookDate');
  if (!dateEl.value) dateEl.value = new Date().toISOString().slice(0, 10);
  loadAvailability();
}

async function loadAvailability() {
  const mandiId = document.getElementById('mandiSel').value;
  const date = document.getElementById('bookDate').value;
  if (!mandiId || !date) return;
  selectedMandi = Number(mandiId); selectedSlot = null;
  document.getElementById('bookFormCard').style.display = 'none';
  try {
    const d = await api(`/api/public/availability?mandiId=${mandiId}&date=${date}`);
    document.getElementById('waitEst').textContent =
      (state.lang === 'hi' ? 'अनुमानित प्रतीक्षा: ' : 'Est. wait: ') + d.estimatedWaitMinutes + ' min · ' +
      (state.lang === 'hi' ? 'प्रतीक्षा सूची: ' : 'waitlist: ') + d.waitlistCount;
    const today = new Date().toISOString().slice(0, 10);
    const nowHM = new Date().toTimeString().slice(0, 5);
    document.getElementById('availability').innerHTML = d.bridges.map(b => `
      <div class="bridge"><h4>${b.code}</h4><div class="cells">
      ${b.cells.map(c => {
        const past = date === today && c.time < nowHM;
        const cls = c.available ? (past ? 'cell past' : 'cell free') : 'cell taken';
        return `<button class="${cls}" data-time="${c.time}" ${c.available && !past ? `onclick="pickSlot(this,'${c.time} · ${b.code}')"` : 'disabled'}>${c.time}</button>`;
      }).join('')}
      </div></div>`).join('') || '<p class="muted">No active weighbridges</p>';
  } catch (e) { toast(e.message, true); }
}

function pickSlot(el, label) {
  document.querySelectorAll('.cell').forEach(c => c.classList.remove('sel'));
  el.classList.add('sel');
  selectedSlot = el.dataset.time;
  document.getElementById('bookFormCard').style.display = '';
  document.getElementById('bookFormTitle').textContent = (state.lang === 'hi' ? 'चुना गया स्लॉट: ' : 'Selected slot: ') + label;
}

async function doBook(ev) {
  ev.preventDefault();
  if (!state.token) { toast(state.lang === 'hi' ? 'पहले लॉगिन करें' : 'Please log in first', true); nav('auth'); return false; }
  const f = ev.target;
  try {
    const slot = await api('/api/slots/book', { method: 'POST', body: JSON.stringify({
      mandiId: selectedMandi, date: document.getElementById('bookDate').value, startTime: selectedSlot,
      vehicleNumber: f.vehicleNumber.value, crop: f.crop.value, quantityTons: Number(f.quantityTons.value) }) });
    toast((state.lang === 'hi' ? 'टोकन बना: ' : 'Token issued: ') + slot.token);
    f.reset(); document.getElementById('bookFormCard').style.display = 'none';
    loadAvailability();
  } catch (e) { toast(e.message, true); }
  return false;
}

async function joinWaitlist() {
  if (!state.token) { toast(state.lang === 'hi' ? 'पहले लॉगिन करें' : 'Please log in first', true); nav('auth'); return; }
  try {
    const w = await api('/api/slots/waitlist', { method: 'POST', body: JSON.stringify({
      mandiId: selectedMandi || Number(document.getElementById('mandiSel').value),
      date: document.getElementById('bookDate').value, vehicleNumber: 'NA', crop: 'गेहूँ / Wheat', quantityTons: 1 }) });
    toast((state.lang === 'hi' ? 'प्रतीक्षा सूची में क्रमांक ' : 'Waitlist position ') + w.position);
  } catch (e) { toast(e.message, true); }
}

async function loadTokens() {
  if (!state.token) { document.getElementById('tokensList').innerHTML = `<p class="muted">${state.lang === 'hi' ? 'देखने के लिए लॉगिन करें' : 'Log in to view'}</p>`; return; }
  try {
    const slots = await api('/api/slots/mine');
    document.getElementById('tokensList').innerHTML = slots.length ? slots.map(s => `
      <div class="item">
        <div class="grow"><span class="tok">${s.token}</span>
          <div class="muted">${s.date} · ${s.startTime}–${s.endTime} · ${s.bridgeCode} · ${s.mandiName}</div>
          <div class="muted">${s.vehicleNumber} · ${s.crop} · ${s.quantityTons}t ${s.rescheduledFromToken ? ' · ↻ ' + s.rescheduledFromToken : ''}</div>
        </div>
        <span class="badge ${s.status}">${s.status}</span>
        <div class="actions">
          ${s.status === 'BOOKED' ? `<button class="secondary" onclick="checkin('${s.token}')">Check-in</button>
          <button class="ghost" onclick="cancelSlot(${s.id})">Cancel</button>` : ''}
        </div>
      </div>`).join('') : `<p class="muted">${state.lang === 'hi' ? 'कोई टोकन नहीं — स्लॉट बुक करें' : 'No tokens yet — book a slot'}</p>`;
    const wl = await api('/api/slots/waitlist/mine');
    document.getElementById('waitlistList').innerHTML = wl.length ? `<h3>${state.lang === 'hi' ? 'प्रतीक्षा सूची' : 'Waitlist'}</h3>` + wl.map(w => `
      <div class="item"><div class="grow"><b>${w.mandiName}</b><div class="muted">${w.date} · ${w.crop} · pos ${w.position}</div></div>
      <span class="badge ${w.status}">${w.status}</span></div>`).join('') : '';
  } catch (e) { toast(e.message, true); }
}
async function checkin(token) {
  try { await api('/api/slots/checkin', { method: 'POST', body: JSON.stringify({ token }) }); toast('Checked in ✓'); loadTokens(); } catch (e) { toast(e.message, true); }
}
async function cancelSlot(id) {
  try { await api('/api/slots/' + id + '/cancel', { method: 'POST' }); toast('Cancelled'); loadTokens(); } catch (e) { toast(e.message, true); }
}

async function loadPrices() {
  try {
    const rows = await api('/api/market/prices');
    document.querySelector('#pricesTable tbody').innerHTML = rows.map(r =>
      `<tr><td><b>${r.commodity}</b></td><td>₹${r.price}</td><td>${r.unit}</td><td>${r.mandi}</td><td>${r.date}</td></tr>`).join('');
  } catch (e) { toast(e.message, true); }
}

async function switchAuth(which) {
  document.getElementById('tabLogin').classList.toggle('active', which === 'login');
  document.getElementById('tabReg').classList.toggle('active', which === 'register');
  document.getElementById('loginForm').style.display = which === 'login' ? '' : 'none';
  document.getElementById('regForm').style.display = which === 'register' ? '' : 'none';
}
async function doLogin(ev) {
  ev.preventDefault();
  const f = ev.target;
  try {
    const r = await api('/api/auth/login', { method: 'POST', body: JSON.stringify({ phone: f.phone.value, password: f.password.value }) });
    setSession(r); toast('✓ ' + r.user.name); nav(r.user.role === 'ADMIN' ? 'admin' : 'book');
  } catch (e) { toast(e.message, true); }
  return false;
}
async function doRegister(ev) {
  ev.preventDefault();
  const f = ev.target;
  try {
    const r = await api('/api/auth/register', { method: 'POST', body: JSON.stringify({
      name: f.name.value, phone: f.phone.value, password: f.password.value,
      language: state.lang, featurePhone: f.featurePhone.checked }) });
    setSession(r); toast('✓ ' + r.user.name); nav('book');
  } catch (e) { toast(e.message, true); }
  return false;
}
function setSession(r) { state.token = r.token; state.user = r.user; localStorage.setItem('kb_token', r.token); localStorage.setItem('kb_user', JSON.stringify(r.user)); renderAuthChip(); }
async function requestOtp(purpose) {
  const phone = document.querySelector('#regForm [name=phone]').value;
  if (!/^\d{10}$/.test(phone)) { toast('10 digit phone required', true); return; }
  try { await api('/api/otp/request', { method: 'POST', body: JSON.stringify({ phone, purpose }) }); document.getElementById('otpState').textContent = 'OTP sent (mock SMS → server log)'; } catch (e) { toast(e.message, true); }
}
async function verifyOtp(purpose) {
  const f = document.getElementById('regForm');
  try {
    const r = await api('/api/otp/verify', { method: 'POST', body: JSON.stringify({ phone: f.phone.value, code: f.otp.value, purpose }) });
    document.getElementById('otpState').textContent = r.verified ? '✓ Verified' : '✗ Wrong/expired code';
    if (r.verified) f.otp.dataset.ok = '1';
  } catch (e) { toast(e.message, true); }
}

async function initAdmin() {
  if (!state.user || state.user.role !== 'ADMIN') { toast('Admin only', true); nav('home'); return; }
  const sel = document.getElementById('adminMandiSel');
  if (!sel.options.length) {
    const mandis = await api('/api/public/mandis');
    sel.innerHTML = '<option value="">All mandis</option>' + mandis.map(m => `<option value="${m.id}">${m.name}</option>`).join('');
  }
  if (!document.getElementById('adminDate').value) document.getElementById('adminDate').value = new Date().toISOString().slice(0, 10);
  loadAdmin();
}
async function loadAdmin() {
  const mandiId = document.getElementById('adminMandiSel').value;
  const date = document.getElementById('adminDate').value;
  const q = `${mandiId ? 'mandiId=' + mandiId + '&' : ''}date=${date}`;
  try {
    const s = await api('/api/admin/dashboard?' + q);
    document.getElementById('statCards').innerHTML = [
      ['Bookings', s.bookings], ['Checked-in', s.checkedIn], ['Completed', s.completed],
      ['No-shows', s.noShow], ['Cancelled', s.cancelled], ['No-show %', s.noShowRatePct],
      ['Farmers', s.farmers], ['Slots today', s.totalSlotsToday]
    ].map(([k, v]) => `<div class="stat"><b>${v}</b><span>${k}</span></div>`).join('');
    const max = Math.max(1, ...Object.values(s.hourlyThroughput));
    document.getElementById('hourlyBars').innerHTML = Object.entries(s.hourlyThroughput).map(([h, v]) =>
      `<div class="bar"><i style="height:${Math.round(v / max * 100)}%"></i><span>${h}<br>${v}</span></div>`).join('');
    document.getElementById('eventList').innerHTML = (s.recentEvents || []).map(e =>
      `<div class="item"><span class="badge ${e.type}">${e.type}</span><div class="grow">${e.detail}</div><span class="muted">${e.at.slice(11, 19)}</span></div>`).join('') || '<p class="muted">No events</p>';

    if (mandiId) {
      const slots = await api(`/api/admin/slots?mandiId=${mandiId}&date=${date}`);
      document.querySelector('#slotsTable tbody').innerHTML = slots.map(x => `
        <tr><td><b>${x.token}</b></td><td>${x.startTime}</td><td>${x.bridgeCode}</td>
        <td>${x.farmerName}<br><small>${x.farmerPhone || ''}</small></td><td>${x.vehicleNumber}</td>
        <td><span class="badge ${x.status}">${x.status}</span></td>
        <td>${x.status === 'CHECKED_IN' ? `<button class="secondary" style="padding:.3rem .6rem;font-size:.8rem" onclick="checkout(${x.id})">Complete</button>` : ''}</td></tr>`).join('');
    } else document.querySelector('#slotsTable tbody').innerHTML = '<tr><td colspan="7" class="muted">Select a mandi to see slot list</td></tr>';

    const sms = await api('/api/admin/sms');
    document.getElementById('smsList').innerHTML = sms.map(m =>
      `<div class="item"><span class="badge BOOKED">${m.channel}</span><div class="grow"><b>${m.toPhone}</b> — ${m.body}</div><span class="muted">${(m.createdAt || '').slice(11, 19)}</span></div>`).join('') || '<p class="muted">No SMS yet</p>';
  } catch (e) { toast(e.message, true); }
}
async function checkout(id) { try { await api(`/api/admin/slots/${id}/checkout`, { method: 'POST' }); toast('Completed ✓'); loadAdmin(); } catch (e) { toast(e.message, true); } }

async function ivrStart() {
  ivr = { node: null, active: true, phone: '9876543210' };
  document.getElementById('ivrScreen').innerHTML = '';
  await ivrSend('', '');
}
async function ivrEnd() { ivr.active = false; ivrLine('— call ended —', 'system'); }
function ivrLine(txt, cls) {
  const el = document.getElementById('ivrScreen');
  const d = document.createElement('div'); d.className = 'ivr-line ' + (cls || ''); d.textContent = txt;
  el.appendChild(d); el.scrollTop = el.scrollHeight;
}
async function ivrSend(node, digits) {
  try {
    const r = await api('/api/ivr/incoming', { method: 'POST', body: JSON.stringify({ phone: ivr.phone, node, digits }) });
    ivrLine((digits ? '⌨ ' + digits + '  ' : '') + r.prompt, 'system');
    if (r.data && r.data.token) ivrLine('🎫 ' + r.data.token + (r.data.time ? ' · ' + r.data.time : ''));
    ivr.node = r.node;
    if (r.action === 'hangup') ivr.active = false;
  } catch (e) { ivrLine('Error: ' + e.message); ivr.active = false; }
}
function ivrKey(k) {
  if (!ivr.active) { ivrLine('Press Call first', 'system'); return; }
  ivrLine('⌨ ' + k);
  if (k === '0' && (ivr.node === 'menu_en' ||ivr.node === 'menu_hi' || ivr.node === 'welcome')) { ivrEnd(); return; }
  ivrSend(ivr.node, k);
}

window.addEventListener('hashchange', route);
window.addEventListener('load', () => {
  applyI18n(); route();
  if ('serviceWorker' in navigator) navigator.serviceWorker.register('/sw.js').catch(() => {});
});
setInterval(() => { if (location.hash === '#admin' && state.user && state.user.role === 'ADMIN') loadAdmin(); }, 15000);
