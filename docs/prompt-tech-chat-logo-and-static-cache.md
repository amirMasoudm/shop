# تسکِ چت الف — دو ایرادِ کوچک با اثرِ بزرگ: لوگوی ۴۰۴ و کشِ صفرِ استاتیک

هر دو سنجیده‌شده روی پراد، هر دو مستقل، و هر دو کوچک.

---

# یک — `/logo.png` روی کلِ سایت ۴۰۴ است

```
https://dadehnama.com/logo.png       ->  404
https://dadehnama.com/img/logo.png   ->  200   <- fayl inja-st
```

فایل در `src/main/resources/static/img/logo.png` است، ولی کد `/logo.png` صدا
می‌زند. **پنج جا** (دست‌کم):

```
StoreWebController.java:815   JSON-LD  "logo": baseUrl + "/logo.png"
StoreWebController.java:846   model.addAttribute("ogImage", baseUrl + "/logo.png")
blog.html:88                  <img> fallback-e kaver-e maghale
CL.html:2914                  <img> fallback-e aks-e mahsul
CL.html:3045                  hamintor
```

## چرا مهم است

- 🔴 **JSON-LDِ `Organization.logo` به ۴۰۴ اشاره می‌کند.** سرچ کنسول این را
  خطای دادهٔ ساختاریافته می‌گیرد و گوگل نمی‌تواند لوگو را در نتایج و پنلِ
  دانش استفاده کند.
- 🔴 **`og:image`ِ پیش‌فرض ۴۰۴ است.** هر مقاله یا محصولی که تصویر ندارد، در
  تلگرام و واتساپ و لینکدین کارتِ شکسته نشان می‌دهد. (و همین الان دست‌کم یک
  مقاله را می‌شناسیم که کاور ندارد و به همین fallback می‌افتد.)
- عکسِ شکسته در فهرستِ بلاگ و کارتِ محصول.

## کار

مسیر را در هر پنج جا به `/img/logo.png` اصلاح کن.

⚠️ **دنبالِ موردِ ششم هم بگرد** — `grep -rn "logo.png" src/main` و مطمئن شو
جایی جا نمانده (مثلاً در `sitemap` یا فرگمنت‌ها).

⚠️ **فایل را جابه‌جا نکن.** بردنِ `logo.png` به ریشه ساده‌تر به‌نظر می‌رسد ولی
هر جای دیگری که `/img/logo.png` را درست صدا می‌زند می‌شکند. مقصد را درست کن،
نه فایل را.

## آزمون

```bash
curl -sI --noproxy '*' https://dadehnama.com/img/logo.png | head -1     # 200
```

و بعد از استقرار، در سورسِ یک مقالهٔ بی‌کاور:

```
og:image  -> .../img/logo.png     (na /logo.png)
JSON-LD "logo" -> .../img/logo.png
```

---

# دو — هیچ فایلِ استاتیکی کش نمی‌شود

سنجیدم، روی **همهٔ** فایل‌های استاتیک:

```
/js/pricing-workspace.js      cache-control: no-cache, no-store, max-age=0, must-revalidate
/img/certs/…-logo.jpg         cache-control: no-cache, no-store, max-age=0, must-revalidate
```

یعنی مرورگر **هیچ‌چیز** را نگه نمی‌دارد: هر بازدیدِ هر صفحه، دوباره کلِ
جاوااسکریپت و سی‌اس‌اس و عکس‌ها را دانلود می‌کند.

## ریشه

نه در `application.properties` تنظیمِ `spring.web.resources.cache` هست، نه در
`SecurityConfig` کاری با هدرهای کش شده. پس این **پیش‌فرضِ اسپرینگ سکیوریتی**
است که روی *همهٔ* پاسخ‌ها `no-store` می‌گذارد — منطقی برای صفحهٔ پنل، فاجعه
برای فایلِ استاتیک.

⚠️ nginx برای `/uploads/` این را درست کرده
(`Cache-Control: public, max-age=2592000, immutable`) ولی بقیهٔ مسیرها از
`location /` به بک‌اند پراکسی می‌شوند و هدرِ اسپرینگ برنده است.

## چرا مهم است

سرعتِ بارگذاری مستقیماً در رتبه‌بندیِ گوگل اثر دارد (Core Web Vitals)، و
کاربرانِ ما روی شبکهٔ داخلی‌اند که پهنای باند و تأخیرش ارزان نیست.

## کار — و 🔴 چیزی که نباید بشکند

کش را برای **فایل‌های استاتیک** روشن کن، نه برای صفحه‌های پنل و API.

```
/js/**  /css/**  /img/**  /fonts/**  /favicon.ico   ->  cache-e tulani
/Admin.html  /SalesPanel.html  /api/**  /blog/**  /shop/**  ->  bedun-e taghyir
```

🔴 **و بدونِ نسخه‌گذاری، کشِ طولانی خطرناک است.** اگر `pricing-workspace.js` را
یک ماه کش کنیم، کارشناسی که فردا وارد پنل می‌شود نسخهٔ کهنه را می‌بیند و ما
نمی‌فهمیم چرا. پس **یکی از این دو**:

- **الف)** کشِ کوتاه و امن (مثلاً `max-age=3600` + `must-revalidate`) بدونِ
  نسخه‌گذاری. ساده، و بیشترِ سود را می‌دهد.
- **ب)** کشِ طولانی + `immutable`، **ولی فقط اگر** نامِ فایل نسخه بگیرد
  (`pricing-workspace.<hash>.js`) و ارجاع‌ها در قالب‌ها خودکار به‌روز شوند.

**پیشنهادِ من گزینهٔ الف است** برای این پاس. گزینهٔ ب کارِ بیشتری است و تا وقتی
سه چتِ محتوا روی پراد کار می‌کنند، ریسکِ «نسخهٔ کهنه در پنل» را نمی‌خواهیم.
اگر نظرِ فنیِ دیگری داری بگو، ولی **خودسرانه ب را نزن**.

⚠️ `Admin.html` و `SalesPanel.html` باید **حتماً** بدونِ کش بمانند — همین چند
روز چند بار دیپلوی شده‌اند.

## آزمون

```bash
curl -sI --noproxy '*' https://dadehnama.com/js/pricing-workspace.js | grep -i cache-control   # bayad cache dashte bashad
curl -sI --noproxy '*' https://dadehnama.com/Admin.html           | grep -i cache-control      # bayad no-store bemanad
curl -sI --noproxy '*' https://dadehnama.com/api/v1/products      | grep -i cache-control      # bayad no-store bemanad
```

و یک بارِ دستی: بعد از دیپلوی، `Ctrl+Shift+R` نزن و ببین پنل نسخهٔ تازه را
می‌گیرد یا نه.

---

## گزارش

- مسیرِ لوگو در چند جا اصلاح شد و موردِ ششمی بود؟
- کدام گزینهٔ کش را زدی و چرا
- خروجیِ هر سه دستورِ آزمونِ کش
- هر مسیرِ استاتیکِ دیگری که پیدا کردی و در فهرستِ من نبود
