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
          شمارهٔ تلفن عمداً کامل نشان داده نمی‌شود و جست‌وجو هم فقط با نام است.
        </p>
        <div data-c-list></div>
      </div>

      <!-- پروندهٔ مشتری و سفرِ کاربر، هر دو در همین یک کادر؛ عمداً مودالِ خودِ
           ماژول است و نه مودالِ Admin.html، چون این فایل در پنلِ فروش هم بار می‌شود. -->
      <div data-c-modal class="fixed inset-0 bg-black/60 z-50 hidden items-center justify-center p-3">
        <div class="bg-white w-full max-w-3xl rounded-2xl shadow-2xl flex flex-col" style="max-height:88vh;">
          <div class="shrink-0 flex justify-between items-center px-4 py-3 bg-indigo-900 text-white rounded-t-2xl">
            <h3 data-c-modal-title class="text-sm font-bold">پروندهٔ مشتری</h3>
            <button data-c-modal-close class="text-white/70 hover:text-white text-2xl leading-none px-1">✕</button>
          </div>
          <div data-c-modal-body class="flex-1 overflow-y-auto p-4" style="min-height:0;"></div>
        </div>
      </div>`;

    let cHost = null, cMounted = false, cRows = [];

    function mountCustomers(host) {
        cHost = host;
        if (!cMounted) {
            host.innerHTML = CUSTOMERS_SHELL;
            host.querySelector('[data-c-reload]').addEventListener('click', loadCustomers);
            host.querySelector('[data-c-search]').addEventListener('input', renderCustomers);
            host.querySelector('[data-c-modal-close]').addEventListener('click', closeCustomerModal);
            host.querySelector('[data-c-modal]').addEventListener('click', e => {
                if (e.target === e.currentTarget) closeCustomerModal();
            });
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
        table.appendChild(headRow(['نام', 'شماره', 'تاریخ عضویت', 'تعداد سفارش', '']));
        const tbody = document.createElement('tbody');
        tbody.className = 'divide-y text-xs';
        rows.forEach(r => {
            const tr = document.createElement('tr');
            tr.appendChild(textCell(fullName(r) || '— بی‌نام —'));
            tr.appendChild(ltrCell(r.maskedPhone || '—'));
            tr.appendChild(textCell(jalali(r.createdAt)));
            tr.appendChild(chipCell(faNum(r.orderCount || 0) + ' سفارش', (r.orderCount || 0) > 0));

            const act = document.createElement('td');
            act.className = 'p-2 whitespace-nowrap';
            act.appendChild(smallBtn('پروندهٔ کامل', () => openCustomerFile(r.id)));
            act.appendChild(smallBtn('سفر', () => openCustomerJourney(r.id)));
            tr.appendChild(act);
            tbody.appendChild(tr);
        });
        table.appendChild(tbody);
        box.appendChild(wrap(table));
    }

    function fullName(r) {
        return ((r.firstName || '') + ' ' + (r.lastName || '')).trim();
    }

    // ---------- پروندهٔ کامل ----------
    async function openCustomerFile(id) {
        const body = openCustomerModal('پروندهٔ مشتری');
        body.textContent = 'در حال بارگذاری…';
        let d;
        try {
            d = await getJson('/api/users/panel/customers/' + encodeURIComponent(id));
        } catch (e) {
            body.textContent = 'واکشیِ پرونده ناموفق بود.';
            return;
        }
        body.innerHTML = '';
        cHost.querySelector('[data-c-modal-title]').textContent =
            'پروندهٔ ' + (fullName(d) || 'مشتری');

        body.appendChild(summaryStrip(d));
        body.appendChild(section('نشانی‌ها', d.addresses.length
            ? d.addresses.map(addressCard)
            : [emptyLine('نشانی‌ای ثبت نشده است.')]));
        body.appendChild(section('فاکتورها', d.orders.length
            ? d.orders.map(orderCard)
            : [emptyLine('سفارشی ثبت نکرده است.')]));
        body.appendChild(section('نظرها', d.comments.length
            ? d.comments.map(commentCard)
            : [emptyLine('نظری ثبت نکرده است.')]));
    }

    function summaryStrip(d) {
        const box = document.createElement('div');
        box.className = 'flex flex-wrap gap-2 mb-4';
        [['شماره', d.maskedPhone || '—'],
            ['عضویت', jalali(d.createdAt)],
            ['سفارش‌ها', faNum(d.orders.length)],
            ['کل خرید', faNum(d.totalSpent || 0) + ' تومان']].forEach(([k, v]) => {
            const c = document.createElement('div');
            c.className = 'flex-1 min-w-[110px] border rounded-xl p-2 text-center bg-gray-50';
            const t = document.createElement('div');
            t.className = 'text-[10px] text-gray-500';
            t.textContent = k;
            const n = document.createElement('div');
            n.className = 'text-xs font-bold text-gray-800 mt-1';
            n.dir = 'auto';
            n.textContent = v;
            c.appendChild(t);
            c.appendChild(n);
            box.appendChild(c);
        });
        return box;
    }

    function addressCard(a) {
        const el = document.createElement('div');
        el.className = 'border rounded-lg p-2 text-[11px] leading-6 bg-gray-50';
        line(el, 'گیرنده', a.recipientName || '—');
        line(el, 'نشانی', [a.state, a.city, a.fullAddress].filter(Boolean).join('، ') || '—');
        line(el, 'کد پستی', a.postalCode || '—');
        // ⚠️ همین هم پوشانده از سرور می‌آید؛ اینجا هیچ بازسازی‌ای ممکن نیست.
        line(el, 'شمارهٔ گیرنده', a.maskedRecipientPhone || '—');
        return el;
    }

    function orderCard(o) {
        const el = document.createElement('div');
        el.className = 'border rounded-lg p-2 text-[11px] leading-6 bg-white flex flex-wrap gap-x-4';
        line(el, 'فاکتور', o.orderCode || (o.id || '').substring(0, 6));
        line(el, 'تاریخ', jalali(o.orderDate));
        line(el, 'مبلغ', faNum(o.totalAmount || 0) + ' تومان');
        line(el, 'وضعیت', o.status || '—');
        return el;
    }

    function commentCard(c) {
        const el = document.createElement('div');
        el.className = 'border rounded-lg p-2 text-[11px] leading-6 bg-white';
        line(el, 'متن', c.text || '—');
        line(el, 'امتیاز', c.rating == null ? '—' : faNum(c.rating));
        line(el, 'وضعیت', statusLabel(c.status));
        line(el, 'تاریخ', jalali(c.createdAt));
        return el;
    }

    // ---------- سفرِ کاربر ----------
    async function openCustomerJourney(id) {
        const body = openCustomerModal('سفرِ کاربر');
        body.textContent = 'در حال بارگذاری…';
        let d;
        try {
            d = await getJson('/api/users/panel/customers/' + encodeURIComponent(id) + '/journey');
        } catch (e) {
            body.textContent = 'واکشیِ سفر ناموفق بود.';
            return;
        }
        body.innerHTML = '';
        if (!d.events || !d.events.length) {
            body.appendChild(emptyLine('رویدادی برای این مشتری ثبت نشده است.'));
            return;
        }
        const table = document.createElement('table');
        table.className = 'w-full text-right';
        table.appendChild(headRow(['زمان', 'رویداد', 'صفحه/مورد']));
        const tbody = document.createElement('tbody');
        tbody.className = 'divide-y text-[11px]';
        d.events.forEach(e => {
            const tr = document.createElement('tr');
            tr.appendChild(textCell(dateTime(e.at)));
            tr.appendChild(textCell(e.type || '—'));
            tr.appendChild(textCell(e.entityName || e.path || e.entityId || '—'));
            tbody.appendChild(tr);
        });
        table.appendChild(tbody);
        body.appendChild(wrap(table));
    }

    // ---------- مودال ----------
    function openCustomerModal(title) {
        const m = cHost.querySelector('[data-c-modal]');
        m.classList.remove('hidden');
        m.classList.add('flex');
        cHost.querySelector('[data-c-modal-title]').textContent = title;
        const body = cHost.querySelector('[data-c-modal-body]');
        body.innerHTML = '';
        return body;
    }

    function closeCustomerModal() {
        const m = cHost.querySelector('[data-c-modal]');
        m.classList.add('hidden');
        m.classList.remove('flex');
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

    function chipCell(text, good) {
        const td = document.createElement('td');
        td.className = 'p-2 whitespace-nowrap';
        const sp = document.createElement('span');
        sp.className = 'text-[10px] font-bold px-2 py-1 rounded-full '
            + (good ? 'bg-green-50 text-green-700' : 'bg-gray-100 text-gray-500');
        sp.textContent = text;
        td.appendChild(sp);
        return td;
    }

    function smallBtn(label, onClick) {
        const b = document.createElement('button');
        b.className = 'text-[10px] border rounded px-2 py-1 ml-1 text-indigo-700 '
            + 'border-indigo-300 hover:bg-indigo-600 hover:text-white';
        b.textContent = label;
        b.addEventListener('click', onClick);
        return b;
    }

    /** یک سطرِ «برچسب: مقدار». مقدار همیشه textContent — دادهٔ مشتری است. */
    function line(parent, label, value) {
        const row = document.createElement('div');
        const k = document.createElement('span');
        k.className = 'text-gray-500 ml-1';
        k.textContent = label + ': ';
        const v = document.createElement('span');
        v.className = 'text-gray-800';
        v.textContent = value;
        row.appendChild(k);
        row.appendChild(v);
        parent.appendChild(row);
        return row;
    }

    function section(title, children) {
        const box = document.createElement('div');
        box.className = 'mb-4';
        const h = document.createElement('div');
        h.className = 'text-xs font-bold text-gray-700 mb-2';
        h.textContent = title;
        box.appendChild(h);
        const list = document.createElement('div');
        list.className = 'space-y-2';
        children.forEach(c => list.appendChild(c));
        box.appendChild(list);
        return box;
    }

    function emptyLine(text) {
        const el = document.createElement('div');
        el.className = 'text-[11px] text-gray-400 border rounded-lg p-3 text-center';
        el.textContent = text;
        return el;
    }

    function statusLabel(s) {
        const u = String(s || '').toUpperCase();
        return u === 'APPROVED' ? 'تأییدشده' : u === 'REJECTED' ? 'ردشده' : 'در انتظار';
    }

    /** ⚠️ تقویمِ persian صریح، مثلِ jalali — ولی با ساعت، چون سفر خطِ زمانی است. */
    function dateTime(v) {
        if (!v) return '—';
        try {
            const d = new Date(v);
            return d.toLocaleDateString('fa-IR', {calendar: 'persian'}) + ' '
                + d.toLocaleTimeString('fa-IR', {hour: '2-digit', minute: '2-digit'});
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
