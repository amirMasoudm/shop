// تنظیمِ آنتن و پیش‌تنظیمِ شهرهای داده‌لینک ۱.۱.۰-rc3 — web/alignment.js.
// تفاوت‌ها با اپ، همه در «بسته‌بندی» نه در رفتار:
//  · پیکربندی از window.DADEHLINK_CONFIG می‌آید (اپ آن را با build.py جاگذاری می‌کرد).
//  · دادهٔ شهرها از engine/cities.json خوانده می‌شود، همان فایلِ بایت‌به‌بایت.
//  · تنظیمِ آنتن پنجره نیست: بخشِ ثابتِ زیرِ کادرِ ماشین‌حساب، پیش‌فرض نقشهٔ مسیر (خواستهٔ مالک).
//    مینیمال: روی نقشه فقط A، B، خطِ مسیر و فلش. قطب‌نما که در وب از اول کار نمی‌کرد حذف شد.
//  · GeographicLib و Leaflet وقتی این بخش به دید رسید بار می‌شوند (IntersectionObserver).
//  · فرمِ ثبت‌نام و پانویسِ «powered by» برداشته شد (ورود بعداً با پیامکِ خودِ سایت).
// Native location/heading bridge is available only in the Android build.
let CITY_PRESETS=[];
const APP_CONFIG=window.DADEHLINK_CONFIG;
const citiesReady=fetch(APP_CONFIG.citiesUrl).then(r=>{if(!r.ok)throw Error();return r.json()}).then(a=>{CITY_PRESETS=a}).catch(()=>{});
window.registrationPending=false;
function viewportHeight(){document.documentElement.style.setProperty('--visible-height',(window.visualViewport?.height||innerHeight)+'px')}
window.visualViewport?.addEventListener('resize',viewportHeight);window.addEventListener('resize',viewportHeight);viewportHeight();
const originalWeather=weatherDialog;
weatherDialog=function(){originalWeather();if(!CITY_PRESETS.length){const opened=$('weatherCity');citiesReady.then(()=>{if(CITY_PRESETS.length&&$('weatherCity')===opened)fillCities()});return}fillCities()};
function fillCities(){const sel=$('weatherCity');for(let i=0;i<CITY_PRESETS.length;i++){const opt=document.createElement('option');opt.value=i;opt.textContent=CITY_PRESETS[i].name;sel.append(opt)}let savedCity='manual';try{savedCity=localStorage.getItem('dadehlink.weatherCity')||'manual'}catch(e){}if(CITY_PRESETS[+savedCity]&&[['rate','rainRate'],['temp','temp'],['humidity','humidity'],['pressure','pressure']].every(([a,b])=>CITY_PRESETS[+savedCity][a]===state[b]))sel.value=savedCity;const applyCity=$('applyAtmos').onclick;$('applyAtmos').onclick=()=>{if($('applyAtmos').disabled)return;try{localStorage.setItem('dadehlink.weatherCity',sel.value)}catch(e){}applyCity()};sel.onchange=()=>{if(sel.value==='manual')return;const c=CITY_PRESETS[+sel.value];for(const [k,v]of Object.entries({rainRate:c.rate,temp:c.temp,humidity:c.humidity,pressure:c.pressure})){$(k+'Input').value=v;$(k+'Input').dispatchEvent(new Event('input'))}};for(const id of ['rainRateInput','rainRateRange','tempInput','tempRange','humidityInput','humidityRange','pressureInput','pressureRange'])$(id).addEventListener('input',e=>{if(e.isTrusted)sel.value='manual'})}
$('atmosBtn').onclick=weatherDialog;
const Geo={
 route(a,b){const r=geodesic.Geodesic.WGS84.Inverse(a.lat,a.lon,b.lat,b.lon);return {distance:r.s12,bearing:(r.azi1+360)%360}},
 point(lat,lon){return Number.isFinite(lat)&&Number.isFinite(lon)&&Math.abs(lat)<=85&&Math.abs(lon)<=180},
 delta(a,b){return ((a-b+540)%360)-180}
};
window.Geo=Geo;
let align={a:{lat:'',lon:''},b:{lat:'',lon:''}};
try{const v=JSON.parse(localStorage.getItem('dadehlink.alignment'));if(v)for(const k of ['a','b'])if(v[k]&&Geo.point(v[k].lat,v[k].lon))align[k]=v[k]}catch(e){}
let map=null,mapLayer=null,routeLayer=null,pinLayer=null,mapResize=null,pickSite=null,pendingPoint=null,pendingMarker=null,geoReady=false;
function mountAlignment(){
 const host=$('dlAlign');if(!host)return;
 host.innerHTML=`<div class="alHead"><h2>تنظیم آنتن</h2><span class="alignLine" id="alignLine">فاصله <b id="alignDistance" dir="ltr">—</b> · جهت <b id="bearingValue" dir="ltr">—</b></span></div><div class="alignCanvas" id="alignCanvas"><div class="alignWait" id="alignWait">نقشه در حال بارگذاری…</div></div><div class="alignActions"><button id="loca">⌖ موقعیت من (A)</button><button id="pickB">انتخاب B روی نقشه</button></div><button id="applyLinkDistance" class="applyLinkDistance" disabled>اعمال فاصله در صفحه اصلی</button><details class="manualCoords"><summary>مختصات دستی و قبله</summary><div class="coordGrid">${['a','b'].map(k=>`<label>${k.toUpperCase()} lat<input id="${k}Lat" inputmode="decimal" aria-label="عرض ${k}"></label><label>${k.toUpperCase()} lon<input id="${k}Lon" inputmode="decimal" aria-label="طول ${k}"></label>`).join('')}</div><div class="alignMore"><label><input type="checkbox" id="qibla"> جهت قبله</label><button id="editA">انتخاب A روی نقشه</button></div></details><div class="alignStatus" id="alignStatus" role="status"></div>`;
 for(const k of ['a','b'])for(const [id,key]of [['Lat','lat'],['Lon','lon']]){$(k+id).value=align[k][key];$(k+id).oninput=()=>{readAlign();drawAlignment();if(map)fitRoute()}}
 $('loca').onclick=requestLocation;$('pickB').onclick=()=>startPick('b');$('editA').onclick=()=>startPick('a');
 $('qibla').onchange=()=>{drawAlignment();if(map)fitRoute()};$('applyLinkDistance').onclick=applyLinkDistance;
 readAlign();
 // نقشه فقط وقتی بخش به دید رسید؛ مرورگرِ بی‌IntersectionObserver همان لحظه بار می‌کند.
 const start=()=>loadMapLibs().then(()=>{geoReady=true;initMap();drawAlignment()},()=>{$('alignWait').textContent='نقشه بارگذاری نشد؛ اتصال را بررسی کنید.'});
 if('IntersectionObserver' in window){const io=new IntersectionObserver(es=>{if(es.some(e=>e.isIntersecting)){io.disconnect();start()}});io.observe(host)}else start();
}
// «اعمالِ فاصله در صفحهٔ اصلی» — از APKِ داده‌لینک ۱٫۱٫۰ (docs/reports/dadehlink-1.1.0-apply-distance.md).
// خودکار نیست: فاصلهٔ دستیِ کاربر فقط با همین کلیک بازنویسی می‌شود. فاصله همیشه A تا B است، نه تا قبله.
function linkDistanceKm(){if(!geoReady)return null;if(!Geo.point(align.a.lat,align.a.lon)||!Geo.point(align.b.lat,align.b.lon))return null;const km=Geo.route(align.a,align.b).distance/1000;return Number.isFinite(km)&&km>=ranges.distance[0]&&km<=ranges.distance[1]?km:null}
function applyLinkDistance(){const km=linkDistanceKm();if(km===null||$('qibla').checked)return;state.distance=Number(km.toFixed(2));setControls();flashDistance();toast('فاصلهٔ لینک در صفحه اصلی اعمال شد')}
// در سایت پنجره‌ای بسته نمی‌شود؛ پس خانهٔ فاصله یک لحظه برجسته می‌شود. اگر در دید نیست (موبایل)، اول به آن می‌رود.
function flashDistance(){const f=$('distance').closest('.field'),b=f.getBoundingClientRect();if(b.top<60||b.bottom>innerHeight)f.scrollIntoView({behavior:'smooth',block:'center'});f.classList.remove('flash');void f.offsetWidth;f.classList.add('flash');setTimeout(()=>f.classList.remove('flash'),1600)}
function syncCoordinates(){for(const k of ['a','b']){$(k+'Lat').value=align[k].lat;$(k+'Lon').value=align[k].lon}}
function readAlign(){let ok=true;for(const k of ['a','b']){const lat=normalNumber($(k+'Lat').value),lon=normalNumber($(k+'Lon').value);align[k]={lat:/^[-+]?(?:\d+(?:\.\d*)?|\.\d+)$/.test(lat)?Number(lat):'',lon:/^[-+]?(?:\d+(?:\.\d*)?|\.\d+)$/.test(lon)?Number(lon):''};if(!Geo.point(align[k].lat,align[k].lon))ok=false}try{localStorage.setItem('dadehlink.alignment',JSON.stringify(align))}catch(e){}return ok}
function destination(){return $('qibla').checked?{lat:21.422487,lon:39.826206}:align.b}
function selectedRoute(){if(!geoReady)return null;const b=destination();return Geo.point(align.a.lat,align.a.lon)&&Geo.point(b.lat,b.lon)?Geo.route(align.a,b):null}
function drawAlignment(){if(!$('alignCanvas'))return;const q=$('qibla').checked,r=selectedRoute(),usable=r&&r.distance>1&&r.distance<19900000;
 const km=linkDistanceKm();$('applyLinkDistance').disabled=km===null||q;$('applyLinkDistance').textContent=q?'برای اعمال فاصله، تیک قبله را بردارید':km===null?'اعمال فاصله در صفحه اصلی':'اعمال فاصلهٔ '+km.toFixed(2)+' km در صفحه اصلی';$('applyLinkDistance').title=km===null?'مبدأ و مقصد معتبر با فاصلهٔ ۰٫۰۱ تا ۱۱۰ کیلومتر انتخاب کنید.':'';
 $('bearingValue').textContent=usable?r.bearing.toFixed(1)+'°':'—';$('alignDistance').textContent=usable?(r.distance/1000).toFixed(2)+' km':'—';
 drawMapRoute();
}
function initMap(){
 $('alignCanvas').innerHTML='<div id="mapSurface" class="mapSurface" aria-label="نقشهٔ مسیرِ لینک"></div><div class="mapNotice" id="mapNotice" role="status"></div><div class="mapPick" id="mapPick" hidden><span id="pickLabel"></span><button id="confirmPoint" disabled>تأیید محل</button><button id="cancelPoint" class="cancelPick">انصراف</button></div>';
 // چرخِ ماوس صفحه را اسکرول می‌کند نه نقشه را — نقشه وسطِ صفحه است، نه تمام‌صفحه.
 map=L.map('mapSurface',{minZoom:3,maxZoom:19,zoomControl:true,attributionControl:true,scrollWheelZoom:false});map.attributionControl.setPrefix(false);
 const a=align.a;map.setView(Geo.point(a.lat,a.lon)?[a.lat,a.lon]:[32.65,53],Geo.point(a.lat,a.lon)?16:5);
 pinLayer=L.layerGroup().addTo(map);routeLayer=L.layerGroup().addTo(map);setMapLayer();
 $('confirmPoint').onclick=()=>{if(!pendingPoint||!pickSite)return;align[pickSite]=pendingPoint;syncCoordinates();readAlign();cancelPick();drawAlignment();fitRoute()};$('cancelPoint').onclick=cancelPick;
 map.on('click',e=>{if(!pickSite)return;const p={lat:Number(e.latlng.lat.toFixed(7)),lon:Number(e.latlng.wrap().lng.toFixed(7))};if(!Geo.point(p.lat,p.lon))return;pendingPoint=p;if(pendingMarker)map.removeLayer(pendingMarker);pendingMarker=siteMarker(p,'؟',true).addTo(map);$('confirmPoint').disabled=false;$('pickLabel').textContent='محل '+pickSite.toUpperCase()+' را تأیید کنید'});
 map.on('zoomend moveend',drawMapRoute);mapResize=new ResizeObserver(()=>map?.invalidateSize());mapResize.observe($('alignCanvas'));fitRoute();
}
function setMapLayer(){$('mapNotice').textContent='در حال بارگذاری نقشه…';
 mapLayer=L.tileLayer(APP_CONFIG.streetUrl,{maxZoom:19,maxNativeZoom:19,keepBuffer:1,updateWhenIdle:true,attribution:'© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'}).addTo(map);
 let failed=0;mapLayer.on('tileload',()=>{$('mapNotice').textContent=failed?'بخشی از نقشه بارگذاری نشد؛ اتصال را بررسی کنید.':''});mapLayer.on('tileerror',()=>{failed++;$('mapNotice').textContent='نقشه بارگذاری نشد؛ اتصال اینترنت را بررسی کنید.'});
}
function siteMarker(p,label,b){return L.marker([p.lat,p.lon],{icon:L.divIcon({className:'mapPin'+(b?' mapPinB':''),html:label,iconSize:[26,26],iconAnchor:[13,13]}),interactive:false})}
function fitRoute(){if(!map)return;const r=selectedRoute();if(r&&r.distance>1)map.fitBounds([[align.a.lat,align.a.lon],[destination().lat,destination().lon]],{padding:[35,50],maxZoom:17});else if(Geo.point(align.a.lat,align.a.lon))map.setView([align.a.lat,align.a.lon],16)}
function drawMapRoute(){if(!map||!routeLayer)return;routeLayer.clearLayers();pinLayer.clearLayers();const a=align.a,b=destination(),r=selectedRoute();if(Geo.point(a.lat,a.lon))siteMarker(a,'A',false).addTo(pinLayer);if(Geo.point(b.lat,b.lon))siteMarker(b,$('qibla').checked?'ق':'B',true).addTo(pinLayer);if(!r||r.distance<=1)return;
 const line=geodesic.Geodesic.WGS84.InverseLine(a.lat,a.lon,b.lat,b.lon),pts=Array.from({length:101},(_,i)=>{const p=line.Position(r.distance*i/100);return [p.lat2,p.lon2]});L.polyline(pts,{color:'#fff',weight:7,opacity:.85,interactive:false}).addTo(routeLayer);L.polyline(pts,{color:'#cf292f',weight:4,interactive:false}).addTo(routeLayer);
 // Place an arrow inside the viewport along the geodesic, including close views near A.
 const bounds=map.getBounds(),center=map.getCenter();let best=null,bestDist=Infinity;
 for(let i=0;i<100;i++){const p1=map.latLngToContainerPoint(pts[i]),p2=map.latLngToContainerPoint(pts[i+1]),v=p2.subtract(p1),c=map.latLngToContainerPoint(center),den=v.x*v.x+v.y*v.y,t=den?Math.max(0,Math.min(1,((c.x-p1.x)*v.x+(c.y-p1.y)*v.y)/den)):0,point=p1.add(v.multiplyBy(t)),ll=map.containerPointToLatLng(point),d=point.distanceTo(c);if(bounds.contains(ll)&&d<bestDist){bestDist=d;best={ll,angle:Math.atan2(v.x,-v.y)*180/Math.PI}}}
 if(best)L.marker(best.ll,{interactive:false,icon:L.divIcon({className:'mapArrow',iconSize:[26,26],iconAnchor:[13,13],html:`<svg viewBox="0 0 26 26"><path d="M13 2L24 24L13 18L2 24Z" fill="#cf292f" stroke="white" stroke-width="1.5" transform="rotate(${best.angle} 13 13)"/></svg>`})}).addTo(routeLayer);
}
function startPick(k){if(!map){toast('نقشه هنوز بارگذاری نشده است');return}$('qibla').checked=false;cancelPick();pickSite=k;$('mapPick').hidden=false;$('pickLabel').textContent='محل '+k.toUpperCase()+' را روی نقشه لمس کنید';$('alignStatus').textContent='بزرگ‌نمایی کنید، محل دکل را لمس و سپس تأیید کنید.';drawAlignment()}
function cancelPick(){if(pendingMarker&&map)map.removeLayer(pendingMarker);pendingMarker=null;pendingPoint=null;pickSite=null;if($('mapPick')){$('mapPick').hidden=true;$('confirmPoint').disabled=true}$('alignStatus').textContent=''}
function requestLocation(){if(!navigator.geolocation)return toast('موقعیت در دسترس نیست');$('alignStatus').textContent='در حال دریافت موقعیت…';navigator.geolocation.getCurrentPosition(p=>window.receiveLocation({lat:p.coords.latitude,lon:p.coords.longitude,accuracy:p.coords.accuracy}),()=>{$('alignStatus').textContent='';toast('موقعیت دریافت نشد؛ محل A را روی نقشه انتخاب کنید')},{enableHighAccuracy:true,maximumAge:0,timeout:20000})}
window.receiveLocation=p=>{if(!$('alignCanvas'))return;if(p.error){$('alignStatus').textContent=p.error;return}if(!Geo.point(p.lat,p.lon))return;align.a={lat:p.lat,lon:p.lon};syncCoordinates();readAlign();drawAlignment();if(map)fitRoute();$('alignStatus').textContent='دقت اعلام‌شده: ±'+Math.round(p.accuracy)+' متر؛ محل دکل را بررسی کنید.'};
// کتابخانه‌های نقشه (۲۶۰ کیلوبایت) فقط وقتی بخشِ تنظیمِ آنتن به دید رسید.
let mapLibs=null;
function loadMapLibs(){if(mapLibs)return mapLibs;const v=APP_CONFIG.vendor,css=document.createElement('link');css.rel='stylesheet';css.href=v.leafletCss;document.head.append(css);const js=src=>new Promise((ok,fail)=>{const s=document.createElement('script');s.src=src;s.onload=ok;s.onerror=()=>{s.remove();fail()};document.head.append(s)});mapLibs=Promise.all([js(v.geodesic),js(v.leafletJs)]).catch(e=>{mapLibs=null;throw e});return mapLibs}
// دکمهٔ بالای ابزار حالا فقط به بخشِ تنظیمِ آنتن می‌برد.
$('alignBtn').onclick=()=>$('dlAlign')?.scrollIntoView({behavior:'smooth',block:'start'});
mountAlignment();
