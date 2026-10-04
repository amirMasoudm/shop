# تسکِ چت الف — `PUT /api/categories/{id}` فیلدهای نفرستاده را پاک می‌کند و دسته را به ریشه می‌برد

این همان کلاسِ باگی است که در **تسکِ ۷** روی `saveProduct` دیدیم و آنجا با
merge به‌جای replace حل شد. اینجا هنوز باز است.

## دو مشکل، هر دو تأییدشده در کد

### الف) فیلدهای بی‌قید ست می‌شوند

در `CategoryService.update`:

```java
category.setFilterKeys(dto.getFilterKeys());
category.setStripPosition(dto.getStripPosition());
category.setSeoTitle(dto.getSeoTitle());
category.setColor(normalizeColor(dto.getColor()));
category.setSeoDescription(dto.getSeoDescription());
category.setIntroText(dto.getIntroText());
```

هر فراخوانی که این فیلدها را نفرستد، **پاکشان می‌کند**. یعنی یک اسکریپتِ ساده که
فقط می‌خواهد نامِ دسته را عوض کند، سئو و رنگ و filterKeys را نابود می‌کند.

### ب) 🔴 نفرستادنِ `parentId` دسته را به ریشه می‌برد

```java
String target = dto.getNewParentId() != null ? dto.getNewParentId() : dto.getParentId();
if (target != null && target.trim().isEmpty()) target = null;
String current = category.getParentId();
boolean parentChanged = (target == null) ? (current != null) : !target.equals(current);
if (parentChanged) return moveCategory(id, target);
```

اگر هیچ‌کدام نیاید → `target = null` → برای هر دستهٔ **غیرریشه**
`parentChanged` درست می‌شود → `moveCategory(id, null)`.

**یعنی درختِ دسته‌ها با یک PUTِ بی‌گناه به هم می‌ریزد.** این بدترینِ دو مشکل است،
چون برخلافِ سئو، بازسازیِ درخت دستی و پرخطاست.

⚠️ همین تله در `CategoryService.reorder` هم هست: `c.setParentId(pid)` را بی‌قید
از پیلود می‌گیرد.

---

## 🔴 چیزی که نباید با «فقط اگر null نبود ست کن» خراب شود

**`stripPosition` عمداً بی‌قید است.** کامنتِ خودِ کد می‌گوید:

> ⚠️ برخلافِ position این یکی بی‌قید ست می‌شود: خالی‌گذاشتنِ فیلد در پنل یعنی
> «برگرد به ترتیبِ منو»، و این باید پاک‌شدنی باشد.

پس رفعِ سرراستِ «اگر null بود ست نکن» این قابلیت را می‌کشد: کاربر دیگر نمی‌تواند
تگِ یک دسته را از نوارِ زیرِ جست‌وجو بردارد. همین دربارهٔ `seoTitle` و `color` هم
صادق است — پاک‌کردنشان از پنل باید ممکن بماند.

**پس مسئله «null ننویس» نیست؛ مسئله این است که سرور فرقِ این دو را نمی‌فهمد:**

```
{"name": "..."}                  -> «stripPosition ra dast nazan»
{"name": "...", "stripPosition": null} -> «stripPosition ra pak kon»
```

هر دو الان به یک شکل می‌رسند (`getStripPosition() == null`).

---

## رفعِ پیشنهادی

**تشخیصِ «نیامده» از «صریحاً null».** دو راهِ جاافتاده:

1. `JsonNullable<T>` (کتابخانهٔ `openapi-jackson-nullable`) برای همان فیلدهایی
   که باید پاک‌شدنی بمانند.
2. یا بدونِ وابستگیِ تازه: در DTO یک `Set<String> presentFields` نگه دار و با
   `@JsonAnySetter` یا یک `@JsonCreator` پرش کن؛ سپس فقط فیلدهای حاضر ست شوند.

هر کدام را انتخاب کردی، **معیار این است:**

```
field nayamade          -> maghdar-e ghabli dast-nakhorde bemanad
field amade ba null     -> pak shavad
field amade ba maghdar  -> set shavad
```

و برای `parentId`: اگر **نیامده** بود، والد **دست نخورد** — نه اینکه به ریشه
برود. فقط `parentId`ِ صریحاً null یعنی «ببرش به ریشه».

⚠️ `newParentId` هم همین منطق را بگیرد، و اولویتش نسبت به `parentId` حفظ شود.

---

## آزمون‌ها

**باید سرِ جایش بماند (رگرسیون):**

```
PUT {"name":"X"}                       -> seoTitle, color, filterKeys, stripPosition, parentId hame dast-nakhorde
PUT {"name":"X","stripPosition":null}  -> stripPosition pak shavad
PUT {"name":"X","seoTitle":null}       -> seoTitle pak shavad
PUT {"name":"X","parentId":null}       -> be rishe beravad (raftar-e fe'li, amdi)
PUT {"name":"X","parentId":"<ID>"}     -> montaghel shavad + ancestors/level dorost
```

**و بعدش در پنل دستی چک شود:** ساختنِ دسته، ویرایشِ نام، پاک‌کردنِ تگِ نوارِ
جست‌وجو (`stripPosition`)، و جابه‌جاییِ یک زیردسته — چون پنل خودش همیشه فرمِ
کامل می‌فرستد و ممکن است باگ را بپوشاند.

⚠️ `reorder` را هم با همان منطق یکدست کن یا دستِ‌کم در جاواداکش صریح بنویس که
`parentId` اجباری است.

---

## چرا حالا

میزِ کارِ قیمت‌گذاری همین روزها با اسکریپت روی دادهٔ زنده کار می‌کند. یک
فراخوانِ اشتباه به این اندپوینت، هم سئوی دسته‌ها را می‌برد هم درخت را — و درخت
چیزی است که کلِ آدرس‌دهیِ فروشگاه رویش سوار است.

⚠️ **دامنه فقط `CategoryService` و DTOاش.** به `ProductService` و
`LegacyPathFallback` دست نزن.
