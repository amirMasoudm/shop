/**
 * مهاجرتِ یک‌بارهٔ قیمت‌ها از String به Decimal128
 * ============================================================
 * چرا: Spring Data به‌صورتِ پیش‌فرض BigDecimal را String ذخیره می‌کند. خواندن/نوشتن
 * از اپ سالم است، ولی مونگو رشته و عدد را با هم مقایسه نمی‌کند — پس فیلترِ بازهٔ
 * قیمت هیچ‌چیز مچ نمی‌کند و سورتِ قیمت لغوی می‌شود («۹٬۰۰۰٬۰۰۰» > «۱۰٬۰۰۰٬۰۰۰»).
 *
 * ⚠️ این دیتایِ پول است. قبل از اجرا بک‌آپ (mongodump) الزامی است.
 *
 * اجرا (حالتِ آزمایشی — چیزی نمی‌نویسد):
 *   mongosh ... --eval "var DRY_RUN=true" --file decimal128-migration.js
 * اجرا (واقعی):
 *   mongosh ... --file decimal128-migration.js
 *
 * ویژگی‌ها:
 *  - idempotent: مقادیری که از قبل عددی‌اند دست‌نخورده می‌مانند
 *  - مقدارِ نامعتبر/خالی هرگز صفر نمی‌شود؛ جدا گزارش می‌شود
 *    («قیمتِ صفر» در این پروژه معنایِ خاص دارد: قانونِ ۱۲ و حذف از خروجیِ ترب)
 *  - آرایهٔ تودرتویِ items[] هم پوشش داده می‌شود
 */

var DRY_RUN = (typeof DRY_RUN !== 'undefined') ? DRY_RUN : false;

var PLAN = [
    { coll: 'products', fields: [
        'price', 'onlinePrice', 'discountedPrice', 'basePrice',
        'partnerUnitPrice', 'partnerBulkPrice', 'dollarPrice',
        'torobFloorPrice', 'digikalaFloorPrice'
    ], arrays: [] },

    { coll: 'store_settings', fields: ['rfqThreshold', 'sitePriceFactor'], arrays: [] },

    { coll: 'orders', fields: ['itemsTotal', 'shippingCost', 'totalAmount'],
      arrays: [{ path: 'items', fields: ['unitPrice'] }] },

    { coll: 'rfqs', fields: ['itemsListTotal', 'adminTotalAmount'],
      arrays: [
          { path: 'items',  fields: ['listUnitPrice', 'proposedUnitPrice'] },
          { path: 'offers', fields: ['amount'] }
      ] }
];

var converted = 0, skippedNumeric = 0, docsTouched = 0;
var problems = [];

/** رشتهٔ عددیِ معتبر → Decimal128، وگرنه null (و ثبت در problems). */
function toDecimal(value, where) {
    if (value === null || value === undefined) return null;      // نبودِ مقدار مشکل نیست
    if (typeof value !== 'string') { skippedNumeric++; return null; } // از قبل عددی

    var s = value.trim();
    if (s === '') { problems.push(where + ' → رشتهٔ خالی (دست‌نخورده ماند)'); return null; }
    if (isNaN(Number(s))) { problems.push(where + ' → نامعتبر: "' + s + '" (دست‌نخورده ماند)'); return null; }

    return Decimal128.fromString(String(Number(s)));
}

print(DRY_RUN ? '=== حالتِ آزمایشی (چیزی نوشته نمی‌شود) ===' : '=== اجرایِ واقعی ===');
print('');

PLAN.forEach(function (spec) {
    var coll = db.getCollection(spec.coll);
    var total = coll.countDocuments({});
    if (total === 0) { print(spec.coll + ': خالی — رد شد'); return; }

    var collConverted = 0, collDocs = 0;

    coll.find({}).forEach(function (doc) {
        var set = {};

        // فیلدهایِ سطحِ بالا
        spec.fields.forEach(function (f) {
            var d = toDecimal(doc[f], spec.coll + '.' + f + ' (_id=' + doc._id + ')');
            if (d !== null) { set[f] = d; collConverted++; }
        });

        // آرایه‌هایِ تودرتو — کلِ آرایه بازنویسی می‌شود تا ایندکس‌ها جابه‌جا نشوند
        spec.arrays.forEach(function (arrSpec) {
            var arr = doc[arrSpec.path];
            if (!Array.isArray(arr) || arr.length === 0) return;
            var changed = false;

            arr.forEach(function (item) {
                arrSpec.fields.forEach(function (f) {
                    var d = toDecimal(item[f],
                        spec.coll + '.' + arrSpec.path + '[].' + f + ' (_id=' + doc._id + ')');
                    if (d !== null) { item[f] = d; collConverted++; changed = true; }
                });
            });

            if (changed) set[arrSpec.path] = arr;
        });

        if (Object.keys(set).length > 0) {
            collDocs++;
            if (!DRY_RUN) coll.updateOne({ _id: doc._id }, { $set: set });
        }
    });

    converted += collConverted;
    docsTouched += collDocs;
    print(spec.coll + ': ' + collConverted + ' مقدار در ' + collDocs + ' سند (از ' + total + ')');
});

print('');
print('='.repeat(58));
print('مجموعِ مقادیرِ تبدیل‌شده : ' + converted);
print('اسنادِ تغییرکرده        : ' + docsTouched);
print('از قبل عددی (رد شد)     : ' + skippedNumeric);
print('مقادیرِ مشکل‌دار         : ' + problems.length);
problems.slice(0, 20).forEach(function (p) { print('   ⚠️ ' + p); });
if (problems.length > 20) print('   … و ' + (problems.length - 20) + ' مورد دیگر');
print('='.repeat(58));
