/**
 * اصلاحِ نام/اسلاگِ دو محصولِ میکروتیک — طبقِ تطبیق با لیستِ رسمیِ توزیع‌کنندگان (moq.xlsx)
 * ============================================================
 * موضوع: عنوانِ دو محصول نامِ تجاریِ رسمیِ میکروتیک را نداشت:
 *   - NetMetal 5 (RB921UAGS-5SHPacT-NM) → نامِ رسمی «NetMetal 5SHP triple» است (نسخهٔ سه‌آنتنه)
 *   - Wireless Wire Dish (RBLHGG-60adkit) → نامِ رسمی‌اش «LHG 60G» اصلاً در عنوان نبود
 *
 * این دو تغییر قبلاً روی دیتابیسِ لوکال با PUT /api/v1/products/{id} از پنل اعمال و
 * تأیید شده‌اند. این اسکریپت همان دو تغییر را روی دیتابیسِ سرور می‌زند.
 *
 * idempotent و امن است: قبل از نوشتن چک می‌کند سندِ فعلی هنوز دقیقاً همان مقدارِ
 * قدیمیِ موردِ‌انتظار را دارد. اگر قبلاً اجرا شده یا کسی دستی چیزِ دیگری روی سرور
 * نوشته، آن ردیف رد می‌شود و بازنویسی نمی‌کند (پرینت می‌شود چرا رد شده).
 *
 * ⚠️ فرضِ این اسکریپت: _idِ این دو سند در دیتابیسِ سرور همان چیزی است که در لوکال
 * است (طبقِ روالِ پروژه، دو دیتابیس با dump/restore هم‌گام نگه داشته می‌شوند). اگر
 * پیامِ «پیدا نشد» گرفتید، یعنی این فرض درست نبوده — قبل از کاری، دستی چک کنید.
 *
 * اجرا (آزمایشی — چیزی نمی‌نویسد):
 *   mongosh <connection-string>/<db> --eval "var DRY_RUN=true" --file mikrotik-marketing-name-fix.js
 * اجرا (واقعی):
 *   mongosh <connection-string>/<db> --file mikrotik-marketing-name-fix.js
 */

var DRY_RUN = (typeof DRY_RUN !== 'undefined') ? DRY_RUN : false;

var PLAN = [
    {
        _id: ObjectId('6a5cbdf889e6bfd0ee9627cf'),
        expectName: 'رادیو وایرلس میکروتیک NetMetal 5 (RB921UAGS-5SHPacT-NM)',
        set: {
            name: 'رادیو وایرلس میکروتیک NetMetal 5SHP triple (RB921UAGS-5SHPacT-NM)'
            // اسلاگ دست‌نخورده می‌ماند — از قبل «...netmetal-5shp-triple» بود، درست است
        }
    },
    {
        _id: ObjectId('6a5cbdfe89e6bfd0ee9627d5'),
        expectName: 'لینک بی‌سیم میکروتیک Wireless Wire Dish (مدل RBLHGG-60adkit)',
        set: {
            name: 'لینک بی‌سیم میکروتیک LHG 60G — Wireless Wire Dish (مدل RBLHGG-60adkit)',
            slug: 'لینک-بی‌سیم-میکروتیک-lhg-60g'
        }
    }
];

print(DRY_RUN ? '=== حالتِ آزمایشی (چیزی نوشته نمی‌شود) ===' : '=== اجرایِ واقعی ===');
print('');

var coll = db.getCollection('products');
var applied = 0, skippedAlready = 0, missing = 0, mismatch = 0;

PLAN.forEach(function (item) {
    var doc = coll.findOne({ _id: item._id });
    if (!doc) {
        print('❌ پیدا نشد: _id=' + item._id);
        missing++;
        return;
    }
    if (doc.name === item.set.name) {
        print('⏭  از قبل اعمال شده: ' + item.set.name);
        skippedAlready++;
        return;
    }
    if (doc.name !== item.expectName) {
        print('⚠️  نامِ فعلی با مقدارِ موردِ‌انتظار یکی نیست — رد شد (شاید دستی تغییر کرده):');
        print('     فعلی    : ' + doc.name);
        print('     انتظار  : ' + item.expectName);
        mismatch++;
        return;
    }

    print('✅ ' + doc.name);
    print('   -> ' + item.set.name);
    if (item.set.slug) print('   اسلاگ: ' + doc.slug + ' -> ' + item.set.slug);

    if (!DRY_RUN) coll.updateOne({ _id: item._id }, { $set: item.set });
    applied++;
});

print('');
print('='.repeat(50));
print('اعمال‌شده        : ' + applied);
print('از قبل انجام‌شده : ' + skippedAlready);
print('پیدا نشد         : ' + missing);
print('نامِ غیرمنتظره    : ' + mismatch);
print('='.repeat(50));
