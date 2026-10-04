> ⚠️ **این فایل بایگانی است.** محتوایش در رانبوکِ واحدِ
> `docs/prompt-devops-chat-domain-golive.md` ادغام شد. از آن استفاده کن، نه این.

# پرامپتِ چت ج — TLS قبل از سوییچِ DNS + رفعِ هدرِ Host

## هدف
گرفتنِ گواهیِ TLS برایِ `dadehnama.com` **قبل از** جابه‌جاییِ DNS، تا لحظهٔ سوییچ سایت مستقیم رویِ HTTPS بالا بیاید (بدونِ پنجرهٔ HTTP یا خطایِ گواهی).

## ⚠️ مشکلِ مرغ‌و‌تخم‌مرغ و راهِ حلش
`dadehnama.com` هنوز به وردپرسِ قدیم اشاره می‌کند، پس **چالشِ HTTP-01 کار نمی‌کند** (Let's Encrypt فایلِ چالش را رویِ سرورِ قدیم می‌جوید، نه سرورِ ما).

**راهِ حلِ انتخابی: چالشِ DNS-01.**
- `certbot certonly --manual --preferred-challenges dns -d dadehnama.com -d www.dadehnama.com`
- certbot یک مقدارِ TXT می‌دهد → کاربر آن را به‌عنوانِ رکوردِ `_acme-challenge.dadehnama.com` در پنلِ DNS ثبت می‌کند → تأیید → گواهی صادر می‌شود، **بدونِ هیچ تغییری در رکوردِ A**.
- ⚠️ DNS-01 دستی است، پس **تمدیدِ خودکار کار نمی‌کند**. بعد از سوییچِ DNS باید به HTTP-01/webroot سوییچ کنیم تا `certbot renew` خودکار شود. این را در `INFRA-CHANGELOG` یادداشت کن که فراموش نشود (گواهی ۹۰ روزه است).
- اگر پنلِ DNS پروایدر API دارد، پلاگینِ متناظرِ certbot گزینهٔ بهتری است (تمدیدِ خودکار با DNS-01). اول از کاربر بپرس پنلش چیست.

## 🔴 باگِ تأییدشده که باید همراهِ TLS رفع شود — هدرِ Host
تستِ زنده (۱۶ اوت):
```
curl -H "Host: dadehnama.com" http://185.239.3.236/robots.txt
→ Sitemap: http://185.239.3.236/sitemap.xml      ← باید dadehnama.com می‌بود
```
یعنی **اپ هدرِ `Host`ِ اصلی را نمی‌بیند** و `request.getServerName()` آی‌پی برمی‌گرداند. کانفیگِ nginxِ داخلِ مخزن (`nginx/nginx.conf`) خطِ `proxy_set_header Host $host;` را دارد، ولی کانفیگی که تو با heredoc رویِ سرور نوشتی ظاهراً ندارد — **کانفیگِ سرور و مخزن از هم واگرا شده‌اند.**

**چرا حیاتی است:** `buildBaseUrl()` در `StoreWebController` از `getScheme()` + `getServerName()` ساخته می‌شود و **همه‌ی این‌ها از آن می‌آیند**: `canonical`، `og:url`، `sitemap.xml`، `robots.txt`، آدرسِ `Location` در ریدایرکتِ ۳۰۱ِ هیبرید، و URLهایِ داخلِ JSON-LD. اگر با همین وضع DNS را سوییچ کنیم، گوگل canonicalِ `http://185.239.3.236/...` می‌بیند — یعنی کلِ مهاجرتِ دامنه بی‌اثر (بدتر: مضر) می‌شود.

**کار:** `location /` رویِ سرور باید همان چهار هدرِ کانفیگِ مخزن را داشته باشد (`Host $host`, `X-Real-IP`, `X-Forwarded-For`, `X-Forwarded-Proto $scheme`). بهتر: **کانفیگِ سرور را با نسخهٔ مخزن هم‌سان کن** تا دوباره واگرا نشود.

## کارهایِ این تسک، به ترتیب
1. از کاربر بپرس پنلِ DNS دامنه چیست (برایِ انتخابِ DNS-01 دستی یا پلاگینِ API).
2. گواهی را با DNS-01 بگیر (رکوردِ A دست‌نخورده می‌ماند).
3. بلوکِ `listen 443 ssl` + `http2` به nginx اضافه کن؛ `server_name dadehnama.com www.dadehnama.com;`.
4. **ریدایرکتِ ۸۰→۴۴۳** اضافه کن، ولی مسیرِ `/.well-known/acme-challenge/` را از ریدایرکت مستثنا کن (برایِ تمدیدهایِ بعدی).
5. هدرهایِ `proxy_set_header` (بندِ بالا) را در هر دو بلوک درست کن.
6. `SERVER_SERVLET_SESSION_COOKIE_SECURE=true` در `.env` (چت ج قبلاً یادداشت کرده بود که «بعدِ Certbot باید true شود» — الان همان لحظه است). بعدِ تغییر، **نسخهٔ `.env` در KeePassXC هم به‌روز شود** (قاعدهٔ خودت).
7. HSTS: **فعلاً نزن** یا با `max-age` کوتاه شروع کن. با `max-age` طولانی اگر مشکلی پیش بیاید برگشت‌ناپذیر می‌شود؛ بعد از چند روز پایداری بالا ببر.
8. تست **قبل از سوییچِ DNS** (با `--resolve` می‌توان DNS را دور زد):
   ```
   curl -I --resolve dadehnama.com:443:185.239.3.236 https://dadehnama.com/
   curl -s --resolve dadehnama.com:443:185.239.3.236 https://dadehnama.com/robots.txt
   ```
   انتظار: ۲۰۰، گواهیِ معتبر، و `Sitemap: https://dadehnama.com/sitemap.xml` (نه آی‌پی، نه http).

## ⛔ سه پیش‌نیازِ سوییچِ DNS (تا این‌ها نشده، رکوردِ A را عوض نکن)
1. **همین تسک** (TLS + رفعِ Host) — بندهای بالا.
2. **`server.forward-headers-strategy`** — تسکِ چت الف (`docs/prompt-tech-chat-base-url-https.md`). بدونش `getScheme()` زیرِ TLS هم `http` می‌ماند و canonicalها `http://` می‌شوند. (در `application.properties` مخزن نیست؛ چک کن رویِ سرور به‌صورتِ env ست نشده باشد.)
3. **نقشهٔ ریدایرکتِ ۳۰۱** آدرس‌هایِ سایتِ قدیم — هنوز پیاده نشده (فقط دیتایش هست). بدونش ~۱۱۰۰ آدرسِ ایندکس‌شدهٔ وردپرس در لحظهٔ سوییچ ۴۰۴ می‌شوند.

**یادآوریِ جانبی:** بعدِ سوییچ، وردپرسِ قدیم رویِ آن دامنه ناپدید می‌شود؛ پس آن **۲ مقالهٔ ایمپورت‌شده که هنوز تصویرشان از `wp-content` می‌آید** همان لحظه عکسِ شکسته می‌گیرند (`docs/prompt-tech-chat-wordpress-import-followup.md`). قبل از سوییچ رفع شود.
