// دو زبانه، محصولاتِ پیشنهادی و «محاسبه بر اساسِ تجهیزات» — قراردادِ داده‌لینک نسخهٔ ۲.
//
// · قاعدهٔ انتخابِ کالا فقط در سرور است (rf/suggest)؛ این‌جا فقط نشان داده می‌شود.
// · محاسبه همیشه با همان RF.calculate و همان کادرِ نتیجه. زبانهٔ دوم فقط ورودی‌ها را از
//   دادهٔ فنی پر می‌کند: tx از رادیوی A، حساسیت از رادیوی B، بهره از آنتن یا رادیوی داخلی.
// · بخشِ نقشه، فاصله، افتِ کابل و آب‌وهوا بینِ دو زبانه مشترک است.
// · 🔴 همهٔ مقدارها از دیتابیس و فایلِ بیرونی‌اند: فقط textContent، هرگز innerHTML با داده.
(()=>{
const API='/api/app/v1/rf',TAB_KEY='dadehlink.tab',EQ_KEY='dadehlink.equipment';
const sugg=$('suggested-products'),equip=$('dlEquip');
if(!sugg||!equip)return;
let tab='calc',saved1=null,catalog=null,timer=null,lastQuery='',seq=0;
const el=(tag,cls,text)=>{const e=document.createElement(tag);if(cls)e.className=cls;if(text!==undefined&&text!==null)e.textContent=text;return e};
const faNum=n=>Number(n).toLocaleString('fa-IR');
const ghz=m=>(m/1000).toLocaleString('en-US',{maximumFractionDigits:3});
const nn=v=>v!==null&&v!==undefined&&Number.isFinite(Number(v));

// ---------------------------------------------------------------- کارت
function bandText(rf){return (rf.bands||[]).map(b=>ghz(b.minMhz)+'–'+ghz(b.maxMhz)+' GHz').join('، ')||'باند: نامشخص'}
function card(c){
 const a=el('a','dlCard');a.href=c.url;   // همان تب، طبقِ خواستهٔ تسک
 const box=el('div','dlCardImg');if(c.image){const img=el('img');img.src=c.image;img.alt='';img.loading='lazy';box.append(img)}a.append(box);
 a.append(el('b','dlCardName',c.name));
 const rf=c.rf||{},facts=el('div','dlCardFacts');
 if(rf.kind==='ANTENNA'){facts.append(el('span',null,nn(rf.antenna?.gainDbi)?rf.antenna.gainDbi+' dBi':'بهره: نامشخص'),el('span',null,bandText(rf)))}
 else{const r=rf.radio||{};facts.append(el('span',null,nn(r.txMaxDbm)?'توان '+r.txMaxDbm+' dBm':'توان: نامشخص'),el('span',null,nn(r.sensLowDbm)?'حساسیت '+r.sensLowDbm+' dBm':'حساسیت: نامشخص'));
  if(rf.kind==='RADIO_INTEGRATED'&&nn(rf.antenna?.gainDbi))facts.append(el('span',null,'آنتنِ داخلی '+rf.antenna.gainDbi+' dBi'))}
 a.append(facts);
 const foot=el('div','dlCardFoot');
 foot.append(el('span','dlPrice',c.priceToman?faNum(c.priceToman)+' تومان':'استعلامِ قیمت'),el('span',c.inStock?'dlStock in':'dlStock out',c.inStock?'موجود':'ناموجود'));
 a.append(foot);return a;
}
function group(title,cards,emptyText){
 const g=el('div','dlGroup');g.append(el('h3',null,title));
 if(!cards.length){const p=el('p','dlEmpty',emptyText+' ');const l=el('a',null,'استعلامِ قیمت از فروشگاه');l.href=APP_CONFIG.pricesUrl;p.append(l);g.append(p);return g}
 const row=el('div','dlCards');cards.forEach(c=>row.append(card(c)));g.append(row);return g;
}

// ---------------------------------------------------------------- زبانهٔ اول: پیشنهاد
function querySuggest(){
 if(tab!=='calc'||!window.dadehlinkAuthed?.()||!result){return}
 const q={fMhz:Math.round(state.f*1000),gainA:result.ga,gainB:result.gb,marginDb:Math.max(-400,Math.min(400,Number(result.margin.toFixed(2)))),txDbm:state.tx,sensDbm:state.sensitivity,weather:state.weatherEnabled?1:0};
 const qs=new URLSearchParams(q).toString();if(qs===lastQuery)return;
 clearTimeout(timer);timer=setTimeout(async()=>{
  const my=++seq;let body;
  try{const r=await fetch(API+'/suggest?'+qs);if(!r.ok)return;body=await r.json()}catch(e){return}
  if(my!==seq||tab!=='calc')return;lastQuery=qs;renderSuggest(body,q);
 },600);
}
function renderSuggest(s,q){
 sugg.replaceChildren();sugg.hidden=false;
 sugg.append(el('h2',null,'تجهیزاتِ پیشنهادی برای این لینک'));
 if(s.notice){sugg.append(el('p','dlNotice',s.notice));return}
 const need=[];
 if(s.minGainA>q.gainA+0.05)need.push(s.minGainA===s.minGainB?'آنتنِ دست‌کم '+s.minGainA+' dBi':'آنتنِ دست‌کم '+s.minGainA+' dBi در سرِ A');
 if(s.minGainB>q.gainB+0.05&&s.minGainA!==s.minGainB)need.push('دست‌کم '+s.minGainB+' dBi در سرِ B');
 if(need.length)sugg.append(el('p','dlWhy','برای رسیدن به حاشیهٔ '+faNum(s.targetMarginDb)+' دسی‌بل، '+need.join(' و ')+' لازم است.'));
 const none='در فروشگاه آنتنی که این شرط را برآورد کند نیست.';
 if(s.minGainA===s.minGainB)sugg.append(group('آنتن',s.antennasA,none));
 else sugg.append(group('آنتنِ سرِ A',s.antennasA,none),group('آنتنِ سرِ B',s.antennasB,none));
 sugg.append(group('رادیو',s.radios,'در فروشگاه رادیویی که این توان و حساسیت را بدهد نیست.'));
}
const baseUpdate=update;
update=function(){baseUpdate();if(tab==='calc')querySuggest();else recomputeEquip()};
document.addEventListener('dadehlink:auth',()=>{lastQuery='';querySuggest()});

// ---------------------------------------------------------------- زبانه‌ها
function setTab(t,restoring){
 if(t===tab&&!restoring)return;
 const toEquip=t==='equip';
 $('tabCalc').classList.toggle('on',!toEquip);$('tabEquip').classList.toggle('on',toEquip);
 $('tabCalc').setAttribute('aria-selected',String(!toEquip));$('tabEquip').setAttribute('aria-selected',String(toEquip));
 document.querySelector('.dl .app').classList.toggle('eqMode',toEquip);
 equip.hidden=!toEquip;
 if(toEquip){
  saved1={...state};
  // دادهٔ واقعی بیرونِ بازهٔ اسلایدرهای زبانهٔ اول است (مثلاً حساسیتِ ‎-96)؛ موتور اهمیتی نمی‌دهد.
  widen(true);storeState=()=>{};   // حالتِ زبانهٔ اول در حافظهٔ مرورگر دست نخورد
  tab='equip';loadEquip();
 }else{
  tab='calc';widen(false);storeState=baseStore;
  if(saved1){state={...state,...pick(saved1,['f','tx','sensitivity','gainA','gainB','modeA','modeB','diaA','diaB'])}}
  setControls();lastQuery='';querySuggest();
 }
 try{localStorage.setItem(TAB_KEY,t)}catch(e){}
}
const baseStore=storeState;
const NARROW={tx:[...ranges.tx],sensitivity:[...ranges.sensitivity]};
function widen(on){ranges.tx=on?[-30,50]:NARROW.tx;ranges.sensitivity=on?[-130,-20]:NARROW.sensitivity;for(const k of ['tx','sensitivity']){$(k).min=ranges[k][0];$(k).max=ranges[k][1]}}
function pick(o,keys){const r={};for(const k of keys)r[k]=o[k];return r}
$('tabCalc').onclick=()=>setTab('calc');$('tabEquip').onclick=()=>setTab('equip');

// ---------------------------------------------------------------- زبانهٔ دوم
let sel={a:'',b:'',antA:'',antB:'',f:null,rate:'low'};
try{const v=JSON.parse(localStorage.getItem(EQ_KEY));if(v&&typeof v==='object')sel={...sel,...v}}catch(e){}
const saveSel=()=>{try{localStorage.setItem(EQ_KEY,JSON.stringify(sel))}catch(e){}};
async function loadEquip(){
 if(catalog){renderEquip();return}
 equip.replaceChildren(el('p','dlEmpty','فهرستِ تجهیزات در حالِ بارگذاری…'));
 try{const r=await fetch(API+'/equipment');catalog=await r.json()}catch(e){equip.replaceChildren(el('p','dlEmpty','فهرستِ تجهیزات بارگذاری نشد؛ اتصال را بررسی کنید.'));return}
 renderEquip();
}
const byId=id=>catalog&&[...catalog.radios,...catalog.antennas].find(c=>c.id===id);
const fams=c=>(c?.rf?.radio?.compatFamily||'').split(';').filter(Boolean);
function overlap(A,B){const out=[];for(const a of A)for(const b of B){const lo=Math.max(a.minMhz,b.minMhz),hi=Math.min(a.maxMhz,b.maxMhz);if(lo<=hi)out.push([lo,hi])}return out.sort((x,y)=>x[0]-y[0])}
function picker(id,label,items,value,onChange){
 const wrap=el('div','eqPick');const lab=el('label',null,label);lab.htmlFor=id;
 const search=el('input','eqSearch');search.type='search';search.placeholder='جست‌وجو…';search.setAttribute('aria-label',label+' — جست‌وجو');
 const s=el('select');s.id=id;
 const fill=f=>{s.replaceChildren();items.filter(c=>!f||c.name.toLowerCase().includes(f.toLowerCase())).forEach(c=>{const o=el('option',null,c.name+(c.inStock?'':' — ناموجود'));o.value=c.id;s.append(o)});s.value=value;if(s.value!==value&&s.options.length)s.selectedIndex=0};
 fill('');search.oninput=()=>{fill(search.value.trim());if(s.value!==value)onChange(s.value)};
 s.onchange=()=>onChange(s.value);
 wrap.append(lab,search,s);return wrap;
}
function renderEquip(){
 equip.replaceChildren();
 const radios=catalog.radios.filter(c=>c.rf?.radio&&(c.rf.bands||[]).length);
 if(!radios.length){equip.append(el('p','dlEmpty','هنوز رادیویی با دادهٔ فنی در فروشگاه نیست.'));return}
 if(!byId(sel.a))sel.a=radios[0].id;
 const A=byId(sel.a);
 const sameFam=radios.filter(c=>fams(c).some(f=>fams(A).includes(f)));
 if(!sameFam.some(c=>c.id===sel.b))sel.b=A.id;   // پیش‌فرض: همان مدلِ A
 const B=byId(sel.b);
 equip.append(el('h2',null,'تجهیزاتِ دو سرِ لینک'));
 const grid=el('div','eqGrid');
 grid.append(picker('eqRadioA','رادیوی A',radios,sel.a,v=>{sel.a=v;sel.antA='';saveSel();renderEquip()}));
 grid.append(picker('eqRadioB','رادیوی B — فقط همان خانواده',sameFam,sel.b,v=>{sel.b=v;sel.antB='';saveSel();renderEquip()}));
 const common=overlap(A.rf.bands,B.rf.bands);
 if(!common.length){grid.append(el('p','dlEmpty','این دو رادیو باندِ مشترکی ندارند.'));equip.append(grid);showSelected([A,B]);return}
 const lo=common[0][0],hi=common[common.length-1][1];
 let f=sel.f&&common.some(([x,y])=>sel.f>=x&&sel.f<=y)?sel.f:(nn(state.f*1000)&&common.some(([x,y])=>state.f*1000>=x&&state.f*1000<=y)?Math.round(state.f*1000):Math.round((common[0][0]+common[0][1])/2));
 sel.f=f;
 const fw=el('div','eqPick');const fl=el('label',null,'فرکانس (مگاهرتز) — بازهٔ مشترک: '+common.map(([x,y])=>x+'–'+y).join('، '));fl.htmlFor='eqFreq';
 const fi=el('input');fi.id='eqFreq';fi.type='number';fi.min=lo;fi.max=hi;fi.step=1;fi.value=f;fi.dir='ltr';
 fi.onchange=()=>{const v=Number(normalNumber(fi.value));if(common.some(([x,y])=>v>=x&&v<=y)){sel.f=v;saveSel();renderEquip()}else{fi.value=sel.f;toast('فرکانس باید در بازهٔ مشترکِ دو رادیو باشد')}};
 fw.append(fl,fi);grid.append(fw);
 const antPick=(side,R,key)=>{
  if(R.rf.kind!=='RADIO')return null;
  // پربهره‌ترین اول: پیش‌فرضِ الفبایی یک همه‌جهتهٔ ۶٫۷ dBi را برای لینکِ نقطه‌به‌نقطه می‌گذاشت
  const list=catalog.antennas.filter(c=>(c.rf.bands||[]).some(b=>f>=b.minMhz&&f<=b.maxMhz)).sort((x,y)=>(y.rf.antenna?.gainDbi??-1)-(x.rf.antenna?.gainDbi??-1));
  if(!list.length){const p=el('p','dlEmpty','آنتنی که '+f+' مگاهرتز را بپوشاند در فروشگاه نیست.');grid.append(p);return null}
  if(!list.some(c=>c.id===sel[key]))sel[key]=list[0].id;
  grid.append(picker('eqAnt'+side,'آنتنِ سرِ '+side+' (رادیو کانکتوردار است)',list,sel[key],v=>{sel[key]=v;saveSel();renderEquip()}));
  return byId(sel[key]);
 };
 const antA=antPick('A',A,'antA'),antB=antPick('B',B,'antB');
 // نرخ
 const rates=(A.rf.rates||[]).filter(x=>x.minMhz==null||(f>=x.minMhz&&f<=x.maxMhz));
 const rw=el('div','eqPick');const rl=el('label',null,'نرخ');rl.htmlFor='eqRate';const rs=el('select');rs.id='eqRate';
 const opt=(v,t)=>{const o=el('option',null,t);o.value=v;rs.append(o)};
 opt('low','کمترین نرخ'+(A.rf.radio.sensLowCond?' — '+A.rf.radio.sensLowCond:''));opt('high','بیشترین نرخ'+(A.rf.radio.sensHighCond?' — '+A.rf.radio.sensHighCond:''));
 rates.forEach((x,i)=>opt('r'+i,[x.rateLabel,x.channelMhz?x.channelMhz+' MHz':null,x.rateMbps?x.rateMbps+' Mbps':null].filter(Boolean).join(' · ')));
 if(![...rs.options].some(o=>o.value===sel.rate))sel.rate='low';rs.value=sel.rate;rs.onchange=()=>{sel.rate=rs.value;saveSel();renderEquip()};
 rw.append(rl,rs);grid.append(rw);equip.append(grid);
 // ورودیِ موتور
 const rate=sel.rate.startsWith('r')?rates[+sel.rate.slice(1)]:null;
 let tx=rate&&nn(rate.txDbm)?rate.txDbm:A.rf.radio.txMaxDbm;
 let sens;
 if(rate){const m=(B.rf.rates||[]).find(x=>x.rateLabel===rate.rateLabel&&x.channelMhz===rate.channelMhz&&(x.minMhz==null||(f>=x.minMhz&&f<=x.maxMhz)));sens=m?m.sensDbm:(B.id===A.id?rate.sensDbm:null)}
 else sens=sel.rate==='high'?B.rf.radio.sensHighDbm:B.rf.radio.sensLowDbm;
 const gA=antA?antA.rf.antenna?.gainDbi:A.rf.antenna?.gainDbi,gB=antB?antB.rf.antenna?.gainDbi:B.rf.antenna?.gainDbi;
 const src=c=>c?.rf?.source?.url;
 const rows=[['توانِ ارسالِ A',tx,'dBm',A],['حساسیتِ گیرندهٔ B',sens,'dBm',B],['بهرهٔ آنتنِ A',gA,'dBi',antA||A],['بهرهٔ آنتنِ B',gB,'dBi',antB||B]];
 const facts=el('ul','eqFacts');
 rows.forEach(([k,v,u,c])=>{const li=el('li');li.append(el('span',null,k+': '),el('b',nn(v)?null:'unk',nn(v)?v+' '+u:'نامشخص'));
  if(src(c)){const l=el('a',null,'منبع ↗');l.href=src(c);l.target='_blank';l.rel='noopener';li.append(' ',l)}facts.append(li)});
 equip.append(facts);
 const missing=rows.filter(r=>!nn(r[1])).map(r=>r[0]);
 if(missing.length){equip.append(el('p','dlNotice','داده برای محاسبه کامل نیست: '+missing.join('، ')+'.'));showSelected([A,B,antA,antB]);return}
 equipInputs={f:f/1000,tx:Number(tx),sensitivity:Number(sens),gainA:Number(gA),gainB:Number(gB)};
 applyEquip();showSelected([A,B,antA,antB]);
}
let equipInputs=null,applying=false;
function applyEquip(){if(!equipInputs)return;applying=true;Object.assign(state,equipInputs,{modeA:'manual',modeB:'manual'});setControls();applying=false}
function recomputeEquip(){/* فاصله، افتِ کابل و آب‌وهوا از خودِ ماشین‌حساب می‌آیند؛ ورودیِ تجهیزات ثابت می‌ماند */if(!applying&&equipInputs&&tab==='equip'){for(const k of ['f','tx','sensitivity','gainA','gainB'])if(state[k]!==equipInputs[k]){applyEquip();return}}}
function showSelected(list){
 sugg.replaceChildren();sugg.hidden=false;
 const seen=new Set(),cards=list.filter(c=>c&&!seen.has(c.id)&&seen.add(c.id));
 sugg.append(group('تجهیزاتِ انتخاب‌شده',cards,''));
}

// ---------------------------------------------------------------- شروع
let startTab='calc';try{startTab=localStorage.getItem(TAB_KEY)==='equip'?'equip':'calc'}catch(e){}
if(startTab==='equip')setTab('equip',true);else querySuggest();
})();
