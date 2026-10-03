/**
 * دو تبِ تازهٔ پنلِ فروشِ حضوری: مدیریتِ مشتریان و مدیریتِ نظرات.
 *
 * 🔴 <b>امنیت:</b> نامِ مشتری و متنِ نظر هر دو از بیرون می‌آیند. هیچ‌جای این فایل
 * innerHTML با دادهٔ کاربر پر نمی‌شود؛ اسکلتِ ثابت innerHTML دارد، مقدارها همیشه
 * textContent — همان قاعدهٔ تبِ آدرس‌های قدیمی.
 *
 * 🔴 <b>شمارهٔ تلفن:</b> سرور اصلاً نمی‌فرستدش (CustomerSummaryDto). این فایل هیچ
 * جایی شماره را بازسازی نمی‌کند؛ فقط شکلِ پوشانده را نشان می‌دهد.
 */
(function () {
    const API = '/api/v1';

    // ==========================================================
    // مدیریتِ مشتریان
    // ==========================================================
    const CUSTOMERS_SHELL = `
      <div class="bg-white border rounded-xl p-4">
        <div class="flex flex-wrap items-center gap-2 mb-3">
          <div class="font-bold text-sm">مدیریت مشتریان</div>
          <span data-c-count class="text-[11px] text-gray-500"></span>
          <input data-c-search type="text" placeholder="جست‌وجوی نام…"
                 class="mr-auto w-full sm:w-56 p-2 border rounded-lg text-sm outline-none">
          <button data-c-reload class="text-xs border rounded-lg px-3 py-2 hover:bg-gray-50">تازه‌سازی</button>
        </div>
        <p class="text-[11px] text-gray-500 leading-6 mb-3">
          فقط کاربرانِ با نقشِ «مشتری» — ادمین و کارشناس‌ها در این فهرست نیستند.
          شمارهٔ تلفن عمداً کامل نشان داده نمی‌شود.
        </p>
        <div data-c-list></div>
      </div>`;

    let cHost = null, cMounted = false, cRows = [];

    function mountCustomers(host) {
        cHost = host;
        if (!cMounted) {
            host.innerHTML = CUSTOMERS_SHELL;
            host.querySelector('[data-c-reload]').addEventListener('click', loadCustomers);
            host.querySelector('[data-c-search]').addEventListener('input', renderCustomers);
            cMounted = true;
        }
        loadCustomers();
    }

    async function loadCustomers() {
        const box = cHost.querySelector('[data-c-list]');
        box.innerHTML = '';
        try {
            cRows = await getJson('/api/users/panel/customers');
        } catch (e) {
            return note(box, 'واکشیِ مشتریان ناموفق بود.');
        }
        renderCustomers();
    }

    function renderCustomers() {
        const box = cHost.querySelector('[data-c-list]');
        const q = (cHost.querySelector('[data-c-search]').value || '').trim().toLowerCase();
        const rows = cRows.filter(r => !q || fullName(r).toLowerCase().includes(q));

        cHost.querySelector('[data-c-count]').textContent =
            'مجموعاً ' + faNum(cRows.length) + ' مشتری';
        box.innerHTML = '';
        if (!rows.length) return note(box, cRows.length ? 'موردی با این نام نیست.' : 'هنوز مشتری‌ای ثبت نشده.');

        const table = document.createElement('table');
        table.className = 'w-full text-right';
        table.appendChild(headRow(['نام', 'شماره', 'تاریخ عضویت']));
        const tbody = document.createElement('tbody');
        tbody.className = 'divide-y text-xs';
        rows.forEach(r => {
            const tr = document.createElement('tr');
            tr.appendChild(textCell(fullName(r) || '— بی‌نام —'));
            tr.appendChild(ltrCell(r.maskedPhone || '—'));
            tr.appendChild(textCell(jalali(r.createdAt)));
            tbody.appendChild(tr);
        });
        table.appendChild(tbody);
        box.appendChild(wrap(table));
    }

    function fullName(r) {
        return ((r.firstName || '') + ' ' + (r.lastName || '')).trim();
    }

    // ==========================================================
    // مدیریتِ نظرات
    // ==========================================================
    const COMMENTS_SHELL = `
      <div class="bg-white border rounded-xl p-4">
        <div class="flex flex-wrap items-center gap-2 mb-3">
          <div class="font-bold text-sm">مدیریت نظرات</div>
          <span data-m-count class="text-[11px] text-gray-500"></span>
          <button data-m-reload class="mr-auto text-xs border rounded-lg px-3 py-2 hover:bg-gray-50">تازه‌سازی</button>
        </div>
        <div data-m-list></div>
      </div>`;

    let mHost = null, mMounted = false, mCanEdit = false;

    function mountComments(host, opts) {
        mHost = host;
        mCanEdit = !!(opts && opts.canEdit);
        if (!mMounted) {
            host.innerHTML = COMMENTS_SHELL;
            host.querySelector('[data-m-reload]').addEventListener('click', loadComments);
            mMounted = true;
        }
        loadComments();
    }

    async function loadComments() {
        const box = mHost.querySelector('[data-m-list]');
        box.innerHTML = '';
        let rows;
        try {
            rows = await getJson('/api/comments/admin/list');
        } catch (e) {
            return note(box, 'واکشیِ نظرات ناموفق بود.');
        }
        mHost.querySelector('[data-m-count]').textContent = 'مجموعاً ' + faNum(rows.length) + ' نظر';
        if (!rows.length) return note(box, 'هنوز نظری ثبت نشده.');

        const table = document.createElement('table');
        table.className = 'w-full text-right';
        table.appendChild(headRow(['وضعیت', 'متن', 'امتیاز', 'تاریخ', '']));
        const tbody = document.createElement('tbody');
        tbody.className = 'divide-y text-xs';
        rows.forEach(c => tbody.appendChild(commentRow(c)));
        table.appendChild(tbody);
        box.appendChild(wrap(table));
    }

    function commentRow(c) {
        const tr = document.createElement('tr');

        const st = document.createElement('td');
        st.className = 'p-2 whitespace-nowrap';
        const chip = document.createElement('span');
        const s = String(c.status || '').toUpperCase();
        chip.className = 'text-[10px] font-bold px-2 py-1 rounded-full ' + (
            s === 'APPROVED' ? 'bg-green-50 text-green-700'
                : s === 'REJECTED' ? 'bg-red-50 text-red-700'
                    : 'bg-amber-50 text-amber-700');
        chip.textContent = s === 'APPROVED' ? 'تأییدشده' : s === 'REJECTED' ? 'ردشده' : 'در انتظار';
        st.appendChild(chip);
        tr.appendChild(st);

        tr.appendChild(textCell(c.text || c.content || '—'));
        tr.appendChild(textCell(c.rating == null ? '—' : faNum(c.rating)));
        tr.appendChild(textCell(jalali(c.createdAt)));

        const act = document.createElement('td');
        act.className = 'p-2 whitespace-nowrap';
        // مرزِ واقعی سمتِ سرور است (PUT /api/comments/admin/**، هر چهار نقشِ پنل)؛
        // این فقط برایِ این است که دکمه‌ای نشان داده نشود که کلیکش ۴۰۳ می‌گیرد.
        if (mCanEdit && s !== 'APPROVED') act.appendChild(actionBtn('تأیید', c.id, 'approve', 'green'));
        if (mCanEdit && s !== 'REJECTED') act.appendChild(actionBtn('رد', c.id, 'reject', 'red'));
        tr.appendChild(act);
        return tr;
    }

    function actionBtn(label, id, action, color) {
        const b = document.createElement('button');
        b.className = 'text-[10px] border rounded px-2 py-1 ml-1 '
            + (color === 'green' ? 'text-green-700 border-green-300 hover:bg-green-600 hover:text-white'
                : 'text-red-700 border-red-300 hover:bg-red-600 hover:text-white');
        b.textContent = label;
        b.addEventListener('click', async () => {
            b.disabled = true;
            try {
                const res = await fetch('/api/comments/admin/' + encodeURIComponent(id) + '/' + action,
                    {method: 'PUT', credentials: 'include', headers: csrfHeader()});
                if (!res.ok) { b.disabled = false; return alert(await res.text()); }
                loadComments();
            } catch (e) {
                b.disabled = false;
                alert('انجام نشد.');
            }
        });
        return b;
    }

    // ==========================================================
    // کمکی‌ها
    // ==========================================================
    function headRow(labels) {
        const thead = document.createElement('thead');
        thead.className = 'bg-gray-50 text-xs text-gray-500';
        const tr = document.createElement('tr');
        labels.forEach(h => {
            const th = document.createElement('th');
            th.className = 'p-2 font-bold';
            th.textContent = h;
            tr.appendChild(th);
        });
        thead.appendChild(tr);
        return thead;
    }

    function textCell(t) {
        const td = document.createElement('td');
        td.className = 'p-2 text-gray-700';
        td.textContent = t;
        return td;
    }

    /** عدد و شمارهٔ پوشانده لاتین‌اند و وسطِ ردیفِ راست‌چین تکه‌تکه دیده می‌شوند. */
    function ltrCell(t) {
        const td = document.createElement('td');
        td.className = 'p-2 whitespace-nowrap';
        const b = document.createElement('span');
        b.dir = 'ltr';
        b.style.unicodeBidi = 'isolate';
        b.className = 'font-mono text-[11px] text-gray-700';
        b.textContent = t;
        td.appendChild(b);
        return td;
    }

    function wrap(table) {
        const box = document.createElement('div');
        box.className = 'border rounded-xl overflow-x-auto';
        box.appendChild(table);
        return box;
    }

    function note(box, text) {
        const el = document.createElement('div');
        el.className = 'border rounded-xl p-6 text-center text-gray-400 text-sm';
        el.textContent = text;
        box.appendChild(el);
    }

    /** ⚠️ تقویمِ persian صریح — بدونِ آن مرورگرِ کاربر ممکن است میلادی بدهد. */
    function jalali(v) {
        if (!v) return '—';
        try {
            return new Date(v).toLocaleDateString('fa-IR', {calendar: 'persian'});
        } catch (e) {
            return '—';
        }
    }

    function faNum(v) {
        return (v === null || v === undefined) ? '—' : Number(v).toLocaleString('fa-IR');
    }

    function csrfHeader() {
        const m = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
        return m ? {'X-XSRF-TOKEN': decodeURIComponent(m[1])} : {};
    }

    async function getJson(url) {
        const res = await fetch(url, {credentials: 'include'});
        if (!res.ok) throw new Error(String(res.status));
        return res.json();
    }

    window.SalesCustomersTab = {mount: mountCustomers};
    window.SalesCommentsTab = {mount: mountComments};
})();
