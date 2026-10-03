/**
 * تبِ «ریدایرکتِ آدرس‌های قدیمی».
 *
 * 🔴 <b>امنیت:</b> مسیرها از دنیای بیرون می‌آیند (فایلِ نگاشتِ وردپرس، و بعداً هر
 * چیزی که ادمین وارد می‌کند). هیچ‌جای این فایل innerHTML با دادهٔ ریدایرکت پر
 * نمی‌شود؛ همه با textContent می‌نشیند. اسکلتِ ثابت innerHTML دارد، مقدارها هرگز.
 *
 * 🔴 <b>خوانایی:</b> مسیرها لاتین و درصد-کدشده‌اند و در ردیفِ راست‌چین تکه‌تکه
 * جابه‌جا دیده می‌شوند. هر مسیر در جعبهٔ dir=ltr با unicode-bidi:isolate می‌نشیند —
 * همان درسی که در جدولِ رفتارِ کاربران گرفتیم.
 */
(function () {
    const API = '/api/v1/legacy-redirects';

    let host = null;
    let mounted = false;

    function mount(hostEl) {
        host = hostEl;
        if (!mounted) {
            host.innerHTML = SHELL;
            host.querySelector('[data-lr-reload]').addEventListener('click', load);
            host.querySelector('[data-lr-add]').addEventListener('click', addOne);
            host.querySelector('[data-lr-import]').addEventListener('click', importCsv);
            mounted = true;
        }
        load();
    }

    async function load() {
        const box = host.querySelector('[data-lr-list]');
        box.innerHTML = '';
        let rows;
        try {
            rows = await getJson(API);
        } catch (e) {
            return note(box, 'واکشی ناموفق بود.');
        }
        host.querySelector('[data-lr-count]').textContent =
            'مجموعاً ' + rows.length.toLocaleString('fa-IR') + ' آدرسِ قدیمی';
        if (!rows.length) {
            return note(box, 'هنوز ریدایرکتی ثبت نشده.');
        }

        const table = document.createElement('table');
        table.className = 'w-full text-right';
        const thead = document.createElement('thead');
        thead.className = 'bg-gray-50 text-xs text-gray-500';
        const htr = document.createElement('tr');
        ['برخورد', 'آخرین برخورد', 'آدرسِ قدیمی', 'مقصد', 'یادداشت', ''].forEach(h => {
            const th = document.createElement('th');
            th.className = 'p-3 font-bold';
            th.textContent = h;
            htr.appendChild(th);
        });
        thead.appendChild(htr);
        table.appendChild(thead);

        const tbody = document.createElement('tbody');
        tbody.className = 'divide-y text-xs';
        rows.forEach(r => tbody.appendChild(rowEl(r)));
        table.appendChild(tbody);

        const wrap = document.createElement('div');
        wrap.className = 'bg-white border rounded-xl overflow-x-auto';
        wrap.appendChild(table);
        box.appendChild(wrap);
    }

    function rowEl(r) {
        const tr = document.createElement('tr');
        tr.className = 'hover:bg-gray-50';

        const hits = document.createElement('td');
        hits.className = 'p-3 font-bold ' + (r.hits > 0 ? 'text-indigo-700' : 'text-gray-300');
        hits.textContent = Number(r.hits || 0).toLocaleString('fa-IR');
        tr.appendChild(hits);

        const last = document.createElement('td');
        last.className = 'p-3 text-gray-400 whitespace-nowrap';
        last.textContent = r.lastHitAt ? faDate(r.lastHitAt) : '—';
        tr.appendChild(last);

        tr.appendChild(pathCell(r.fromPath, false));
        tr.appendChild(pathCell(r.toPath, true));

        const note = document.createElement('td');
        note.className = 'p-3 text-gray-400';
        note.textContent = r.note || '—';
        tr.appendChild(note);

        const actions = document.createElement('td');
        actions.className = 'p-3';
        const del = document.createElement('button');
        del.type = 'button';
        del.className = 'text-red-600 font-bold';
        del.textContent = 'حذف';
        del.addEventListener('click', () => removeOne(r));
        actions.appendChild(del);
        tr.appendChild(actions);
        return tr;
    }

    /** مسیر: دیکدشده و خوانا، ولی داخلِ جعبهٔ LTR تا ردیفِ راست‌چین را نشکند. */
    function pathCell(rawPath, linked) {
        const td = document.createElement('td');
        td.className = 'p-3';
        const el = document.createElement(linked ? 'a' : 'span');
        if (linked) {
            el.href = rawPath;
            el.target = '_blank';
            el.rel = 'noopener';
            el.className = 'text-indigo-600 hover:underline';
        } else {
            el.className = 'text-gray-600';
        }
        el.dir = 'ltr';
        el.title = rawPath;
        el.style.cssText = 'unicode-bidi:isolate;direction:ltr;text-align:left;display:inline-block;'
            + 'white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:20rem;';
        el.textContent = decodePath(rawPath);   // ← textContent، نه innerHTML
        td.appendChild(el);
        return td;
    }

    /** ⚠️ در try/catch: مسیرِ خرابِ درصد-کدشده نباید کلِ جدول را بشکند. */
    function decodePath(p) {
        if (!p) return '';
        try { return decodeURIComponent(p); } catch (e) { return String(p); }
    }

    function faDate(value) {
        try {
            return new Date(value).toLocaleDateString('fa-IR', {calendar: 'persian'});
        } catch (e) { return '—'; }
    }

    async function addOne() {
        const from = host.querySelector('[data-lr-from]').value.trim();
        const to = host.querySelector('[data-lr-to]').value.trim();
        if (!from || !to) return alert('مسیرِ قدیمی و مقصد هر دو لازم‌اند.');
        try {
            const res = await fetch(API, {
                method: 'POST', credentials: 'include',
                headers: jsonHeaders(),
                body: JSON.stringify({fromPath: from, toPath: to, note: 'دستی'})
            });
            // پیامِ سرور همان‌طور که هست نشان داده می‌شود: وقتی مقصد وجود ندارد یا
            // مسیر رزروِ خودِ اپ است، خودش دلیلش را می‌گوید.
            if (!res.ok) return alert(await res.text());
            host.querySelector('[data-lr-from]').value = '';
            host.querySelector('[data-lr-to]').value = '';
            load();
        } catch (e) {
            alert('ثبت ناموفق بود.');
        }
    }

    async function importCsv() {
        const csv = host.querySelector('[data-lr-csv]').value.trim();
        if (!csv) return alert('محتوای CSV را بگذارید — هر خط: مسیرِ قدیمی، مقصد، یادداشت.');
        try {
            const res = await fetch(API + '/import', {
                method: 'POST', credentials: 'include',
                headers: Object.assign(jsonHeaders(), {'Content-Type': 'text/csv; charset=utf-8'}),
                body: csv
            });
            if (!res.ok) return alert(await res.text());
            const r = await res.json();
            const missing = r.skippedMissingTarget || [];
            const invalid = r.skippedInvalid || [];
            alert('انجام شد.\n'
                + Number(r.created).toLocaleString('fa-IR') + ' تازه، '
                + Number(r.updated).toLocaleString('fa-IR') + ' به‌روز.\n'
                + (missing.length ? missing.length.toLocaleString('fa-IR') + ' ردیف چون مقصدش وجود نداشت رد شد.\n' : '')
                + (invalid.length ? invalid.length.toLocaleString('fa-IR') + ' ردیف نامعتبر بود.' : ''));
            host.querySelector('[data-lr-csv]').value = '';
            load();
        } catch (e) {
            alert('واردکردن ناموفق بود.');
        }
    }

    async function removeOne(r) {
        if (!confirm('این ریدایرکت حذف شود؟ آدرسِ قدیمی از آن به بعد ۴۰۴ می‌گیرد.\n\n'
                + decodePath(r.fromPath))) return;
        try {
            const res = await fetch(API + '/' + r.id, {
                method: 'DELETE', credentials: 'include', headers: jsonHeaders()
            });
            if (!res.ok) return alert(await res.text());
            load();
        } catch (e) {
            alert('حذف ناموفق بود.');
        }
    }

    function note(box, text) {
        const el = document.createElement('div');
        el.className = 'bg-white border rounded-xl p-8 text-center text-gray-400 text-sm';
        el.textContent = text;
        box.appendChild(el);
    }

    function jsonHeaders() {
        const m = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
        const h = {'Content-Type': 'application/json'};
        if (m) h['X-XSRF-TOKEN'] = decodeURIComponent(m[1]);
        return h;
    }

    async function getJson(url) {
        const res = await fetch(url, {credentials: 'include'});
        if (!res.ok) throw new Error(await res.text());
        return res.json();
    }

    const SHELL = `
        <div class="flex flex-wrap items-center justify-between gap-3 mb-3">
            <div>
                <h2 class="text-2xl font-bold text-gray-800">ریدایرکتِ آدرس‌های قدیمی</h2>
                <p data-lr-count class="text-sm text-gray-500"></p>
            </div>
            <button data-lr-reload class="px-4 py-2 rounded-lg bg-indigo-600 text-white text-sm font-bold">نمایش</button>
        </div>

        <p class="text-[11px] text-amber-800 bg-amber-50 border border-amber-200 rounded-lg p-3 mb-4 leading-6">
            این‌ها آدرس‌هایی‌اند که سایتِ قدیم سال‌ها با آن‌ها ایندکس شده بود. هر کدام با کدِ ۳۰۱
            به صفحهٔ تازه‌اش می‌رود تا اعتبارش منتقل شود. ستونِ «برخورد» می‌گوید کدام‌یک واقعاً
            ترافیک دارند — بعد از سوییچِ دامنه، همین ستون تصمیم می‌گیرد کدام آدرسِ دیگری ارزشِ
            اضافه‌شدن دارد. حذفِ یک ردیف یعنی آن آدرس از آن به بعد ۴۰۴ می‌گیرد.
        </p>

        <div class="bg-white border rounded-xl p-4 mb-4">
            <h3 class="font-bold text-gray-700 mb-2 text-sm">افزودنِ تکی</h3>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-2 text-sm">
                <input data-lr-from placeholder="مسیرِ قدیمی — مثلاً /فلان-مقاله/" class="p-2 border rounded-lg" dir="ltr">
                <input data-lr-to placeholder="مقصد — مثلاً /blog/فلان-مقاله" class="p-2 border rounded-lg" dir="ltr">
                <button data-lr-add class="px-4 py-2 rounded-lg bg-gray-700 text-white text-sm font-bold">ثبت</button>
            </div>
            <p class="text-[11px] text-gray-500 mt-2 leading-6">
                مقصد باید واقعاً باز شود؛ اگر مقاله‌اش منتشر نشده باشد ثبت نمی‌شود — ریدایرکت به
                صفحهٔ ۴۰۴ از خودِ ۴۰۴ بدتر است.
            </p>
        </div>

        <details class="bg-white border rounded-xl p-4 mb-5">
            <summary class="font-bold text-gray-700 text-sm cursor-pointer">واردکردنِ دسته‌ای از CSV</summary>
            <textarea data-lr-csv rows="5" dir="ltr"
                class="w-full mt-3 p-2 border rounded-lg text-xs font-mono"
                placeholder="/old-path/,/blog/new-slug,یادداشت"></textarea>
            <button data-lr-import class="mt-2 px-4 py-2 rounded-lg bg-gray-700 text-white text-sm font-bold">واردکردن</button>
        </details>

        <div data-lr-list></div>`;

    window.LegacyRedirectsTab = {mount};
})();
