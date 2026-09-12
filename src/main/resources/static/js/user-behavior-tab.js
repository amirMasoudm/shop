/**
 * تبِ «رفتارِ کاربران» — شش نما.
 *
 * 🔴 <b>قاعدهٔ سختِ کارایی:</b> نماهای «نمای کلی»، «منبعِ ورود» و «جست‌وجوها» فقط از
 * daily_stats می‌خوانند و هرگز به user_events دست نمی‌زنند. تنها نمایی که اجازهٔ
 * دادهٔ خام دارد «سفرِ کاربر» است که ذاتاً به یک نفر محدود و صفحه‌بندی‌شده است.
 * اگر داشبورد روی دادهٔ خام کوئری بزند، ماهِ ششم باز نمی‌شود.
 *
 * 🔴 <b>امنیت:</b> عبارتِ جست‌وجو، برچسب‌های UTM و دامنهٔ ارجاع‌دهنده را «بیرون»
 * می‌نویسد و اینجا رندر می‌شوند — و UTM را می‌شود صرفاً با ساختنِ یک لینک تزریق
 * کرد. پس هیچ‌جای این فایل innerHTML با دادهٔ رویداد پر نمی‌شود؛ همه‌چیز با
 * textContent می‌نشیند. اسکلتِ ثابتِ جدول‌ها innerHTML دارد، مقدارها هرگز.
 */
(function () {
    const API = '/api/v1/analytics';

    /** برچسبِ کانال‌ها. DIRECT عمداً «نامعلوم» هم دارد — نگاه کن به توضیحِ زیر. */
    const CHANNEL_LABELS = {
        // مرورگرِ داخلیِ تلگرام و اینستاگرام ارجاع نمی‌فرستند و Referrer-Policy هم
        // حذفش می‌کند، پس DIRECT یعنی «نمی‌دانیم»، نه «آدرس را تایپ کرد». برچسب
        // باید همین را بگوید وگرنه روی عددی تصمیم می‌گیریم که معنایش را اشتباه فهمیده‌ایم.
        DIRECT: 'مستقیم / نامعلوم',
        ORGANIC_SEARCH: 'جست‌وجوی طبیعی',
        PAID: 'تبلیغاتِ پولی',
        SOCIAL: 'شبکه‌های اجتماعی',
        MARKETPLACE: 'بازارگاه (ترب و مشابه)',
        REFERRAL: 'ارجاع از سایتِ دیگر'
    };
    const DEVICE_LABELS = {MOBILE: 'موبایل', DESKTOP: 'دسکتاپ', TABLET: 'تبلت'};

    let host = null;
    let mounted = false;
    let stats = [];

    function mount(hostEl) {
        host = hostEl;
        if (!mounted) {
            host.innerHTML = SHELL;
            const to = new Date();
            const from = new Date(to.getTime() - 29 * 86400000);
            host.querySelector('[data-from]').value = iso(from);
            host.querySelector('[data-to]').value = iso(to);
            host.querySelectorAll('[data-view-btn]').forEach(b =>
                b.addEventListener('click', () => showView(b.getAttribute('data-view-btn'))));
            host.querySelector('[data-reload]').addEventListener('click', load);
            host.querySelector('[data-journey-go]').addEventListener('click', loadJourney);
            host.querySelector('[data-visitor-filter]').addEventListener('change', loadVisitors);
            host.querySelector('[data-export]').addEventListener('click', exportRange);
            host.querySelector('[data-archive-now]').addEventListener('click', archiveNow);
            mounted = true;
        }
        showView('overview');
        load();
    }

    function iso(d) { return d.toISOString().slice(0, 10); }
    function range() {
        return {
            from: host.querySelector('[data-from]').value,
            to: host.querySelector('[data-to]').value
        };
    }

    function showView(name) {
        host.querySelectorAll('[data-view]').forEach(v =>
            v.classList.toggle('hidden', v.getAttribute('data-view') !== name));
        host.querySelectorAll('[data-view-btn]').forEach(b => {
            const on = b.getAttribute('data-view-btn') === name;
            b.className = on
                ? 'px-4 py-2 rounded-lg text-sm font-bold bg-indigo-600 text-white'
                : 'px-4 py-2 rounded-lg text-sm font-bold bg-white border text-gray-600 hover:bg-gray-50';
        });
        if (name === 'visitors') loadVisitors();
        if (name === 'data') loadArchives();
    }

    // ==========================================================
    // بارگذاری — فقط daily_stats
    // ==========================================================
    async function load() {
        const {from, to} = range();
        try {
            stats = await getJson(`${API}/overview?from=${from}&to=${to}`);
        } catch (e) {
            stats = [];
        }
        renderOverview();
        renderSources();
        renderSearches();
    }

    function sum(field) { return stats.reduce((a, s) => a + (s[field] || 0), 0); }

    function emptyNote(box, text) {
        box.innerHTML = '';
        const p = document.createElement('p');
        p.className = 'p-8 text-center text-gray-400 text-sm';
        p.textContent = text;
        box.appendChild(p);
    }

    // ---- نمای ۱: نمای کلی + قیف ----
    function renderOverview() {
        const box = host.querySelector('[data-overview]');
        if (!stats.length) return emptyNote(box, 'برای این بازه داده‌ای نیست.');

        const f = stats.reduce((a, s) => {
            const x = s.funnel || {};
            a.visits += x.visits || 0; a.productViews += x.productViews || 0;
            a.addToCart += x.addToCart || 0; a.beginCheckout += x.beginCheckout || 0;
            a.orders += x.orders || 0;
            return a;
        }, {visits: 0, productViews: 0, addToCart: 0, beginCheckout: 0, orders: 0});

        box.innerHTML = '';
        box.appendChild(tiles([
            ['بازدید', sum('visits')],
            ['بازدیدکنندهٔ یکتا', sum('uniqueVisitors')],
            ['مشاهدهٔ صفحه', sum('pageViews')],
            ['سفارش', f.orders]
        ]));
        box.appendChild(funnelEl(f));
        box.appendChild(dailyTable());
    }

    function tiles(pairs) {
        const grid = document.createElement('div');
        grid.className = 'grid grid-cols-2 md:grid-cols-4 gap-4 mb-6';
        pairs.forEach(([label, value]) => {
            const card = document.createElement('div');
            card.className = 'bg-white p-4 rounded-xl shadow-sm border';
            const l = document.createElement('p');
            l.className = 'text-gray-500 text-xs';
            l.textContent = label;
            const v = document.createElement('h3');
            v.className = 'text-2xl font-bold text-gray-800 mt-1';
            v.textContent = Number(value || 0).toLocaleString('fa-IR');
            card.appendChild(l); card.appendChild(v);
            grid.appendChild(card);
        });
        return grid;
    }

    /** قیف با درصدِ ریزشِ هر پله — همان چیزی که می‌گوید کجا مشتری را از دست می‌دهیم. */
    function funnelEl(f) {
        const wrap = document.createElement('div');
        wrap.className = 'bg-white p-4 rounded-xl shadow-sm border mb-6';
        const h = document.createElement('h3');
        h.className = 'font-bold text-gray-700 mb-3';
        h.textContent = 'قیفِ تبدیل';
        wrap.appendChild(h);

        const steps = [
            ['بازدید', f.visits], ['مشاهدهٔ محصول', f.productViews],
            ['افزودن به سبد', f.addToCart], ['شروعِ پرداخت', f.beginCheckout], ['سفارش', f.orders]
        ];
        const top = steps[0][1] || 0;
        steps.forEach(([label, value], i) => {
            const row = document.createElement('div');
            row.className = 'mb-2';
            const head = document.createElement('div');
            head.className = 'flex justify-between text-xs mb-1';
            const l = document.createElement('span');
            l.className = 'font-bold text-gray-700';
            l.textContent = label;
            const r = document.createElement('span');
            r.className = 'text-gray-500';
            const prev = i === 0 ? value : steps[i - 1][1];
            const drop = (i > 0 && prev > 0) ? ' — ریزش ' + Math.round((1 - value / prev) * 100) + '٪' : '';
            r.textContent = Number(value || 0).toLocaleString('fa-IR') + drop;
            head.appendChild(l); head.appendChild(r);

            const bar = document.createElement('div');
            bar.className = 'h-3 bg-gray-100 rounded overflow-hidden';
            const fill = document.createElement('div');
            fill.className = 'h-3 bg-indigo-500';
            fill.style.width = (top > 0 ? Math.max(2, Math.round(value / top * 100)) : 0) + '%';
            bar.appendChild(fill);

            row.appendChild(head); row.appendChild(bar);
            wrap.appendChild(row);
        });
        return wrap;
    }

    function dailyTable() {
        const rows = stats.map(s => [s.id, s.visits, s.uniqueVisitors, s.pageViews,
            (s.funnel || {}).orders || 0]);
        return tableEl(['روز', 'بازدید', 'یکتا', 'مشاهدهٔ صفحه', 'سفارش'], rows);
    }

    // ---- نمای ۲: منبعِ ورود ----
    function renderSources() {
        const box = host.querySelector('[data-sources]');
        if (!stats.length) return emptyNote(box, 'برای این بازه داده‌ای نیست.');

        const byChannel = {};
        const byCity = {};
        const byDevice = {};
        stats.forEach(s => {
            Object.entries(s.byChannel || {}).forEach(([k, v]) => {
                const t = byChannel[k] || (byChannel[k] = {visits: 0, productViews: 0, addToCart: 0, orders: 0});
                t.visits += v.visits || 0; t.productViews += v.productViews || 0;
                t.addToCart += v.addToCart || 0; t.orders += v.orders || 0;
            });
            Object.entries(s.byCity || {}).forEach(([k, v]) => byCity[k] = (byCity[k] || 0) + v);
            Object.entries(s.byDevice || {}).forEach(([k, v]) => byDevice[k] = (byDevice[k] || 0) + v);
        });

        box.innerHTML = '';

        // ستونِ «کارزار» در فازِ ۳ اینجا اضافه می‌شود — ساختارِ ردیف از همین حالا
        // آرایه است تا افزودنِ یک ستون بازنویسی نخواهد.
        const chRows = Object.entries(byChannel)
            .sort((a, b) => b[1].visits - a[1].visits)
            .map(([k, v]) => [
                CHANNEL_LABELS[k] || k, v.visits, v.productViews, v.addToCart, v.orders,
                (v.visits ? (v.orders / v.visits * 100).toFixed(1) : '0.0') + '٪'
            ]);
        box.appendChild(section('کانالِ ورود',
            tableEl(['کانال', 'بازدید', 'مشاهدهٔ محصول', 'سبد', 'سفارش', 'نرخِ تبدیل'], chRows)));

        const cityRows = Object.entries(byCity).sort((a, b) => b[1] - a[1]).slice(0, 30);
        box.appendChild(section('شهرها (تقریبی)',
            tableEl(['شهر', 'رویداد'], cityRows),
            // CGNATِ اپراتورهای موبایل کاربرِ اصفهانی را ممکن است تهران نشان دهد.
            'این عدد جهت‌نماست، نه قابلِ استناد: اپراتورهای موبایل CGNAT دارند و شهر ممکن است اشتباه تشخیص داده شود.'));

        const devRows = Object.entries(byDevice).sort((a, b) => b[1] - a[1])
            .map(([k, v]) => [DEVICE_LABELS[k] || k, v]);
        box.appendChild(section('دستگاه', tableEl(['دستگاه', 'رویداد'], devRows)));
    }

    // ---- نمای ۵: جست‌وجوها ----
    function renderSearches() {
        const box = host.querySelector('[data-searches]');
        if (!stats.length) return emptyNote(box, 'برای این بازه داده‌ای نیست.');

        const top = {}, zero = {};
        stats.forEach(s => {
            (s.topSearches || []).forEach(t => top[t.term] = (top[t.term] || 0) + t.count);
            (s.zeroSearches || []).forEach(t => zero[t.term] = (zero[t.term] || 0) + t.count);
        });

        box.innerHTML = '';
        box.appendChild(section('پرتکرارترین جست‌وجوها',
            tableEl(['عبارت', 'دفعات'], Object.entries(top).sort((a, b) => b[1] - a[1]).slice(0, 50))));
        box.appendChild(section('جست‌وجوهای بی‌نتیجه',
            tableEl(['عبارت', 'دفعات'], Object.entries(zero).sort((a, b) => b[1] - a[1]).slice(0, 50)),
            'ارزشمندترین دادهٔ این صفحه: دقیقاً می‌گوید مشتری دنبالِ چه چیزی آمده که ما نداریم.'));
    }

    // ---- نمای ۳: بازدیدکنندگان ----
    async function loadVisitors() {
        const box = host.querySelector('[data-visitors]');
        const filter = host.querySelector('[data-visitor-filter]').value;
        let rows;
        try {
            rows = await getJson(`${API}/visitors?filter=${encodeURIComponent(filter)}`);
        } catch (e) { return emptyNote(box, 'واکشی ناموفق بود.'); }
        if (!rows.length) return emptyNote(box, 'بازدیدکننده‌ای با این فیلتر نیست.');

        box.innerHTML = '';
        const table = tableEl(
            ['شناسه', 'کاربر', 'آخرین حضور', 'اولین منبع', 'بازدید', 'مشاهدهٔ محصول', 'سفارش', ''],
            rows.map(v => [
                String(v._id || '').slice(0, 8),
                v.userId ? 'شناخته‌شده' : 'ناشناس',
                v.lastSeenAt ? new Date(v.lastSeenAt).toLocaleDateString('fa-IR') : '—',
                (v.firstTouch && CHANNEL_LABELS[v.firstTouch.channel]) || '—',
                v.visits, v.productViews, v.orders,
                {button: 'سفر', anonId: v._id, userId: v.userId}
            ]));
        box.appendChild(table);
    }

    // ---- نمای ۴: سفرِ کاربر (تنها نمایی که دادهٔ خام می‌خواند) ----
    async function loadJourney(anonId, userId) {
        const box = host.querySelector('[data-journey]');
        const input = host.querySelector('[data-journey-id]');
        const q = typeof anonId === 'string' ? anonId : input.value.trim();
        if (!q) return emptyNote(box, 'شناسهٔ کاربر یا شناسهٔ ناشناس را وارد کنید.');
        input.value = q;

        let data;
        try {
            const param = userId ? `userId=${encodeURIComponent(userId)}` : `anonId=${encodeURIComponent(q)}`;
            data = await getJson(`${API}/journey?${param}`);
        } catch (e) { return emptyNote(box, 'واکشی ناموفق بود.'); }
        if (!data.events || !data.events.length) return emptyNote(box, 'رویدادی برای این شناسه نیست.');

        box.innerHTML = '';
        const note = document.createElement('p');
        note.className = 'text-xs text-gray-500 mb-3';
        note.textContent = 'شناسه‌های این شخص: ' + (data.anonIds || []).length
            + ' دستگاه/مرورگر — همهٔ رویدادهایشان زیرِ یک سفر آمده است.';
        box.appendChild(note);

        // گروه‌بندی بر اساسِ بازدید، چون سؤالِ واقعی «در هر بازدید چه کرد» است.
        const bySession = new Map();
        data.events.slice().reverse().forEach(e => {
            if (!bySession.has(e.sessionId)) bySession.set(e.sessionId, []);
            bySession.get(e.sessionId).push(e);
        });

        bySession.forEach((events, sid) => {
            const card = document.createElement('div');
            card.className = 'bg-white border rounded-xl p-4 mb-3';
            const head = document.createElement('div');
            head.className = 'text-xs font-bold text-indigo-700 mb-2';
            const ch = events[0] && events[0].channel;
            head.textContent = 'بازدید ' + String(sid).slice(0, 8)
                + ' — منبع: ' + ((CHANNEL_LABELS[ch] || ch) || '؟');
            card.appendChild(head);

            events.forEach(e => {
                const row = document.createElement('div');
                row.className = 'flex gap-3 text-xs py-1 border-b last:border-0';
                row.appendChild(cell(new Date(e.at).toLocaleString('fa-IR'), 'text-gray-400 shrink-0'));
                row.appendChild(cell(e.type, 'font-bold text-gray-700 shrink-0 w-36'));
                row.appendChild(cell(e.entityName || e.path || '', 'text-gray-600 truncate'));
                card.appendChild(row);
            });
            box.appendChild(card);
        });
    }

    function cell(text, cls) {
        const d = document.createElement('div');
        d.className = cls;
        d.textContent = text;      // ← همیشه textContent، هرگز innerHTML
        return d;
    }

    // ---- نمای ۶: داده و آرشیو ----
    async function loadArchives() {
        const box = host.querySelector('[data-archives]');
        box.innerHTML = '';
        let status, list;
        try {
            status = await getJson(`${API}/retention-status`);
            list = await getJson(`${API}/archives`);
        } catch (e) { return emptyNote(box, 'واکشی ناموفق بود.'); }

        if (status.archiveOverdue) {
            const warn = document.createElement('div');
            warn.className = 'bg-red-50 border border-red-200 text-red-800 rounded-lg p-3 mb-4 text-sm font-bold';
            warn.textContent = 'هشدار: ' + Number(status.pendingRows).toLocaleString('fa-IR')
                + ' رویدادِ کهنه‌تر از ' + status.retentionDays
                + ' روز هنوز آرشیو نشده‌اند و به همین دلیل حذف هم نشده‌اند.'
                + (status.lastError ? ' آخرین خطا: ' + status.lastError : '');
            box.appendChild(warn);
        } else {
            const ok = document.createElement('div');
            ok.className = 'bg-green-50 border border-green-200 text-green-800 rounded-lg p-3 mb-4 text-sm';
            ok.textContent = 'همهٔ دادهٔ کهنه‌تر از ' + status.retentionDays + ' روز آرشیو شده است.';
            box.appendChild(ok);
        }

        box.appendChild(section('آرشیوهای گرفته‌شده', tableEl(
            ['بازه', 'ردیف', 'حجم', 'تاریخ', ''],
            (list || []).map(a => [
                new Date(a.from).toLocaleDateString('fa-IR') + ' تا ' + new Date(a.to).toLocaleDateString('fa-IR'),
                Number(a.rowCount).toLocaleString('fa-IR'),
                Math.round(a.sizeBytes / 1024) + ' KB',
                new Date(a.createdAt).toLocaleDateString('fa-IR'),
                {link: API + '/archives/' + a.id + '/download', text: 'دانلود'}
            ])),
            'هیچ آرشیوی خودکار حذف نمی‌شود؛ حذفشان تصمیمِ شماست.'));
    }

    async function archiveNow() {
        if (!confirm('چرخهٔ آرشیو-سپس-حذف همین حالا اجرا شود؟')) return;
        try {
            const r = await postJson(`${API}/archive-now`);
            alert('انجام شد. ' + Number(r.deleted || 0).toLocaleString('fa-IR') + ' رویداد آرشیو و حذف شد.');
        } catch (e) {
            alert('اجرا ناموفق بود.');
        }
        loadArchives();
    }

    function exportRange() {
        const {from, to} = range();
        // دانلود از همان اندپوینتِ ADMIN؛ خودِ سرور در activity_logs ثبتش می‌کند.
        window.location.href = `${API}/export?from=${from}T00:00:00Z&to=${to}T23:59:59Z`;
    }

    // ==========================================================
    // ابزارهای مشترک
    // ==========================================================

    /**
     * جدولِ عمومی. مقدارها همیشه با textContent می‌نشینند — هیچ رشته‌ای از رویداد
     * به innerHTML نمی‌رود، وگرنه یک utm_campaignِ ساخته‌شده با یک لینک، در پنلِ
     * داخلی اجرا می‌شد.
     */
    function tableEl(headers, rows) {
        const wrap = document.createElement('div');
        wrap.className = 'bg-white rounded-xl border shadow-sm overflow-x-auto';
        const table = document.createElement('table');
        table.className = 'w-full text-right';

        const thead = document.createElement('thead');
        thead.className = 'bg-gray-50 text-gray-500 text-[11px]';
        const htr = document.createElement('tr');
        headers.forEach(h => {
            const th = document.createElement('th');
            th.className = 'p-3';
            th.textContent = h;
            htr.appendChild(th);
        });
        thead.appendChild(htr);
        table.appendChild(thead);

        const tbody = document.createElement('tbody');
        tbody.className = 'divide-y text-xs';
        rows.forEach(r => {
            const tr = document.createElement('tr');
            tr.className = 'hover:bg-gray-50';
            r.forEach(v => {
                const td = document.createElement('td');
                td.className = 'p-3';
                if (v && typeof v === 'object' && v.link) {
                    const a = document.createElement('a');
                    a.href = v.link;
                    a.className = 'text-indigo-600 font-bold';
                    a.textContent = v.text;
                    td.appendChild(a);
                } else if (v && typeof v === 'object' && v.button) {
                    const b = document.createElement('button');
                    b.className = 'text-indigo-600 font-bold';
                    b.textContent = v.button;
                    b.addEventListener('click', () => {
                        showView('journey');
                        loadJourney(v.anonId, v.userId);
                    });
                    td.appendChild(b);
                } else {
                    td.textContent = (typeof v === 'number')
                        ? Number(v).toLocaleString('fa-IR') : (v == null ? '—' : String(v));
                }
                tr.appendChild(td);
            });
            tbody.appendChild(tr);
        });
        table.appendChild(tbody);
        wrap.appendChild(table);
        return wrap;
    }

    function section(title, child, note) {
        const wrap = document.createElement('div');
        wrap.className = 'mb-6';
        const h = document.createElement('h3');
        h.className = 'font-bold text-gray-700 mb-2';
        h.textContent = title;
        wrap.appendChild(h);
        if (note) {
            const p = document.createElement('p');
            p.className = 'text-[11px] text-amber-700 bg-amber-50 border border-amber-200 rounded p-2 mb-2';
            p.textContent = note;
            wrap.appendChild(p);
        }
        wrap.appendChild(child);
        return wrap;
    }

    async function getJson(url) {
        const res = await fetch(url, {credentials: 'include'});
        if (!res.ok) throw new Error(await res.text());
        return res.json();
    }

    async function postJson(url) {
        const m = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
        const res = await fetch(url, {
            method: 'POST', credentials: 'include',
            headers: m ? {'X-XSRF-TOKEN': decodeURIComponent(m[1])} : {}
        });
        if (!res.ok) throw new Error(await res.text());
        return res.json();
    }

    const SHELL = `
        <div class="flex flex-col md:flex-row md:items-center justify-between gap-3 mb-4">
            <div>
                <h2 class="text-xl md:text-2xl font-bold text-gray-800">رفتار کاربران</h2>
                <p class="text-xs text-gray-500 mt-1">
                    نماهای کلی از جمع‌بندیِ روزانه خوانده می‌شوند؛ فقط «سفر کاربر» به دادهٔ خام نگاه می‌کند.
                </p>
            </div>
            <div class="flex flex-wrap gap-2 items-center">
                <input type="date" data-from class="p-2 border rounded-lg text-sm">
                <input type="date" data-to class="p-2 border rounded-lg text-sm">
                <button data-reload class="px-4 py-2 rounded-lg bg-indigo-600 text-white text-sm font-bold">نمایش</button>
            </div>
        </div>

        <div class="flex flex-wrap gap-2 mb-5">
            <button data-view-btn="overview">نمای کلی</button>
            <button data-view-btn="sources">منبع ورود</button>
            <button data-view-btn="visitors">کاربران</button>
            <button data-view-btn="journey">سفر کاربر</button>
            <button data-view-btn="searches">جست‌وجوها</button>
            <button data-view-btn="data">داده و آرشیو</button>
        </div>

        <div data-view="overview"><div data-overview></div></div>
        <div data-view="sources" class="hidden"><div data-sources></div></div>

        <div data-view="visitors" class="hidden">
            <select data-visitor-filter class="p-2 border rounded-lg text-sm bg-white mb-3">
                <option value="">همهٔ بازدیدکنندگان</option>
                <option value="cartNoOrder">سبد دارد ولی سفارش نداده</option>
                <option value="rfq">استعلام داده</option>
                <option value="stockNotify">منتظرِ موجودی است</option>
            </select>
            <div data-visitors></div>
        </div>

        <div data-view="journey" class="hidden">
            <div class="flex gap-2 mb-3">
                <input type="text" data-journey-id placeholder="شناسهٔ ناشناس یا شناسهٔ کاربر"
                       class="flex-1 p-2 border rounded-lg text-sm" dir="ltr">
                <button data-journey-go class="px-4 py-2 rounded-lg bg-indigo-600 text-white text-sm font-bold">نمایشِ سفر</button>
            </div>
            <div data-journey></div>
        </div>

        <div data-view="searches" class="hidden"><div data-searches></div></div>

        <div data-view="data" class="hidden">
            <div class="flex flex-wrap gap-2 mb-4">
                <button data-export class="px-4 py-2 rounded-lg bg-green-600 text-white text-sm font-bold">خروجیِ بازهٔ انتخاب‌شده</button>
                <button data-archive-now class="px-4 py-2 rounded-lg border text-gray-600 text-sm font-bold hover:bg-gray-50">اجرای آرشیو</button>
            </div>
            <div data-archives></div>
            <p class="text-[10px] text-gray-400 mt-6">
                داده‌های شهر از پایگاه‌دادهٔ DB-IP (dbip.com) — مجوز CC BY 4.0.
            </p>
        </div>`;

    window.UserBehaviorTab = {mount};
})();
