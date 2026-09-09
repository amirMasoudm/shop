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
            </div>
            <div class="flex flex-wrap gap-2">
                <button onclick="refreshAllDigikala()" class="px-4 py-2 rounded-lg border text-gray-600 hover:bg-gray-50 text-sm font-bold" title="فقط محصولاتی که قبلاً به دیجی‌کالا وصل شده‌اند">↻ به‌روزرسانی کف دیجی‌کالا</button>
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

        <div class="mb-3 flex flex-col md:flex-row gap-2">
            <input type="text" id="pricing-search" placeholder="جستجوی نام محصول…" oninput="renderPricingRows()"
                   class="flex-1 p-2.5 border rounded-lg outline-none text-sm">
            <label class="flex items-center gap-2 text-xs text-gray-600 bg-white border rounded-lg px-3">
                <input type="checkbox" id="pricing-only-flagged" onchange="renderPricingRows()"> فقط «خیلی بفروشید»
            </label>
        </div>

        <div class="bg-white rounded-xl border shadow-sm overflow-x-auto" style="max-height:70vh; overflow-y:auto;">
            <table class="w-full text-right min-w-[1250px]">
                <thead class="bg-gray-50 text-gray-500 text-[11px] sticky top-0 z-10">
                <tr>
                    <th class="p-3">نام محصول</th>
                    <th class="p-3">قیمت سایت (تومان)</th>
                    <th class="p-3">همکار تک (تومان)</th>
                    <th class="p-3" title="قیمت عمده / خرید چندتایی">فروش تعدادی (تومان)</th>
                    <th class="p-3" title="قیمت خرید/مرجع به دلار">قیمت دلاری ($)</th>
                    <th class="p-3" title="فقط‌خواندنی — منبع: ورود دسته‌ای">اصفهان</th>
                    <th class="p-3" title="فقط‌خواندنی — منبع: ورود دسته‌ای">تهران</th>
                    <th class="p-3" title="خریداری‌شده ولی نرسیده — در موجودی فروش شمرده نمی‌شود">در راه</th>
                    <th class="p-3" title="کمترین قیمت رقبا در ترب (تومان)">کف ترب (تومان)</th>
                    <th class="p-3" title="کمترین قیمت رقبا در دیجی‌کالا (تومان)">کف دیجی‌کالا (تومان)</th>
                    <th class="p-3" title="همان ستون «خیلی بفروشید» شیت شرکت که با رنگ نارنجی مشخص شده بود">خیلی بفروشید</th>
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
            // کارشناسِ فروش هم ویرایش دارد؛ فقط یک ستون برایش قفل است، پس به‌جایِ
            // «فقط مشاهده» همان محدودیتِ واقعی نوشته می‌شود.
            const note = document.getElementById('pricing-readonly-note');
            if (!pricingCanEdit) {
                note.innerText = 'شما دسترسی «فقط مشاهده» دارید؛ ویرایش قیمت‌ها برای نقش شما فعال نیست.';
                note.classList.remove('hidden');
            } else if (!canEditBulkPrice) {
                note.innerText = 'ستون «فروش تعدادی» فقط توسط کارشناس قیمت‌گذاری قابل تغییر است؛ بقیه ستون‌ها برای شما باز است.';
                note.classList.remove('hidden');
            } else {
                note.classList.add('hidden');
            }
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
        const q = (document.getElementById('pricing-search').value || '').trim().toLowerCase();
        const onlyFlagged = document.getElementById('pricing-only-flagged').checked;

        const rows = pricingRows.filter(r => {
            if (onlyFlagged && !r.pushSaleFlag) return false;
            if (q && !(r.name || '').toLowerCase().includes(q)) return false;
            return true;
        });

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
                return `<span dir="ltr" class="text-gray-500"${why}>${shownVal}</span>`;
            }
            const key = `${r.id}|${field}`;
            const pending = pricingDirty.has(key);
            const shown = pending ? pricingDirty.get(key).value : (value ?? '');
            // oninput نه onchange: onchange فقط موقعِ ازدست‌دادنِ فوکوس شلیک می‌شود،
            // یعنی کاربر باید Tab بزند/جای دیگر کلیک کند تا نتیجه را ببیند.
            return `<input type="text" inputmode="decimal" value="${shown === null ? '' : shown}"
                       data-id="${r.id}" data-field="${field}"
                       oninput="onPricingEdit(this)"
                       class="${width} p-1.5 border rounded text-left ${pending ? 'bg-yellow-50 border-yellow-400' : ''}" dir="ltr">`;
        };

        document.getElementById('pricing-body').innerHTML = rows.length ? rows.map(r => `
            <tr class="hover:bg-gray-50 ${r.pushSaleFlag ? 'bg-orange-50' : ''}">
                <td class="p-2 font-medium text-gray-800">${escapeHTML(r.name || '')}</td>
                <td class="p-2">
                    ${priceInput(r, 'onlinePrice', r.onlinePrice)}
                    ${r.priceOverride ? `
                        <div class="flex items-center gap-1 mt-1">
                            <span class="text-[9px] bg-amber-100 text-amber-800 px-1.5 py-0.5 rounded font-bold" title="این قیمت دستی ثبت شده و با تغییر «فروش تعدادی» بازنویسی نمی‌شود">✋ دستی</span>
                            <span class="text-[9px] text-gray-400" dir="ltr" data-suggested-for="${r.id}" title="مقدار پیشنهادی فرمول">${r.suggestedOnlinePrice ? '≈' + fmtMoney(r.suggestedOnlinePrice) : ''}</span>
                            ${pricingCanEdit ? `<button onclick="revertToFormula('${r.id}')" class="text-[9px] text-indigo-600 hover:underline shrink-0" title="پرچم دستی برداشته و قیمت دوباره از فرمول محاسبه شود">بازگشت به فرمول</button>` : ''}
                        </div>` : ''}
                </td>
                <td class="p-2">${priceInput(r, 'partnerUnitPrice', r.partnerUnitPrice)}</td>
                <td class="p-2">${priceInput(r, 'partnerBulkPrice', r.partnerBulkPrice)}</td>
                <td class="p-2">${priceInput(r, 'dollarPrice', r.dollarPrice, 'w-20')}</td>
                <td class="p-2 text-center text-gray-500">${r.stockIsfahan ?? '—'}</td>
                <td class="p-2 text-center text-gray-500">${r.stockTehran ?? '—'}</td>
                <td class="p-2 text-center ${r.incomingStock ? 'text-blue-600 font-bold' : 'text-gray-400'}">${r.incomingStock ?? '—'}</td>
                <td class="p-2">
                    <div class="flex items-center gap-1">
                        ${priceInput(r, 'torobFloorPrice', r.torobFloorPrice, 'w-24')}
                        ${pricingCanEdit ? `<button onclick="openTorobSearch('${r.id}')" class="text-[10px] bg-gray-100 hover:bg-indigo-600 hover:text-white border rounded px-1.5 py-1 shrink-0" title="صفحه جست‌وجوی ترب را باز می‌کند؛ کمترین قیمت را ببین و اینجا وارد کن">🔍 ترب</button>` : ''}
                    </div>
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
        `).join('') : '<tr><td colspan="11" class="p-8 text-center text-gray-400">موردی یافت نشد</td></tr>';
    }

    function onPricingEdit(input) {
        const id = input.dataset.id, field = input.dataset.field;
        const raw = input.value.trim().replace(/,/g, '');
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
    async function openTorobSearch(id) {
        const row = pricingRows.find(r => r.id === id);
        if (!row) return;
        const {value: query} = await Swal.fire({
            title: 'جست‌وجو در ترب',
            input: 'text',
            inputValue: row.name || '',
            inputLabel: 'متن جست‌وجو (معمولاً فقط مدل بهتر جواب می‌دهد)',
            html: '<div style="font-size:12px;text-align:right;color:#666">ترب دسترسی خودکار را می‌بندد، پس صفحه برایت باز می‌شود؛ کمترین قیمت را ببین و در همان فیلد وارد کن.</div>',
            showCancelButton: true, confirmButtonText: 'باز کن', cancelButtonText: 'انصراف'
        });
        if (!query) return;
        try {
            const res = await axios.get(`${API}/v1/pricing/marketplace/search-url`,
                {params: {market: 'torob', query}});
            window.open(res.data.url, '_blank', 'noopener');
        } catch (err) {
            Swal.fire('خطا', serverError(err, 'ساخت آدرس ناموفق بود'), 'error');
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
        PRODUCT_CREATE: 'افزودن محصول', PRODUCT_UPDATE: 'ویرایش محصول',
        PRODUCT_DELETE: 'حذف محصول'
    };
    const LOG_SOURCE_LABELS = {MANUAL: 'دستی', DERIVED: 'خودکار (فرمول)', BATCH: 'دسته‌ای', HOLOO: 'هلو'};
    // نوعِ موجودیت — پایه‌ی دوقسمتی‌شدنِ لاگ؛ نوعِ جدید فقط یک ردیف اینجا می‌خواهد
    const LOG_ENTITY_LABELS = {PRODUCT: 'محصول', SETTINGS: 'تنظیمات', CATEGORY: 'دسته', ARTICLE: 'مقاله', USER: 'کاربر'};

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
    daysAgoLabel, fetchActivityLogs, restrictPanelToPricingWorkspace
  });
  // میزبان (Admin.html) بعد از تشخیصِ نقش این را ست می‌کند
  Object.defineProperty(window, 'pricingCanEdit', {
    get: () => pricingCanEdit, set: v => { pricingCanEdit = v; }, configurable: true
  });
  Object.defineProperty(window, 'pricingRows', { get: () => pricingRows, configurable: true });
  Object.defineProperty(window, 'pricingFactor', { get: () => pricingFactor, configurable: true });
})();
