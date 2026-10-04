/**
 * بخشِ «مشخصاتِ رادیویی» در مودالِ ویرایشِ محصول — شیءِ rf قراردادِ داده‌لینک (نسخهٔ ۲).
 *
 * جدا از ذخیرهٔ خودِ محصول ذخیره می‌شود (PUT /api/v1/rf-specs/{id})، و فقط اگر چیزی عوض
 * شده باشد. دلیل: updateProduct نگهبان‌های دیتالاس دارد و rf را لمس نمی‌کند؛ این‌طور
 * ذخیرهٔ عادیِ مودال هرگز rf را نال نمی‌کند.
 *
 * 🔴 خانهٔ خالی یعنی «منبع نداده» و نال فرستاده می‌شود، نه صفر.
 * 🔴 هیچ مقداری با innerHTML از داده نمی‌نشیند؛ مقدارها با .value پر می‌شوند.
 */
(function () {
    let original = null;   // JSONِ rf هنگامِ بازشدن، برای «عوض شده؟»

    const KINDS = [['', '— بدونِ دادهٔ رادیویی —'], ['ANTENNA', 'آنتن'], ['RADIO', 'رادیوی کانکتوردار'], ['RADIO_INTEGRATED', 'رادیو با آنتنِ داخلی']];
    const TYPES = ['', 'DISH', 'SECTOR', 'OMNI', 'PANEL', 'GRID', 'HORN', 'OTHER'];
    const POLS = ['', 'SINGLE', 'DUAL'];
    const inp = (cls, ph, ltr) => `<input class="${cls} p-1.5 border rounded text-xs w-full" placeholder="${ph}"${ltr ? ' dir="ltr"' : ''}>`;
    const sel = (cls, opts) => `<select class="${cls} p-1.5 border rounded text-xs w-full">${opts.map(o => Array.isArray(o) ? `<option value="${o[0]}">${o[1]}</option>` : `<option value="${o}">${o || '—'}</option>`).join('')}</select>`;

    const SHELL = `
      <div class="flex justify-between items-center border-b pb-2 mb-3">
        <h4 class="text-emerald-800 font-bold flex gap-2 text-sm md:text-base"><span>📡</span> مشخصاتِ رادیویی (داده‌لینک)</h4>
      </div>
      <p class="text-[10px] text-gray-500 mb-2 leading-5">برای پیشنهادِ کالا و «محاسبه بر اساسِ تجهیزات». خانهٔ خالی یعنی «منبع نداده»، نه صفر. جدا از بقیهٔ مودال و فقط اگر عوض شده باشد ذخیره می‌شود.</p>
      <div class="grid grid-cols-2 md:grid-cols-4 gap-2 mb-3">
        <label class="text-[11px] text-gray-600 col-span-2">نوع ${sel('rf-kind', KINDS)}</label>
        <label class="text-[11px] text-gray-600 col-span-2">منبع (دیتاشیتِ سازنده) ${inp('rf-src-url', 'https://…', true)}</label>
        <label class="text-[11px] text-gray-600">تاریخِ بررسی ${inp('rf-src-checked', '۱۴۰۵/۰۷/۱۲')}</label>
      </div>
      <div class="mb-3"><div class="flex justify-between items-center"><b class="text-[11px] text-gray-700">باندها (مگاهرتز)</b>
        <button type="button" data-rf-add="band" class="text-[11px] bg-emerald-600 text-white px-2 py-0.5 rounded">＋ باند</button></div>
        <div data-rf-bands class="space-y-1 mt-1"></div></div>
      <div data-rf-antenna class="grid grid-cols-2 md:grid-cols-5 gap-2 mb-3 bg-emerald-50/50 p-2 rounded">
        <label class="text-[11px] text-gray-600">بهره (dBi) ${inp('rf-a-gain', '', true)}</label>
        <label class="text-[11px] text-gray-600">نوعِ آنتن ${sel('rf-a-type', TYPES)}</label>
        <label class="text-[11px] text-gray-600">قطر (cm) ${inp('rf-a-dia', '', true)}</label>
        <label class="text-[11px] text-gray-600">پهنای پرتو (°) ${inp('rf-a-beam', '', true)}</label>
        <label class="text-[11px] text-gray-600">قطبش ${sel('rf-a-pol', POLS)}</label>
      </div>
      <div data-rf-radio class="grid grid-cols-2 md:grid-cols-3 gap-2 mb-3 bg-sky-50/50 p-2 rounded">
        <label class="text-[11px] text-gray-600">بیشینهٔ توان (dBm) ${inp('rf-r-tx', '', true)}</label>
        <label class="text-[11px] text-gray-600 md:col-span-2">شرطِ توان ${inp('rf-r-txc', 'MCS0, 5 GHz')}</label>
        <label class="text-[11px] text-gray-600">حساسیت در کمترین نرخ ${inp('rf-r-sl', '-96', true)}</label>
        <label class="text-[11px] text-gray-600 md:col-span-2">شرط ${inp('rf-r-slc', 'MCS0, 20 MHz')}</label>
        <label class="text-[11px] text-gray-600">حساسیت در بیشترین نرخ ${inp('rf-r-sh', '-70', true)}</label>
        <label class="text-[11px] text-gray-600 md:col-span-2">شرط ${inp('rf-r-shc', 'MCS9, 80 MHz')}</label>
        <label class="text-[11px] text-gray-600 md:col-span-3">خانوادهٔ سازگاری ${inp('rf-r-fam', 'UBNT_AIRMAX_AC', true)}</label>
      </div>
      <div data-rf-rates-box><div class="flex justify-between items-center"><b class="text-[11px] text-gray-700">جدولِ نرخ‌ها</b>
        <button type="button" data-rf-add="rate" class="text-[11px] bg-sky-600 text-white px-2 py-0.5 rounded">＋ نرخ</button></div>
        <div class="grid grid-cols-8 gap-1 text-[10px] text-gray-400 mt-1" dir="ltr"><span>min MHz</span><span>max MHz</span><span>channel</span><span>rate</span><span>tx dBm</span><span>sens dBm</span><span>Mbps</span><span></span></div>
        <div data-rf-rates class="space-y-1 mt-1 max-h-64 overflow-y-auto"></div></div>`;

    let host = null;

    function bandRow() {
        return `<div class="rf-band grid grid-cols-3 gap-1" dir="ltr">${inp('rf-b-min', 'min')}${inp('rf-b-max', 'max')}<button type="button" class="text-red-500 text-xs" data-rf-del>✕</button></div>`;
    }
    function rateRow() {
        return `<div class="rf-rate grid grid-cols-8 gap-1" dir="ltr">${inp('rf-x-min', '')}${inp('rf-x-max', '')}${inp('rf-x-ch', '')}${inp('rf-x-label', '')}${inp('rf-x-tx', '')}${inp('rf-x-sens', '')}${inp('rf-x-mbps', '')}<button type="button" class="text-red-500 text-xs" data-rf-del>✕</button></div>`;
    }
    const q = (s, root) => (root || host).querySelector(s);
    const setv = (s, v, root) => { const el = q(s, root); if (el) el.value = (v === null || v === undefined) ? '' : String(v); };

    function render(hostEl, rf) {
        host = hostEl;
        host.hidden = false;   // پنلِ فروش این اسکریپت را ندارد و بخش پنهان می‌ماند
        host.innerHTML = SHELL;
        host.onclick = onClick;   // نه addEventListener: هر بازشدنِ مودال یک شنونده اضافه می‌کرد
        q('.rf-kind').addEventListener('change', toggleGroups);
        rf = rf || null;
        setv('.rf-kind', rf ? rf.kind : '');
        setv('.rf-src-url', rf && rf.source ? rf.source.url : '');
        setv('.rf-src-checked', rf && rf.source ? rf.source.checked : '');
        (rf && rf.bands || []).forEach(b => { q('[data-rf-bands]').insertAdjacentHTML('beforeend', bandRow()); const r = q('[data-rf-bands]').lastElementChild; setv('.rf-b-min', b.minMhz, r); setv('.rf-b-max', b.maxMhz, r); });
        const a = rf && rf.antenna || {};
        setv('.rf-a-gain', a.gainDbi); setv('.rf-a-type', a.type || ''); setv('.rf-a-dia', a.diameterCm); setv('.rf-a-beam', a.beamwidthDeg); setv('.rf-a-pol', a.polarization || '');
        const r = rf && rf.radio || {};
        setv('.rf-r-tx', r.txMaxDbm); setv('.rf-r-txc', r.txCond); setv('.rf-r-sl', r.sensLowDbm); setv('.rf-r-slc', r.sensLowCond);
        setv('.rf-r-sh', r.sensHighDbm); setv('.rf-r-shc', r.sensHighCond); setv('.rf-r-fam', r.compatFamily);
        (rf && rf.rates || []).forEach(x => {
            q('[data-rf-rates]').insertAdjacentHTML('beforeend', rateRow());
            const row = q('[data-rf-rates]').lastElementChild;
            setv('.rf-x-min', x.minMhz, row); setv('.rf-x-max', x.maxMhz, row); setv('.rf-x-ch', x.channelMhz, row); setv('.rf-x-label', x.rateLabel, row);
            setv('.rf-x-tx', x.txDbm, row); setv('.rf-x-sens', x.sensDbm, row); setv('.rf-x-mbps', x.rateMbps, row);
        });
        toggleGroups();
        original = JSON.stringify(collect());
    }

    function onClick(e) {
        const add = e.target.closest('[data-rf-add]');
        if (add) q(add.dataset.rfAdd === 'band' ? '[data-rf-bands]' : '[data-rf-rates]').insertAdjacentHTML('beforeend', add.dataset.rfAdd === 'band' ? bandRow() : rateRow());
        const del = e.target.closest('[data-rf-del]');
        if (del) del.parentElement.remove();
    }

    function toggleGroups() {
        const k = q('.rf-kind').value;
        q('[data-rf-antenna]').style.display = (k === 'ANTENNA' || k === 'RADIO_INTEGRATED') ? '' : 'none';
        q('[data-rf-radio]').style.display = (k === 'RADIO' || k === 'RADIO_INTEGRATED') ? '' : 'none';
        q('[data-rf-rates-box]').style.display = (k === 'RADIO' || k === 'RADIO_INTEGRATED') ? '' : 'none';
    }

    /** خالی → null؛ ارقامِ فارسی پذیرفته می‌شوند. */
    function num(el) {
        const v = (el && el.value || '').trim().replace(/[۰-۹]/g, c => '۰۱۲۳۴۵۶۷۸۹'.indexOf(c)).replace(/[٫،]/g, '.').replace(/−/g, '-');
        if (v === '') return null;
        const n = Number(v);
        return Number.isFinite(n) ? n : NaN;
    }
    const txt = el => { const v = (el && el.value || '').trim(); return v === '' ? null : v; };

    function collect() {
        const kind = q('.rf-kind').value;
        if (!kind) return null;
        const rf = {kind, bands: [], rates: []};
        host.querySelectorAll('.rf-band').forEach(r => { const mn = num(q('.rf-b-min', r)), mx = num(q('.rf-b-max', r)); if (mn !== null || mx !== null) rf.bands.push({minMhz: mn, maxMhz: mx}); });
        if (kind !== 'RADIO') rf.antenna = {gainDbi: num(q('.rf-a-gain')), type: txt(q('.rf-a-type')), diameterCm: num(q('.rf-a-dia')), beamwidthDeg: num(q('.rf-a-beam')), polarization: txt(q('.rf-a-pol'))};
        if (kind !== 'ANTENNA') {
            rf.radio = {txMaxDbm: num(q('.rf-r-tx')), txCond: txt(q('.rf-r-txc')), sensLowDbm: num(q('.rf-r-sl')), sensLowCond: txt(q('.rf-r-slc')),
                sensHighDbm: num(q('.rf-r-sh')), sensHighCond: txt(q('.rf-r-shc')), compatFamily: txt(q('.rf-r-fam'))};
            host.querySelectorAll('.rf-rate').forEach(r => rf.rates.push({minMhz: num(q('.rf-x-min', r)), maxMhz: num(q('.rf-x-max', r)), channelMhz: num(q('.rf-x-ch', r)),
                rateLabel: txt(q('.rf-x-label', r)), txDbm: num(q('.rf-x-tx', r)), sensDbm: num(q('.rf-x-sens', r)), rateMbps: num(q('.rf-x-mbps', r))}));
        }
        const url = txt(q('.rf-src-url')), checked = txt(q('.rf-src-checked'));
        if (url || checked) rf.source = {url, checked};
        return rf;
    }

    /** پس از ذخیرهٔ موفقِ محصول؛ اگر چیزی عوض نشده، هیچ درخواستی نمی‌رود. */
    async function save(productId) {
        if (!host || !productId) return;
        const now = collect();
        if (JSON.stringify(now) === original) return;
        if (now && JSON.stringify(now).includes('NaN')) throw new Error('یکی از خانه‌های عددیِ مشخصاتِ رادیویی عدد نیست');
        const api = '/api/v1/rf-specs/' + encodeURIComponent(productId);
        if (now === null) await axios.delete(api);
        else await axios.put(api, now);
        original = JSON.stringify(now);
    }

    window.RfSpecEditor = {render, save, collect};
})();
