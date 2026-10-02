// دروازهٔ ورودِ ابزار — قرارداد: docs/dadehlink-api-contract.md، بخشِ «رفتارِ کلاینت».
//
// · صفحه بی‌دروازه بار می‌شود؛ گوگل همه‌چیز را می‌بیند و مودال هرگز خودکار باز نمی‌شود.
// · دروازه فقط روی محدودهٔ ابزار (#dlApp) است؛ منو، پاورقی و متنِ زیرِ ابزار آزادند.
// · اولین تعامل (کلیک، لمس، حرکتِ اسلایدر، ورود به خانه) مودال را باز می‌کند و همان
//   تعامل اجرا نمی‌شود.
// · پس از ثبت، توکن در localStorage با کلیدِ dadehlink.auth می‌ماند و دیگر چیزی پرسیده
//   نمی‌شود. فقط پاسخِ ۴۰۱ِ me ثبت را باطل می‌کند؛ خطای شبکه نه.
// · کاربرِ ازقبل‌واردشدهٔ سایت با auth/session-token بی‌پیامک توکن می‌گیرد و مودال نمی‌بیند.
//   این پرسش در اولین تعامل است، نه هنگامِ بارِ صفحه: وگرنه هر بازدیدکنندهٔ ناشناس (و هر
//   خزنده) یک ۴۰۱ِ قرمز در کنسول می‌گرفت. کلیکِ کاربرِ واردشده پس از گرفتنِ توکن تکرار می‌شود.
(()=>{
const API='/api/app/v1',AUTH_KEY='dadehlink.auth',INST_KEY='dadehlink.installationId';
const OFFLINE='برای اولین ثبت، اینترنت لازم است.';
const root=$('dlApp');if(!root)return;
let memoryAuth=null,armed=false,suppressClickUntil=0,sessionTried=false,opening=false,draft={name:'',phone:'',company:'',phoneNorm:''},resendTimer=null,verifying=false;

function esc(s){return String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))}
function fa(n){return Number(n).toLocaleString('fa-IR')}
function readAuth(){try{const v=JSON.parse(localStorage.getItem(AUTH_KEY));if(v&&typeof v.token==='string'&&v.token.length>20)return v}catch(e){}return memoryAuth}
function writeAuth(v){memoryAuth=v;try{localStorage.setItem(AUTH_KEY,JSON.stringify(v))}catch(e){}}
function clearAuth(){memoryAuth=null;try{localStorage.removeItem(AUTH_KEY)}catch(e){}}
function installationId(){let id='';try{id=localStorage.getItem(INST_KEY)||''}catch(e){}if(!/^[0-9a-f]{32}$/.test(id)){id=Array.from(crypto.getRandomValues(new Uint8Array(16)),n=>n.toString(16).padStart(2,'0')).join('');try{localStorage.setItem(INST_KEY,id)}catch(e){}}return id}
function normalPhone(v){let p=normalNumber(v||'').replace(/[\s\-()]/g,'');if(p.startsWith('+98'))p='0'+p.slice(3);else if(p.startsWith('0098'))p='0'+p.slice(4);return /^09\d{9}$/.test(p)?p:null}
async function post(path,body,headers){const r=await fetch(API+path,{method:'POST',headers:{'Content-Type':'application/json',...headers},body:JSON.stringify(body)});let b={};try{b=await r.json()}catch(e){}return {status:r.status,body:b}}

// ---------------------------------------------------------------- دروازه
const TYPES=['pointerdown','mousedown','touchstart','click','focusin','input','change','keydown'];
function guard(e){
 if(!armed)return;
 const t=e.target;if(!(t instanceof Element))return;
 // پیوندهای ناوبری (قیمت تجهیزات، لوگو) ابزار را ترک می‌کنند؛ بستنِ راهِ فروشگاه هدفِ دروازه نیست.
 if(t.closest('a[href]'))return;
 const onMap=!!t.closest('.leaflet-container');
 switch(e.type){
  case 'pointerdown':
   // لمسی که روی نقشه نیست شاید شروعِ اسکرولِ صفحه است؛ منتظرِ click یا input می‌مانیم.
   if(e.pointerType==='touch'&&!onMap)return;
   e.preventDefault();e.stopPropagation();
   if(e.pointerType==='touch')openGate(true);   // کشیدنِ نقشه با انگشت click نمی‌سازد
   return;
  case 'mousedown':e.preventDefault();e.stopPropagation();return;   // نه فوکوس، نه کشیدنِ اسلایدر
  case 'touchstart':if(onMap)e.stopPropagation();return;
  case 'click':e.preventDefault();e.stopPropagation();openGate(false,()=>t.click());return;
  case 'focusin':e.stopPropagation();t.blur?.();openGate(true,()=>t.focus());return;   // ورود با Tab
  case 'input':case 'change':e.stopPropagation();setControls();openGate(true);return;   // اسلایدرِ لمسی: برگرداندن
  case 'keydown':if(e.key==='Tab'||e.key==='Shift')return;e.preventDefault();e.stopPropagation();openGate(true);return;
 }
}
function arm(){if(armed)return;armed=true;for(const t of TYPES)root.addEventListener(t,guard,{capture:true,passive:false})}
function disarm(){armed=false;for(const t of TYPES)root.removeEventListener(t,guard,{capture:true})}
// «کلیکِ شبح»: وقتی مودال پیش از click باز شود، همان click روی پس‌زمینهٔ مودال می‌افتد و می‌بندَدَش.
// فقط تا وقتی مودال باز است؛ اگر کاربر خودش بست، کلیکِ بعدی‌اش کلیکِ واقعی است.
document.addEventListener('click',e=>{if(Date.now()<suppressClickUntil&&$('modal').classList.contains('open')){e.preventDefault();e.stopPropagation()}},true);
// replay: همان کنشِ کاربر، وقتی معلوم شد از قبل در سایت وارد است و مودال لازم نیست.
async function openGate(ghost,replay){
 if(ghost)suppressClickUntil=Date.now()+700;
 if(opening||$('modal').classList.contains('open'))return;
 if(!sessionTried){
  sessionTried=true;opening=true;
  try{const r=await post('/auth/session-token',{installationId:installationId()});if(r.status===200&&r.body.token){opening=false;complete(r.body,true);replay?.();return}}catch(e){}
  opening=false;
 }
 step1();
}

// ---------------------------------------------------------------- مرحلهٔ ۱
function step1(message){
 clearInterval(resendTimer);
 show(`<h2 id="dialogTitle">ورود به ابزار</h2><p class="gateLead">یک بار ثبت کنید؛ دفعه‌های بعد چیزی پرسیده نمی‌شود.</p><form id="gateForm" class="gateForm" novalidate><label for="gateName">نام و نام خانوادگی</label><input id="gateName" type="text" maxlength="100" autocomplete="name"><label for="gatePhone">شمارهٔ موبایل</label><input id="gatePhone" type="tel" inputmode="tel" maxlength="16" autocomplete="tel" dir="ltr" placeholder="09xxxxxxxxx"><label for="gateCompany">نام شرکت <span>(اختیاری)</span></label><input id="gateCompany" type="text" maxlength="150" autocomplete="organization"><p class="gateConsent">${esc(APP_CONFIG.consentText)}</p><div class="gateError" id="gateError" role="alert"></div><button class="action" id="gateSend" type="submit">ارسال کد تأیید</button><button class="action cancel" type="button" onclick="closeDialog()">انصراف</button></form>`);
 $('sheet').classList.add('gateSheet');
 $('gateName').value=draft.name;$('gatePhone').value=draft.phone;$('gateCompany').value=draft.company;
 if(message)showError(message);
 $('gateForm').onsubmit=e=>{e.preventDefault();sendCode()};
}
async function sendCode(){
 draft={...draft,name:$('gateName').value.trim(),phone:$('gatePhone').value.trim(),company:$('gateCompany').value.trim()};
 if(!draft.name)return showError('نام و نام خانوادگی را وارد کنید.',$('gateName'));
 const phone=normalPhone(draft.phone);
 if(!phone)return showError('شمارهٔ موبایل معتبر نیست؛ مثلاً ۰۹۱۲۳۴۵۶۷۸۹.',$('gatePhone'));
 if(!navigator.onLine)return showError(OFFLINE);
 const btn=$('gateSend');btn.disabled=true;
 let r;try{r=await requestCode(phone)}catch(e){btn.disabled=false;return showError(OFFLINE)}
 btn.disabled=false;
 if(r.status===200){draft.phoneNorm=phone;return step2(r.body.resendAfterSec||60)}
 showError(errorText(r));
}
function requestCode(phone){return post('/auth/request-code',{name:draft.name,phone,company:draft.company,installationId:installationId(),client:'web',appVersion:APP_CONFIG.appVersion,consentVersion:'1'})}

// ---------------------------------------------------------------- مرحلهٔ ۲
function step2(resendAfter){
 show(`<h2 id="dialogTitle">کد تأیید</h2><p class="gateLead">کد پنج‌رقمی به <b dir="ltr">${esc(draft.phoneNorm)}</b> فرستاده شد.</p><form id="gateCodeForm" class="gateForm" novalidate><label for="gateCode">کد تأیید</label><input id="gateCode" class="gateCode" type="text" inputmode="numeric" autocomplete="one-time-code" maxlength="5" dir="ltr"><div class="gateError" id="gateError" role="alert"></div><button class="action" id="gateVerify" type="submit">تأیید و ورود</button><div class="gateLinks"><button type="button" id="gateResend" class="gateLink" disabled></button><button type="button" id="gateEdit" class="gateLink">ویرایش شماره</button></div></form>`);
 $('sheet').classList.add('gateSheet');
 countdown(resendAfter);
 $('gateCode').oninput=()=>{if(normalNumber($('gateCode').value).replace(/\D/g,'').length===5)verify()};
 $('gateCodeForm').onsubmit=e=>{e.preventDefault();verify()};
 $('gateResend').onclick=resend;
 $('gateEdit').onclick=()=>step1();
}
function countdown(sec){
 clearInterval(resendTimer);let left=Math.max(0,Math.round(sec));
 const tick=()=>{const b=$('gateResend');if(!b){clearInterval(resendTimer);return}b.disabled=left>0;b.textContent=left>0?'ارسال دوباره ('+fa(left)+')':'ارسال دوباره';if(left--<=0)clearInterval(resendTimer)};
 tick();resendTimer=setInterval(tick,1000);
}
async function resend(){
 if(!navigator.onLine)return showError(OFFLINE);
 $('gateResend').disabled=true;
 let r;try{r=await requestCode(draft.phoneNorm)}catch(e){$('gateResend').disabled=false;return showError(OFFLINE)}
 if(r.status===200){countdown(r.body.resendAfterSec||60);$('gateCode').value='';return showError('کدِ تازه فرستاده شد.',$('gateCode'),true)}
 $('gateResend').disabled=false;showError(errorText(r));
}
async function verify(){
 if(verifying)return;
 const code=normalNumber($('gateCode').value).replace(/\s/g,'');
 if(!/^\d{5}$/.test(code))return showError('کد پنج‌رقمی را کامل وارد کنید.',$('gateCode'));
 if(!navigator.onLine)return showError(OFFLINE);
 verifying=true;$('gateVerify').disabled=true;
 let r;try{r=await post('/auth/verify',{phone:draft.phoneNorm,code,installationId:installationId()})}catch(e){r=null}
 verifying=false;if($('gateVerify'))$('gateVerify').disabled=false;
 if(!r)return showError(OFFLINE);
 if(r.status===200&&r.body.token)return complete(r.body,false);
 showError(errorText(r));$('gateCode')?.select();
}

// ---------------------------------------------------------------- پایان
function complete(body,silent){
 writeAuth({token:body.token,name:body.user?.name||'',phone:body.user?.phone||'',at:Date.now()});
 clearInterval(resendTimer);disarm();
 if($('modal').classList.contains('open')&&$('sheet').classList.contains('gateSheet'))closeDialog();
 if(!silent)toast(body.user?.name?'خوش آمدید، '+body.user.name:'ثبت شد؛ از ابزار استفاده کنید');
}
function showError(text,focus,ok){const el=$('gateError');if(!el)return;el.textContent=text;el.classList.add('visible');el.classList.toggle('ok',!!ok);focus?.focus()}
function errorText(r){
 const b=r.body||{};
 if(r.status===400&&b.error==='invalid'){return {name:'نام و نام خانوادگی را وارد کنید (حداکثر ۱۰۰ نویسه).',phone:'شمارهٔ موبایل معتبر نیست؛ مثلاً ۰۹۱۲۳۴۵۶۷۸۹.',company:'نام شرکت حداکثر ۱۵۰ نویسه است.',code:'کد پنج‌رقمی را کامل وارد کنید.'}[b.field]||'ثبت انجام نشد؛ صفحه را دوباره باز کنید.'}
 if(r.status===400&&b.error==='wrong-code')return 'کد درست نیست؛ '+fa(b.attemptsLeft)+' تلاشِ دیگر باقی است.';
 if(r.status===410)return 'کد منقضی شده یا تلاش‌ها تمام شده؛ کدِ تازه بگیرید.';
 if(r.status===429){const s=Number(b.retryAfterSec)||60;return 'درخواست‌ها زیاد بود؛ '+(s<90?fa(s)+' ثانیه':fa(Math.ceil(s/60))+' دقیقه')+' دیگر دوباره امتحان کنید.'}
 if(r.status===503)return 'پیامک الان فرستاده نمی‌شود؛ چند دقیقهٔ دیگر دوباره امتحان کنید.';
 return 'ثبت انجام نشد؛ کمی بعد دوباره امتحان کنید.';
}

// ---------------------------------------------------------------- شروع
const saved=readAuth();
if(saved){
 // فقط ۴۰۱ یعنی ثبت از دست رفته. خطای شبکه و هر پاسخِ دیگر نادیده؛ ابزار آفلاین هم باز است.
 if(navigator.onLine)fetch(API+'/me',{headers:{Authorization:'Bearer '+saved.token}}).then(r=>{if(r.status===401){clearAuth();arm()}}).catch(()=>{});
}else arm();
})();
