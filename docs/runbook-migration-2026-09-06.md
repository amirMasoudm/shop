# رانبوکِ مهاجرتِ داده و اپ — ۱۴۰۵/۰۶/۱۵ (۲۰۲۶-۰۹-۰۶)

مقصد: `185.239.3.236` · پوشهٔ پروژه `/root/shop/` · کانتینرِ مونگو **`shop_mongo`**
(نامش با لوکال فرق دارد: لوکال `shop_mongodb`) · دیتابیس `shopdb` · یوزرِ روت `shopadmin`.

> دستورها **تک‌خطی** نوشته شده‌اند چون چندخطیِ `\` موقعِ پیست به‌هم می‌ریزد.
> مالک خودش روی سرور نشسته است (`root@srv5197010364`)، پس دستورهایِ این سند بدونِ
> `ssh` نوشته شده‌اند و مستقیم همان‌جا پیست می‌شوند. انتقالِ فایل هم با **FileZilla**
> در مسیرِ **`/root/`** انجام می‌شود، نه با `scp`.
>
> هرجا `PASTE` دیدید یعنی همان‌جا مقدارِ واقعی را از KeePassXC بگذارید.
> **هیچ رمزی را در چت نگذارید.**

---

## بخش ۰ — قبل از شروع بخوانید

### 🔴 اپ هنوز آمادهٔ دیپلوی نیست (داده هست)

درختِ کاری الان **تمیز نیست** — هشت فایل تغییرِ کامیت‌نشده دارند
(`SecurityConfig`، `Role`، `AuthController`، `UserResource`، `UserService`،
`PricingWorkspaceService`، `Admin.html`، `SalesPanel.html`) و چت الف هم‌زمان دارد
روی «اعتبارسنجیِ سشن بدونِ رفرش» کار می‌کند.

مسئله فقط «تمیز نبودنِ درخت» نیست. کامیتِ فعلیِ `HEAD` (`b887e97`) نقشِ
**`PRODUCT_EDITOR`** را دارد و `SUPPORT` را حذف کرده — ولی همان تغییراتِ کامیت‌نشده
دقیقاً **برعکسش** را می‌کنند: `PRODUCT_EDITOR` را برمی‌دارند و `SUPPORT` را با معنیِ
تازه برمی‌گردانند. یعنی طراحیِ نقش‌ها بینِ `HEAD` و درختِ کاری با هم **در تضاد** است.

اگر همین `HEAD` را دیپلوی کنیم، نقشی روی پراد می‌نشیند که خودِ چت الف کنارش گذاشته.
و چون **نقشِ هر کاربر در دیتابیس ذخیره می‌شود**، اگر روی پراد کاربری با
`PRODUCT_EDITOR` ساخته شود، دیپلویِ بعدی که آن مقدار را از enum برداشته، موقعِ خواندنِ
همان کاربر خطا می‌دهد — دقیقاً همان خطری که کامنتِ خودِ `Role.java` دربارهٔ `SUPPORT`
هشدار داده بود.

**پس: بخشِ ۱ (داده) الان قابلِ اجراست. بخشِ ۲ (اپ) فقط بعد از اینکه چت الف کارش را
کامیت کرد و درخت تمیز شد.**

### 🔑 چرخاندنِ کلیدهای لو‌رفته

کلیدهایی که از `/uploads/.env` عمومی سرو می‌شدند **هنوز چرخانده نشده‌اند**:
رمزِ روتِ مونگو، `ADMIN_PASSWORD`، `SMS_API_KEY`، `OPENAI_KEY`، `TAPIN_TOKEN`،
`SNAPP_TOKEN`. مهاجرت طبیعی‌ترین لحظه برای این کار است، چون رمزِ مونگو در همین
دستورها به‌کار می‌رود و بعدش هم بک‌اند یک‌بار ری‌استارت می‌شود.
اگر می‌خواهید همین حالا انجام شود بگویید تا ترتیبش را بنویسم.

### چه چیزی منتقل می‌شود و چه چیزی نه

| منتقل می‌شود | تعداد | | منتقل **نمی‌شود** | چرا |
|---|---|---|---|---|
| products | ۲۲۷ | | users | کاربرانِ واقعیِ پراد نباید با کاربرِ تستیِ لوکال عوض شوند |
| articles | ۲۸۱ | | orders | ۴ سبدِ نیمه‌کارهٔ تستیِ لوکال |
| education_archive_items | ۱۲۲ | | sessions | ۷۶ سشنِ لاگینِ لوکال |
| categories | ۲۰ | | activity_logs | لاگِ لوکال |
| courses | ۱۲ | | otp_codes / rfqs | گذرا/تستی |
| product_redirects | ۹ | | system.profile | داخلیِ مونگو |
| banners ۶ · landing_sections ۲ · image_row_banners ۱ · counters ۱ · store_settings ۱ · iran_cities ۳۱ · iran_provinces ۳۱ | | | | |

**⚠️ کاتالوگ از ۳۰۲ به ۲۲۷ می‌رسد** — ۷۵ حذفِ عمدی طبقِ تصمیمِ شرکت (کامیتِ `5bb992b`).
بکاپشان در `deleted-products-2026-08-31.json` است. این کاهش **انتظارِ ماست، نه خرابی.**

### فایل‌های آماده در ریشهٔ پروژه

- `shopdb-migrate-20260906.gz` — ۷۲۵ کیلوبایت
- `uploads-delta-20260906.tar.gz` — ۲۱ مگابایت، **۱۹۰ فایلِ جدید** از سینکِ ۳۰ مرداد
  به بعد (نه کلِ ۱۱۰ مگابایت — فقط دلتا، تا آپلود سریع باشد)

---

## بخش ۱ — انتقالِ داده (آمادهٔ اجرا)

### ۱-۱ بکاپِ سرور، قبل از هر چیز

```
docker exec shop_mongo mongodump --username=shopadmin --password='PASTE' --authenticationDatabase=admin --db=shopdb --archive=/tmp/pre-migrate.gz --gzip && docker cp shop_mongo:/tmp/pre-migrate.gz /root/pre-migrate-20260906.gz && ls -la /root/pre-migrate-20260906.gz
```

خروجی باید یک فایلِ چندصد کیلوبایتی نشان دهد. **تا این را ندیده‌اید جلو نروید.**

### ۱-۲ آپلودِ دامپ و دلتای عکس‌ها

**با FileZilla** این دو فایل را از ریشهٔ پروژه بگذار در **`/root/`** روی سرور
(نه `/root/shop/`):

- `shopdb-migrate-20260906.gz`
- `uploads-delta-20260906.tar.gz`

### ۱-۳ بازگردانیِ داده

```
docker cp /root/shopdb-migrate-20260906.gz shop_mongo:/tmp/in.gz && docker exec shop_mongo mongorestore --username=shopadmin --password='PASTE' --authenticationDatabase=admin --archive=/tmp/in.gz --gzip --drop --nsInclude='shopdb.*'
```

`--drop` فقط کالکشن‌هایی را که **داخلِ همین آرشیو هستند** پاک می‌کند — یعنی
`users` و `orders`ِ سرور دست نمی‌خورند، چون اصلاً در آرشیو نیستند.

### ۱-۴ بازکردنِ عکس‌ها داخلِ ولیوم

```
docker cp /root/uploads-delta-20260906.tar.gz shop_backend:/tmp/up.tar.gz && docker exec shop_backend sh -c 'cd /opt/shop/uploads && tar -xzf /tmp/up.tar.gz && ls -1 | wc -l'
```

عددِ آخر باید حدودِ **۱۱۸۰** باشد.

### ۱-۵ تأیید

```
docker exec shop_mongo mongosh -u shopadmin -p 'PASTE' --authenticationDatabase admin shopdb --quiet --eval 'print("products="+db.products.countDocuments({})); print("courses="+db.courses.countDocuments({})); print("archive="+db.education_archive_items.countDocuments({})); print("users="+db.users.countDocuments({}))'
```

انتظار: `products=227` · `courses=12` · `archive=122` · و `users` **همان عددِ قبلیِ سرور**
(اگر عوض شده بود یعنی چیزی اشتباه رفته — برگردید به ۳-۱).

---

## بخش ۲ — دیپلویِ اپ (فقط بعد از کامیتِ چت الف)

پیش‌شرط: `git status` هیچ فایلِ ترک‌شدهٔ تغییرکرده‌ای نشان ندهد.

بیلد را **من** انجام می‌دهم، از یک ورک‌تریِ تمیزِ `HEAD` — نه از درختِ کاری. دلیلش این
است که `src/main/java/org/example/shop1/tools/WordPressImportRunner.java` آن‌ترکد است و
`CommandLineRunner`؛ اگر از درختِ کاری بیلد شود داخلِ jar می‌رود و **هر بار که بک‌اند بالا
بیاید، ایمپورتِ وردپرس را دوباره اجرا می‌کند.** بعد از بیلد تأیید می‌کنم که آن کلاس داخلِ
jar نیست، و آن‌وقت این دستورها را می‌دهم:

**با FileZilla** فایلِ `target/shop1-0.0.1-SNAPSHOT.jar` را بگذار در **`/root/`**،
بعد سمتِ سرور سرِ جایش ببر:

```
cp /root/shop1-0.0.1-SNAPSHOT.jar /root/shop/target/shop1-0.0.1-SNAPSHOT.jar
```

```
cd /root/shop && docker compose build backend && docker compose up -d backend && sleep 10 && docker ps --format '{{.Names}} {{.Status}}' | grep shop_
```

**⚠️ یادآوریِ ۵۰۲:** اگر این نسخه پراپرتیِ `${ENV_VAR}`ِ جدیدِ بدونِ دیفالت داشته باشد،
باید **دو جا** باشد: `application-prod.properties` (در گیت) و بخشِ `environment`ِ
composeِ **سرور** که هرگز از لوکال کپی نمی‌شود. دفعهٔ قبل `MELLAT_TERMINAL_ID` همین‌جا
۵۰۲ داد و با `docker-compose.override.yml` حل شد.

### تستِ بعد از دیپلوی

```
for p in / /shop /learn /wimaxnear /blog; do printf '%-12s %s\n' \$p \$(curl -s -o /dev/null -w '%{http_code}' -H 'Host: dadehnama.com' http://localhost\$p); done
```

هر پنج تا باید `200` بدهند. (هدرِ `Host` لازم است چون nginx قفلِ Host دارد.)

---

## بخش ۳ — بازگردانی

### ۳-۱ برگرداندنِ داده

```
docker cp /root/pre-migrate-20260906.gz shop_mongo:/tmp/rb.gz && docker exec shop_mongo mongorestore --username=shopadmin --password='PASTE' --authenticationDatabase=admin --archive=/tmp/rb.gz --gzip --drop
```

### ۳-۲ برگرداندنِ اپ

jarِ قبلی روی سرور با `docker compose build` جایگزین شده، پس نسخهٔ قبلی روی سرور
نمانده. برایِ برگشت باید از کامیتِ قبلی دوباره بیلد شود — بگویید تا بزنم.
(بهتر است قبل از بخشِ ۲، jarِ فعلیِ سرور را با
`ssh root@185.239.3.236 "cp /root/shop/target/shop1-0.0.1-SNAPSHOT.jar /root/jar-backup-20260906.jar"` کنار بگذاریم.)
