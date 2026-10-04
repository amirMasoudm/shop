# رانبوکِ انتشارِ داده‌لینک — سایت، داده و اپ

نوشتهٔ مدیر، ۱۱ مهر ۱۴۰۵. دستورها را مالک اجرا می‌کند؛ راستی‌آزمایی از بیرون با مدیر.

## الف — استقرارِ سایت

**روی لپ‌تاپ** — باید هیچ خطی چاپ نکند:

```bash
cd /c/Users/m/Desktop/shop1 && git status --short | grep -v '^??'
```

```bash
mvn clean package -DskipTests
```

`target/shop1-0.0.1-SNAPSHOT.jar` با FileZilla (حالتِ Binary) به `/root/`.

**روی سرور:**

1. دو خطِ ناامن‌کنندهٔ کوکی. اگر چیزی با `COOKIE_SECURE=false` چاپ شد، دستورِ بعدی؛ وگرنه رد شو.

```bash
grep -n -i cookie /root/shop/docker-compose.yml
```

```bash
cp /root/shop/docker-compose.yml /root/docker-compose.yml.before-1405-07-11 && sed -i '/COOKIE_SECURE=false/d' /root/shop/docker-compose.yml
```

2. **جابه‌جاییِ جار — با FileZilla:**
   - در پوشهٔ `/root/shop/target/` جارِ قبلی را **تغییرِ نام** بده به `shop1-before-1405-07-11.jar.bak`.
     پسوند باید از `.jar` بیفتد، چون Dockerfile همهٔ `target/*.jar` را کپی می‌کند.
   - جارِ تازه را از `/root/` به `/root/shop/target/` بکش (یا مستقیم از لپ‌تاپ همان‌جا آپلود کن).
   - در `target` باید **فقط یک** فایل با پسوندِ `.jar` باشد.

3. بالاآوردن و لاگ:

```bash
cd /root/shop && docker compose up -d --build backend && sleep 25 && docker compose logs --since 3m backend | grep -iE 'error|exception|started' | tail -20
```

4. **پوشهٔ فایل‌های اپ.** این دستور نشان می‌دهد پوشهٔ `uploads` روی سرور کجاست:

```bash
cd /root/shop && docker inspect $(docker compose ps -q backend) --format '{{range .Mounts}}{{.Source}} -> {{.Destination}}{{println}}{{end}}' | grep uploads
```

   روی سرورِ فعلی جواب این است (۱۱ مهر): `/var/lib/docker/volumes/shop_uploads_data/_data`. `/opt/shop/uploads`
   مسیرِ **داخلِ کانتینر** است و روی خودِ سرور وجود ندارد. با FileZilla داخلِ `_data` یک پوشهٔ تازه بساز به نامِ
   `app-dadehlink`. به بقیهٔ محتوای `_data` دست نزن؛ عکس‌های محصولات آن‌جاست.

**برگشت، اگر چیزی خراب شد:** با FileZilla جارِ تازه را از `target` بیرون ببر و `shop1-before-1405-07-11.jar.bak`
را دوباره `shop1-0.0.1-SNAPSHOT.jar` نام بده. بعد:

```bash
cd /root/shop && docker compose up -d --build backend
```

## ب — پس از بالاآمدن

- **مدیر** از بیرون می‌سنجد: صفحهٔ ابزار، دو مسیرِ داده، صفحهٔ دانلود (`noindex` تا فایلِ اپ نیامده)، `/privacy` و نقشهٔ سایت.
- **پیامکِ واقعی:** `dadehnama.com/support/link-cal` را باز کن، روی ابزار بزن، شمارهٔ خودت، کدِ پنج‌رقمی. رفرش کن؛ دیگر نباید چیزی بپرسد.
- **پنل:** تبِ «ثبت‌نام‌های اپ» ثبتِ تو را با تاریخِ شمسی نشان بدهد؛ یک خروجیِ CSV بگیر. ورودِ دومرحله‌ایِ ادمین هم سالم باشد.
- **کوکی:** در ابزارِ توسعه‌دهندهٔ مرورگر، کوکیِ `SESSION` علامتِ `Secure` داشته باشد.

## ج — دادهٔ تجهیزات روی سایتِ زنده

استقرار فقط کد می‌برد؛ داده جدا وارد می‌شود. در پنل، تبِ **«دادهٔ رادیویی»**، این دو فایل از روی لپ‌تاپ:

```
C:\Users\m\Desktop\shop1\docs\reports\rf-specs-1405-07.csv
C:\Users\m\Desktop\shop1\docs\reports\rf-specs-mcs-1405-07.csv
```

پیش‌نمایش را ببین (انتظار: حدودِ ۸۱ محصول بی‌خطا؛ `DN-0191` و `DN-0218` عمداً بی‌نوع و ردشده) و «اعمال».

**در همان ورود:** چت ب کارتِ دو آنتنِ وایمکس‌نیر را روی سایتِ زنده اصلاح می‌کند. پنل در مرورگرِ داخلیِ چت ب باز باشد و به او بگو.

## د — اپ

1. چت د اپ را روی سایتِ زنده می‌آزماید (ورود با پیامکِ واقعی، پیشنهادِ کالا، زبانهٔ تجهیزات).
2. **کلیدِ اصلی** — یک بار برای همیشه، اگر هنوز نساخته‌ای. دستورش در `C:\Users\m\Desktop\dadehlink\README.md`. در KeePassXC، با یک پشتیبان بیرون از لپ‌تاپ.
3. **بیلدِ انتشار** با همان README. خطِ آخرِ خروجی هش و اندازه را برای `latest.json` می‌دهد.
4. APK و `latest.json` با FileZilla در پوشهٔ `app-dadehlink` (قدمِ ۴ بخشِ الف).
5. صفحهٔ `dadehnama.com/dadehlink` خودش دکمهٔ دانلود را نشان می‌دهد و قابلِ ایندکس می‌شود. مدیر می‌سنجد.
6. نصب روی دو سه گوشیِ واقعی، از روی همان صفحه.

## سابقهٔ انتشار

| نسخه | کدِ نسخه | فایل | sha256 | تاریخ |
|---|---|---|---|---|
| 1.4.0 | 115 | `DadehLink-1.4.0.apk` | `fb3b7c5b2e9efd367b211e11d709deff052927c9407114bbb65cd396a1d65433` | ۱۱ مهر ۱۴۰۵ |

**اثرِ انگشتِ گواهیِ کلیدِ اصلی** (SHA-256، عمومی است و رمز نیست):
`0ea503ae88772eea2872ac859b72322b515eb135a8a5f26abc07ca9df50c488f`

هر نسخهٔ بعدی باید دقیقاً همین را نشان بدهد (`apksigner verify --print-certs`). اگر نشان نداد، با کلیدِ دیگری
امضا شده و روی گوشی‌ها نصب نمی‌شود.
