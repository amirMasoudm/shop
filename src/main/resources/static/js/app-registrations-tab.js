/**
 * تبِ «ثبت‌نام‌های اپ» — کاربرانی که در اپِ اندرویدی یا ابزارِ سایت ثبت کرده‌اند.
 * فقط ADMIN (هم سرور، هم این‌که تب برای نقش‌های دیگر نمایش داده نمی‌شود).
 *
 * 🔴 امنیت: نام و شرکت را خودِ کاربر تایپ کرده؛ هیچ مقداری با innerHTML نمی‌نشیند.
 * 🔴 تاریخ: شمسی از خودِ سرور می‌آید (dateFa)، نه از تقویمِ پیش‌فرضِ مرورگر.
 */
(function () {
    const API = '/api/v1/app-registrations';
    let host = null;
    let mounted = false;

    function mount(hostEl) {
        host = hostEl;
        if (!mounted) {
            host.innerHTML = SHELL;
            host.querySelector('[data-ar-reload]').addEventListener('click', load);
            mounted = true;
        }
        load();
    }

    async function load() {
        const box = host.querySelector('[data-ar-list]');
        box.innerHTML = '';
        let rows;
        try {
            const res = await fetch(API, {credentials: 'include'});
            if (!res.ok) throw new Error(res.status);
            rows = await res.json();
        } catch (e) {
            return note(box, 'واکشی ناموفق بود.');
        }
        const active = rows.filter(r => r.tokenActive).length;
        host.querySelector('[data-ar-count]').textContent =
            rows.length.toLocaleString('fa-IR') + ' ثبت‌نام · ' + active.toLocaleString('fa-IR') + ' دستگاهِ فعال';
        if (!rows.length) return note(box, 'هنوز کسی ثبت نکرده.');

        const table = document.createElement('table');
        table.className = 'w-full text-right';
        const htr = document.createElement('tr');
        ['تاریخ', 'نام', 'موبایل', 'شرکت', 'از', 'نسخه', 'وضعیت', ''].forEach(h => {
            const th = document.createElement('th');
            th.className = 'p-3 font-bold';
            th.textContent = h;
            htr.appendChild(th);
        });
        const thead = document.createElement('thead');
        thead.className = 'bg-gray-50 text-xs text-gray-500';
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
        tr.appendChild(cell(DnJalali.toFaDigits(r.dateFa || ''), 'text-gray-500 whitespace-nowrap'));
        tr.appendChild(cell(r.name || '—', 'font-bold text-gray-800'));
        const phone = cell(r.phone || '—', 'text-gray-700 whitespace-nowrap');
        phone.dir = 'ltr';
        phone.style.textAlign = 'right';
        tr.appendChild(phone);
        tr.appendChild(cell(r.company || '—', 'text-gray-600'));
        const from = r.client === 'android' ? 'اپِ اندروید' : 'سایت';
        tr.appendChild(cell(r.method === 'session' ? from + ' (ازقبل‌وارد)' : from, 'text-gray-600 whitespace-nowrap'));
        const ver = cell(r.appVersion || '—', 'text-gray-400');
        ver.dir = 'ltr';
        tr.appendChild(ver);
        tr.appendChild(cell(r.tokenActive ? 'فعال' : 'باطل‌شده',
            r.tokenActive ? 'text-green-700 font-bold' : 'text-gray-400'));

        const actions = document.createElement('td');
        actions.className = 'p-3';
        if (r.tokenActive) {
            const b = document.createElement('button');
            b.type = 'button';
            b.className = 'text-red-600 font-bold';
            b.textContent = 'ابطال';
            b.addEventListener('click', () => revoke(r));
            actions.appendChild(b);
        }
        tr.appendChild(actions);
        return tr;
    }

    async function revoke(r) {
        const ok = await Swal.fire({
            title: 'ابطالِ دسترسیِ این دستگاه؟',
            text: (r.name || '') + ' — بارِ بعد که ابزار را لمس کند، دوباره باید ثبت کند.',
            icon: 'warning', showCancelButton: true,
            confirmButtonText: 'ابطال', cancelButtonText: 'انصراف'
        });
        if (!ok.isConfirmed) return;
        const m = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
        const headers = m ? {'X-XSRF-TOKEN': decodeURIComponent(m[1])} : {};
        const res = await fetch(API + '/' + encodeURIComponent(r.id) + '/revoke',
            {method: 'POST', credentials: 'include', headers});
        if (!res.ok) return Swal.fire('ناموفق', 'ابطال انجام نشد.', 'error');
        load();
    }

    function cell(text, cls) {
        const td = document.createElement('td');
        td.className = 'p-3 ' + (cls || '');
        td.textContent = text;
        return td;
    }

    function note(box, text) {
        const el = document.createElement('div');
        el.className = 'bg-white border rounded-xl p-8 text-center text-gray-400 text-sm';
        el.textContent = text;
        box.appendChild(el);
    }

    const SHELL = `
        <div class="flex flex-wrap items-center justify-between gap-3 mb-3">
            <div>
                <h2 class="text-2xl font-bold text-gray-800">ثبت‌نام‌های اپ</h2>
                <p data-ar-count class="text-sm text-gray-500"></p>
            </div>
            <div class="flex gap-2">
                <a href="${API}/export.csv" class="px-4 py-2 rounded-lg bg-gray-700 text-white text-sm font-bold">خروجیِ CSV</a>
                <button data-ar-reload class="px-4 py-2 rounded-lg bg-indigo-600 text-white text-sm font-bold">نمایش</button>
            </div>
        </div>
        <p class="text-[11px] text-gray-600 bg-gray-50 border rounded-lg p-3 mb-4 leading-6">
            کاربرانی که در اپِ داده‌لینک یا ابزارِ «محاسبه لینک وایرلس» سایت ثبت کرده‌اند. شماره با کدِ
            پیامکی تأیید شده است. «ابطال» دسترسیِ همان دستگاه را می‌گیرد؛ کاربر بارِ بعد دوباره ثبت می‌کند.
        </p>
        <div data-ar-list></div>`;

    window.AppRegistrationsTab = {mount};
})();
