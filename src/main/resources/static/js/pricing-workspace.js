/**
 * میزِ کارِ قیمت‌گذاری — ماژولِ مشترکِ UI.
 *
 * عمداً یک فایلِ واحد است و در «هر دو» پنل استفاده می‌شود (پنلِ ادمین و پنلِ فروشِ
 * حضوری). اگر جدولِ میزِ کار دو نسخه می‌شد، هر تغییرِ بعدی باید دو جا انجام می‌گرفت
 * و یکی‌شان عقب می‌ماند.
 *
 * وابستگی‌هایی که میزبان باید فراهم کند: axios، Swal، escapeHTML، toggleLoader،
 * serverError و ثابتِ API.
 *
 * مرزِ دسترسیِ واقعی سمتِ سرور است (/api/v1/pricing/**)؛ چیزهایی که اینجا پنهان
 * می‌شوند فقط برایِ تجربهٔ کاربری‌اند.
 */
(function () {

  const WORKSPACE_HTML = String.raw`
        <div class="flex flex-col md:flex-row md:justify-between md:items-center gap-3 mb-4">
            <div>
                <h2 class="text-xl md:text-2xl font-bold text-gray-800">میز کار قیمت‌گذاری</h2>
                <p class="text-xs text-gray-500 mt-1">
                    همه مبالغ به <b>تومان</b> است. برای ویرایش روی خانه کلیک کنید؛ تغییرات با «ذخیره تغییرات» یکجا ثبت می‌شود.
                </p>
                <p class="text-[11px] text-gray-500 mt-1 leading-5">
                    <b>کفِ ترب چیست:</b> دکمهٔ «ترب» یک‌راست صفحهٔ همان محصول در ترب را باز می‌کند.
                    آن عددِ بزرگِ بالایِ صفحه (کنارِ مشخصاتِ محصول) کفِ ما نیست.
                    کفِ ترب <b>اولین ردیف از فهرستِ فروشگاه‌هاست که پایینِ همان مشخصات می‌آید</b>.
                    همان ردیف را کپی کنید و در خانهٔ «کف ترب» <b>Ctrl+V</b> بزنید؛ لازم نیست عدد را
                    تایپ کنید — نامِ فروشگاه و «تومان» و بقیه خودشان کنار می‌روند.
                </p>
            </div>
            <div class="flex flex-wrap gap-2">
                <button id="pricing-add-product" onclick="addProductFromPricing()"
                        class="hidden px-4 py-2 rounded-lg bg-indigo-600 text-white text-sm font-bold shadow"
                        title="محصولِ تازه با همین مودالِ محصولات ساخته می‌شود">＋ افزودن محصول</button>
                <button onclick="refreshAllDigikala()" class="px-4 py-2 rounded-lg border text-gray-600 hover:bg-gray-50 text-sm font-bold" title="فقط محصولاتی که قبلاً به دیجی‌کالا وصل شده‌اند">↻ به‌روزرسانی کف دیجی‌کالا</button>
                <button id="pricing-mikrotik-btn" onclick="syncMikrotikPrices()" class="px-4 py-2 rounded-lg border text-gray-600 hover:bg-gray-50 text-sm font-bold" title="قیمتِ پیشنهادیِ خودِ میکروتیک (MSRP) را از mikrotik.com می‌گیرد و پیش از نوشتن به تأیید می‌دهد">$ مرجعِ دلاریِ میکروتیک</button>
                <button onclick="exportPricingExcel()" class="px-4 py-2 rounded-lg border text-gray-600 hover:bg-gray-50 text-sm font-bold">⬇ خروجی اکسل</button>
                <button id="pricing-save-btn" onclick="savePricingChanges()" disabled
                        class="px-5 py-2 rounded-lg bg-green-600 text-white font-bold text-sm shadow disabled:opacity-40 disabled:cursor-not-allowed">
                    💾 ذخیره تغییرات (<span id="pricing-dirty-count">0</span>)
                </button>
            </div>
        </div>

        <div id="pricing-readonly-note" class="hidden mb-3 p-3 rounded-lg bg-amber-50 border border-amber-200 text-amber-800 text-xs font-bold">
            شما دسترسی «فقط مشاهده» دارید؛ ویرایش قیمت‌ها برای نقش شما فعال نیست.
        </div>

        <div class="mb-2 flex flex-wrap items-center gap-2">
            <input type="text" id="pricing-search" placeholder="جستجوی نام محصول…" oninput="renderPricingRows()"
                   class="flex-1 min-w-[180px] p-2 border rounded-lg outline-none text-sm">
            <select id="pricing-cat" onchange="renderPricingRows()"
                    class="p-2 border rounded-lg bg-white outline-none text-sm"
                    title="درصدِ تغییرات روی همین نما می‌نشیند، پس فیلترِ دسته یعنی «درصد بر اساسِ دسته»">
                <option value="">همهٔ دسته‌ها</option>
            </select>
            <label class="flex items-center gap-2 text-xs text-gray-600 bg-white border rounded-lg px-3 py-2">
                <input type="checkbox" id="pricing-hide-oos" onchange="renderPricingRows()"> فقط موجودها
            </label>
            <label class="flex items-center gap-2 text-xs text-gray-600 bg-white border rounded-lg px-3 py-2">
                <input type="checkbox" id="pricing-only-flagged" onchange="renderPricingRows()"> فقط «خیلی بفروشید»
            </label>
            <div id="pricing-pct-box" class="flex items-center gap-2 bg-white border rounded-lg px-3 py-1.5"
                 title="درصد را وارد کن و «اعمال» بزن؛ تغییر روی همین ردیف‌هایِ نما می‌نشیند و تا «ذخیره» قطعی نیست">
                <span class="text-xs text-gray-600 font-bold">درصد تغییرات</span>
                <input type="text" id="pricing-pct" inputmode="decimal" placeholder="مثلاً ‎5‎ یا ‎-3‎"
                       class="w-24 p-1.5 border rounded text-center text-sm font-bold" dir="ltr">
                <button onclick="applyPricingPercent()"
                        class="px-3 py-1.5 rounded bg-indigo-600 text-white text-xs font-bold">اعمال</button>
            </div>
        </div>

        <style>
            /* 🔴 همهٔ خانه‌ها از بالا تراز می‌شوند. پیش از این تراز وسط بود، پس ردیفی که
               زیرِ قیمتش یادداشتِ «دستی» داشت بلندتر می‌شد و خودِ فیلد نسبت به بقیهٔ
               ردیف بالاتر می‌افتاد — همان چیزی که در پنل دیده می‌شد. */
            #pricing-body td { vertical-align: top; }
            /* یادداشتِ زیرِ فیلد: ریز، بی‌جعبه، و بدونِ اثر روی ارتفاعِ ردیف */
            .pricing-note {
                display: flex; align-items: center; gap: .25rem;
                font-size: 9px; line-height: 1.3; margin-top: 2px; white-space: nowrap;
            }
            .pricing-note button { font-size: 9px; }

            /* دستگیرهٔ جابه‌جایی — فقط از همین‌جا می‌شود ردیف را کشید، نه از
               کلِ ردیف؛ وگرنه کشیدنِ متنِ داخلِ اینپوت‌ها با درگِ ردیف قاطی می‌شد. */
            .pw-handle {
                cursor: grab; color: #cbd5e1; user-select: none;
                font-size: 15px; line-height: 1; padding: 2px;
            }
            .pw-handle:hover { color: #6366f1; }
            .pw-handle:active { cursor: grabbing; }
            /* وقتی جست‌وجو/فیلتر فعال است جابه‌جایی قفل می‌شود (دلیلش در JS) */
            #pricing-body.pw-locked .pw-handle { cursor: not-allowed; color: #e2e8f0; }

            /* ظاهرِ ردیف در حالِ کشیدن و جایِ خالیِ مقصد */
            .pw-ghost { opacity: .35; }
            .pw-chosen { background: #eef2ff !important; }

            /* نوارِ باریکِ رنگِ دسته، چسبیده به لبهٔ راستِ ردیف (شروعِ سطر در RTL).
               ⚠️ کادرِ دورش پررنگ است چون «سفید» یک رنگِ معتبرِ دسته است و روی
               زمینهٔ سفیدِ جدول بی‌کادر اصلاً دیده نمی‌شد — یعنی سفید و بی‌رنگ
               به چشم یکی می‌شدند، درست همان چیزی که نباید. */
            .pw-cat-bar {
                display: inline-block; width: 5px; height: 26px;
                border-radius: 2px; vertical-align: middle;
                border: 1px solid rgba(15, 23, 42, .32);
                box-shadow: 0 0 0 1px rgba(255, 255, 255, .7);
            }
            .pw-num { color: #94a3b8; font-size: 10px; font-variant-numeric: tabular-nums; }

            /* خوانایی — خواستهٔ مالک: نام و قیمت‌ها درشت‌تر و پررنگ‌تر.
               tabular-nums یعنی رقم‌ها هم‌عرض‌اند و ستونِ اعداد تکان نمی‌خورد. */
            .pw-name { font-size: 13.5px; font-weight: 700; color: #0f172a; line-height: 1.75; }
            #pricing-body input[data-field] {
                font-size: 13.5px; font-weight: 700; font-variant-numeric: tabular-nums;
            }
            .pw-ro { font-size: 13px; font-weight: 700; font-variant-numeric: tabular-nums; }
            /* موجودی‌ها — خواستهٔ مالک: بولدتر. صفر عمداً کم‌رنگ می‌ماند تا
               «هست ولی کم» از «نیست» فرق کند. */
            /* ⚠️ با #pricing-body قید خورده‌اند چون خانهٔ موجودی حالا خودش input
               است و قاعدهٔ عمومیِ اینپوت‌ها (وزنِ ۷۰۰) ویژگیِ بالاتری دارد. */
            #pricing-body input.pw-stock,
            .pw-stock { font-size: 13px; font-weight: 800; font-variant-numeric: tabular-nums; color: #334155; }
            #pricing-body input.pw-stock-zero,
            .pw-stock-zero { font-size: 13px; font-weight: 700; color: #cbd5e1; }
            .pw-excluded { opacity: .55; }

            /* ⚠️ کفِ بازار وقتی باکسِ خریدِ دیجی‌کالا دستِ خودمان است، رقیب نیست:
               خودِ ماییم. آبی یعنی «برای جلوزدن از خودت قیمت پایین نیاور». */
            .pw-ours input, .pw-ours .pw-ro {
                color: #1d4ed8; border-color: #93c5fd; background: #eff6ff;
            }
            .pw-ours-tag { font-size: 9px; font-weight: 800; color: #1d4ed8; white-space: nowrap; }
        </style>
        <div class="bg-white rounded-xl border shadow-sm overflow-x-auto" style="max-height:70vh; overflow-y:auto;">
            <table class="w-full text-right min-w-[1250px]">
                <thead class="bg-gray-50 text-gray-500 text-[11px] sticky top-0 z-10">
                <tr>
                    <th class="p-2 w-10 text-center" title="شمارهٔ ردیف در همین نما">#</th>
                    <th class="p-2 w-8" title="برایِ جابه‌جایی، ردیف را از این دستگیره بکشید"></th>
                    <th class="p-2 w-10 text-center" title="تیک‌خورده = «درصد تغییرات» این ردیف را رد می‌کند">استثنا</th>
                    <th class="p-2">نام محصول</th>
                    <th class="p-2" title="کارتِ همین محصول در فروشگاه">کارت</th>
                    <th class="p-2">قیمت سایت (تومان)</th>
                    <th class="p-2">همکار تک (تومان)</th>
                    <th class="p-2" title="قیمت عمده / خرید چندتایی">فروش تعدادی (تومان)</th>
                    <th class="p-3 w-20" title="قیمتِ پیشنهادیِ خودِ میکروتیک (MSRP) — فقط برایِ محصولاتِ میکروتیک">مرجع $</th>
                    <th class="p-2 w-14 text-center" title="فقط‌خواندنی — منبع: ورود دسته‌ای">اصفهان</th>
                    <th class="p-2 w-14 text-center" title="فقط‌خواندنی — منبع: ورود دسته‌ای">تهران</th>
                    <th class="p-2 w-14 text-center" title="خریداری‌شده ولی نرسیده — در موجودی فروش شمرده نمی‌شود">در راه</th>
                    <th class="p-2" title="کمترین قیمت رقبا در ترب (تومان)">کف ترب (تومان)</th>
                    <th class="p-2" title="کمترین قیمت رقبا در دیجی‌کالا (تومان)">کف دیجی‌کالا (تومان)</th>
                    <th class="p-2 w-12 text-center" title="همان ستون «خیلی بفروشید» شیت شرکت که با رنگ نارنجی مشخص شده بود">بفروشید</th>
                </tr>
                </thead>
                <tbody id="pricing-body" class="divide-y text-xs"></tbody>
            </table>
        </div>
        <p class="text-[11px] text-gray-400 mt-2">
            موجودی‌ها فقط‌خواندنی‌اند — منبع: ورود دسته‌ای (به‌زودی هلو). «در راه» عمداً در موجودی فروش شمرده نمی‌شود.
        </p>
    `;
  const LOG_HTML_FULL  = String.raw`
        <h2 class="text-xl md:text-2xl font-bold text-gray-800 mb-1">لاگ فعالیت</h2>
        <p class="text-xs text-gray-500 mb-4">این لاگ حذف‌نشدنی است — حتی توسط ادمین.</p>

        <div class="flex flex-col md:flex-row gap-2 mb-3">
            <select id="log-entity-type" onchange="fetchActivityLogs()"
                    class="p-2.5 border rounded-lg outline-none text-sm bg-white">
                <option value="">همه رویدادها</option>
                <option value="PRODUCT">قیمت‌گذاری محصولات</option>
                <option value="SETTINGS">تنظیمات فروشگاه</option>
            </select>
            <input type="text" id="log-username" placeholder="نام کاربری (خالی = همه)"
                   class="flex-1 p-2.5 border rounded-lg outline-none text-sm">
            <input type="date" id="log-from" class="p-2.5 border rounded-lg outline-none text-sm">
            <input type="date" id="log-to" class="p-2.5 border rounded-lg outline-none text-sm">
            <button onclick="fetchActivityLogs()" class="px-5 py-2 rounded-lg bg-indigo-600 text-white font-bold text-sm">جستجو</button>
        </div>

        <div class="bg-white rounded-xl border shadow-sm overflow-x-auto">
            <table class="w-full text-right min-w-[800px]">
                <thead class="bg-gray-50 text-gray-500 text-[11px]">
                <tr>
                    <th class="p-3">زمان</th>
                    <th class="p-3">کاربر</th>
                    <th class="p-3">رویداد</th>
                    <th class="p-3">محصول</th>
                    <th class="p-3">فیلد</th>
                    <th class="p-3">از</th>
                    <th class="p-3">به</th>
                    <th class="p-3">مسیر</th>
                </tr>
                </thead>
                <tbody id="log-body" class="divide-y text-xs"></tbody>
            </table>
        </div>
    `;

  // پنلِ فروش: بدونِ سلکتِ نوعِ رویداد و با عنوانِ مناسبِ خودش.
  // محدودسازیِ واقعی سمتِ سرور است (اندپوینتِ /logs/products)، این فقط UI است.
  const LOG_HTML_PRODUCTS = LOG_HTML_FULL
      .replace(/<select id="log-entity-type"[\s\S]*?<\/select>/, '')
      .replace('لاگ فعالیت', 'تاریخچه تغییرات قیمت')
      .replace('این لاگ حذف‌نشدنی است — حتی توسط ادمین.',
               'تغییرات قیمت محصولات — تا بدانید چرا قیمت یک کالا عوض شده.');

  let logProductsOnly = false;

  // ═════════ رنگِ دسته‌ها ═════════
  // درختِ دسته یک بار گرفته و به نقشهٔ تخت تبدیل می‌شود تا رنگِ هر محصول با
  // یک lookup پیدا شود. رنگِ ارثی هم همین‌جا حساب و کش می‌شود.
  let catById = {};
  let resolvedColor = {};
  let catRoots = [];

  /** کشویِ فیلترِ دسته را پر می‌کند و انتخابِ فعلی را نگه می‌دارد. */
  function fillCategoryFilter() {
    const sel = document.getElementById('pricing-cat');
    if (!sel) return;
    const keep = sel.value;
    sel.innerHTML = '<option value="">همهٔ دسته‌ها</option>'
      + catRoots.map(c => `<option value="${c.id}">${escapeHTML(c.name)}</option>`).join('');
    sel.value = keep;
  }

  /** آیا این ردیف زیرِ همان دسته (یا یکی از زیردسته‌هایش) است؟ گاردِ حلقه دارد. */
  function rowInCategory(r, catId) {
    if (!catId) return true;
    let cur = r && r.categoryId ? catById[r.categoryId] : null, guard = 0;
    while (cur && guard++ < 25) {
      if (cur.id === catId) return true;
      cur = cur.parentId ? catById[cur.parentId] : null;
    }
    return false;
  }

  async function loadCategoryColors() {
    try {
      const res = await axios.get(`${API}/categories/tree?type=ONLINE`);
      catById = {};
      resolvedColor = {};
      (function flat(list) {
        (list || []).forEach(c => { catById[c.id] = c; flat(c.children); });
      })(res.data || []);
      catRoots = (res.data || []).map(c => ({id: c.id, name: c.name}));
      fillCategoryFilter();
    } catch (e) {
      // رنگ‌ها تزئینی‌اند؛ اگر نیامدند جدول باید کارِ خودش را بکند
      catById = {};
      resolvedColor = {};
    }
  }

  /**
   * رنگِ مؤثرِ یک دسته: رنگِ خودش، وگرنه رنگِ نزدیک‌ترین والدِ رنگ‌دار.
   * اگر هیچ نیایی رنگ نداشت، '' برمی‌گردد و هیچ نواری کشیده نمی‌شود.
   * ⚠️ گاردِ زنجیرهٔ حلقه‌ای دارد: دادهٔ خرابِ parentId نباید مرورگر را قفل کند.
   */
  function categoryColor(catId) {
    if (!catId) return '';
    if (resolvedColor[catId] !== undefined) return resolvedColor[catId];
    let cur = catById[catId], guard = 0, out = '';
    while (cur && guard++ < 25) {
      if (cur.color) { out = cur.color; break; }
      cur = cur.parentId ? catById[cur.parentId] : null;
    }
    resolvedColor[catId] = out;
    return out;
  }

  function catColorCell(r) {
    const color = categoryColor(r.categoryId);
    if (!color) return '';
    const name = (catById[r.categoryId] && catById[r.categoryId].name) || '';
    return `<span class="pw-cat-bar" style="background:${color}" title="${escapeHTML(name)}"></span>`;
  }

  window.PricingWorkspace = {
    mountWorkspace(el) { el.innerHTML = WORKSPACE_HTML; },
    mountLog(el, opts) {
      logProductsOnly = !!(opts && opts.productsOnly);
      el.innerHTML = logProductsOnly ? LOG_HTML_PRODUCTS : LOG_HTML_FULL;
    },
    isProductsOnly() { return logProductsOnly; }
  };


    // ==========================================
    // میز کار قیمت‌گذاری
    // ==========================================
    let pricingRows = [];
    // همه‌ی نقش‌هایِ کارکنان (ADMIN/PRICER/SALES) ویرایش دارند؛ فقط UI است،
    // مرزِ واقعی سمتِ سرور است.
    let pricingCanEdit = true;
    // «فروش تعدادی» فقط برایِ ADMIN/PRICER — از /pricing/capabilities خوانده می‌شود
    let canEditBulkPrice = true;
    // ضریبِ قیمتِ سایت برای پیش‌نمایشِ لحظه‌ای. محاسبهٔ معتبر همچنان سمتِ سرور است؛
    // این فقط برای این است که کارشناس قبل از «ذخیره» هم نتیجه را ببیند.
    let pricingFactor = 1.08;
    const pricingDirty = new Map(); // key: `${id}|${field}` → {id, field, value}

    // برایِ نقشِ کارشناسِ قیمت‌گذاری/فروش: فقط تبِ میزِ کار نشان داده شود، نه کلِ داشبورد
    function restrictPanelToPricingWorkspace() {
        document.querySelectorAll('nav .sidebar-link').forEach(el => {
            const m = (el.getAttribute('onclick') || '').match(/switchTab\('([^']+)'\)/);
            if ((m && m[1]) !== 'pricing') el.style.display = 'none';
            else el.classList.add('active');
        });
        document.querySelectorAll('main > div[id^="tab-"]').forEach(el => el.classList.add('hidden-section'));
        const host = document.getElementById('tab-pricing');
        host.classList.remove('hidden-section');
        // مارک‌آپ از همین ماژول تزریق می‌شود؛ قبلاً inline بود
        window.PricingWorkspace.mountWorkspace(host);
        if (!pricingCanEdit) {
            document.getElementById('pricing-save-btn').style.display = 'none';
            const pctBox = document.getElementById('pricing-pct-box');
            if (pctBox) pctBox.style.display = 'none';
            const mtBtn = document.getElementById('pricing-mikrotik-btn');
            if (mtBtn) mtBtn.style.display = 'none';
        }
        fetchPricingRows();
    }

    const fmtMoney = v => (v === null || v === undefined || v === '') ? '' : Number(v).toLocaleString('en-US');

    async function fetchPricingRows() {
        toggleLoader(true);
        try {
            // ضریب را هم بگیر تا پیش‌نمایشِ لحظه‌ای با همان چیزی که سرور حساب می‌کند یکی باشد
            const [res, fRes, capRes] = await Promise.all([
                axios.get(`${API}/v1/pricing/rows`),
                axios.get(`${API}/v1/settings/site-price-factor`).catch(() => null),
                axios.get(`${API}/v1/pricing/capabilities`).catch(() => null)
            ]);
            if (fRes && fRes.data && fRes.data.factor) pricingFactor = Number(fRes.data.factor);
            if (capRes && capRes.data) canEditBulkPrice = !!capRes.data.canEditBulkPrice;
            pricingRows = res.data || [];
            pricingDirty.clear();
            updatePricingDirtyUi();
            // کارشناسِ فروش باید بداند چرا خانه‌ها قابلِ تایپ نیستند، وگرنه فکر می‌کند خراب است
            const note = document.getElementById('pricing-readonly-note');
            if (!pricingCanEdit) {
                note.innerText = 'شما دسترسی «فقط مشاهده» دارید؛ ویرایش قیمت‌ها برای ادمین و کارشناس ارشد فعال است.';
                note.classList.remove('hidden');
            } else if (!canEditBulkPrice) {
                note.innerText = 'ستون «فروش تعدادی» فقط توسط ادمین و کارشناس ارشد قابل تغییر است؛ بقیه ستون‌ها برای شما باز است.';
                note.classList.remove('hidden');
            } else {
                note.classList.add('hidden');
            }
            // دکمهٔ ذخیره برایِ نقشِ فقط-مشاهده اصلاً نباشد: کلیکش قطعاً ۴۰۳ می‌گیرد
            // و دکمه‌ای که همیشه شکست می‌خورد بدتر از نبودنش است. (پنلِ ادمین این را
            // در restrictPanelToPricingWorkspace هم انجام می‌داد؛ حالا هر دو میزبان.)
            const saveBtn = document.getElementById('pricing-save-btn');
            if (saveBtn) saveBtn.style.display = pricingCanEdit ? '' : 'none';
            syncAddProductButton();
            // رنگ‌ها موازیِ ردیف‌ها لازم‌اند؛ اگر نیایند جدول بی‌نوارِ رنگ رندر
            // می‌شود، نه این‌که کلاً رندر نشود.
            await loadCategoryColors();
            renderPricingRows();
        } catch (err) {
            if (err.response && err.response.status === 403) {
                // نقشِ بدونِ دسترسی؛ جدول را خالی و پیام را نشان بده
                pricingRows = [];
                renderPricingRows();
                document.getElementById('pricing-readonly-note').classList.remove('hidden');
                document.getElementById('pricing-readonly-note').innerText =
                    'شما به میز کار قیمت‌گذاری دسترسی ندارید.';
            } else {
                Swal.fire('خطا', serverError(err, 'دریافت اطلاعات میز کار ناموفق بود'), 'error');
            }
        } finally {
            toggleLoader(false);
        }
    }

    function renderPricingRows() {
        const rows = visiblePricingRows();

        // ⚠️ فقط خودِ اینپوت را برمی‌گرداند، نه <td>. قبلاً <td> برمی‌گرداند و برای ستون‌های
        // «کف ترب/دیجی‌کالا» داخل یک <td> دیگر پیچیده می‌شد؛ <td> تودرتو HTML نامعتبر است و
        // مرورگر جدول را می‌شکست (ستون خالی می‌ماند و یک اینپوت بیرون از جدول می‌افتاد).
        const priceInput = (r, field, value, width = 'w-28') => {
            // «فروش تعدادی» برایِ کارشناسِ فروش قفل است (بقیه‌ی ستون‌ها باز)
            const fieldLocked = !pricingCanEdit
                || (field === 'partnerBulkPrice' && !canEditBulkPrice);
            if (fieldLocked) {
                const shownVal = value !== null && value !== undefined && value !== '' ? fmtMoney(value) : '—';
                const why = (field === 'partnerBulkPrice' && !canEditBulkPrice)
                    ? ' title="فقط کارشناس قیمت‌گذاری می‌تواند این را تغییر دهد"' : '';
                return `<span dir="ltr" class="text-gray-600 pw-ro"${why}>${shownVal}</span>`;
            }
            const key = `${r.id}|${field}`;
            let pending = pricingDirty.has(key);
            let rawShown = pending ? pricingDirty.get(key).value : (value ?? '');
            // ⚠️ قیمتِ سایتِ مشتق از فرمول: وقتی «فروش تعدادی» تغییرِ ذخیره‌نشده دارد،
            // باید همین‌جا هم عددِ تازه دیده شود. بدونِ این، کاربر درصد را اعمال
            // می‌کرد و قیمتِ سایت تکان نمی‌خورد و فکر می‌کرد اعمال نشده.
            let derived = false;
            if (field === 'onlinePrice' && !r.priceOverride && pricingDirty.has(`${r.id}|partnerBulkPrice`)) {
                const b = Number(pricingDirty.get(`${r.id}|partnerBulkPrice`).value);
                if (b > 0) { rawShown = String(Math.round(b * pricingFactor)); derived = true; pending = false; }
            }
            // ⚠️ فقط ستون‌هایِ تومانی کاما می‌گیرند. «مرجع $» اعشار دارد (۷۹.۰۰)
            // و کامازدن به آن عدد را خراب می‌کند.
            const shown = TOMAN_FIELDS.has(field) ? fmtMoney(rawShown) : rawShown;
            // oninput نه onchange: onchange فقط موقعِ ازدست‌دادنِ فوکوس شلیک می‌شود،
            // یعنی کاربر باید Tab بزند/جای دیگر کلیک کند تا نتیجه را ببیند.
            return `<input type="text" inputmode="decimal" value="${shown === null ? '' : shown}"
                       data-id="${r.id}" data-field="${field}"
                       oninput="onPricingEdit(this)"
                       onpaste="onPricePaste(event)"
                       ${derived ? `title="محاسبه‌شده از فروش تعدادی × ${pricingFactor} — با ذخیره ثبت می‌شود"` : ''}
                       class="${width} p-1.5 border rounded text-left ${pending ? 'bg-yellow-50 border-yellow-400' : ''}${derived ? ' bg-green-50 border-green-400' : ''}" dir="ltr">`;
        };

        // جابه‌جایی فقط وقتی مجاز است که نما کاملِ لیست باشد: کشیدنِ ردیف در
        // فهرستِ فیلترشده، ترتیبِ ردیف‌هایِ پنهان را بی‌خبر به‌هم می‌ریزد.
        const filtered = isPricingFiltered();
        const canDrag = pricingCanEdit && !filtered;

        const body = document.getElementById('pricing-body');
        body.classList.toggle('pw-locked', !canDrag);
        body.innerHTML = rows.length ? rows.map((r, i) => `
            <tr class="hover:bg-gray-50 ${r.pushSaleFlag ? 'bg-orange-50' : ''} ${pricingExcluded.has(r.id) ? 'pw-excluded' : ''}" data-id="${r.id}">
                <td class="p-2 text-center pw-num">${i + 1}</td>
                <td class="p-2 text-center">${canDrag
                    ? `<span class="pw-handle" title="بکشید و جابه‌جا کنید">⠿</span>`
                    : `<span class="pw-handle" title="${filtered ? 'برایِ جابه‌جایی اول جست‌وجو/فیلتر را پاک کنید' : 'شما دسترسیِ ویرایش ندارید'}">⠿</span>`}</td>
                <!-- ⚠️ ترتیبِ این خانه باید دقیقاً همان ترتیبِ سرستون‌ها باشد؛
                     قبلاً یک ستون جلوتر بود و تیترِ «استثنا» بالایِ چک‌باکسِ
                     خودش نمی‌افتاد. -->
                <td class="p-2 text-center">
                    <input type="checkbox" ${pricingExcluded.has(r.id) ? 'checked' : ''}
                           data-id="${r.id}" onchange="togglePricingExclude(this)" class="w-4 h-4"
                           title="تیک بزن تا «درصد تغییرات» این ردیف را رد کند">
                </td>
                <td class="p-2">
                    <span class="inline-flex items-center gap-2">
                        ${catColorCell(r)}
                        <span class="pw-name">${escapeHTML(r.name || '')}</span>
                    </span>
                </td>
                <td class="p-2 whitespace-nowrap">${cardCell(r)}</td>
                <td class="p-2">
                    ${priceInput(r, 'onlinePrice', r.onlinePrice)}
                    ${r.priceOverride ? `
                        <div class="pricing-note" title="${r.pricePercentAdjusted
                            ? 'قیمتِ پایه دستی ست شده و بعد «درصد تغییرات» هم رویش خورده'
                            : 'این قیمت دستی ثبت شده و با تغییر «فروش تعدادی» بازنویسی نمی‌شود'}">
                            <span class="text-amber-700">${r.pricePercentAdjusted ? '✋ دستی درصدی' : '✋ دستی'}</span>
                            <span class="text-gray-400" dir="ltr" data-suggested-for="${r.id}" title="مقدار پیشنهادی فرمول">${r.suggestedOnlinePrice ? '≈' + fmtMoney(r.suggestedOnlinePrice) : ''}</span>
                            ${pricingCanEdit ? `<button onclick="revertToFormula('${r.id}')" class="text-indigo-600 hover:underline" title="پرچم دستی برداشته و قیمت دوباره از فرمول محاسبه شود">بازگشت به فرمول</button>` : ''}
                        </div>` : ''}
                </td>
                <td class="p-2">${priceInput(r, 'partnerUnitPrice', r.partnerUnitPrice)}</td>
                <td class="p-2">${priceInput(r, 'partnerBulkPrice', r.partnerBulkPrice)}</td>
                <td class="p-2 text-center">${isMikrotikRow(r)
                    ? priceInput(r, 'dollarPrice', r.dollarPrice, 'w-16')
                    : '<span class="text-gray-300" title="این ستون فقط برایِ محصولاتِ میکروتیک معنی دارد">—</span>'}</td>
                <td class="p-2 text-center">${stockCell(r, 'stockIsfahan')}</td>
                <td class="p-2 text-center">${stockCell(r, 'stockTehran')}</td>
                <td class="p-2 text-center">${stockCell(r, 'incomingStock')}</td>
                <td class="p-2 ${r.weOwnBuyBox ? 'pw-ours' : ''}">
                    <div class="flex items-center gap-1">
                        ${priceInput(r, 'torobFloorPrice', r.torobFloorPrice, 'w-24')}
                        ${pricingCanEdit ? `<button onclick="openTorobSearch('${r.id}')" class="text-[10px] bg-gray-100 hover:bg-indigo-600 hover:text-white border rounded px-1.5 py-1 shrink-0" title="${TOROB_RULE}">🔍 ترب</button>` : ''}
                    </div>
                    ${r.weOwnBuyBox ? `<div class="pw-ours-tag mt-0.5" title="فروشندهٔ باکسِ خرید در دیجی‌کالا: ${escapeHTML(r.digikalaSellerTitle || '')}">★ باکسِ خرید دستِ خودمان است</div>` : ''}
                </td>
                <td class="p-2">
                    <div class="flex items-center gap-1">
                        ${priceInput(r, 'digikalaFloorPrice', r.digikalaFloorPrice, 'w-24')}
                        ${r.digikalaUrl ? `<a href="${escapeHTML(r.digikalaUrl)}" target="_blank" rel="noopener" class="text-indigo-600 shrink-0" title="باز کردن صفحه دیجی‌کالا">↗</a>` : ''}
                        ${pricingCanEdit ? (r.digikalaDkp
                            ? `<button onclick="refreshDigikala('${r.id}')" class="text-[10px] bg-green-50 text-green-700 hover:bg-green-600 hover:text-white border border-green-300 rounded px-1.5 py-1 shrink-0" title="محصول متناظر قبلاً تأیید شده (DKP ${escapeHTML(r.digikalaDkp)}) — قیمت را به‌روز کن">↻ به‌روز</button>`
                            : `<button onclick="findDigikala('${r.id}')" class="text-[10px] bg-gray-100 hover:bg-indigo-600 hover:text-white border rounded px-1.5 py-1 shrink-0" title="جست‌وجو و انتخاب محصول متناظر در دیجی‌کالا">🔗 پیداکردن</button>`) : ''}
                    </div>
                    ${r.floorPriceCheckedAt ? `<div class="text-[9px] text-gray-400 mt-0.5">${daysAgoLabel(r.floorPriceCheckedAt)}</div>` : ''}
                </td>
                <td class="p-2 text-center">
                    <input type="checkbox" ${r.pushSaleFlag ? 'checked' : ''} ${pricingCanEdit ? '' : 'disabled'}
                           data-id="${r.id}" data-field="pushSaleFlag"
                           onchange="onPricingFlag(this)" class="w-4 h-4">
                </td>
            </tr>
        `).join('') : '<tr><td colspan="15" class="p-8 text-center text-gray-400">موردی یافت نشد</td></tr>';

        initRowSortable(canDrag);
    }

    // ═════════ جابه‌جاییِ ردیف‌ها ═════════
    let rowSortable = null;

    function initRowSortable(enabled) {
        const body = document.getElementById('pricing-body');
        if (!body || typeof Sortable === 'undefined') return;
        if (!rowSortable) {
            rowSortable = Sortable.create(body, {
                handle: '.pw-handle',
                draggable: 'tr[data-id]',
                animation: 150,
                ghostClass: 'pw-ghost',
                chosenClass: 'pw-chosen',
                onEnd: saveRowOrder
            });
        }
        rowSortable.option('disabled', !enabled);
    }

    /**
     * ترتیبِ تازه را ذخیره می‌کند.
     * <p>
     * کلِ ترتیب فرستاده می‌شود، نه فقط ردیفِ جابه‌جاشده — چون یک جابه‌جایی جایِ
     * همهٔ ردیف‌هایِ بعدش را هم عوض می‌کند. شماره‌ها ۱۰تا۱۰ فاصله می‌گیرند تا
     * اگر روزی «درج در میان» لازم شد، جا باشد و همهٔ ردیف‌ها بازنویسی نشوند.
     */
    async function saveRowOrder() {
        const ids = [...document.querySelectorAll('#pricing-body tr[data-id]')].map(tr => tr.dataset.id);
        if (!ids.length) return;

        // ترتیبِ حافظه را هم همان لحظه هم‌راست می‌کنیم تا شماره‌ها بعد از رندرِ
        // بعدی نپرند و نیازی به واکشیِ کاملِ جدول نباشد.
        const order = {};
        ids.forEach((id, i) => { order[id] = (i + 1) * 10; });
        pricingRows.forEach(r => { if (order[r.id] !== undefined) r.workspacePosition = order[r.id]; });
        pricingRows.sort((a, b) => (a.workspacePosition ?? Infinity) - (b.workspacePosition ?? Infinity));

        try {
            await axios.put(`${API}/v1/pricing/reorder`,
                    ids.map((id, i) => ({id, position: (i + 1) * 10})));
            // شماره‌ها را دوباره بکش (۱،۲،۳… طبقِ جایِ تازه)
            renderPricingRows();
        } catch (err) {
            Swal.fire('خطا', serverError(err, 'ذخیرهٔ ترتیبِ ردیف‌ها ناموفق بود'), 'error');
            // ترتیبِ واقعی را از سرور برگردان تا نمای کاربر با دیتابیس یکی شود
            fetchPricingRows();
        }
    }

    /**
     * لینکِ کارتِ فروشگاه، یا دکمهٔ تکمیلِ کارت.
     * <p>
     * 🔴 محصول همیشه با شناسه باز می‌شود، پس «کارت ندارد» یعنی کارتش برای مشتری
     * خالی است، نه اینکه صفحه‌اش ۴۰۴ بدهد. ملاک همان سه چیزی است که مالک گفت:
     * عکس، توضیحات، دستهٔ سایت. محصولی که از ورودِ دسته‌ایِ انبار آمده معمولاً هیچ‌کدام
     * را ندارد و در فروشگاه کارتِ بی‌عکس و بی‌متن نشان می‌دهد.
     */
    function cardCell(r) {
        const missing = r.cardMissing || [];
        const href = '/shop/product/' + encodeURIComponent(r.slug || r.id);
        const view = `<a href="${href}" target="_blank" rel="noopener"
                         class="text-indigo-600 hover:underline shrink-0" title="بازکردنِ کارتِ محصول در فروشگاه">↗ کارت</a>`;
        if (!missing.length) return view;

        const why = 'این محصول ' + missing.join(' و ') + ' ندارد؛ کارتش در فروشگاه ناقص دیده می‌شود.';
        const fix = pricingCanEdit
            ? `<button onclick="completeProductCard('${r.id}')"
                       class="text-[10px] bg-amber-100 text-amber-800 border border-amber-300 rounded px-1.5 py-1 shrink-0 hover:bg-amber-500 hover:text-white"
                       title="${escapeHTML(why)}">✎ تکمیل کارت</button>`
            : `<span class="text-[10px] text-amber-700" title="${escapeHTML(why)}">ناقص</span>`;
        return `<div class="flex items-center gap-1">${view}${fix}</div>`;
    }

    /**
     * هر دو دکمه همان مودالِ تبِ محصولات را باز می‌کنند، نه فرمِ دومی.
     * <p>
     * ⚠️ فرمِ جداگانه یعنی دو جا برای همان اعتبارسنجی و دو جا برای فراموش‌کردنِ یک
     * فیلد. مودال در پنلِ فروش از قبل mount شده (برایِ ADMIN/PRICER)، پس فقط صدا
     * زده می‌شود. بعد از ذخیره، ردیف‌های میز دوباره خوانده می‌شوند تا ستونِ کارت
     * همان لحظه به‌روز شود.
     */
    function addProductFromPricing() {
        if (typeof openProductModal !== 'function') {
            return alert('تبِ محصولات هنوز آماده نیست؛ یک بار تبِ «محصولات» را باز کنید.');
        }
        openProductModal();
    }

    function completeProductCard(id) {
        if (typeof editProduct !== 'function') {
            return alert('تبِ محصولات هنوز آماده نیست؛ یک بار تبِ «محصولات» را باز کنید.');
        }
        editProduct(id);
    }

    /**
     * ردیف‌هایی که «همین الان روی میز دیده می‌شوند».
     * ⚠️ یک منبعِ واحد برایِ رندر و برایِ «درصد تغییرات» — اگر دو تا می‌شد،
     * روزی درصد روی ردیفی می‌نشست که کاربر اصلاً نمی‌دید.
     */
    function visiblePricingRows() {
        const q = (document.getElementById('pricing-search').value || '').trim().toLowerCase();
        const onlyFlagged = document.getElementById('pricing-only-flagged').checked;
        const catId = (document.getElementById('pricing-cat') || {}).value || '';
        const hideOos = (document.getElementById('pricing-hide-oos') || {}).checked;
        return pricingRows.filter(r => {
            if (onlyFlagged && !r.pushSaleFlag) return false;
            if (q && !(r.name || '').toLowerCase().includes(q)) return false;
            if (catId && !rowInCategory(r, catId)) return false;
            if (hideOos && !(Number(r.sellableStock) > 0)) return false;
            return true;
        });
    }

    function isPricingFiltered() {
        const q = (document.getElementById('pricing-search').value || '').trim();
        return !!q
            || document.getElementById('pricing-only-flagged').checked
            || !!((document.getElementById('pricing-cat') || {}).value)
            || !!((document.getElementById('pricing-hide-oos') || {}).checked);
    }

    /** ردیف‌هایی که «درصد تغییرات» عمداً رد می‌کند (فقط همین نشست، ذخیره نمی‌شود). */
    const pricingExcluded = new Set();

    function togglePricingExclude(box) {
        if (box.checked) pricingExcluded.add(box.dataset.id);
        else pricingExcluded.delete(box.dataset.id);
        const tr = box.closest('tr');
        if (tr) tr.classList.toggle('pw-excluded', box.checked);
    }

    /**
     * خانهٔ موجودی — حالا ویرایش‌پذیر (خواستهٔ مالک: همهٔ خانه‌ها قابلِ اصلاح).
     * عددِ بولد، ولی صفر/نبود کم‌رنگ می‌ماند تا «کم» از «نیست» فرق کند.
     */
    function stockCell(r, field) {
        const v = r[field];
        const n = Number(v);
        const has = !(v === null || v === undefined || v === '');
        if (!pricingCanEdit) {
            if (!has) return '<span class="pw-stock-zero">—</span>';
            return n > 0 ? `<span class="pw-stock">${n}</span>` : `<span class="pw-stock-zero">${n}</span>`;
        }
        const key = `${r.id}|${field}`;
        const pending = pricingDirty.has(key);
        const shown = pending ? pricingDirty.get(key).value : (has ? v : '');
        const tone = field === 'incomingStock' && n > 0 ? 'text-blue-600 ' : '';
        return `<input type="text" inputmode="numeric" value="${shown}"
                   data-id="${r.id}" data-field="${field}"
                   oninput="onPricingEdit(this)"
                   class="w-12 p-1 border rounded text-center ${tone}${n > 0 ? 'pw-stock' : 'pw-stock-zero'} ${pending ? 'bg-yellow-50 border-yellow-400' : ''}" dir="ltr">`;
    }

    /** ستون‌هایِ تومانی — این‌ها جداکنندهٔ هزارگان می‌گیرند، «مرجع $» نه. */
    const TOMAN_FIELDS = new Set(['onlinePrice', 'partnerUnitPrice', 'partnerBulkPrice',
                                  'torobFloorPrice', 'digikalaFloorPrice']);

    const TOROB_RULE = 'یک‌راست صفحهٔ همین محصول در ترب باز می‌شود (نتیجهٔ اولِ جست‌وجو). '
        + '⚠️ عددِ بزرگِ بالایِ آن صفحه کف نیست؛ کف، اولین ردیفِ فهرستِ فروشگاه‌هایِ '
        + 'پایینِ مشخصات است. همان ردیف را کپی کن و همین‌جا Ctrl+V بزن — عدد خودش درمی‌آید.';

    /** ارقامِ فارسی/عربی → لاتین، تا ورودیِ کیبوردِ فارسی هم عدد حساب شود. */
    function normalizeDigits(s) {
        return String(s == null ? '' : s)
            .replace(/[۰-۹]/g, d => '۰۱۲۳۴۵۶۷۸۹'.indexOf(d))
            .replace(/[٠-٩]/g, d => '٠١٢٣٤٥٦٧٨٩'.indexOf(d));
    }

    /**
     * کاماگذاریِ زندهٔ یک اینپوتِ تومانی، با نگه‌داشتنِ جایِ مکان‌نما.
     * <p>
     * ⚠️ بدونِ بازگرداندنِ مکان‌نما، با هر کاما که اضافه می‌شود نشانگر می‌پرد
     * آخرِ خط و تایپِ وسطِ عدد غیرممکن می‌شود. معیار «چند رقم پیش از مکان‌نما
     * بود» است، نه شمارهٔ کاراکتر — چون تعدادِ کاماها عوض می‌شود.
     */
    function reformatMoneyInput(input) {
        const before = input.value;
        const caret = input.selectionStart == null ? before.length : input.selectionStart;
        const digitsBefore = normalizeDigits(before.slice(0, caret)).replace(/\D/g, '').length;
        const clean = normalizeDigits(before).replace(/\D/g, '');
        const out = clean === '' ? '' : Number(clean).toLocaleString('en-US');
        if (out === before) return;
        input.value = out;
        let seen = 0, pos = digitsBefore === 0 ? 0 : out.length;
        for (let i = 0; i < out.length && digitsBefore > 0; i++) {
            if (out[i] >= '0' && out[i] <= '9') seen++;
            if (seen === digitsBefore) { pos = i + 1; break; }
        }
        try { input.setSelectionRange(pos, pos); } catch (e) { /* اینپوتِ بی‌فوکوس */ }
    }

    /**
     * «درصد تغییرات» — درصدِ مثبت/منفی روی هر سه ستونِ قیمتِ ردیف‌هایِ <b>همین نما</b>.
     * <p>
     * ⚠️ دو تصمیمِ عمدی:
     * <br>۱) نتیجه مثلِ ویرایشِ دستی «در انتظارِ ذخیره» (زرد) می‌نشیند، نه مستقیم روی
     * سرور — تا قبلِ «ذخیره تغییرات» برگشت‌پذیر باشد.
     * <br>۲) روی ردیف‌هایی که قیمتشان از فرمولِ «فروش تعدادی × ضریب» می‌آید، درصد
     * روی «فروش تعدادی» اعمال می‌شود نه روی قیمتِ سایت؛ چون نوشتنِ مستقیمِ قیمتِ
     * سایت آن ردیف را برایِ همیشه «دستی» می‌کند و فرمول دیگر به‌روزش نمی‌کند.
     */
    function applyPricingPercent() {
        if (!pricingCanEdit) return;
        const box = document.getElementById('pricing-pct');
        const pct = Number(normalizeDigits((box.value || '').replace(/[٪%\s,]/g, '')));
        if (!isFinite(pct) || pct === 0) {
            Swal.fire('عدد بده', 'یک درصدِ مثبت یا منفی وارد کن، مثلاً ‎5‎ یا ‎-3‎', 'warning');
            return;
        }

        const all = visiblePricingRows();
        const rows = all.filter(r => !pricingExcluded.has(r.id));
        const skipped = all.length - rows.length;
        if (!rows.length) {
            Swal.fire('ردیفی نیست', all.length ? 'همهٔ ردیف‌هایِ این نما استثنا شده‌اند' : 'این نما خالی است', 'info');
            return;
        }

        const catSel = document.getElementById('pricing-cat');
        const catName = catSel && catSel.value ? catSel.options[catSel.selectedIndex].text : '';
        const scope = catName ? `دستهٔ «${catName}»` : (isPricingFiltered() ? 'ردیف‌هایِ همین نما' : 'همهٔ ردیف‌هایِ میز');
        const skipNote = skipped ? `<br><b>${skipped} ردیفِ استثناشده</b> دست نمی‌خورد.` : '';

        Swal.fire({
            icon: 'question',
            title: `${pct > 0 ? '+' : ''}${pct}٪ روی ${rows.length} ردیف`,
            html: `دامنه: <b>${scope}</b>.${skipNote}`
                + '<br>ستون‌ها: قیمت سایت، همکار تک، فروش تعدادی.'
                + '<br>نتیجه زرد می‌شود و تا زدنِ «ذخیره تغییرات» قطعی نیست.',
            showCancelButton: true, confirmButtonText: 'اعمال کن', cancelButtonText: 'انصراف'
        }).then(res => {
            if (!res.isConfirmed) return;
            const mul = 1 + pct / 100;
            const bump = (r, field, via) => {
                const cur = Number(r[field]);
                if (!isFinite(cur) || cur <= 0) return false;
                const next = Math.round(cur * mul);
                if (next === cur) return false;
                const item = {id: r.id, field, value: String(next)};
                // ⚠️ همین نشانه است که سرور از رویش برچسبِ «دستی درصدی» می‌گذارد؛
                // بدونش، اعمالِ درصد از تایپِ دستی قابلِ تفکیک نبود.
                if (via) item.via = via;
                pricingDirty.set(`${r.id}|${field}`, item);
                return true;
            };

            let touched = 0;
            rows.forEach(r => {
                if (bump(r, 'partnerUnitPrice')) touched++;

                // قیمتِ سایت دو مسیر دارد و هر دو در نهایت عوضش می‌کنند:
                //   • پایهٔ فرمولی → درصد روی «فروش تعدادی»، و سرور قیمتِ سایت را
                //     دوباره حساب می‌کند. ردیف فرمولی می‌ماند و برچسبی نمی‌گیرد.
                //   • پایهٔ دستی (یا بی‌فروشِ تعدادی) → مستقیم روی قیمتِ سایت،
                //     و برچسبش «دستی درصدی» می‌شود.
                const hasBulk = Number(r.partnerBulkPrice) > 0;
                const formulaBased = !r.priceOverride && hasBulk && canEditBulkPrice;
                if (hasBulk && canEditBulkPrice) {
                    if (bump(r, 'partnerBulkPrice')) touched++;
                }
                if (!formulaBased) {
                    if (bump(r, 'onlinePrice', 'percent')) touched++;
                }
            });

            renderPricingRows();
            updatePricingDirtyUi();
            box.value = '';
            Swal.fire({icon: 'success', title: `${touched} خانه تغییر کرد`,
                       text: 'برایِ قطعی‌شدن «ذخیره تغییرات» را بزن', timer: 2200, showConfirmButton: false});
        });
    }

    /**
     * اعدادِ داخلِ یک متنِ درهم را درمی‌آورد.
     * <p>
     * ⚠️ جداکننده‌ها عمداً همه‌ی این‌ها را شامل می‌شوند: {@code , . ٫ ٬ ،} —
     * ترب عددش را با «٫» (جداکنندهٔ اعشاریِ عربی) می‌نویسد، نه کاما؛ اگر جا
     * می‌افتاد، «۱۹٫۵۰۰٫۰۰۰» سه عددِ جدا خوانده می‌شد.
     * ⚠️ فاصله عمداً جداکننده نیست، وگرنه «۶۸ فروشنده» و عددِ بعدی به هم
     * می‌چسبیدند.
     */
    function numbersInText(text) {
        const t = normalizeDigits(String(text == null ? '' : text));
        const re = /\d[\d.,\u066B\u066C\u060C]*\d|\d/g;
        const out = [];
        let m;
        while ((m = re.exec(t)) !== null) {
            const digits = m[0].replace(/\D/g, '');
            if (digits) out.push(Number(digits));
        }
        return out;
    }

    /**
     * پیستِ متنِ کپی‌شده از صفحهٔ ترب (یا هر جای دیگر) داخلِ خانهٔ قیمت.
     * <p>
     * کارشناس ردیفِ فروشگاه را کپی می‌کند و همین‌جا می‌چسباند؛ نامِ فروشگاه و
     * «تومان» و ستاره و بقیه ریخته می‌شود دور و فقط عدد می‌ماند.
     * <p>
     * ⚠️ بدونِ این، پیستِ متنِ درهم فاجعه بود نه فقط بی‌فایده: پاک‌سازیِ فعلی
     * <b>همهٔ</b> رقم‌های متن را به هم می‌چسباند، پس «★۵ … ۱۹٫۵۰۰٫۰۰۰ تومان»
     * می‌شد عددِ ۵۱۹۵۰۰۰۰۰.
     * <p>
     * ⚠️ وقتی چند قیمت در متن باشد <b>حدس نمی‌زنیم</b>: عددِ بالایِ صفحهٔ ترب و
     * ردیفِ اولِ فروشگاه‌ها هر دو شش‌رقمی‌اند و هیچ قاعده‌ای نمی‌تواند بینشان
     * درست انتخاب کند — از کاربر پرسیده می‌شود.
     */
    async function onPricePaste(e) {
        const input = e.target;
        if (!input || !TOMAN_FIELDS.has(input.dataset.field)) return;

        const cb = e.clipboardData || window.clipboardData;
        const text = cb ? cb.getData('text') : '';
        if (!text) return;

        const nums = numbersInText(text);
        if (!nums.length) return;            // عددی نبود؛ بگذار پیستِ عادی کار کند
        e.preventDefault();

        // شش‌رقمی‌به‌بالا یعنی «قیمت»؛ بقیه ستاره و شمارِ فروشنده و سالِ عضویت‌اند
        const prices = [...new Set(nums.filter(n => n >= 100000))];
        let value;
        if (prices.length === 1) {
            value = prices[0];
        } else if (prices.length > 1) {
            const opts = {};
            prices.sort((a, b) => a - b).forEach(n => { opts[String(n)] = n.toLocaleString('en-US') + ' تومان'; });
            const pick = await Swal.fire({
                icon: 'question',
                title: 'کدام عدد؟',
                html: 'در متنِ کپی‌شده چند قیمت بود.<br>کفِ ترب قیمتِ <b>اولین ردیفِ فهرستِ فروشگاه‌ها</b>ست، '
                    + 'نه عددِ بالای صفحه.',
                input: 'radio',
                inputOptions: opts,
                inputValue: String(prices[0]),
                showCancelButton: true, confirmButtonText: 'همین', cancelButtonText: 'بی‌خیال'
            });
            if (!pick.isConfirmed || !pick.value) return;
            value = Number(pick.value);
        } else {
            value = Math.max.apply(null, nums);
        }

        input.value = String(value);
        onPricingEdit(input);

        // 🔴 تقسیمِ خودسرانه بر ده نمی‌کنیم: اشتباهِ ده‌برابری در این ستون یعنی
        // تصمیمِ قیمتیِ غلط. فقط می‌گوییم و انتخاب با آدم است.
        if (/ریال|rial/i.test(normalizeDigits(text))) {
            Swal.fire({
                icon: 'warning',
                title: 'متن «ریال» داشت',
                html: `عدد <b>${value.toLocaleString('en-US')}</b> همان‌طور که بود ثبت شد و تقسیم بر ۱۰ <b>نشد</b>.`
                    + '<br>اگر واقعاً ریالی است، خودتان تومانش کنید.'
            });
        }
    }

    /**
     * «مرجعِ دلاریِ میکروتیک» — کاتالوگ را تازه می‌کند، پیشنهاد می‌گیرد،
     * <b>به تأیید می‌دهد</b>، بعد می‌نویسد.
     * <p>
     * ⚠️ سه مرحله عمدی است: خزشِ ۵۶۱ صفحهٔ محصول چند ده ثانیه طول می‌کشد، پس
     * نمی‌شود در یک درخواستِ HTTP نگهش داشت؛ و نوشتنِ خودکارِ بی‌تأیید یعنی
     * ریسکِ نشستنِ قیمت روی محصولِ اشتباه.
     */
    async function syncMikrotikPrices() {
        if (!pricingCanEdit) return;
        try {
            await axios.post(`${API}/v1/pricing/mikrotik/refresh`);

            // نوارِ پیشرفت تا وقتی خزش تمام شود
            let st = null;
            Swal.fire({
                title: 'گرفتنِ کاتالوگِ میکروتیک',
                html: '<div id="mt-prog" style="font-size:13px">در حال شروع…</div>',
                allowOutsideClick: false, showConfirmButton: false,
                didOpen: () => Swal.showLoading && Swal.showLoading()
            });
            for (let i = 0; i < 400; i++) {
                const res = await axios.get(`${API}/v1/pricing/mikrotik/status`);
                st = res.data || {};
                const el = document.getElementById('mt-prog');
                if (el) {
                    el.innerHTML = st.running
                        ? `${st.done || 0} از ${st.total || '؟'} صفحه خوانده شد…`
                        : `کاتالوگ آماده است: <b>${st.count || 0}</b> محصولِ قیمت‌دار`;
                }
                if (!st.running) break;
                await new Promise(r => setTimeout(r, 1200));
            }
            if (st && st.error) { Swal.fire('خطا', 'گرفتنِ کاتالوگ ناموفق بود: ' + st.error, 'error'); return; }
            if (!st || !st.count) { Swal.fire('چیزی نیامد', 'کاتالوگِ میکروتیک خالی برگشت', 'warning'); return; }

            const pr = (await axios.get(`${API}/v1/pricing/mikrotik/proposals`)).data || {};
            const list = pr.proposals || [];
            if (!list.length) {
                Swal.fire({icon: 'success', title: 'همه‌چیز به‌روز است',
                    html: `کاتالوگ ${pr.catalogSize} محصول دارد و <b>${pr.unchanged}</b> محصولِ ما از قبل همین عدد را داشت.`});
                return;
            }

            // ⚠️ هر ردیف تیکِ خودش را دارد. در آزمونِ واقعی دیدیم که محصولِ
            // دستِدوم هم به همان کدِ قطعه می‌خورد؛ قیمتِ مرجعِ نو برایِ آن معنی ندارد
            // و کارشناس باید بتواند تکی کنارش بگذارد.
            const rows = list.map((x, i) => `
                <tr>
                    <td style="padding:3px 6px"><input type="checkbox" checked data-mt="${i}"></td>
                    <td style="text-align:right;padding:3px 6px">${escapeHTML(x.name || '')}</td>
                    <td style="padding:3px 6px;direction:ltr;color:#6b7280">${escapeHTML(x.code || '')}</td>
                    <td style="padding:3px 6px;direction:ltr;color:#9ca3af">${x.current == null ? '—' : '$' + x.current}</td>
                    <td style="padding:3px 6px;direction:ltr;font-weight:700;color:#166534">$${x.usd}</td>
                </tr>`).join('');

            const ok = await Swal.fire({
                icon: 'question',
                title: `${list.length} محصول تغییر می‌کند`,
                width: 760,
                html: `<div style="font-size:12px;text-align:right;margin-bottom:6px">`
                    + `کاتالوگ: <b>${pr.catalogSize}</b> محصولِ میکروتیک · از قبل درست: <b>${pr.unchanged}</b>`
                    + `<br>🔴 این عدد <b>قیمتِ پیشنهادیِ میکروتیک (MSRP)</b> است، نه قیمتِ خریدِ نماینده.</div>`
                    + `<div style="max-height:320px;overflow:auto;border:1px solid #e5e7eb;border-radius:8px">`
                    + `<table style="width:100%;font-size:11px;border-collapse:collapse">`
                    + `<thead style="position:sticky;top:0;background:#f9fafb"><tr>`
                    + `<th style="padding:4px"><input type="checkbox" checked id="mt-all"></th>`
                    + `<th style="padding:4px">محصولِ ما</th><th style="padding:4px">کدِ قطعه</th>`
                    + `<th style="padding:4px">فعلی</th><th style="padding:4px">تازه</th></tr></thead>`
                    + `<tbody>${rows}</tbody></table></div>`,
                showCancelButton: true, confirmButtonText: 'تیک‌خورده‌ها را ثبت کن', cancelButtonText: 'انصراف',
                didOpen: () => {
                    const all = document.getElementById('mt-all');
                    if (all) all.addEventListener('change', () => {
                        document.querySelectorAll('[data-mt]').forEach(b => { b.checked = all.checked; });
                    });
                },
                preConfirm: () => [...document.querySelectorAll('[data-mt]')]
                    .filter(b => b.checked).map(b => Number(b.dataset.mt))
            });
            if (!ok.isConfirmed) return;
            const picked = (ok.value || []).map(i => list[i]).filter(Boolean);
            if (!picked.length) { Swal.fire('چیزی تیک نخورد', 'هیچ محصولی انتخاب نشد', 'info'); return; }

            toggleLoader(true);
            const res = await axios.post(`${API}/v1/pricing/mikrotik/apply`,
                picked.map(x => ({id: x.id, usd: x.usd})));
            toggleLoader(false);
            await Swal.fire({icon: 'success', title: `${res.data.applied} محصول به‌روز شد`,
                timer: 1800, showConfirmButton: false});
            await fetchPricingRows();
        } catch (err) {
            toggleLoader(false);
            if (err.response && err.response.status === 403) {
                Swal.fire('دسترسی ندارید', 'نقشِ شما اجازهٔ این کار را ندارد.', 'error');
            } else {
                Swal.fire('خطا', serverError(err, 'گرفتنِ قیمتِ میکروتیک ناموفق بود'), 'error');
            }
        }
    }

    /** آیا ریشهٔ دستهٔ این ردیف میکروتیک است؟ گاردِ حلقه دارد. */
    function isMikrotikRow(r) {
        let cur = r && r.categoryId ? catById[r.categoryId] : null, guard = 0;
        while (cur && guard++ < 25) {
            const slug = (cur.slug || '').toLowerCase();
            if (slug === 'mikrotik' || (cur.name || '').includes('میکروتیک')) return true;
            cur = cur.parentId ? catById[cur.parentId] : null;
        }
        return false;
    }

    function onPricingEdit(input) {
        const id = input.dataset.id, field = input.dataset.field;
        if (TOMAN_FIELDS.has(field)) reformatMoneyInput(input);
        const raw = normalizeDigits(input.value).trim().replace(/,/g, '');
        const row = pricingRows.find(r => r.id === id);
        const original = row ? (row[field] ?? '') : '';

        if (String(original) === raw) {
            pricingDirty.delete(`${id}|${field}`);
            input.classList.remove('bg-yellow-50', 'border-yellow-400');
        } else {
            pricingDirty.set(`${id}|${field}`, {id, field, value: raw});
            input.classList.add('bg-yellow-50', 'border-yellow-400');
        }

        // پیش‌نمایشِ لحظه‌ایِ قیمتِ سایت وقتی «فروش تعدادی» عوض می‌شود —
        // بدونِ این، کارشناس تا نزدنِ «ذخیره» نتیجه‌ی فرمول را نمی‌دید.
        // فقط وقتی قیمت دستی ست نشده باشد (همان قاعده‌ی سمتِ سرور).
        if (field === 'partnerBulkPrice' && row && !row.priceOverride) {
            const bulk = Number(raw);
            const onlineInput = document.querySelector(
                `#pricing-body input[data-id="${id}"][data-field="onlinePrice"]`);
            if (onlineInput && bulk > 0) {
                const derived = Math.round(bulk * pricingFactor);
                onlineInput.value = derived;
                // همان نشانِ زردِ «تغییرِ ذخیره‌نشده»، ولی به‌عنوانِ مشتق‌شده نه ویرایشِ دستی
                onlineInput.classList.add('bg-green-50', 'border-green-400');
                onlineInput.title = `محاسبه‌شده از فروش تعدادی × ${pricingFactor} — با ذخیره ثبت می‌شود`;
            } else if (onlineInput && !raw) {
                // «فروش تعدادی» پاک شد → پیش‌نمایش برداشته شود
                onlineInput.value = row.onlinePrice ?? '';
                onlineInput.classList.remove('bg-green-50', 'border-green-400');
                onlineInput.title = '';
            }
        }

        // اگر قیمت دستی است، خودِ قیمت دست نمی‌خورد ولی عددِ «پیشنهادی» باید تازه بماند
        if (field === 'partnerBulkPrice' && row && row.priceOverride) {
            const sug = document.querySelector(`#pricing-body [data-suggested-for="${id}"]`);
            const bulk = Number(raw);
            if (sug) sug.textContent = bulk > 0 ? `≈${fmtMoney(Math.round(bulk * pricingFactor))}` : '';
        }

        updatePricingDirtyUi();
    }

    function onPricingFlag(box) {
        const id = box.dataset.id;
        const row = pricingRows.find(r => r.id === id);
        const original = !!(row && row.pushSaleFlag);
        if (original === box.checked) pricingDirty.delete(`${id}|pushSaleFlag`);
        else pricingDirty.set(`${id}|pushSaleFlag`, {id, field: 'pushSaleFlag', value: box.checked});
        updatePricingDirtyUi();
    }

    // ==========================================================
    // کفِ قیمتِ رقبا — دو بازار، دو رفتار
    // دیجی‌کالا: خودکار (پیداکردن → تأییدِ انسان → به‌روزرسانیِ یک‌کلیکی)
    // ترب: نیمه‌خودکار (فقط بازکردنِ صفحه؛ سدّ ضدرباتش دور زده نمی‌شود)
    // ==========================================================

    function daysAgoLabel(iso) {
        const d = Math.floor((Date.now() - new Date(iso).getTime()) / 86400000);
        if (d <= 0) return 'بررسی: امروز';
        if (d === 1) return 'بررسی: دیروز';
        return `بررسی: ${d} روز پیش`;
    }

    /** ترب — فقط صفحه را با کوئریِ آماده باز می‌کند. */
    /**
     * جست‌وجوی ترب برای یک ردیف.
     * <p>
     * ⚠️ متنِ جست‌وجو ذخیره می‌شود (Product.torobQuery) چون نامِ کاملِ محصول
     * برای جست‌وجو بد است — خریدار مدل را خلاصه می‌زند. کارشناس یک بار
     * عبارتِ درست را می‌نویسد و دفعهٔ بعد همان می‌آید.
     * <p>
     * ⚠️ خالی‌کردنِ فیلد یعنی «برگرد به ساختِ خودکار از عنوان»، نه
     * «جست‌وجوی خالی» — برای همین با isConfirmed کار می‌کنیم نه با خودِ مقدار.
     */
    async function openTorobSearch(id) {
        const row = pricingRows.find(r => r.id === id);
        if (!row) return;
        const saved = row.torobQuery || '';
        const res = await Swal.fire({
            title: 'یافتنِ صفحهٔ ترب',
            input: 'text',
            inputValue: saved || row.name || '',
            inputLabel: saved
                ? 'متن جست‌وجو (ذخیره‌شده — خالی‌اش کنی، دوباره از عنوان ساخته می‌شود)'
                : 'متن جست‌وجو (معمولاً فقط مدل بهتر جواب می‌دهد؛ ذخیره می‌شود)',
            html: '<div style="font-size:12px;text-align:right;color:#666;line-height:2">'
                + '<b>یک‌راست صفحهٔ همین محصول در ترب</b> باز می‌شود (نتیجهٔ اولِ جست‌وجو).<br>'
                + 'برو پایینِ مشخصات، <b>اولین ردیفِ فهرستِ فروشگاه‌ها</b> را کپی کن '
                + 'و همین‌جا در خانهٔ «کف ترب» Ctrl+V بزن.<br>'
                + '⚠️ عددِ بزرگِ بالای صفحه کف نیست.</div>',
            showCancelButton: true, confirmButtonText: 'باز کن', cancelButtonText: 'انصراف'
        });
        if (!res.isConfirmed) return;

        const typed = String(res.value == null ? '' : res.value).trim();
        // خالی → از عنوان ساخته شود
        const query = typed || (row.name || '').trim();
        if (!query) return;

        // فقط وقتی عوض شده ذخیره کن، و بی‌سروصدا — این یک تنظیم است نه قیمت،
        // پس کاربر نباید برایش «ذخیره تغییرات» بزند.
        if (typed !== saved) saveTorobQuery(row, typed);

        try {
            const u = await axios.get(`${API}/v1/pricing/marketplace/search-url`,
                {params: {market: 'torob', query}});
            window.open(u.data.url, '_blank', 'noopener');
        } catch (err) {
            Swal.fire('خطا', serverError(err, 'ساخت آدرس ناموفق بود'), 'error');
        }
    }

    /** ذخیرهٔ بی‌سروصدای متنِ جست‌وجو؛ خالی = پاک‌کردن (برگشت به ساختِ خودکار). */
    async function saveTorobQuery(row, typed) {
        try {
            await axios.post(`${API}/v1/pricing/batch`,
                [{id: row.id, field: 'torobQuery', value: typed || null}]);
            row.torobQuery = typed || null;   // نمایِ محلی هم تازه بماند
        } catch (err) {
            // ذخیره‌نشدنِ متنِ جست‌وجو نباید جلوی بازشدنِ صفحه را بگیرد
            console.warn('ذخیرهٔ متنِ جست‌وجوی ترب ناموفق بود', err);
        }
    }

    /** دیجی‌کالا مرحلهٔ A — نامزدها با تأییدِ انسان. هرگز خودکار انتخاب نمی‌شود. */
    async function findDigikala(id) {
        const row = pricingRows.find(r => r.id === id);
        if (!row) return;

        const {value: query} = await Swal.fire({
            title: 'پیدا کردن در دیجی‌کالا',
            input: 'text',
            inputValue: row.name || '',
            inputLabel: 'متن جست‌وجو — قابل ویرایش (نام کامل فارسی معمولاً نتیجه بد می‌دهد؛ فقط مدل را بزن)',
            showCancelButton: true, confirmButtonText: 'جست‌وجو', cancelButtonText: 'انصراف'
        });
        if (!query) return;

        toggleLoader(true);
        let candidates = [];
        try {
            const res = await axios.post(`${API}/v1/pricing/marketplace/candidates`, {market: 'digikala', query});
            candidates = res.data.candidates || [];
        } catch (err) {
            toggleLoader(false);
            Swal.fire('خطا', serverError(err, 'جست‌وجو ناموفق بود'), 'error');
            return;
        }
        toggleLoader(false);

        if (!candidates.length) {
            Swal.fire('نتیجه‌ای نبود', 'با متن دیگری امتحان کن (مثلاً فقط شماره مدل).', 'info');
            return;
        }

        // ⚠️ حتی اگر یک نتیجه باشد، انتخاب با انسان است
        const html = `
            <div style="text-align:right;font-size:12px;color:#666;margin-bottom:8px">
                محصول ما: <b>${escapeHTML(row.name || '')}</b>${row.onlinePrice ? ` — قیمت ما: ${fmtMoney(row.onlinePrice)} تومان` : ''}
            </div>
            <div style="text-align:right">
            ${candidates.map((c, i) => `
                <label style="display:flex;gap:8px;align-items:flex-start;padding:8px;border:1px solid #eee;border-radius:8px;margin-bottom:6px;cursor:pointer">
                    <input type="radio" name="dkcand" value="${i}" style="margin-top:4px">
                    <span style="flex:1">
                        <span style="font-size:12px;font-weight:700">${escapeHTML(c.title || '')}</span><br>
                        <span style="font-size:11px;color:#16a34a">${c.priceToman ? fmtMoney(c.priceToman) + ' تومان' : 'ناموجود'}</span>
                        <span style="font-size:10px;color:#999"> — DKP ${escapeHTML(c.externalId)}</span>
                        <a href="${escapeHTML(c.url)}" target="_blank" rel="noopener" style="font-size:10px;margin-right:6px">مشاهده ↗</a>
                    </span>
                </label>`).join('')}
            </div>`;

        const pick = await Swal.fire({
            title: 'کدام محصول درست است؟',
            html, width: 620, showCancelButton: true,
            confirmButtonText: 'همین است، ذخیره کن', cancelButtonText: 'هیچ‌کدام',
            preConfirm: () => {
                const sel = document.querySelector('input[name="dkcand"]:checked');
                if (!sel) { Swal.showValidationMessage('یکی را انتخاب کن'); return false; }
                return Number(sel.value);
            }
        });
        if (!pick.isConfirmed) return;

        const chosen = candidates[pick.value];
        toggleLoader(true);
        try {
            await axios.post(`${API}/v1/pricing/marketplace/link`,
                {productId: id, market: 'digikala', externalId: chosen.externalId, url: chosen.url});
            await refreshDigikala(id, true); // بلافاصله قیمت را هم بگیر
        } catch (err) {
            Swal.fire('خطا', serverError(err, 'ذخیره شناسه ناموفق بود'), 'error');
        } finally {
            toggleLoader(false);
        }
    }

    /** دیجی‌کالا مرحلهٔ B — به‌روزرسانیِ یک‌کلیکی برایِ محصولِ تأییدشده. */
    async function refreshDigikala(id, silent) {
        toggleLoader(true);
        try {
            const res = await axios.post(`${API}/v1/pricing/marketplace/refresh`,
                {productId: id, market: 'digikala', force: true});
            const d = res.data;
            await fetchPricingRows();

            if (d.status === 'ok' || d.status === 'unchanged') {
                const warn = d.weAreAboveMarket
                    ? '<div style="color:#b45309;font-size:12px;margin-top:6px">⚠️ قیمت ما از کف بازار بالاتر است — تصمیم با شماست، چیزی خودکار تغییر نکرد.</div>'
                    : '';
                Swal.fire({icon: 'success', title: 'کف قیمت به‌روز شد',
                    html: `<div style="font-size:13px">${fmtMoney(d.price)} تومان${warn}</div>`,
                    timer: warn ? undefined : 1600, showConfirmButton: !!warn});
            } else if (d.status === 'out-of-range') {
                Swal.fire('ذخیره نشد', d.message, 'warning');
            } else if (d.status === 'not-found') {
                Swal.fire('قیمتی برگردانده نشد', d.message, 'info');
            } else if (!silent) {
                Swal.fire('توجه', d.message || d.status, 'info');
            }
        } catch (err) {
            if (err.response && err.response.status === 403) {
                Swal.fire('دسترسی ندارید', 'نقش شما اجازه به‌روزرسانی کف قیمت را ندارد.', 'error');
            } else {
                Swal.fire('خطا', serverError(err, 'به‌روزرسانی ناموفق بود'), 'error');
            }
        } finally {
            toggleLoader(false);
        }
    }

    /** به‌روزرسانیِ همه — فقط محصولاتی که هویتشان قبلاً تأیید شده. */
    async function refreshAllDigikala() {
        const linked = pricingRows.filter(r => r.digikalaDkp).length;
        if (!linked) {
            Swal.fire('محصول متصلی نیست', 'اول با دکمه «پیداکردن» چند محصول را به دیجی‌کالا وصل کن.', 'info');
            return;
        }
        const ok = await Swal.fire({
            icon: 'question', title: `به‌روزرسانی ${linked} محصول؟`,
            html: '<div style="font-size:13px;text-align:right">فقط محصولاتی که قبلاً تأیید شده‌اند به‌روز می‌شوند.<br>بین درخواست‌ها فاصله گذاشته می‌شود، پس ممکن است طول بکشد.</div>',
            showCancelButton: true, confirmButtonText: 'شروع', cancelButtonText: 'انصراف'
        });
        if (!ok.isConfirmed) return;

        toggleLoader(true);
        try {
            const res = await axios.post(`${API}/v1/pricing/marketplace/refresh-all`, {market: 'digikala'});
            const d = res.data;
            await fetchPricingRows();
            const problems = (d.problems || []).slice(0, 8)
                .map(p => `• ${escapeHTML(p.name)}: ${escapeHTML(p.message || p.status)}`).join('<br>');
            Swal.fire({
                icon: d.failed ? 'warning' : 'success',
                title: `${d.ok} به‌روز شد`,
                html: `<div style="font-size:12px;text-align:right">
                        کل: ${d.total} | موفق: ${d.ok} | ردشده: ${d.skipped} | مشکل‌دار: ${d.failed}
                        ${problems ? '<hr style="margin:8px 0">' + problems : ''}</div>`
            });
        } catch (err) {
            Swal.fire('خطا', serverError(err, 'به‌روزرسانی گروهی ناموفق بود'), 'error');
        } finally {
            toggleLoader(false);
        }
    }

    // «بازگشت به فرمول»: پرچمِ دستی برداشته می‌شود و قیمتِ سایت دوباره از
    // «فروش تعدادی × ضریب» محاسبه می‌شود.
    async function revertToFormula(id) {
        const row = pricingRows.find(r => r.id === id);
        const ok = await Swal.fire({
            icon: 'question',
            title: 'بازگشت به فرمول؟',
            html: `<div style="font-size:13px;text-align:right">قیمت دستیِ <b>${escapeHTML(row ? row.name : '')}</b> برداشته می‌شود و
                   قیمت سایت دوباره از «فروش تعدادی × ضریب» محاسبه خواهد شد.
                   ${row && row.suggestedOnlinePrice ? `<br><br>مقدار جدید: <b>${fmtMoney(row.suggestedOnlinePrice)}</b> تومان` : ''}</div>`,
            showCancelButton: true, confirmButtonText: 'بله', cancelButtonText: 'انصراف'
        });
        if (!ok.isConfirmed) return;

        toggleLoader(true);
        try {
            await axios.post(`${API}/v1/pricing/batch`, [{id, field: 'priceOverride', value: false}]);
            await fetchPricingRows();
            Swal.fire({icon: 'success', title: 'به فرمول برگشت', timer: 1200, showConfirmButton: false});
        } catch (err) {
            Swal.fire('خطا', serverError(err, 'بازگشت به فرمول ناموفق بود'), 'error');
        } finally {
            toggleLoader(false);
        }
    }

    /** دکمهٔ «افزودن محصول» فقط برایِ نقشی که حقِ نوشتنِ محصول دارد. */
    function syncAddProductButton() {
        const btn = document.getElementById('pricing-add-product');
        if (btn) btn.classList.toggle('hidden', !pricingCanEdit);
    }

    function updatePricingDirtyUi() {
        document.getElementById('pricing-dirty-count').innerText = pricingDirty.size;
        document.getElementById('pricing-save-btn').disabled = pricingDirty.size === 0;
    }

    async function savePricingChanges() {
        if (!pricingDirty.size) return;
        const payload = [...pricingDirty.values()];
        toggleLoader(true);
        try {
            let res = await axios.post(`${API}/v1/pricing/batch`, payload);

            // تلهٔ هزاربرابری: سرور ردیف‌هایِ جهش‌دار را اعمال نکرده و برگردانده
            if (res.data.needsConfirm && res.data.needsConfirm.length) {
                const list = res.data.needsConfirm.map(c =>
                    `<div style="text-align:right">• <b>${escapeHTML(c.name)}</b> — ${c.field}: ${fmtMoney(c.oldValue)} → <b>${fmtMoney(c.newValue)}</b></div>`).join('');
                const ok = await Swal.fire({
                    icon: 'warning',
                    title: 'تغییر قیمت غیرعادی',
                    html: `<div style="font-size:13px">این تغییرها بیش از ۱۰ برابر (یا کمتر از یک‌دهم) مقدار قبلی‌اند.<br>واحد <b>تومان</b> است — مطمئنید؟<br><br>${list}</div>`,
                    showCancelButton: true, confirmButtonText: 'بله، اعمال کن', cancelButtonText: 'انصراف'
                });
                if (ok.isConfirmed) {
                    const confirmedPayload = res.data.needsConfirm.map(c => ({id: c.id, field: c.field, value: c.newValue, confirmed: true}));
                    const res2 = await axios.post(`${API}/v1/pricing/batch`, confirmedPayload);
                    res = {data: {applied: (res.data.applied || 0) + (res2.data.applied || 0), errors: [...(res.data.errors||[]), ...(res2.data.errors||[])], needsConfirm: []}};
                }
            }

            const errs = res.data.errors || [];
            await Swal.fire({
                icon: errs.length ? 'warning' : 'success',
                title: `${res.data.applied} تغییر ثبت شد`,
                html: errs.length ? `<div style="font-size:12px;text-align:right">${errs.map(escapeHTML).join('<br>')}</div>` : undefined,
                timer: errs.length ? undefined : 1500,
                showConfirmButton: !!errs.length
            });
            await fetchPricingRows();
        } catch (err) {
            if (err.response && err.response.status === 403) {
                Swal.fire('دسترسی ندارید', 'نقش شما اجازه ویرایش قیمت را ندارد.', 'error');
            } else {
                Swal.fire('خطا', serverError(err, 'ذخیره تغییرات ناموفق بود'), 'error');
            }
        } finally {
            toggleLoader(false);
        }
    }

    function exportPricingExcel() {
        const q = (document.getElementById('pricing-search').value || '').trim().toLowerCase();
        const rows = pricingRows.filter(r => !q || (r.name || '').toLowerCase().includes(q));
        const head = ['نام محصول','قیمت سایت (تومان)','همکار تک (تومان)','فروش تعدادی (تومان)','قیمت دلاری ($)','اصفهان','تهران','در راه','موجودی فروش','کف ترب (تومان)','لینک ترب','کف دیجی‌کالا (تومان)','لینک دیجی‌کالا','خیلی بفروشید'];
        const esc = v => `"${String(v ?? '').replace(/"/g, '""')}"`;
        const csv = [head.map(esc).join(',')].concat(rows.map(r => [
            r.name, r.onlinePrice, r.partnerUnitPrice, r.partnerBulkPrice, r.dollarPrice, r.stockIsfahan, r.stockTehran,
            r.incomingStock, r.sellableStock, r.torobFloorPrice, r.torobUrl,
            r.digikalaFloorPrice, r.digikalaUrl, r.pushSaleFlag ? 'بله' : 'خیر'
        ].map(esc).join(','))).join('\n');

        // BOM تا اکسل فارسی را درست بخواند
        const blob = new Blob(['﻿' + csv], {type: 'text/csv;charset=utf-8;'});
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = `pricing-${new Date().toISOString().slice(0,10)}.csv`;
        a.click();
        URL.revokeObjectURL(a.href);
    }

    // ==========================================
    // لاگ فعالیت
    // ==========================================
    const LOG_ACTION_LABELS = {
        PRICE_CHANGE: 'تغییر قیمت', FLOOR_PRICE_CHANGE: 'تغییر کف رقبا',
        FLAG_CHANGE: 'تغییر پرچم فروش', LOGIN: 'ورود به سیستم',
        STOCK_CHANGE: 'اصلاح موجودی',
        PRODUCT_CREATE: 'افزودن محصول', PRODUCT_UPDATE: 'ویرایش محصول',
        PRODUCT_DELETE: 'حذف محصول',
        ANALYTICS_EXPORT: 'خروجیِ دادهٔ رفتاری', ANALYTICS_ERASE: 'حذفِ دادهٔ رفتاریِ کاربر'
    };
    const LOG_SOURCE_LABELS = {MANUAL: 'دستی', DERIVED: 'خودکار (فرمول)', BATCH: 'دسته‌ای', HOLOO: 'هلو'};
    // نوعِ موجودیت — پایه‌ی دوقسمتی‌شدنِ لاگ؛ نوعِ جدید فقط یک ردیف اینجا می‌خواهد
    const LOG_ENTITY_LABELS = {PRODUCT: 'محصول', SETTINGS: 'تنظیمات', CATEGORY: 'دسته', ARTICLE: 'مقاله', USER: 'کاربر', ANALYTICS: 'دادهٔ رفتاری'};

    async function fetchActivityLogs() {
        toggleLoader(true);
        try {
            const params = new URLSearchParams();
            const u = document.getElementById('log-username').value.trim();
            const f = document.getElementById('log-from').value;
            const t = document.getElementById('log-to').value;
            // سلکتِ نوعِ رویداد فقط در پنلِ ادمین وجود دارد
            const typeSel = document.getElementById('log-entity-type');
            const et = typeSel ? typeSel.value : '';
            if (u) params.set('username', u);
            if (et) params.set('entityType', et);
            if (f) params.set('from', f);
            if (t) params.set('to', t);
            params.set('size', '200');

            // پنلِ فروش اندپوینتِ فقط-محصولات را می‌خواند؛ محدودسازی سمتِ سرور است
            // تا رویدادهایِ مدیریتی (تغییرِ ضریب، کاربران) آنجا دیده نشوند.
            const path = logProductsOnly ? '/v1/pricing/logs/products' : '/v1/pricing/logs';
            const res = await axios.get(`${API}${path}?${params.toString()}`);
            const items = (res.data && res.data.content) || [];
            document.getElementById('log-body').innerHTML = items.length ? items.map(l => `
                <tr class="hover:bg-gray-50">
                    <td class="p-2 text-gray-500 whitespace-nowrap">${new Date(l.at).toLocaleString('fa-IR')}</td>
                    <td class="p-2 font-medium">${escapeHTML(l.username || '')}</td>
                    <td class="p-2">${LOG_ACTION_LABELS[l.action] || l.action || ''}</td>
                    <td class="p-2">
                        ${escapeHTML(l.productName || '—')}
                        ${l.entityType ? `<span class="text-[9px] text-gray-400 mr-1">(${LOG_ENTITY_LABELS[l.entityType] || l.entityType})</span>` : ''}
                    </td>
                    <td class="p-2 text-gray-500" dir="ltr">${escapeHTML(l.field || '—')}</td>
                    <td class="p-2 text-red-600" dir="ltr">${escapeHTML(l.oldValue ?? '—')}</td>
                    <td class="p-2 text-green-700 font-bold" dir="ltr">${escapeHTML(l.newValue ?? '—')}</td>
                    <td class="p-2 text-gray-400">${LOG_SOURCE_LABELS[l.source] || l.source || ''}</td>
                </tr>`).join('')
                : '<tr><td colspan="8" class="p-8 text-center text-gray-400">رویدادی ثبت نشده است</td></tr>';
        } catch (err) {
            if (err.response && err.response.status === 403) {
                document.getElementById('log-body').innerHTML =
                    '<tr><td colspan="8" class="p-8 text-center text-amber-600 font-bold">فقط ادمین به لاگ فعالیت دسترسی دارد.</td></tr>';
            } else {
                Swal.fire('خطا', serverError(err, 'دریافت لاگ ناموفق بود'), 'error');
            }
        } finally {
            toggleLoader(false);
        }
    }
  // onclickهایِ داخلِ مارک‌آپ سراسری‌اند، پس این‌ها روی window قرار می‌گیرند.
  Object.assign(window, {
    fetchPricingRows, renderPricingRows, onPricingEdit, onPricingFlag,
    savePricingChanges, exportPricingExcel, revertToFormula,
    findDigikala, refreshDigikala, openTorobSearch, refreshAllDigikala,
    daysAgoLabel, fetchActivityLogs, restrictPanelToPricingWorkspace,
    addProductFromPricing, completeProductCard, applyPricingPercent,
    togglePricingExclude, syncMikrotikPrices, onPricePaste
  });
  // میزبان (Admin.html) بعد از تشخیصِ نقش این را ست می‌کند
  Object.defineProperty(window, 'pricingCanEdit', {
    get: () => pricingCanEdit, set: v => { pricingCanEdit = v; }, configurable: true
  });
  Object.defineProperty(window, 'pricingRows', { get: () => pricingRows, configurable: true });
  Object.defineProperty(window, 'pricingFactor', { get: () => pricingFactor, configurable: true });
})();
