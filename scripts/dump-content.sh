#!/usr/bin/env bash
# دامپ فقط کالکشن‌های محتوایی از مونگوی لوکال (کانتینر shop_mongodb) برای انتشار روی سرور.
# هرگز orders/users/comments/otp_codes/iran_cities/iran_provinces را لمس نمی‌کند (لیست‌سفید زیر).
set -euo pipefail

cd "$(dirname "$0")/.."

if [ -f .env ]; then
  set -a
  source .env
  set +a
fi

: "${MONGO_ROOT_USERNAME:?MONGO_ROOT_USERNAME در .env تنظیم نشده}"
: "${MONGO_ROOT_PASSWORD:?MONGO_ROOT_PASSWORD در .env تنظیم نشده}"

CONTAINER="shop_mongodb"
DB_NAME="shopdb"
OUT_DIR="content-dump"
# اسلش دوتایی: جلوگیری از تبدیل مسیر MSYS در Git Bash ویندوز (داخل کانتینر همان /tmp است)
TMP_IN_CONTAINER="//tmp/content-dump"

# ⚠️ لیست‌سفید — فقط این ۴ کالکشن محتوایی دامپ می‌شوند
COLLECTIONS=(products categories articles landing_sections)

if ! docker ps --format '{{.Names}}' | grep -qx "$CONTAINER"; then
  echo "کانتینر $CONTAINER در حال اجرا نیست. اول docker compose up -d را بزن." >&2
  exit 1
fi

echo "=== دامپ کالکشن‌های محتوایی از $CONTAINER ($DB_NAME) ==="
docker exec "$CONTAINER" rm -rf "$TMP_IN_CONTAINER"

for col in "${COLLECTIONS[@]}"; do
  echo "-> $col"
  docker exec "$CONTAINER" mongodump \
    --username "$MONGO_ROOT_USERNAME" \
    --password "$MONGO_ROOT_PASSWORD" \
    --authenticationDatabase admin \
    --db "$DB_NAME" \
    --collection "$col" \
    --out "$TMP_IN_CONTAINER" \
    --quiet
done

rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"
# در آرگومان docker cp چون با نام کانتینر شروع می‌شود، تبدیل مسیر MSYS رخ نمی‌دهد → اسلش تکی
docker cp "$CONTAINER:${TMP_IN_CONTAINER#/}/$DB_NAME" "$OUT_DIR/$DB_NAME"
docker exec "$CONTAINER" rm -rf "$TMP_IN_CONTAINER"

echo
echo "=== دامپ در $OUT_DIR/$DB_NAME تمام شد ==="
ls -la "$OUT_DIR/$DB_NAME"
echo
for col in "${COLLECTIONS[@]}"; do
  if [ ! -f "$OUT_DIR/$DB_NAME/$col.bson" ]; then
    echo "⚠️ توجه: کالکشن $col در دیتابیس لوکال خالی/ناموجود بود؛ دامپی برایش ساخته نشد."
  fi
done
echo "این پوشه را با FileZilla به سرور منتقل کن (راهنما: scripts/README-publish.md)."
