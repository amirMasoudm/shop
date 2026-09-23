/**
 * تبِ «کدِ کالا و موجودی».
 *
 * 🔴 <b>امنیت:</b> نامِ کالا و یادداشتِ رزرو و نامِ فایل همه از بیرون می‌آیند. هیچ‌جای
 * این فایل innerHTML با دادهٔ کاربر پر نمی‌شود؛ اسکلتِ ثابت innerHTML دارد، مقدارها
 * همیشه textContent — همان قاعده‌ی تبِ آدرس‌های قدیمی.
 *
 * 🔴 <b>چرا پیش‌نمایش اجباری است:</b> «نبودن در فایل یعنی صفر» تنها تفسیرِ درستِ
 * گزارشِ موجودی است، ولی یک فایلِ ناقص با همین قاعده می‌تواند کلِ کاتالوگ را صفر
 * کند و چون availabilityِ ترب از موجودی می‌آید، اشتباه فوراً منتشر می‌شود. پس
 * دکمهٔ «اعمال» تا وقتی پیش‌نمایش دیده نشده اصلاً وجود ندارد.
 */
(function () {
    const API = '/api/v1/holoo';

    let host = null;
    let mounted = false;
    let preview = null;

    const SHELL = `
      <div class="space-y-6">

        <!-- وضعیتِ شمارنده -->
        <div class="bg-white border rounded-xl p-4 flex flex-wrap items-center gap-6">
          <div>
            <div class="text-[11px] text-gray-500">کدِ بعدی</div>
            <div data-h-next dir="ltr" class="text-lg font-bold text-indigo-700">—</div>
          </div>
          <div>
            <div class="text-[11px] text-gray-500">شمارندهٔ فعلی</div>
            <div data-h-counter class="text-lg font-bold text-gray-700">—</div>
          </div>
          <div>
            <div class="text-[11px] text-gray-500">رزروِ بی‌صاحب</div>
            <div data-h-open class="text-lg font-bold text-amber-600">—</div>
          </div>
          <button data-h-reload class="mr-auto text-xs border rounded-lg px-3 py-1.5 hover:bg-gray-50">
            تازه‌سازی
          </button>
        </div>

        <!-- گرفتنِ کدِ تازه -->
        <div class="bg-white border rounded-xl p-4">
          <div class="font-bold text-sm mb-1">دریافتِ کدِ هلو</div>
          <p class="text-[11px] text-gray-500 leading-6 mb-3">
            وقتی می‌خواهید کالایی را همین حالا در هلو ثبت کنید ولی هنوز در سایت نساخته‌اید.
            کد رزرو می‌شود و بعداً موقعِ ساختِ محصول از فهرست انتخابش می‌کنید.
            <b>کد هرگز آزاد نمی‌شود</b>، پس بی‌دلیل نگیرید.
          </p>
          <div class="flex flex-wrap gap-2">
            <input data-h-note type="text" placeholder="این کد برای چه کالایی است؟ (اجباری)"
                   class="flex-1 min-w-[220px] p-2 border rounded-lg text-sm outline-none">
            <button data-h-reserve class="bg-indigo-600 text-white text-sm font-bold px-4 py-2 rounded-lg hover:bg-indigo-700">
              کد بگیر
            </button>
          </div>
          <div data-h-reserved class="mt-4"></div>
        </div>

        <!-- ایمپورتِ موجودی -->
        <div class="bg-white border rounded-xl p-4">
          <div class="font-bold text-sm mb-1">ورودِ موجودی از فایلِ هلو</div>
          <p class="text-[11px] text-gray-500 leading-6 mb-3">
            هر دو فایلِ انبار را با هم انتخاب کنید. اگر فقط یکی را بدهید، همان انبار
            به‌روز می‌شود و انبارِ دیگر دست‌نخورده می‌ماند.
            انبارِ هر ردیف از ستونِ «گروه اصلي» خوانده می‌شود، نه از نامِ فایل.
          </p>
          <div class="flex flex-wrap gap-2 items-center">
            <input data-h-files type="file" accept=".xlsx" multiple class="text-xs">
            <button data-h-preview class="bg-gray-800 text-white text-sm font-bold px-4 py-2 rounded-lg hover:bg-gray-900">
              پیش‌نمایش
            </button>
          </div>
          <div data-h-preview-box class="mt-4"></div>
        </div>

        <!-- مهاجرتِ یک‌باره: کدهایِ از پیش‌توافق‌شده -->
        <details class="bg-white border rounded-xl p-4">
          <summary class="font-bold text-sm cursor-pointer">نشاندنِ کدهایِ توافق‌شده (یک‌باره)</summary>
          <p class="text-[11px] text-gray-500 leading-6 mt-2 mb-3">
            فایلِ CSVِ نگاشتِ کد به محصول — همانی که به شرکت هم داده شده. اجرای دوباره
            بی‌اثر است: کدی که از قبل درست نشسته دست نمی‌خورد و کدِ متفاوت فقط گزارش
            می‌شود، نه بازنویسی. در پایان شمارنده تا بزرگ‌ترین کدِ واردشده جلو می‌رود.
          </p>
          <div class="flex flex-wrap gap-2 items-center">
            <input data-h-csv type="file" accept=".csv" class="text-xs">
            <button data-h-migrate class="bg-gray-800 text-white text-sm font-bold px-4 py-2 rounded-lg hover:bg-gray-900">
              اجرا
            </button>
          </div>
          <div data-h-migrate-box class="mt-3"></div>
        </details>

      </div>`;

    // ==========================================================

    function mount(hostEl) {
        host = hostEl;
        if (!mounted) {
            host.innerHTML = SHELL;
            host.querySelector('[data-h-reload]').addEventListener('click', loadStatus);
            host.querySelector('[data-h-reserve]').addEventListener('click', reserve);
            host.querySelector('[data-h-preview]').addEventListener('click', runPreview);
            host.querySelector('[data-h-migrate]').addEventListener('click', runMigration);
            mounted = true;
        }
        loadStatus();
    }

    async function loadStatus() {
        try {
            const status = await getJson(API + '/codes/status');
            host.querySelector('[data-h-next]').textContent = status.nextWouldBe || '—';
            host.querySelector('[data-h-counter]').textContent = faNum(status.counter);
            host.querySelector('[data-h-open]').textContent = faNum(status.openReservations);
        } catch (e) {
            host.querySelector('[data-h-next]').textContent = '—';
        }
        loadReserved();
    }

    async function loadReserved() {
        const box = host.querySelector('[data-h-reserved]');
        box.innerHTML = '';
        let rows;
        try {
            rows = await getJson(API + '/codes/reserved');
        } catch (e) {
            return note(box, 'واکشیِ رزروها ناموفق بود.');
        }
        if (!rows.length) return note(box, 'هیچ کدِ رزروشدهٔ بی‌صاحبی نیست.');

        const table = document.createElement('table');
        table.className = 'w-full text-right';
        table.appendChild(headRow(['کد', 'یادداشت', 'گیرنده', 'تاریخ']));
        const tbody = document.createElement('tbody');
        tbody.className = 'divide-y text-xs';
        rows.forEach(r => {
            const tr = document.createElement('tr');
            tr.appendChild(codeCell(r.code));
            tr.appendChild(textCell(r.note || '—'));
            tr.appendChild(textCell(r.issuedBy || '—'));
            tr.appendChild(textCell(jalali(r.issuedAt)));
            tbody.appendChild(tr);
        });
        table.appendChild(tbody);
        box.appendChild(wrap(table));
    }

    async function reserve() {
        const input = host.querySelector('[data-h-note]');
        const note_ = input.value.trim();
        if (!note_) return alert('یادداشت لازم است — بنویسید این کد برای چه کالایی است.');
        try {
            const res = await fetch(API + '/codes/reserve', {
                method: 'POST', credentials: 'include',
                headers: jsonHeaders(),
                body: JSON.stringify({note: note_})
            });
            if (!res.ok) return alert(await res.text());
            const c = await res.json();
            input.value = '';
            alert('کدِ ' + c.code + ' رزرو شد.\n\nهمین را در هلو روی کالا بنشانید.');
            loadStatus();
        } catch (e) {
            alert('گرفتنِ کد ناموفق بود.');
        }
    }

    // ==========================================================
    // ایمپورت
    // ==========================================================

    async function runPreview() {
        const picker = host.querySelector('[data-h-files]');
        if (!picker.files || !picker.files.length) return alert('فایلِ اکسل را انتخاب کنید.');

        const form = new FormData();
        for (const f of picker.files) form.append('files', f);

        const box = host.querySelector('[data-h-preview-box]');
        box.innerHTML = '';
        note(box, 'در حال خواندنِ فایل…');
        try {
            const res = await fetch(API + '/stock/preview', {
                // ⚠️ Content-Type عمداً ست نمی‌شود: مرورگر باید خودش boundaryِ
                // multipart را بسازد. فقط توکنِ CSRF لازم است.
                method: 'POST', credentials: 'include', headers: csrfHeader(), body: form
            });
            if (!res.ok) {
                box.innerHTML = '';
                return note(box, await res.text());
            }
            preview = await res.json();
            renderPreview();
        } catch (e) {
            box.innerHTML = '';
            note(box, 'پیش‌نمایش ناموفق بود.');
        }
    }

    function renderPreview() {
        const box = host.querySelector('[data-h-preview-box]');
        box.innerHTML = '';

        box.appendChild(summary());

        if (preview.unmappedWarehouses && preview.unmappedWarehouses.length) {
            box.appendChild(warn('انبارهایی که شناخته نشدند: ' + preview.unmappedWarehouses.join('، ')
                + ' — ردیف‌هایشان اعمال نمی‌شوند.'));
        }
        if (preview.massZeroTripped) {
            box.appendChild(warn('⛔ این ایمپورت می‌خواهد موجودیِ ' + faNum(preview.zeroedPercent)
                + '٪ از محصولاتِ کددار را صفر کند (آستانه: ' + faNum(preview.massZeroThreshold)
                + '٪). اگر فایل ناقص است، اعمالش نکنید.'));
        }

        const manual = (preview.changes || []).filter(c => c.overwritesManualEdit);
        if (manual.length) {
            box.appendChild(section('⚠️ این ردیف‌ها ویرایشِ دستیِ انسان را بازمی‌نویسند ('
                + faNum(manual.length) + ')',
                'کسی بعد از آخرین ایمپورت موجودیِ این‌ها را دستی عوض کرده. اگر فاکتوری هنوز '
                + 'در حسابداری ثبت نشده، عددِ فایل کارِ او را پاک می‌کند.',
                changeTable(manual, true)));
        }

        const rest = (preview.changes || []).filter(c => !c.overwritesManualEdit);
        box.appendChild(section('تغییرها (' + faNum(rest.length) + ')', '', changeTable(rest, false)));

        if (preview.unmatched && preview.unmatched.length) {
            box.appendChild(section('ردیف‌های تطبیق‌نخورده (' + faNum(preview.unmatched.length) + ')',
                'این‌ها کالایی‌اند که یا کد ندارند یا روی سایت نیستند — همان فهرستی که می‌گوید '
                + 'چه کالایی ارزشِ افزودن به فروشگاه را دارد.',
                unmatchedTable(preview.unmatched)));
        }

        box.appendChild(applyBar());
    }

    function summary() {
        const el = document.createElement('div');
        el.className = 'grid grid-cols-2 md:grid-cols-4 gap-3 mb-4';
        [['ردیفِ خوانده‌شده', preview.rowsRead],
            ['ردیفِ ردشده', preview.rowsSkipped],
            ['تطبیق‌خورده', preview.rowsMatched],
            ['تغییر', (preview.changes || []).length],
            ['صفر می‌شود', preview.zeroedCount],
            ['نامزدِ پیامک', preview.smsCandidates],
            ['وضعیتِ ترب عوض می‌شود', preview.torobFlips],
            ['انبارِ پوشش‌داده‌شده', (preview.coveredBranches || []).length]
        ].forEach(([label, value]) => {
            const cell = document.createElement('div');
            cell.className = 'bg-gray-50 border rounded-lg p-3';
            const v = document.createElement('div');
            v.className = 'text-lg font-bold text-gray-800';
            v.textContent = faNum(value);
            const l = document.createElement('div');
            l.className = 'text-[11px] text-gray-500';
            l.textContent = label;
            cell.appendChild(v);
            cell.appendChild(l);
            el.appendChild(cell);
        });
        return el;
    }

    function changeTable(rows, highlight) {
        if (!rows.length) return emptyNote('هیچ تغییری نیست.');
        const table = document.createElement('table');
        table.className = 'w-full text-right';
        table.appendChild(headRow(['کد', 'محصول', 'اصفهان', 'تهران', 'موجودیِ فروش']));
        const tbody = document.createElement('tbody');
        tbody.className = 'divide-y text-xs';
        rows.forEach(c => {
            const tr = document.createElement('tr');
            if (highlight) tr.className = 'bg-amber-50';
            tr.appendChild(codeCell(c.holooCode));
            tr.appendChild(textCell(c.productName || '—'));
            tr.appendChild(fromTo(c.oldIsfahan, c.newIsfahan));
            tr.appendChild(fromTo(c.oldTehran, c.newTehran));
            tr.appendChild(fromTo(c.oldStock, c.newStock));
            tbody.appendChild(tr);
        });
        table.appendChild(tbody);
        return wrap(table);
    }

    function unmatchedTable(rows) {
        const table = document.createElement('table');
        table.className = 'w-full text-right';
        table.appendChild(headRow(['کد', 'نام در هلو', 'انبار', 'تعداد', 'دلیل']));
        const tbody = document.createElement('tbody');
        tbody.className = 'divide-y text-xs';
        rows.forEach(u => {
            const tr = document.createElement('tr');
            tr.appendChild(codeCell(u.code || '—'));
            tr.appendChild(textCell(u.name || '—'));
            tr.appendChild(textCell(u.warehouse || '—'));
            tr.appendChild(textCell(u.quantity == null ? '—' : faNum(u.quantity)));
            tr.appendChild(textCell(u.reason || '—'));
            tbody.appendChild(tr);
        });
        table.appendChild(tbody);
        return wrap(table);
    }

    function applyBar() {
        const el = document.createElement('div');
        el.className = 'mt-5 pt-4 border-t space-y-3';

        // 🔴 پیش‌فرض خاموش. اولین ایمپورت صدها محصول را از صفر به مثبت می‌برد و
        // تیکِ روشن یعنی صدها پیامکِ هزینه‌دار با یک کلیک.
        const sms = checkbox('h-sms', 'ارسالِ پیامکِ «موجود شد» به مشترکین ('
            + faNum(preview.smsCandidates) + ' نامزد، سقف ' + faNum(preview.smsCap) + ')');
        el.appendChild(sms.wrapper);

        let confirmBox = null;
        if (preview.massZeroTripped) {
            confirmBox = checkbox('h-confirm-zero',
                'تأیید می‌کنم فایل کامل است و صفرشدنِ انبوه عمدی است');
            el.appendChild(confirmBox.wrapper);
        }

        const btn = document.createElement('button');
        btn.className = 'bg-green-600 text-white text-sm font-bold px-5 py-2.5 rounded-lg hover:bg-green-700';
        btn.textContent = 'اعمالِ این تغییرها';
        btn.addEventListener('click', () => apply(sms.input.checked,
            confirmBox ? confirmBox.input.checked : false));
        el.appendChild(btn);
        return el;
    }

    async function apply(sendSms, confirmMassZero) {
        if (!preview) return;
        if (!confirm('این تغییرها روی سایت اعمال می‌شوند و موجودیِ ترب هم عوض می‌شود. مطمئنید؟')) return;
        try {
            const res = await fetch(API + '/stock/apply', {
                method: 'POST', credentials: 'include',
                headers: jsonHeaders(),
                body: JSON.stringify({token: preview.token, sendSms, confirmMassZero})
            });
            if (!res.ok) return alert(await res.text());
            const r = await res.json();
            alert('انجام شد.\n'
                + faNum(r.changed) + ' محصول عوض شد\n'
                + faNum(r.zeroed) + ' محصول صفر شد\n'
                + faNum(r.smsSent) + ' پیامک فرستاده شد\n'
                + faNum(r.torobFlips) + ' محصول وضعیتشان در ترب عوض شد');
            preview = null;
            host.querySelector('[data-h-preview-box]').innerHTML = '';
            host.querySelector('[data-h-files]').value = '';
            loadStatus();
        } catch (e) {
            alert('اعمال ناموفق بود.');
        }
    }

    // ==========================================================
    // مهاجرتِ یک‌باره
    // ==========================================================

    async function runMigration() {
        const picker = host.querySelector('[data-h-csv]');
        const box = host.querySelector('[data-h-migrate-box]');
        if (!picker.files || !picker.files.length) return alert('فایلِ CSV را انتخاب کنید.');

        const csv = await picker.files[0].text();
        box.innerHTML = '';
        note(box, 'در حال اجرا…');
        try {
            const res = await fetch(API + '/codes/migrate', {
                method: 'POST', credentials: 'include',
                headers: Object.assign(csrfHeader(), {'Content-Type': 'text/csv; charset=utf-8'}),
                body: csv
            });
            box.innerHTML = '';
            if (!res.ok) return note(box, await res.text());
            const r = await res.json();

            const lines = [
                'ردیفِ خوانده‌شده: ' + faNum(r.rows),
                'کد نشانده شد: ' + faNum(r.assigned),
                'از قبل درست بود: ' + faNum(r.alreadyCorrect),
                'محصولش پیدا نشد: ' + faNum(r.missingProduct),
                'تعارض (کدِ متفاوت داشت): ' + faNum(r.conflicts),
                'شمارنده: ' + faNum(r.counterBefore) + ' ← ' + faNum(r.counterAfter),
                'پیشوند: ' + (r.prefix || '—')
            ];
            const el = document.createElement('div');
            el.className = 'border rounded-xl p-3 text-xs leading-7 bg-gray-50';
            lines.forEach(t => {
                const d = document.createElement('div');
                d.textContent = t;
                el.appendChild(d);
            });
            (r.conflictDetail || []).concat(r.missingDetail || []).slice(0, 20).forEach(t => {
                const d = document.createElement('div');
                d.className = 'text-amber-800';
                d.textContent = '• ' + t;
                el.appendChild(d);
            });
            box.appendChild(el);
            loadStatus();
        } catch (e) {
            box.innerHTML = '';
            note(box, 'اجرا ناموفق بود.');
        }
    }

    // ==========================================================
    // ریزه‌کاری‌های نمایش
    // ==========================================================

    function section(title, hint, body) {
        const el = document.createElement('div');
        el.className = 'mt-5';
        const h = document.createElement('div');
        h.className = 'font-bold text-sm mb-1';
        h.textContent = title;
        el.appendChild(h);
        if (hint) {
            const p = document.createElement('p');
            p.className = 'text-[11px] text-gray-500 leading-6 mb-2';
            p.textContent = hint;
            el.appendChild(p);
        }
        el.appendChild(body);
        return el;
    }

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

    function textCell(text) {
        const td = document.createElement('td');
        td.className = 'p-2 text-gray-700';
        td.textContent = text;
        return td;
    }

    /** کد لاتین است و وسطِ ردیفِ راست‌چین تکه‌تکه دیده می‌شود؛ جعبهٔ جداشده لازم دارد. */
    function codeCell(code) {
        const td = document.createElement('td');
        td.className = 'p-2 whitespace-nowrap';
        const box = document.createElement('code');
        box.dir = 'ltr';
        box.style.unicodeBidi = 'isolate';
        box.className = 'text-[11px] font-bold text-indigo-700';
        box.textContent = code || '—';
        td.appendChild(box);
        return td;
    }

    /** «از چه به چه» — بدونِ این، پیش‌نمایش فقط می‌گوید «چیزی عوض می‌شود». */
    function fromTo(oldValue, newValue) {
        const td = document.createElement('td');
        td.className = 'p-2 whitespace-nowrap';
        if (oldValue === newValue) {
            td.className += ' text-gray-400';
            td.textContent = faNum(newValue == null ? '—' : newValue);
            return td;
        }
        const before = document.createElement('span');
        before.className = 'text-gray-400 line-through';
        before.textContent = faNum(oldValue == null ? 0 : oldValue);
        const arrow = document.createElement('span');
        arrow.className = 'mx-1 text-gray-400';
        arrow.textContent = '←';
        const after = document.createElement('b');
        after.className = (Number(newValue) === 0) ? 'text-red-600' : 'text-green-700';
        after.textContent = faNum(newValue == null ? 0 : newValue);
        td.appendChild(before);
        td.appendChild(arrow);
        td.appendChild(after);
        return td;
    }

    function checkbox(id, label) {
        const wrapper = document.createElement('label');
        wrapper.className = 'flex items-center gap-2 cursor-pointer text-xs text-gray-700';
        const input = document.createElement('input');
        input.type = 'checkbox';
        input.id = id;
        input.className = 'w-4 h-4';
        const span = document.createElement('span');
        span.textContent = label;
        wrapper.appendChild(input);
        wrapper.appendChild(span);
        return {wrapper, input};
    }

    function wrap(table) {
        const box = document.createElement('div');
        box.className = 'border rounded-xl overflow-x-auto';
        box.appendChild(table);
        return box;
    }

    function warn(text) {
        const el = document.createElement('div');
        el.className = 'bg-amber-50 border border-amber-300 text-amber-900 rounded-lg p-3 text-xs leading-6 mb-3';
        el.textContent = text;
        return el;
    }

    function note(box, text) {
        const el = document.createElement('div');
        el.className = 'bg-white border rounded-xl p-6 text-center text-gray-400 text-sm';
        el.textContent = text;
        box.appendChild(el);
    }

    function emptyNote(text) {
        const el = document.createElement('div');
        el.className = 'border rounded-xl p-6 text-center text-gray-400 text-sm';
        el.textContent = text;
        return el;
    }

    /**
     * تاریخِ شمسی با تقویمِ صریح.
     * ⚠️ بدونِ {@code calendar:'persian'} مرورگرِ کاربر ممکن است میلادی بدهد —
     * همان تله‌ای که در بقیهٔ پنل هم بسته شده.
     */
    function jalali(value) {
        if (!value) return '—';
        try {
            return new Date(value).toLocaleDateString('fa-IR', {calendar: 'persian'});
        } catch (e) {
            return '—';
        }
    }

    function faNum(v) {
        if (v === null || v === undefined || v === '—') return '—';
        return Number(v).toLocaleString('fa-IR');
    }

    /**
     * توکنِ CSRF از کوکی.
     * ⚠️ بدونِ این، هر POST از پنل ۴۰۳ می‌گیرد — کانفیگِ امنیت CookieCsrfTokenRepository
     * دارد و فقط /torob_api معاف است. همان الگویِ تبِ آدرس‌های قدیمی.
     */
    function csrfHeader() {
        const m = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
        return m ? {'X-XSRF-TOKEN': decodeURIComponent(m[1])} : {};
    }

    function jsonHeaders() {
        return Object.assign(csrfHeader(), {'Content-Type': 'application/json'});
    }

    async function getJson(url) {
        const res = await fetch(url, {credentials: 'include'});
        if (!res.ok) throw new Error(String(res.status));
        return res.json();
    }

    window.HolooTab = {mount};
})();
