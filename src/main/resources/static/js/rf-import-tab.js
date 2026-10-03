/**
 * تبِ «دادهٔ رادیویی» — واردکردنِ فایل‌های چت ب با پیش‌نمایش. فقط ADMIN.
 *
 * پیش‌نمایش هیچ چیزی نمی‌نویسد؛ برای هر محصول می‌گوید چه خانه‌ای چه می‌شود. «اعمال» فقط
 * rf را عوض می‌کند و یک رکوردِ خلاصه در لاگِ فعالیت می‌گذارد.
 * 🔴 مقدارها (نامِ محصول، یادداشت‌ها) از فایلِ بیرونی‌اند: فقط textContent.
 */
(function () {
    const API = '/api/v1/rf-specs/import';
    const STATUS = {new: ['تازه', 'text-green-700'], changed: ['تغییر', 'text-indigo-700'], same: ['بی‌تغییر', 'text-gray-400'],
        invalid: ['نامعتبر', 'text-red-600'], empty: ['بی‌داده', 'text-amber-700'], unmatched: ['بی‌محصول', 'text-red-600']};
    let host = null, mounted = false, token = null;

    function mount(el) {
        host = el;
        if (mounted) return;
        host.innerHTML = SHELL;
        host.querySelector('[data-rf-preview]').addEventListener('click', preview);
        host.querySelector('[data-rf-apply]').addEventListener('click', apply);
        mounted = true;
    }

    function csrf() {
        const m = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
        return m ? {'X-XSRF-TOKEN': decodeURIComponent(m[1])} : {};
    }

    async function preview() {
        const prod = host.querySelector('[data-rf-products]').files[0];
        const rates = host.querySelector('[data-rf-rates]').files[0];
        if (!prod) return Swal.fire('فایل لازم است', 'فایلِ محصولات (rf-specs-….csv) را انتخاب کنید.', 'info');
        const fd = new FormData();
        fd.append('products', prod);
        if (rates) fd.append('rates', rates);
        const box = host.querySelector('[data-rf-result]');
        box.textContent = 'در حالِ خواندن…';
        const res = await fetch(API + '/preview', {method: 'POST', body: fd, credentials: 'include', headers: csrf()});
        const body = await res.json().catch(() => ({}));
        if (!res.ok) { box.textContent = body.message || 'پیش‌نمایش ناموفق بود.'; return; }
        token = body.token;
        render(body);
    }

    function render(pv) {
        const box = host.querySelector('[data-rf-result]');
        box.innerHTML = '';
        const c = pv.counts;
        const sum = document.createElement('p');
        sum.className = 'text-sm text-gray-700 mb-3 leading-7';
        sum.textContent = 'خوانده: ' + pv.rowsRead + ' محصول و ' + pv.rateRowsRead + ' نرخ. تازه ' + c.new + ' · تغییر ' + c.changed
            + ' · بی‌تغییر ' + c.same + ' · نامعتبر ' + c.invalid + ' · بی‌داده ' + c.empty + ' · بی‌محصول ' + c.unmatched;
        box.appendChild(sum);
        const applyBtn = host.querySelector('[data-rf-apply]');
        applyBtn.hidden = !(c.new + c.changed);

        const table = document.createElement('table');
        table.className = 'w-full text-right text-xs';
        const head = document.createElement('tr');
        ['وضعیت', 'محصول', 'چه می‌شود', 'خطا و هشدار'].forEach(h => { const th = document.createElement('th'); th.className = 'p-2 bg-gray-50 text-gray-500'; th.textContent = h; head.appendChild(th); });
        table.appendChild(head);
        const order = ['invalid', 'unmatched', 'empty', 'new', 'changed', 'same'];
        pv.items.slice().sort((a, b) => order.indexOf(a.status) - order.indexOf(b.status)).forEach(it => {
            const tr = document.createElement('tr');
            tr.className = 'border-t align-top';
            const st = document.createElement('td');
            st.className = 'p-2 font-bold whitespace-nowrap ' + (STATUS[it.status] || ['', ''])[1];
            st.textContent = (STATUS[it.status] || [it.status])[0];
            const name = document.createElement('td');
            name.className = 'p-2';
            name.textContent = (it.holooCode ? it.holooCode + ' · ' : '') + (it.productName || it.fileName || it.productId || '—');
            const ch = document.createElement('td');
            ch.className = 'p-2';
            (it.changes || []).forEach(x => {
                const d = document.createElement('div');
                d.dir = 'ltr';
                d.style.cssText = 'text-align:left;unicode-bidi:isolate';
                d.textContent = x.field + ': ' + x.from + ' → ' + x.to;
                ch.appendChild(d);
            });
            const er = document.createElement('td');
            er.className = 'p-2';
            (it.errors || []).forEach(t => { const d = document.createElement('div'); d.className = 'text-red-600'; d.textContent = t; er.appendChild(d); });
            (it.warnings || []).forEach(t => { const d = document.createElement('div'); d.className = 'text-amber-700'; d.textContent = t; er.appendChild(d); });
            tr.append(st, name, ch, er);
            table.appendChild(tr);
        });
        const wrap = document.createElement('div');
        wrap.className = 'bg-white border rounded-xl overflow-x-auto';
        wrap.appendChild(table);
        box.appendChild(wrap);
    }

    async function apply() {
        if (!token) return;
        const ok = await Swal.fire({title: 'اعمالِ دادهٔ رادیویی؟', text: 'فقط مشخصاتِ رادیوییِ محصول‌های «تازه» و «تغییر» عوض می‌شود؛ نام و قیمت و متن دست نمی‌خورد.',
            icon: 'question', showCancelButton: true, confirmButtonText: 'اعمال', cancelButtonText: 'انصراف'});
        if (!ok.isConfirmed) return;
        const res = await fetch(API + '/apply', {method: 'POST', credentials: 'include', headers: {'Content-Type': 'application/json', ...csrf()}, body: JSON.stringify({token})});
        const body = await res.json().catch(() => ({}));
        token = null;
        host.querySelector('[data-rf-apply]').hidden = true;
        if (!res.ok) return Swal.fire('ناموفق', body.message || 'اعمال انجام نشد.', 'error');
        Swal.fire('اعمال شد', 'تازه ' + body.created + ' · عوض‌شده ' + body.changed + ' · بی‌تغییر ' + body.unchanged, 'success');
    }

    const SHELL = `
        <h2 class="text-2xl font-bold text-gray-800 mb-1">دادهٔ رادیویی</h2>
        <p class="text-[11px] text-gray-600 bg-gray-50 border rounded-lg p-3 mb-4 leading-6">
            فایل‌های چت ب: محصولات (<span dir="ltr">rf-specs-….csv</span>) و جدولِ نرخ‌ها (<span dir="ltr">rf-specs-mcs-….csv</span>).
            تطبیق فقط با <span dir="ltr">product_id</span>. اول پیش‌نمایش؛ تا «اعمال» را نزنید هیچ چیزی نوشته نمی‌شود.
            فقط مشخصاتِ رادیویی عوض می‌شود. اجرای دوباره با همان فایل تغییری نمی‌دهد.
        </p>
        <div class="bg-white border rounded-xl p-4 mb-4 grid grid-cols-1 md:grid-cols-3 gap-3 text-sm items-end">
            <label class="text-xs text-gray-600">فایلِ محصولات<input data-rf-products type="file" accept=".csv,text/csv" class="block mt-1 text-xs"></label>
            <label class="text-xs text-gray-600">فایلِ نرخ‌ها (اختیاری)<input data-rf-rates type="file" accept=".csv,text/csv" class="block mt-1 text-xs"></label>
            <div class="flex gap-2"><button data-rf-preview class="px-4 py-2 rounded-lg bg-indigo-600 text-white text-sm font-bold">پیش‌نمایش</button>
            <button data-rf-apply hidden class="px-4 py-2 rounded-lg bg-green-700 text-white text-sm font-bold">اعمال</button></div>
        </div>
        <div data-rf-result class="text-sm text-gray-500"></div>`;

    window.RfImportTab = {mount};
})();
