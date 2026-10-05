const $=(s,r=document)=>r.querySelector(s),$$=(s,r=document)=>[...r.querySelectorAll(s)];
const KEY='sun_key';
const SESSION_KEY='sun_session';

// ---------- Появление блоков при скролле (stagger для вложенных .st) ----------
$$('.stagger').forEach(g=>$$('.st',g).forEach((el,i)=>el.style.setProperty('--i',i)));
const io=new IntersectionObserver(entries=>entries.forEach(x=>{
  if(x.isIntersecting){x.target.classList.add('in');io.unobserve(x.target)}
}),{threshold:.12});
$$('.reveal').forEach(el=>io.observe(el));

// ---------- Тост (вместо alert) ----------
const toastEl=$('#toast');
let toastTimer;
function toast(msg){
  toastEl.textContent=msg;
  toastEl.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer=setTimeout(()=>toastEl.classList.remove('show'),2600);
}

// ---------- До / После: слайдер, табы, клавиатура ----------
(function initCompare(){
  const box=$('#compare-box');
  if(!box)return;
  const handle=$('.compare-handle',box);
  const tagB=$('.tag-before',box),tagA=$('.tag-after',box);
  let isDown=false,animTimer=null,cur=50;

  function setPos(pct,animate=false){
    pct=Math.max(0,Math.min(100,pct));
    cur=pct;
    clearTimeout(animTimer);
    box.classList.toggle('is-animating',animate);
    if(animate)animTimer=setTimeout(()=>box.classList.remove('is-animating'),430);
    box.style.setProperty('--pos',pct+'%');
    handle.setAttribute('aria-valuenow',Math.round(pct));
    tagB.style.opacity=pct<6?'0':'1';
    tagA.style.opacity=pct>94?'0':'1';
    $$('[data-compare-tab]').forEach(t=>{
      const mode=t.dataset.compareTab;
      t.classList.toggle('on',mode==='before'?pct>=50:pct<50);
    });
  }
  const getPos=e=>{const r=box.getBoundingClientRect();return(e.clientX-r.left)/r.width*100};

  $$('[data-compare-tab]').forEach(b=>{
    b.onclick=()=>setPos(b.dataset.compareTab==='before'?100:0,true);
  });

  box.addEventListener('pointerdown',e=>{isDown=true;setPos(getPos(e))});
  window.addEventListener('pointermove',e=>{if(isDown)setPos(getPos(e))});
  const end=()=>{isDown=false};
  window.addEventListener('pointerup',end);
  window.addEventListener('pointercancel',end);

  handle.addEventListener('keydown',e=>{
    const step=e.shiftKey?20:5;
    let next=null;
    if(e.key==='ArrowLeft')next=cur-step;
    else if(e.key==='ArrowRight')next=cur+step;
    else if(e.key==='Home')next=0;
    else if(e.key==='End')next=100;
    if(next===null)return;
    e.preventDefault();
    setPos(next,true);
  });
})();

// ---------- Подробный гайд по лаунчерам: плавное раскрытие ----------
(function initDetails(){
  const btn=$('#btn-details'),panel=$('#install-more');
  if(!btn||!panel)return;
  const lbl=$('.lbl',btn);
  const initialText=lbl?lbl.textContent:'Подробный гайд по лаунчерам';
  btn.addEventListener('click',()=>{
    const open=btn.getAttribute('aria-expanded')!=='true';
    btn.setAttribute('aria-expanded',String(open));
    panel.classList.toggle('open',open);
    panel.toggleAttribute('inert',!open);
    if(lbl)lbl.textContent=open?'Скрыть гайд по лаунчерам':initialText;
  });
})();

// ---------- Подсказки лаунчеров ----------
$$('[data-launcher-tab]').forEach(b=>{
  b.onclick=()=>{
    $$('[data-launcher-tab]').forEach(t=>t.classList.toggle('on',t===b));
    $$('.launcher-info').forEach(i=>i.classList.toggle('on',i.dataset.launcher===b.dataset.launcherTab));
  };
});

// ---------- Копирование пути ----------
function fallbackCopy(text,cb){
  const ta=document.createElement('textarea');
  ta.value=text;
  ta.style.cssText='position:fixed;opacity:0';
  document.body.appendChild(ta);
  ta.select();
  try{document.execCommand('copy');if(cb)cb()}catch(e){}
  document.body.removeChild(ta);
}
$$('[data-copy]').forEach(b=>{
  b.onclick=()=>{
    const text=b.dataset.copy;
    if(!text)return;
    const onSuccess=()=>{
      const orig=b.textContent;
      b.textContent='Скопировано';
      b.classList.add('copied');
      setTimeout(()=>{b.textContent=orig;b.classList.remove('copied')},2000);
    };
    if(navigator.clipboard&&navigator.clipboard.writeText){
      navigator.clipboard.writeText(text).then(onSuccess).catch(()=>fallbackCopy(text,onSuccess));
    }else fallbackCopy(text,onSuccess);
  };
});

// ---------- Ripple на кнопках ----------
$$('.btn').forEach(btn=>{
  btn.addEventListener('click',e=>{
    const r=document.createElement('span');
    r.className='btn-ripple';
    const d=Math.max(btn.clientWidth,btn.clientHeight);
    const rect=btn.getBoundingClientRect();
    r.style.width=r.style.height=d+'px';
    r.style.left=(e.clientX-rect.left-d/2)+'px';
    r.style.top=(e.clientY-rect.top-d/2)+'px';
    btn.appendChild(r);
    setTimeout(()=>r.remove(),600);
  });
});

// ---------- Заглушки соцсетей ----------
$$('.social-icon.is-stub').forEach(icon=>{
  icon.onclick=()=>toast(icon.dataset.stub+' скоро откроется');
});

// ---------- Модальное окно SUN ID (Вход / Регистрация) ----------
const auth = $('#auth'), alertBox = $('#alert'), authNotice = $('#auth-notice'), authNoticeText = $('#auth-notice-text');
const note = (m, ok = false) => {
  if (!alertBox) return;
  alertBox.textContent = m;
  alertBox.classList.toggle('ok', ok && !!m);
  alertBox.hidden = !m;
};

let currentUser = null;
let isRegMode = false;

function openAuthModal(mode = 'log', notice = '') {
  note('');
  if (authNotice) {
    if (notice) {
      if (authNoticeText) authNoticeText.textContent = notice;
      authNotice.hidden = false;
    } else {
      authNotice.hidden = true;
    }
  }

  if (currentUser) {
    $('#auth-box').hidden = true;
    const loggedBox = $('#auth-logged-in-box');
    if (loggedBox) {
      loggedBox.hidden = false;
      const nameEl = $('#logged-user-name');
      if (nameEl) nameEl.textContent = currentUser.username;
    }
  } else {
    $('#auth-box').hidden = false;
    const loggedBox = $('#auth-logged-in-box');
    if (loggedBox) loggedBox.hidden = true;
    setAuthMode(mode === 'reg');
  }

  if (auth && !auth.open) auth.showModal();
}

$$('[data-open-auth]').forEach(b => {
  b.onclick = () => {
    if (currentUser) {
      window.location.href = 'profile.html';
      return;
    }
    openAuthModal('log');
  };
});

const closeBtn = $('[data-close]');
if (closeBtn) {
  closeBtn.onclick = () => {
    if (auth) auth.close();
  };
}

if (auth) {
  // Запрещаем случайное закрытие окна по клику вне формы или клавише Escape
  auth.addEventListener('cancel', e => {
    e.preventDefault();
  });
}

function setAuthMode(reg) {
  isRegMode = reg;
  const fLog = $('#f-log'), fReg = $('#f-reg');
  const tabLog = $('#tab-auth-log'), tabReg = $('#tab-auth-reg');

  if (fLog) fLog.hidden = reg;
  if (fReg) fReg.hidden = !reg;

  if (tabLog) tabLog.classList.toggle('active', !reg);
  if (tabReg) tabReg.classList.toggle('active', reg);

  const titleEl = $('#auth-title');
  const descEl = $('#auth-desc');
  const switchTextEl = $('#auth-switch-text');
  const switchBtn = $('#btn-switch-auth');

  if (titleEl) titleEl.textContent = reg ? 'Создать аккаунт' : 'Вход в аккаунт';
  if (descEl) descEl.textContent = reg ? 'Один профиль SUN ID для доступа к клиенту и обновлениям.' : 'Управляйте доступом и настройками клиента из единого профиля.';
  if (switchTextEl) switchTextEl.textContent = reg ? 'Уже есть аккаунт?' : 'Нет аккаунта?';
  if (switchBtn) switchBtn.textContent = reg ? 'Войти' : 'Зарегистрироваться';
  note('');
}

const tabLogBtn = $('#tab-auth-log');
if (tabLogBtn) tabLogBtn.onclick = () => setAuthMode(false);
const tabRegBtn = $('#tab-auth-reg');
if (tabRegBtn) tabRegBtn.onclick = () => setAuthMode(true);
const switchBtn = $('#btn-switch-auth');
if (switchBtn) switchBtn.onclick = () => setAuthMode(!isRegMode);

// API
async function api(path, body) {
  const r = await fetch('/api/' + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  const d = await r.json().catch(() => null);
  if (!r.ok || !d || !d.success) {
    const err = new Error(d && d.error || 'Ошибка запроса');
    err.status = r.status;
    err.data = d;
    throw err;
  }
  return d;
}

function saveSession(token, key, user) {
  try {
    if (token) {
      localStorage.setItem(SESSION_KEY, token);
      document.cookie = `sun_session=${encodeURIComponent(token)}; Path=/; Max-Age=2592000; SameSite=Lax`;
    }
    if (key) {
      localStorage.setItem(KEY, key);
      document.cookie = `sun_key=${encodeURIComponent(key)}; Path=/; Max-Age=2592000; SameSite=Lax`;
    }
    if (user) {
      localStorage.setItem('sun_user', JSON.stringify(user));
    }
  } catch (e) {}
}

function clearSession() {
  try {
    localStorage.removeItem(SESSION_KEY);
    localStorage.removeItem(KEY);
    localStorage.removeItem('sun_user');
    document.cookie = 'sun_session=; Path=/; Max-Age=0; SameSite=Lax';
    document.cookie = 'sun_key=; Path=/; Max-Age=0; SameSite=Lax';
  } catch (e) {}
}

function getStoredToken() {
  try {
    const fromStorage = localStorage.getItem(SESSION_KEY);
    if (fromStorage) return fromStorage;
    const match = document.cookie.match(/(?:^|;\s*)sun_session=([^;]+)/);
    return match ? decodeURIComponent(match[1]) : '';
  } catch (e) {
    return '';
  }
}

function getStoredUser() {
  try {
    const raw = localStorage.getItem('sun_user');
    return raw ? JSON.parse(raw) : null;
  } catch (e) {
    return null;
  }
}

function updateUserUI(u) {
  if (!u) return;
  currentUser = u;
  saveSession(u.sessionToken, u.key, u);

  const headerBtn = $('#btn-header-auth');
  if (headerBtn) {
    headerBtn.innerHTML = `
      <span style="display:inline-block;width:18px;height:18px;border-radius:50%;background:#3b82f6;color:#fff;font-size:11px;line-height:18px;text-align:center;font-weight:700;margin-right:6px;">${(u.username || 'U')[0].toUpperCase()}</span>
      <span>${u.username}</span>
      <span style="display:inline-flex;align-items:center;gap:3px;color:#ffc107;margin-left:7px;font-weight:700;">
        <img src="assets/sparks.svg" width="13" height="13" class="sparks-mark" alt="">
        ${u.coins || 0}
      </span>
    `;
    headerBtn.title = 'Перейти в Личный кабинет';
    headerBtn.onclick = () => { window.location.href = 'profile.html'; };
  }
}

// Защита скачивания (Пункт 4: скачивание только после регистрации/входа)
$$('.download-main-btn, [data-download-btn]').forEach(btn => {
  btn.addEventListener('click', e => {
    if (!currentUser || !currentUser.key) {
      e.preventDefault();
      openAuthModal('reg', 'Для скачивания SUN Client необходимо зарегистрироваться или войти в SUN ID');
      return false;
    }
  });
});

async function submit(e, path, body) {
  e.preventDefault();
  note('');
  const b = $('button:not([type=button])', e.target);
  if (b) b.disabled = true;
  try {
    const res = await api(path, body());
    updateUserUI(res);
    toast(path === 'register' ? 'Аккаунт успешно создан! Переходим в личный кабинет...' : 'Вход выполнен! Переходим в личный кабинет...');
    setTimeout(() => {
      window.location.href = 'profile.html';
    }, 450);
  } catch (err) {
    note(err instanceof TypeError ? 'Сервер недоступен, попробуйте позже.' : err.message);
  } finally {
    if (b) b.disabled = false;
  }
}

const fLog = $('#f-log');
if (fLog) {
  fLog.onsubmit = e => submit(e, 'login', () => ({
    query: $('#log-login').value.trim(),
    password: $('#log-pass').value.trim()
  }));
}

const fReg = $('#f-reg');
if (fReg) {
  fReg.onsubmit = e => submit(e, 'register', () => ({
    username: $('#reg-user').value.trim(),
    email: $('#reg-email').value.trim(),
    password: $('#reg-pass').value.trim()
  }));
}

// Генератор надежных паролей
function generateSecurePassword(length = 14) {
  const chars = 'abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789!@#$%^&*';
  const buf = new Uint32Array(length);
  crypto.getRandomValues(buf);
  return [...buf].map(n => chars[n % chars.length]).join('');
}
const genPassBtn = $('#btn-gen-pass');
if (genPassBtn) {
  genPassBtn.onclick = () => {
    const passInput = $('#reg-pass');
    if (!passInput) return;
    passInput.value = generateSecurePassword(14);
    passInput.type = 'text';
    updateEyeIcon(true);
    note('Сгенерирован надежный пароль! Сохраните его.', true);
  };
}

// Переключение видимости пароля (SVG иконки вместо эмодзи)
function updateEyeIcon(isText) {
  const eyeBtn = $('#btn-toggle-eye');
  if (!eyeBtn) return;
  if (isText) {
    eyeBtn.innerHTML = '<svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.44-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.61-1.57l3.15 3.15.02-.38c0-1.66-1.34-3-3-3l-.17.23z"/></svg>';
  } else {
    eyeBtn.innerHTML = '<svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/></svg>';
  }
}
const toggleEyeBtn = $('#btn-toggle-eye');
if (toggleEyeBtn) {
  toggleEyeBtn.onclick = () => {
    const passInput = $('#reg-pass');
    if (!passInput) return;
    const isText = passInput.type === 'text';
    passInput.type = isText ? 'password' : 'text';
    updateEyeIcon(!isText);
  };
}

// Быстрое автодополнение @gmail.com
const fillGmailBtn = $('#btn-fill-gmail');
if (fillGmailBtn) {
  fillGmailBtn.onclick = () => {
    const emailInput = $('#reg-email');
    if (!emailInput) return;
    const val = emailInput.value.trim();
    if (!val) {
      emailInput.value = 'user@gmail.com';
      emailInput.focus();
      emailInput.setSelectionRange(0, 4);
      return;
    }
    emailInput.value = (val.includes('@') ? val.split('@')[0] : val) + '@gmail.com';
  };
}

// Вход через Google
const googleAuthBtn = $('#btn-google-auth');
if (googleAuthBtn) {
  googleAuthBtn.onclick = async () => {
    note('Авторизация через Google временно находится на модерации Google Cloud. Пожалуйста, используйте форму входа и регистрации по логину и паролю.');
  };
}

// Выход из аккаунта из модального окна
const modalLogoutBtn = $('#btn-modal-logout');
if (modalLogoutBtn) {
  modalLogoutBtn.onclick = () => {
    clearSession();
    currentUser = null;
    fetch('/api/logout', { method: 'POST' }).catch(() => {});
    const headerBtn = $('#btn-header-auth');
    if (headerBtn) {
      headerBtn.textContent = 'Войти / SUN ID';
      headerBtn.onclick = () => openAuthModal('log');
    }
    const loggedBox = $('#auth-logged-in-box');
    if (loggedBox) loggedBox.hidden = true;
    $('#auth-box').hidden = false;
    setAuthMode(false);
    toast('Вы вышли из профиля');
  };
}

// Проверка сессии при загрузке страницы:
try {
  // 1. Мгновенно отображаем сохраненные данные профиля из кеша (0мс задержка)
  const cachedUser = getStoredUser();
  if (cachedUser) {
    updateUserUI(cachedUser);
  }

  // 2. В фоновом режиме валидируем и обновляем сессию с сервером
  const sessionTok = getStoredToken();
  if (sessionTok) {
    api('login', { sessionToken: sessionTok }).then(u => {
      updateUserUI(u);
    }).catch(err => {
      // Сессия сбрасывается ТОЛЬКО если сервер явно ответил 401 (сессия истекла/отозвана)
      // При сетевых сбоях, снах ноутбука, перезагрузке сервера или задержках — сессия остается активной
      if (err && err.status === 401) {
        clearSession();
        currentUser = null;
        const headerBtn = $('#btn-header-auth');
        if (headerBtn) {
          headerBtn.textContent = 'Войти / SUN ID';
          headerBtn.onclick = () => openAuthModal('log');
        }
      }
    });
  }
} catch (e) {}

// Проверка query параметра ?openAuth=1
try {
  const params = new URLSearchParams(window.location.search);
  if (params.get('openAuth')) {
    setTimeout(() => {
      openAuthModal('log', 'Пожалуйста, войдите в SUN ID для доступа к личному кабинету');
    }, 200);
  }
} catch {}

// ---------- 3D Tilt эффект: карточки разворачиваются лицом к мышке с плавной физикой ----------
(function initCardTilt(){
  if(!window.matchMedia('(hover: hover) and (pointer: fine)').matches) return;
  const cards = $$('.card');
  const MAX_TILT = 7.5; // максимальный угол наклона в градусах
  const LERP_SPEED = 9.5; // скорость плавной интерполяции входа и выхода

  cards.forEach(card => {
    let rafId = null;
    let curX = 0, curY = 0, curScale = 1;
    let targetX = 0, targetY = 0, targetScale = 1;
    let curMouseX = 50, curMouseY = 50;
    let targetMouseX = 50, targetMouseY = 50;
    let isHovered = false;
    let lastTime = 0;

    function updatePhysics(now){
      if(!lastTime) lastTime = now;
      const dt = Math.min((now - lastTime) / 1000, 0.1);
      lastTime = now;

      // Экспоненциальное сглаживание, независимое от частоты кадров (60-240 FPS)
      const factor = 1 - Math.exp(-LERP_SPEED * dt);
      curX += (targetX - curX) * factor;
      curY += (targetY - curY) * factor;
      curScale += (targetScale - curScale) * factor;
      curMouseX += (targetMouseX - curMouseX) * factor;
      curMouseY += (targetMouseY - curMouseY) * factor;

      card.style.setProperty('--mouse-x', `${curMouseX.toFixed(1)}%`);
      card.style.setProperty('--mouse-y', `${curMouseY.toFixed(1)}%`);
      card.style.transform = `perspective(900px) rotateX(${curX.toFixed(2)}deg) rotateY(${curY.toFixed(2)}deg) scale3d(${curScale.toFixed(3)}, ${curScale.toFixed(3)}, ${curScale.toFixed(3)})`;

      const dist = Math.abs(targetX - curX) + Math.abs(targetY - curY) + Math.abs(targetScale - curScale);

      // Когда мышка ушла и карточка плавно вернулась в покой — останавливаем анимацию
      if(!isHovered && dist < 0.015){
        card.style.transform = '';
        card.style.removeProperty('--mouse-x');
        card.style.removeProperty('--mouse-y');
        rafId = null;
        lastTime = 0;
        curX = 0; curY = 0; curScale = 1;
        return;
      }

      rafId = requestAnimationFrame(updatePhysics);
    }

    function startLoop(){
      if(!rafId){
        lastTime = performance.now();
        rafId = requestAnimationFrame(updatePhysics);
      }
    }

    function setTargetFromPointer(e){
      const bounds = card.getBoundingClientRect();
      const mouseX = e.clientX - bounds.left;
      const mouseY = e.clientY - bounds.top;

      const px = Math.max(-1, Math.min(1, ((mouseX / bounds.width) - 0.5) * 2));
      const py = Math.max(-1, Math.min(1, ((mouseY / bounds.height) - 0.5) * 2));

      targetX = -(py * MAX_TILT);
      targetY = px * MAX_TILT;
      targetMouseX = (mouseX / bounds.width) * 100;
      targetMouseY = (mouseY / bounds.height) * 100;
    }

    card.addEventListener('pointerenter', e => {
      isHovered = true;
      targetScale = 1.018;
      setTargetFromPointer(e);
      startLoop();
    });

    card.addEventListener('pointermove', e => {
      if(!isHovered) isHovered = true;
      setTargetFromPointer(e);
      startLoop();
    });

    card.addEventListener('pointerleave', () => {
      isHovered = false;
      targetX = 0;
      targetY = 0;
      targetScale = 1;
      targetMouseX = 50;
      targetMouseY = 50;
      startLoop();
    });
  });
})();
