#!/usr/bin/env bash
# ری‌استور دامپ محتوایی (content-dump/) روی مونگوی مقصد (پیش‌فرض: سرور، کانتینر shop_mongodb).
# فقط کالکشن‌های لیست‌سفید را یکی‌یکی --drop می‌کند؛ هرگز کل دیتابیس را drop نمی‌کند
# و هرگز به orders/users/comments/otp_codes دست نمی‌زند (اصلاً در دامپ نیستند).
#
# استفاده: scripts/restore-content.sh [نام-دیتابیس-مقصد]
#   بدون آرگومان → روی shopdb (پیش‌فرض واقعی سرور) اجرا می‌شود.
#   برای تست: scripts/restore-content.sh shopdb_restore_test
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
IN_DIR="content-dump"
TARGET_DB="${1:-shopdb}"
# اسلش دوتایی: جلوگیری از تبدیل مسیر MSYS در Git Bash ویندوز (داخل کانتینر همان /tmp است)
TMP_IN_CONTAINER="//tmp/content-restore"

# ⚠️ لیست‌سفید — فقط این ۴ کالکشن محتوایی جایگزین می‌شوند
COLLECTIONS=(products categories articles landing_sections)

if [ ! -d "$IN_DIR/shopdb" ]; then
  echo "پوشه‌ی $IN_DIR/shopdb پیدا نشد. اول پوشه‌ی content-dump را با FileZilla کنار پروژه بگذار." >&2
  exit 1
fi

echo "=========================================="
echo "این اسکریپت روی دیتابیس «$TARGET_DB» در کانتینر $CONTAINER اجرا می‌شود."
echo "فقط این کالکشن‌ها REPLACE (drop + restore) می‌شوند: ${COLLECTIONS[*]}"
echo "orders / users / comments / otp_codes / iran_cities / iran_provinces دست‌نخورده می‌مانند"
echo "(اصلاً داخل دامپ نیستند و این اسکریپت هرگز کل دیتابیس را drop نمی‌کند)."
echo "=========================================="
read -rp "مطمئنید؟ برای ادامه دقیقاً yes را تایپ کن: " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
  echo "لغو شد؛ هیچ تغییری اعمال نشد."
  exit 1
fi

docker exec "$CONTAINER" rm -rf "$TMP_IN_CONTAINER"
# در آرگومان docker cp چون با نام کانتینر شروع می‌شود، تبدیل مسیر MSYS رخ نمی‌دهد → اسلش تکی
docker cp "$IN_DIR/shopdb" "$CONTAINER:${TMP_IN_CONTAINER#/}"

for col in "${COLLECTIONS[@]}"; do
  if [ ! -f "$IN_DIR/shopdb/${col}.bson" ]; then
    echo "-> $col در دامپ نبود (احتمالاً در لوکال خالی/ناموجود بوده)؛ رد شد."
    continue
  fi
  BSON="$TMP_IN_CONTAINER/${col}.bson"
  echo "-> restore $col به $TARGET_DB"
  docker exec "$CONTAINER" mongorestore \
    --username "$MONGO_ROOT_USERNAME" \
    --password "$MONGO_ROOT_PASSWORD" \
    --authenticationDatabase admin \
    --db "$TARGET_DB" \
    --collection "$col" \
    --drop \
    "$BSON"
done

docker exec "$CONTAINER" rm -rf "$TMP_IN_CONTAINER"

echo
echo "=== تعداد اسناد در $TARGET_DB بعد از ری‌استور ==="
for col in "${COLLECTIONS[@]}"; do
  COUNT=$(docker exec "$CONTAINER" mongosh --quiet \
    --username "$MONGO_ROOT_USERNAME" --password "$MONGO_ROOT_PASSWORD" \
    --authenticationDatabase admin \
    --eval "print(db.getSiblingDB('$TARGET_DB').getCollection('$col').countDocuments())")
  echo "$col: $COUNT"
done
