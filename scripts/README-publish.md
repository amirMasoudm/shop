# انتشار محتوا از لوکال به سرور

فقط ۴ کالکشن محتوایی منتقل می‌شوند: `products`, `categories`, `articles`, `landing_sections`.
`orders`, `users`, `comments`, `otp_codes` هرگز لمس نمی‌شوند.

## ۱. دامپ لوکال

```bash
scripts/dump-content.sh
```

خروجی در پوشه‌ی `content-dump/` کنار پروژه ساخته می‌شود (در گیت نیست).

## ۲. انتقال به سرور با FileZilla

پوشه‌ی `content-dump/` را کامل به همان مسیر پروژه روی سرور آپلود کن (کنار `docker-compose.yml`).

## ۳. ری‌استور روی سرور

روی سرور:

```bash
scripts/restore-content.sh
```

اسکریپت قبل از هر تغییری دقیقاً می‌گوید چه کاری می‌کند و منتظر تایید (`yes`) می‌ماند، بعد تعداد اسناد هر ۴ کالکشن را چاپ می‌کند.

> برای تست بدون خطر روی یک دیتابیس آزمایشی: `scripts/restore-content.sh shopdb_restore_test`
