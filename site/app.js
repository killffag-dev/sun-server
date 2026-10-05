const $=(s,r=document)=>r.querySelector(s),$$=(s,r=document)=>[...r.querySelectorAll(s)];
const KEY='sun_key';

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

// ---------- Модальное окно SUN ID (кнопка входа сейчас скрыта) ----------
const auth=$('#auth'),alertBox=$('#alert');
const note=(m,ok=false)=>{
  alertBox.textContent=m;
  alertBox.classList.toggle('ok',ok&&!!m);
  alertBox.hidden=!m;
};
$$('[data-open-auth]').forEach(b=>b.onclick=()=>{
  note('');
  auth.showModal();
  loadTurnstile();
});
$('[data-close]').onclick=()=>auth.close();
auth.addEventListener('click',e=>{if(e.target===auth)auth.close()});

// Cloudflare Turnstile грузится только при первом открытии окна.
// Тестовый Always-Pass ключ: перед запуском входа замени на боевой.
const CF_SITEKEY='1x00000000000000000000AA';
let turnstileLogId=null,turnstileRegId=null,turnstileLoading=false;

function loadTurnstile(){
  if(window.turnstile){initTurnstile();return}
  if(turnstileLoading)return;
  turnstileLoading=true;
  const s=document.createElement('script');
  s.src='https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit';
  s.async=true;
  s.onload=initTurnstile;
  document.head.appendChild(s);
}
function initTurnstile(){
  if(typeof turnstile==='undefined')return;
  try{
    if(turnstileLogId===null&&$('#turnstile-log'))turnstileLogId=turnstile.render('#turnstile-log',{sitekey:CF_SITEKEY,theme:'dark'});
    if(turnstileRegId===null&&$('#turnstile-reg'))turnstileRegId=turnstile.render('#turnstile-reg',{sitekey:CF_SITEKEY,theme:'dark'});
  }catch(e){console.log('[Turnstile] Init status:',e)}
}

let isRegMode=false;
function setAuthMode(reg){
  isRegMode=reg;
  $('#f-log').hidden=reg;
  $('#f-reg').hidden=!reg;
  $('#auth-title').textContent=reg?'Создать аккаунт':'Вход в аккаунт';
  $('#auth-desc').textContent=reg?'Один профиль SUN ID для доступа к клиенту и обновлениям.':'Управляй доступом и настройками клиента из единого профиля.';
  $('#auth-switch-text').textContent=reg?'Уже есть аккаунт?':'Нет аккаунта?';
  $('#btn-switch-auth').textContent=reg?'Войти':'Создать';
  note('');
  setTimeout(initTurnstile,50);
}
$('#btn-switch-auth').onclick=()=>setAuthMode(!isRegMode);

// API. Ожидаемый ответ: {success, key, username, expires, active, error}
async function api(path,body){
  const r=await fetch('/api/'+path,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
  const d=await r.json().catch(()=>null);
  if(!r.ok||!d||!d.success)throw new Error(d&&d.error||'Ошибка запроса');
  return d;
}
let currentUser = null;

function show(u){
  currentUser = u;
  $('#p-name').textContent = u.username;
  $('#p-key').textContent = u.key;
  if ($('#p-coins')) $('#p-coins').textContent = u.coins || 0;
  if ($('#p-status-badge')) $('#p-status-badge').textContent = u.active ? 'Активен' : 'Истекла';
  if ($('#p-exp')) $('#p-exp').textContent = u.expires;

  // HWID отображение
  const hwidBadge = $('#p-hwid-badge');
  const hwidText = $('#p-hwid-text');
  const hwidHint = $('#hwid-hint-text');
  if (hwidBadge && hwidText) {
    if (u.hwid) {
      hwidBadge.textContent = '🟢 Привязан к вашему ПК';
      hwidBadge.style.color = 'var(--ok)';
      hwidText.textContent = u.hwid.substring(0, 12) + '...';
      if (hwidHint) hwidHint.textContent = 'Ключ заблокирован за вашим компьютером. Передать ключ другому невозможно.';
    } else {
      hwidBadge.textContent = '🟡 Ожидает первого запуска';
      hwidBadge.style.color = '#ff9800';
      hwidText.textContent = 'Не привязан';
      if (hwidHint) hwidHint.textContent = 'Привязка произойдет автоматически при первом входе в игру через клиент.';
    }
  }

  // Обновление кнопки в шапке сайта
  const headerBtn = $('#btn-header-auth');
  if (headerBtn) {
    headerBtn.innerHTML = `👤 ${u.username} <span style="color:#ff9800;margin-left:5px;font-weight:700;">⚡ ${u.coins || 0}</span>`;
  }

  $('#auth-box').hidden = true;
  $('#profile').hidden = false;
  try{localStorage.setItem(KEY, u.key)}catch{}
}

// Копирование лицензионного ключа
const btnCopyKey = $('#btn-copy-key');
if (btnCopyKey) {
  btnCopyKey.onclick = () => {
    const key = $('#p-key').textContent;
    if (!key) return;
    const onSuccess = () => {
      const orig = btnCopyKey.textContent;
      btnCopyKey.textContent = 'Скопировано!';
      setTimeout(() => btnCopyKey.textContent = orig, 2000);
    };
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(key).then(onSuccess).catch(() => fallbackCopy(key, onSuccess));
    } else {
      fallbackCopy(key, onSuccess);
    }
  };
}

// Сброс привязки HWID
const btnResetHwid = $('#btn-reset-hwid');
if (btnResetHwid) {
  btnResetHwid.onclick = async () => {
    if (!currentUser || !currentUser.key) return;
    if (!confirm('Вы действительно хотите сбросить привязку к этому компьютеру?')) return;
    btnResetHwid.disabled = true;
    btnResetHwid.textContent = 'Сброс...';
    try {
      const res = await api('reset-hwid', { key: currentUser.key });
      currentUser.hwid = null;
      show(currentUser);
      toast(res.message || 'Привязка HWID успешно сброшена!');
    } catch (err) {
      toast(err.message || 'Ошибка сброса HWID');
    } finally {
      btnResetHwid.disabled = false;
      btnResetHwid.textContent = 'Сбросить HWID';
    }
  };
}

async function submit(e,path,body){
  e.preventDefault();
  note('');
  const b=$('button:not([type=button])',e.target);
  b.disabled=true;
  try{
    show(await api(path,body()));
  }catch(err){
    note(err instanceof TypeError?'Сервер недоступен, попробуйте позже.':err.message);
  }finally{
    b.disabled=false;
  }
}

$('#f-log').onsubmit=e=>submit(e,'login',()=>({
  query:$('#log-login').value.trim(),
  password:$('#log-pass').value.trim()
}));
$('#f-reg').onsubmit=e=>submit(e,'register',()=>({
  username:$('#reg-user').value.trim(),
  email:$('#reg-email').value.trim(),
  password:$('#reg-pass').value.trim()
}));

// Генератор паролей (crypto, не Math.random)
function generateSecurePassword(length=14){
  const chars='abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789!@#$%^&*';
  const buf=new Uint32Array(length);
  crypto.getRandomValues(buf);
  return[...buf].map(n=>chars[n%chars.length]).join('');
}
$('#btn-gen-pass').onclick=()=>{
  const passInput=$('#reg-pass');
  passInput.value=generateSecurePassword(14);
  passInput.type='text';
  $('#btn-toggle-eye').textContent='🙈';
  note('Сгенерирован надежный пароль! Сохраните его.',true);
};

// Показать / скрыть пароль
$('#btn-toggle-eye').onclick=()=>{
  const passInput=$('#reg-pass');
  const isText=passInput.type==='text';
  passInput.type=isText?'password':'text';
  $('#btn-toggle-eye').textContent=isText?'👁':'🙈';
};

// Быстрое автодополнение @gmail.com
$('#btn-fill-gmail').onclick=()=>{
  const emailInput=$('#reg-email');
  const val=emailInput.value.trim();
  if(!val){
    emailInput.value='user@gmail.com';
    emailInput.focus();
    emailInput.setSelectionRange(0,4);
    return;
  }
  emailInput.value=(val.includes('@')?val.split('@')[0]:val)+'@gmail.com';
};

// Вход через Google
$('#btn-google-auth').onclick=async()=>{
  note('');
  const googleBtn=$('#btn-google-auth');
  googleBtn.disabled=true;
  $('#google-btn-text').textContent='Подключение к Google...';
  try{
    show(await api('google-auth',{}));
  }catch(err){
    note('Ошибка авторизации через Google: '+err.message);
  }finally{
    googleBtn.disabled=false;
    $('#google-btn-text').textContent='Продолжить с Google';
  }
};

// Выход из аккаунта
$('#logout').onclick=()=>{
  try{localStorage.removeItem(KEY)}catch{}
  currentUser = null;
  const headerBtn = $('#btn-header-auth');
  if (headerBtn) headerBtn.textContent = 'Войти / SUN ID';
  $('#profile').hidden=true;
  $('#auth-box').hidden=false;
  note('');
};

// Сохранённая сессия проверяется на сервере
try{
  const k=localStorage.getItem(KEY);
  if(k)api('login',{key:k}).then(show).catch(()=>localStorage.removeItem(KEY));
}catch{}

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
