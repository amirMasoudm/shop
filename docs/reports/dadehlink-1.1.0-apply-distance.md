# «اعمالِ فاصله در صفحهٔ اصلی» — برداشته از داده‌لینک ۱٫۱٫۰ نویسنده

> **به‌روز — ۱۰ مهر، بعدازظهر:** مالک خواست **بقیهٔ تغییرهای ۱٫۱٫۰ هم بیاید**. در اپ
> اندرویدی کلِ ۱٫۱٫۰ ادغام می‌شود (تسکِ چت د، بخشِ صفر)؛ در سایت فقط این دکمه و حذفِ
> ماهواره (تسکِ چت الف، بندهای ۸ و ۹). بخشِ «برداشته نمی‌شود» در پایینِ این فایل دیگر
> معتبر نیست، جز دکمهٔ قیمت.
>
> تغییرهایی که فقط در APK هستند و در سورسِ ۱٫۱٫۰ نه، بی‌کم‌وکاست در این فایل آمده:
> `docs/reports/dadehlink-1.1.0-apk-only.diff`

خواستهٔ اولِ مالک (۱۰ مهر): «فقط اون قسمتِ اعمالِ فاصله در صفحهٔ اصلی که به تنظیمِ آنتن
اضافه کرده بیاد.»

## منبع

این قابلیت در **سورسِ** ۱٫۱٫۰ نیست؛ فقط در APKِ بعدی‌اش هست:

```
DadehLink-1.1.0-source-1.zip   02:09   web/alignment.js  -> NADARAD
DadehLink-1.1.0-1.apk          03:06   assets/index.html -> DARAD
apk sha256  0612796011547bd7147f16986999002987015f1a08d18b2827de2302088d5b91
```

نسخهٔ APK در `C:\Users\m\Desktop\` و `Downloads\Telegram Desktop\` است. کدِ زیر عیناً از
`assets/index.html` همان APK برداشته شده.

`rf.js` و `cities.json` در ۱٫۱٫۰ با rc3 یکی‌اند؛ موتور عوض نشده.

## رفتار

- دکمه‌ای زیرِ صفحهٔ تنظیمِ آنتن: «اعمال فاصله در صفحه اصلی».
- وقتی A و B هر دو معتبرند و فاصلهٔ ژئودزیکشان در بازهٔ ماشین‌حساب است (۰٫۰۱ تا ۱۱۰
  کیلومتر)، دکمه فعال می‌شود و فاصله را نشان می‌دهد: «اعمال فاصلهٔ ۱۲٫۳۴ km در صفحه اصلی».
- با کلیک، فاصلهٔ ماشین‌حساب **با دو رقمِ اعشار** همان می‌شود و نتیجه دوباره حساب می‌شود.
- **اگر تیکِ قبله روشن است**، دکمه غیرفعال است با متنِ «برای اعمال فاصله، تیک قبله را
  بردارید». فاصله همیشه بین A و B است، نه تا قبله.
- اگر فاصله نامعتبر یا بیرونِ بازه بود: دکمه غیرفعال، با راهنمای «مبدأ و مقصد معتبر با
  فاصلهٔ ۰٫۰۱ تا ۱۱۰ کیلومتر انتخاب کنید.»
- **خودکار نیست.** فاصلهٔ دستیِ کاربر فقط با کلیک بازنویسی می‌شود.

## کد — عیناً از APK

بازهٔ ماشین‌حساب (در ۱٫۱٫۰ در `index.html` تعریف شده):

```js
const ranges={f:[2,80],distance:[.01,110],tx:[-10,33],sensitivity:[-90,-40],gainA:[0,80],gainB:[0,80],lossA:[0,30],lossB:[0,30],pressure:[300,1100],temp:[-50,60],diaA:[10,180],diaB:[10,180],humidity:[0,100],rainRate:[0,150]}
```

دو تابع:

```js
function linkDistanceKm(){if(!Geo.point(align.a.lat,align.a.lon)||!Geo.point(align.b.lat,align.b.lon))return null;const km=Geo.route(align.a,align.b).distance/1000;return Number.isFinite(km)&&km>=ranges.distance[0]&&km<=ranges.distance[1]?km:null}
function applyLinkDistance(){const km=linkDistanceKm();if(km===null||$('qibla').checked)return;state.distance=Number(km.toFixed(2));setControls();closeDialog();toast('فاصلهٔ لینک در صفحه اصلی اعمال شد')}
```

داخلِ `drawAlignment()`، پس از تعریفِ `q` (تیکِ قبله):

```js
const km=linkDistanceKm();$('applyLinkDistance').disabled=km===null||q;$('applyLinkDistance').textContent=q?'برای اعمال فاصله، تیک قبله را بردارید':km===null?'اعمال فاصله در صفحه اصلی':'اعمال فاصلهٔ '+km.toFixed(2)+' km در صفحه اصلی';$('applyLinkDistance').title=km===null?'مبدأ و مقصد معتبر با فاصلهٔ ۰٫۰۱ تا ۱۱۰ کیلومتر انتخاب کنید.':'';
```

نشانه‌گذاری، آخرِ صفحهٔ تنظیمِ آنتن، و اتصال:

```html
<button id="applyLinkDistance" class="applyLinkDistance" disabled>اعمال فاصله در صفحه اصلی</button>
```

```js
$('applyLinkDistance').onclick=applyLinkDistance;
```

استایل:

```css
.applyLinkDistance{min-height:34px;flex-shrink:0;border:0;border-radius:9px;background:#087b43;color:white;font-size:11px;padding:6px}
.applyLinkDistance:disabled{background:#e0ebe3;color:#6e8275}
```

## چیزهایی که از ۱٫۱٫۰ **برداشته نمی‌شود**

~~بقیهٔ تغییرهای ۱٫۱٫۰~~ — منسوخ؛ مالک خواست بیایند. فقط این یکی همچنان نه:

- **دکمهٔ قیمت به `/shop`.** ما آن را به دستهٔ آنتن و دیش (`/shop/category/antenna-dish`)
  وصل کرده‌ایم که برای کسی که لینک طراحی می‌کند مرتبط‌تر است.
