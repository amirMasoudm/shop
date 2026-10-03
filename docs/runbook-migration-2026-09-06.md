# رانبوکِ مهاجرتِ داده و اپ — ۱۴۰۵/۰۶/۱۵ (۲۰۲۶-۰۹-۰۶)

مقصد: `185.239.3.236` · پوشهٔ پروژه `/root/shop/` · کانتینرِ مونگو **`shop_mongo`**
(نامش با لوکال فرق دارد: لوکال `shop_mongodb`) · دیتابیس `shopdb` · یوزرِ روت `shopadmin`.

> دستورها **تک‌خطی** نوشته شده‌اند چون چندخطیِ `\` موقعِ پیست به‌هم می‌ریزد.
> مالک خودش روی سرور نشسته است (`root@srv5197010364`)، پس دستورهایِ این سند بدونِ
> `ssh` نوشته شده‌اند و مستقیم همان‌جا پیست می‌شوند. انتقالِ فایل هم با **FileZilla**
> در مسیرِ **`/root/`** انجام می‌شود، نه با `scp`.
>
> **رمزی لازم نیست دستی وارد شود:** هر دستور اول `/root/shop/.env` خودِ سرور را
> `source` می‌کند و یوزر/رمزِ مونگو را از همان‌جا برمی‌دارد.

---

## بخش ۰ — قبل از شروع بخوانید

### ✅ اپ آمادهٔ دیپلوی است

چت الف کارش را در کامیتِ `1d8faaa` بست و درختِ کاری تمیز است. تأییدهای انجام‌شده:
`grep -r PRODUCT_EDITOR src/` خالی · enum نهایی `ADMIN · USER · PRICER · SALES · SUPPORT` ·
`session-watch.js` سرِ جایش · **هیچ پراپرتیِ env جدیدی اضافه نشده** (یعنی خطرِ ۵۰۲ِ
دفعهٔ قبل اینجا وجود ندارد) · بیلدِ ورک‌تری موفق.

**⚠️ کارِ لازم بعد از دیپلوی:** معنیِ نقشِ `SALES` عوض شده — حالا قیمت‌گذاریِ کامل دارد
(شاملِ ستونِ «فروش تعدادی»). چون `users` عمداً منتقل نمی‌شود، نقش‌های فعلیِ سرور
دست‌نخورده می‌مانند؛ اگر روی پراد کاربری با `SALES` هست که منظور «فقط مشاهده» بوده،
باید بعد از دیپلوی از پنل به `SUPPORT` تغییرش دهی.

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
cd /root/shop && set -a && . ./.env && set +a && docker exec shop_mongo mongodump --username="$MONGO_ROOT_USERNAME" --password="$MONGO_ROOT_PASSWORD" --authenticationDatabase=admin --db=shopdb --archive=/tmp/pre-migrate.gz --gzip && docker cp shop_mongo:/tmp/pre-migrate.gz /root/pre-migrate-20260906.gz && ls -la /root/pre-migrate-20260906.gz
```

خروجی باید یک فایلِ چندصد کیلوبایتی نشان دهد. **تا این را ندیده‌اید جلو نروید.**

### ۱-۲ آپلودِ دامپ و دلتای عکس‌ها

**با FileZilla** این دو فایل را از ریشهٔ پروژه بگذار در **`/root/`** روی سرور
(نه `/root/shop/`):

- `shopdb-migrate-20260906.gz`
- `uploads-delta-20260906.tar.gz`

### ۱-۳ بازگردانیِ داده

```
cd /root/shop && set -a && . ./.env && set +a && docker cp /root/shopdb-migrate-20260906.gz shop_mongo:/tmp/in.gz && docker exec shop_mongo mongorestore --username="$MONGO_ROOT_USERNAME" --password="$MONGO_ROOT_PASSWORD" --authenticationDatabase=admin --archive=/tmp/in.gz --gzip --drop --nsInclude='shopdb.*'
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
cd /root/shop && set -a && . ./.env && set +a && docker exec shop_mongo mongosh -u "$MONGO_ROOT_USERNAME" -p "$MONGO_ROOT_PASSWORD" --authenticationDatabase admin shopdb --quiet --eval 'print("products="+db.products.countDocuments({})); print("courses="+db.courses.countDocuments({})); print("archive="+db.education_archive_items.countDocuments({})); print("users="+db.users.countDocuments({}))'
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
cd /root/shop && set -a && . ./.env && set +a && docker cp /root/pre-migrate-20260906.gz shop_mongo:/tmp/rb.gz && docker exec shop_mongo mongorestore --username="$MONGO_ROOT_USERNAME" --password="$MONGO_ROOT_PASSWORD" --authenticationDatabase=admin --archive=/tmp/rb.gz --gzip --drop
```

### ۳-۲ برگرداندنِ اپ

jarِ قبلی روی سرور با `docker compose build` جایگزین شده، پس نسخهٔ قبلی روی سرور
نمانده. برایِ برگشت باید از کامیتِ قبلی دوباره بیلد شود — بگویید تا بزنم.
(بهتر است قبل از بخشِ ۲، jarِ فعلیِ سرور را با
`cp /root/shop/target/shop1-0.0.1-SNAPSHOT.jar /root/jar-backup-20260906.jar` کنار بگذاریم.)
