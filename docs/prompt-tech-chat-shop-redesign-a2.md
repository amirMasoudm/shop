# تسکِ چتِ الف — بازطراحیِ صفحهٔ فروشگاه با سیستمِ «الف۲ — کنتراست»

**فایل:** `src/main/resources/templates/CL.html` (و یک فایلِ جدید `tokens.css`)
**بوم طراحی:** <https://claude.ai/artifact/NNxLx74v13uwWm63SDo9Yg> — صفحهٔ «فروشگاه — بازطراحیِ دقیق»
**سندِ مرجعِ سیستم:** `docs/design-system-decision.md`
**سقف:** یک نشست. اگر دیدی از این بیشتر می‌شود، **نصفه‌کاره تحویل نده** — بایست و گزارش بده کجا ماندی.

---

## 🔴 قانونِ اولِ این تسک

> **هیچ عنصر و هیچ رفتاری از صفحهٔ فعلی حذف نمی‌شود. هیچ‌کدام.**

این یک بازطراحیِ **ظاهری** است، نه بازنویسیِ کارکرد. تو **CSS و مارکاپِ کارت** را عوض می‌کنی؛
منطقِ جاوااسکریپت دست‌نخورده می‌ماند مگر جایی که این سند صریحاً گفته باشد.

اگر وسطِ کار به رفتاری رسیدی که با طرحِ جدید جور درنمی‌آید، **حذفش نکن** — کارش را نگه دار،
ظاهرش را عوض کن، و در گزارشِ پایانی بنویس کجا مصالحه کردی.

---

## سیاههٔ اجباری — قبل از تحویل تک‌تک را تیک بزن

این فهرست از خودِ `CL.html` استخراج شده. **هر موردی که بعد از تغییرِ تو کار نکند، یعنی تسک ناتمام است.**

### الف) عناصرِ نمای فهرست

| شناسه | چیست | بعد از تغییر باید |
|---|---|---|
| `loadingSkeleton` | اسکلتِ بارگذاری (دو کارتِ ۳۲۰px) | با ابعادِ کارتِ **جدید** هماهنگ شود، نه قدیم |
| `bannerSlider` / `bannerTrack` | اسلایدرِ بنرِ خانه — **عنصرِ LCP و کاملاً SSR** | دست‌نخورده بماند؛ `th:if` و نبودِ `display:none` روی اسلایدِ اول حیاتی است |
| `imageRowBanners` | ردیف‌های بنرِ تصویری + لایت‌باکسشان | کار کند |
| `landingSections` | سکشن‌های داینامیکِ صفحهٔ خانه | کار کند |
| `latestSectionHeader` | تیترِ «جدیدترین محصولات» | با تایپِ جدید هماهنگ شود |
| `productsGrid` | شبکهٔ محصولات | به شبکهٔ هم‌مرز تبدیل شود |
| `loadMoreBtn` | «نمایش محصولات بیشتر» | بماند — **صفحه‌بندیِ شماره‌دار نساز** |
| `catIntroText` | متنِ معرفیِ دسته، **زیرِ** محصولات | همان‌جا بماند، بالا نیاید |
| `filterPanel` / `filterContainer` | پنلِ «فیلتر هوشمند» دسکتاپ | بماند |
| `filterDrawer` / `mobFilterBody` / `filterFab` | همان فیلتر روی موبایل (کشو + دکمهٔ شناور) | بماند |
| `searchInput` / `searchClearBtn` / `searchCatSelect` | نوارِ جست‌وجو و محدودکردن به دسته | بماند |
| `desktopMenu` / `menuScrollWrap` / `menuArrowLeft` / `menuArrowRight` | منویِ افقیِ دسته‌ها با فلش‌های اسکرول | بماند |
| `mobMenuBtn` / `menuDrawer` / `mobileMenuBody` | منویِ موبایل با زیردسته‌های تاشو | بماند |
| `cartDrawer` / `cartBody` / `cartFooter` / `cartCount` | سبد خرید | بماند |
| `authWidget` / `authOverlay` | ورود و ثبت‌نام | بماند |
| `ssr-category` / `ssr-product` / `ssr-course` | **بلوک‌های SSR برای سئو** | ⚠️ **اصلاً دست نزن** — پایین توضیح داده شده |

### ب) رفتارها

هر کدام را بعد از تغییر **عملاً امتحان کن**، نه اینکه فرض کنی کار می‌کند:

- `loadCat` · `loadHome` · `loadSection` · `loadCourseCat` — سوییچِ فید
- `runSearch` · `onSearchInput` · `clearSearch` · `resetSearchUi` · `syncSearchPlaceholder` · `populateSearchCats`
- `setupFilters` · `runFilter` · `clearFilters` — ساختِ فیلتر از `filterKeys`ِ دسته
- `renderGrid` · `renderGridTrimmed` · `renderCard` · `loadMoreProducts`
- `captureFeedSnapshot` · `feedKey` · `startFeed` — **حفظِ وضعیتِ لیست هنگام برگشت از صفحهٔ محصول**
- `openProductDetail` · `closeProductDetail` · `openCourseDetail`
- `addToCart` · `changeQty` · `renderCart` · `saveCart` · `updateCartCount` · `checkout`
- `requestStockNotify` · `requestRfq`
- `toggleDr` · `openDrawer` · `closeAll` · `toggleSub`
- `setBannerSlider` · `startBannerRotation` · `stopBannerRotation` · `goToBannerSlide` · `manualBannerNav` · `initBannerInteractions` — چرخشِ خودکار، ناوبریِ دستی، و درگ
- `openLightbox` · `lbZoom` · `lbStep` · `openIrbLightbox` · `irbLbNav`
- `enhanceHScrollers` · `hsSyncAll` · `setupMenuScroll` · `updateMenuArrows` · `scrollMenu`
- `gaEvent` / `gtag` — **رویدادهای تحلیلی نپرند**
- `getGridColumnCount` — ⚠️ به تعدادِ ستون وابسته است؛ با گریدِ جدید **بازبینی‌اش کن**

### ج) نقاطِ شکستِ ریسپانسیو

`992px` · `768px` · `700px` — هر سه باید بعد از تغییر بررسی شوند.
`@media (hover: hover) and (pointer: fine)` و `@media (prefers-reduced-motion: reduce)`
**از قبل در این فایل هستند** — نگهشان دار و قواعدِ hoverِ جدید را هم داخلِ همان گارد بگذار.

---

## آنچه باید بسازی

### ۱. `tokens.css`

فایلِ جدید در `src/main/resources/static/css/tokens.css`، و لینک در `<head>` همین یک قالب
(فعلاً فقط `CL.html`؛ بقیهٔ صفحه‌ها در تسک‌های بعدی).

```css
:root {
  --primary:#1b4f8a; --primary-lt:#2563ad; --accent:#e8861a; --danger:#e74c3c;
  --ink:#0b1119; --ink-2:#46536b; --ink-3:#94a2b8;
  --dark:#101826; --dark-2:#1c2942; --on-dark:#8fa3c0;
  --surface:#fff; --line:#e9edf2; --line-2:#f2f5f8; --ok:#16a34a;
  --radius:0;
  --dur:140ms; --ease:cubic-bezier(.2,.8,.3,1);
  --s1:6px; --s2:10px; --s3:14px; --s4:18px;
  --ctl-h:36px;
}
```

⚠️ متغیرهای فعلیِ `:root` در `CL.html` (`--text`, `--text-mid`, `--text-muted`, `--border`,
`--bg`, `--radius`, `--radius-lg`, `--hdr`) **حذف نشوند** — جاهای زیادی به آن‌ها ارجاع دارند.
به‌جایش مقدارشان را به توکنِ جدید نگاشت کن:
`--text: var(--ink)` · `--text-mid: var(--ink-2)` · `--text-muted: var(--ink-3)` ·
`--border: var(--line)` · `--radius: 0`.
این‌طور کلِ صفحه با یک تغییر همراه می‌شود و هیچ ارجاعی نمی‌شکند.

### ۲. فونت

- تیترها و عنوان‌ها: **Noto Kufi Arabic** (وزن ۷۰۰)
- متن، رابط، و **نامِ محصول**: **IBM Plex Sans Arabic**
- عدد و قیمت: **IBM Plex Mono** با `font-variant-numeric: tabular-nums`

> نامِ محصول عمداً کوفی **نیست** — کدِ لاتینِ داخلِ نام (`RB5009UPr+S+IN`) در کوفی بد می‌نشیند.

### ۳. کارتِ محصول — تابعِ `renderCard`

مارکاپِ جدید، **با همان دادهٔ فعلی**. ترتیب: نشان‌ها → عکس → خطِ مشخصات → نام → قیمت → وضعیت → دکمه.

- شبکه: `gap:1px` روی گریدی که `background: var(--line)` دارد — کارت‌ها مرزِ مشترک می‌گیرند
- `border-radius: 0` · بدونِ سایه
- **حذف شود:** `transform: translateY(-5px)` و `box-shadow` روی hover.
  جایش فقط `background` عوض شود، و `:active` هم داشته باشد
- عکس: `aspect-ratio: 1`، چسبیده به لبهٔ سلول، بدونِ padding و بدونِ radius
- نشان‌ها: مربع، چسبیده به گوشهٔ بالا-راست، بدونِ radius — رنگِ تخفیف `--danger`، رنگِ نشانِ بخش همان `stickerColor`ِ ادمین
- دکمه: تمامِ عرض، چسبیده به کفِ کارت، ارتفاعِ ۳۲px (موبایل **۴۴px**)

### ۴. سه افزودنیِ کوچک

اینها جدیدند ولی هیچ‌چیزی را حذف نمی‌کنند:

**الف) خطِ مشخصات زیرِ نام** — از `p.specifications` و محدود به `filterKeys`ِ همان دسته،
حداکثر دو مقدار، با مونوی ریز. اگر خالی بود خطش رندر نشود.

**ب) وضعیتِ «در راه»** — فیلدِ `incomingStock` در مدل هست ولی هیچ‌جای سایت نشان داده نمی‌شود.
منطق: `stock > 0` → سبز «موجود» · `stock == 0 && incomingStock > 0` → نارنجی «در راه» ·
وگرنه خاکستری «ناموجود». **نارنجی فقط همین‌جا و زیرِ تبِ فعال مجاز است.**

**ج) شمارشِ کنارِ هر فیلتر** — تعدادِ محصولی که با آن گزینه می‌ماند، در `setupFilters`.

### ۵. مرتب‌سازی — ⏸ در این تسک نه

در طرح هست ولی **الان نساز**. کنترلِ مرتب‌سازی در صفحه وجود ندارد و ساختنش کارِ منطقی است
نه ظاهری. تسکِ جداگانه می‌گیرد. اگر جا گذاشتی که بعداً اضافه شود کافی است.

---

## ⚠️ بلوک‌های SSR — دست نزن

`ssr-product` · `ssr-course` · `ssr-category` فقط برای خزندهٔ گوگل رندر می‌شوند و
`position:fixed; z-index:5000` دارند. ظاهرشان برای کاربر دیده نمی‌شود، ولی **محتوا و
ساختارشان سرمایهٔ سئوی ماست**. در این تسک کاملاً بی‌تغییر بمانند.

همین‌طور: اسلایدِ اولِ بنر **نباید** `display:none` بگیرد — باید در HTMLِ خام دیده شود.

---

## تحویل

۱. قبل از شروع، از `CL.html` یک کپی بگیر تا مقایسه ممکن باشد
۲. بعد از تغییر، **سیاههٔ بالا را مورد به مورد در مرورگر امتحان کن** — هر سه نقطهٔ شکست
۳. گزارشِ پایانی شاملِ:
   - کدام موارد از سیاهه تیک خوردند و کدام نه
   - هر جایی که مجبور به مصالحه شدی
   - اسکرین‌شاتِ نمای دسته در دسکتاپ و موبایل
۴. **کامیت نکن** تا مدیر ببیند

---

**اگر چیزی از این سند با کدِ واقعی نخواند، کد درست است نه سند — بایست و بپرس.**
