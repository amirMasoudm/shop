# ❌ منسوخ — این تسک را انجام نده

**تاریخِ لغو: ۱۷ اوت ۲۰۲۶.** فرضِ زیرِ این پرامپت غلط بود. چت ج با `nginx -T` رویِ خودِ سرور تأیید کرد `server.forward-headers-strategy=framework` **از قبل** در هر دو `application-dev.properties` و `application-prod.properties` ست بوده — من (مدیر) فقط `application.properties` (فایلِ پایه) را چک کرده بودم و فایل‌هایِ پروفایل را ندیده بودم. کاری لازم نیست. جزئیات در `docs/INFRA-CHANGELOG.md` (۱۷ اوت، ورودیِ چت ج) و `docs/launch-checklist.md`.

---

# پرامپتِ چت الف (در صف — بعد از تسکِ فعلی) — درست‌شدنِ baseUrl زیرِ HTTPS/پروکسی

## مسئله
`buildBaseUrl()` در `StoreWebController` (خطِ ~۳۴۴):
```java
return request.getScheme() + "://" + request.getServerName() + ...
```
اپ پشتِ nginx اجرا می‌شود. `server.forward-headers-strategy` در `application.properties` **ست نشده** — یعنی اسپرینگ هدرِ `X-Forwarded-Proto` را نادیده می‌گیرد و `getScheme()` حتی وقتی کاربر رویِ HTTPS است `http` برمی‌گرداند.

خروجیِ `buildBaseUrl` منبعِ **canonical، `og:url`، `sitemap.xml`، `robots.txt`، `Location`ِ ریدایرکتِ ۳۰۱ِ هیبرید، و URLهایِ JSON-LD** است. پس بعدِ راه‌اندازیِ TLS، همهٔ این‌ها `http://` اعلام می‌کنند در حالی که صفحه رویِ `https://` سرو می‌شود → canonicalِ ناهم‌خوان، و ریدایرکتِ ۳۰۱ به `http` که یک هاپِ اضافه و نشتِ اعتبار است.

## کار
1. `server.forward-headers-strategy=framework` را به `application.properties` اضافه کن (یا `native`؛ برایِ nginx معمولاً `framework` کافی و امن‌تر است).
   - ⚠️ اول رویِ سرور چک شود که به‌صورتِ متغیرِ محیطی (`SERVER_FORWARD_HEADERS_STRATEGY`) از قبل ست نشده باشد — دوباره‌کاری/تناقض نشود.
2. بعدِ این تغییر، `getScheme()` به `X-Forwarded-Proto` اعتماد می‌کند. **این فقط وقتی امن است که nginx آن هدر را خودش بازنویسی کند** (یعنی مقدارِ ارسالیِ کلاینت را نپذیرد). کانفیگِ مخزن `proxy_set_header X-Forwarded-Proto $scheme;` را دارد — تأیید کن رویِ سرور هم هست (چت ج در تسکِ TLS همین را درست می‌کند؛ هماهنگ باشید).
3. یک تستِ ساده که ثابت کند `buildBaseUrl` زیرِ `X-Forwarded-Proto: https` مقدارِ `https://...` می‌دهد.

## نکتهٔ مرتبط (کارِ چت ج است، فقط برایِ آگاهی)
تستِ زنده نشان داد اپ هدرِ `Host`ِ اصلی را هم نمی‌بیند (`getServerName()` آی‌پی برمی‌گرداند) چون کانفیگِ nginxِ سرور با نسخهٔ مخزن واگرا شده. آن سمتش را چت ج در `docs/prompt-devops-chat-tls-before-dns.md` رفع می‌کند. **هر دو باید باشند** تا baseUrl کامل درست شود: `Host` صحیح از nginx، و `scheme` صحیح از forward-headers.

## چرا اولویت دارد
این یکی از سه پیش‌نیازِ سوییچِ DNS است. تا درست نشود، مهاجرتِ دامنه canonicalهایِ غلط تولید می‌کند و کلِ کارِ سئو بی‌اثر می‌شود.
