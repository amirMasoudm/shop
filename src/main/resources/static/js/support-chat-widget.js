/**
 * حبابِ چتِ پشتیبانی — سمتِ مشتری.
 *
 * پنجرهٔ گفت‌وگو از /js/chat-core.js می‌آید (همان ماژولی که پنلِ کارشناس هم استفاده
 * می‌کند)؛ این فایل فقط حباب، بازوبسته‌شدن، و زمینهٔ محصول را اضافه می‌کند.
 *
 * 🔒 <b>قانونِ ضدِ دورزدن:</b> در هیچ‌جای این UI شماره، ایمیل یا لینکِ شبکهٔ اجتماعی
 * نیست — نه در راهنما، نه در پیامِ خارج از ساعتِ کاری. کلِ هدفِ این چت این است که
 * جایگزینِ آن کانال‌ها باشد، پس اگر روزی کسی خواست «برای تماسِ سریع‌تر…» اینجا
 * بگذارد، همان لحظه هدفِ فیچر نقض می‌شود.
 *
 * زمینهٔ محصول: صفحه می‌تواند پیش از بازشدنِ حباب مقدار بدهد:
 *     window.supportChatContext = {productName: '…', productUrl: '…'}
 * حبابِ باز شده یک پیامِ SYSTEM ثبت می‌کند تا کارشناس بدونِ پرسیدن بداند موضوع چیست.
 */
(function () {
    const API = '/api/v1/chat';

    let opened = false;
    let started = false;
    let panel = null;
    let bubble = null;
    let contextSent = false;

    /**
     * جایگاهِ حباب از CSS می‌آید نه از استایلِ درون‌خطی.
     * <p>
     * 🔴 دلیلش موبایل است: پنلِ مشتری یک نوارِ پایینِ ثابت دارد و حبابِ چت دقیقاً
     * روی دکمهٔ پروفایل می‌نشست. با استایلِ درون‌خطی نمی‌شود media query نوشت، پس
     * جایگاه به یک متغیرِ CSS منتقل شد که هر صفحه می‌تواند بازنویسی‌اش کند.
     */
    function injectStyles() {
        if (document.getElementById('dn-chat-widget-style')) return;
        const st = document.createElement('style');
        st.id = 'dn-chat-widget-style';
        st.textContent = `
            :root { --dn-chat-bottom: 20px; }
            /* موبایل: بالاتر از نوارِ پایین می‌نشیند تا روی دکمه‌ها نیفتد */
            @media (max-width: 768px) { :root { --dn-chat-bottom: 92px; } }
            /* ⚠️ leftِ خانه عمداً اینجاست نه در استایلِ درون‌خطی: وقتی حباب از
               حالتِ جابه‌جاشده به خانه برمی‌گردد، leftِ درون‌خطی پاک می‌شود و اگر
               تکیه‌گاهِ دیگری نباشد، جعبه در صفحهٔ راست‌چین به لبهٔ راست می‌پرد
               (اندازه‌گیری: left از ۲۰ به ۳۳۶). */
            .dn-chat-bubble { left: var(--dn-chat-left, 20px);
                              bottom: var(--dn-chat-bottom) !important;
                              touch-action: none; cursor: grab;
                              transition: left .26s cubic-bezier(.2,.8,.3,1),
                                          top  .26s cubic-bezier(.2,.8,.3,1); }
            /* وقتی کاربر حباب را جابه‌جا کرده، جایگاه با top/left داده می‌شود؛
               پس bottomِ !importantِ بالا باید کنار برود وگرنه جعبه بینِ دو
               لنگر گیر می‌کند. */
            .dn-chat-bubble.dn-free { bottom: auto !important; }
            .dn-chat-bubble.dn-dragging { transition: none; cursor: grabbing; }
            .dn-chat-bubble.dn-no-anim { transition: none !important; }
            @media (prefers-reduced-motion: reduce) {
                .dn-chat-bubble { transition: none; }
            }
            .dn-chat-panel  { bottom: calc(var(--dn-chat-bottom) + 64px) !important;
                              max-height: calc(100vh - var(--dn-chat-bottom) - 96px) !important; }
        `;
        document.head.appendChild(st);
    }

    function build() {
        injectStyles();
        bubble = document.createElement('button');
        bubble.type = 'button';
        bubble.setAttribute('aria-label', 'پشتیبانی');
        bubble.textContent = '💬';
        bubble.style.cssText = 'position:fixed;bottom:20px;z-index:9998;width:54px;height:54px;'
            + 'border-radius:50%;border:0;background:#4338ca;color:#fff;font-size:24px;cursor:pointer;'
            + 'box-shadow:0 6px 20px rgba(0,0,0,.25);';
        bubble.className = 'dn-chat-bubble';
        // ⚠️ کلیک از toggle جدا شد: هر کشیدن در پایان یک رویدادِ click هم
        // می‌دهد و بدونِ این گارد، جابه‌جاکردنِ حباب پنجره را هم باز می‌کرد.
        bubble.addEventListener('click', function () {
            if (bubble.dataset.dragged === '1') { bubble.dataset.dragged = ''; return; }
            toggle();
        });
        document.body.appendChild(bubble);
        makeDraggable();
        freePos = loadPos();
        applyPos(freePos);
        keepInView();

        panel = document.createElement('div');
        panel.style.cssText = 'position:fixed;bottom:84px;left:20px;z-index:9999;width:340px;max-width:calc(100vw - 40px);'
            + 'height:460px;max-height:calc(100vh - 120px);background:#fff;border-radius:14px;display:none;'
            + 'flex-direction:column;overflow:hidden;box-shadow:0 12px 40px rgba(0,0,0,.28);'
            + 'font-family:inherit;direction:rtl;';
        panel.className = 'dn-chat-panel';
        panel.innerHTML = `
            <div style="background:#4338ca;color:#fff;padding:10px 12px;display:flex;justify-content:space-between;align-items:center;">
                <div>
                    <div style="font-weight:bold;font-size:13px;">پشتیبانی</div>
                    <!-- نامِ کارشناسی که گفت‌وگو را برداشته؛ تا وقتی کسی برنداشته خالی می‌ماند -->
                    <div data-agent style="font-size:11px;opacity:.85;margin-top:1px;display:none;"></div>
                </div>
                <button type="button" data-close style="background:transparent;border:0;color:#fff;font-size:20px;cursor:pointer;line-height:1;">✕</button>
            </div>
            <div data-state style="padding:14px;font-size:13px;color:#4b5563;line-height:2;"></div>
            <div data-thread style="flex:1;min-height:0;display:none;"></div>`;
        panel.querySelector('[data-close]').addEventListener('click', toggle);
        document.body.appendChild(panel);
    }

    /* ═══ جابه‌جاییِ حباب ═══
       خواستهٔ مالک: کاربر بتواند حباب را بگیرد و هرجای صفحه بگذارد تا جلوِ
       چیزی را نگیرد؛ و وقتی زد، حباب بیاید پایین و پنجرهٔ چت همان‌جا باز شود.
       جای انتخابیِ کاربر ذخیره می‌شود و بعد از بستنِ پنجره هم برمی‌گردد. */
    const POS_KEY = 'dn_chat_pos';
    const DRAG_THRESHOLD = 6;   // زیرِ این جابه‌جایی یعنی «زدن»، نه «کشیدن»
    let freePos = null;         // null یعنی سرِ جای خانه (پایین-چپ)

    function clamp(v, lo, hi) { return Math.max(lo, Math.min(hi, v)); }

    function applyPos(p) {
        if (!p) {
            bubble.classList.remove('dn-free');
            bubble.style.left = '';
            bubble.style.top = '';
            return;
        }
        bubble.classList.add('dn-free');
        bubble.style.left = p.left + 'px';
        bubble.style.top = p.top + 'px';
    }

    function savePos() {
        try {
            if (freePos) localStorage.setItem(POS_KEY, JSON.stringify(freePos));
            else localStorage.removeItem(POS_KEY);
        } catch (e) { /* حالتِ ناشناس/مسدود — جایگاه فقط همین نشست می‌ماند */ }
    }

    function loadPos() {
        try {
            const raw = localStorage.getItem(POS_KEY);
            if (!raw) return null;
            const p = JSON.parse(raw);
            return (typeof p.left === 'number' && typeof p.top === 'number') ? p : null;
        } catch (e) { return null; }
    }

    // صفحه که کوچک/بزرگ شود، حبابِ جابه‌جاشده نباید بیرونِ قابِ دید بماند
    function keepInView() {
        if (!freePos) return;
        const m = 6, w = bubble.offsetWidth || 54, h = bubble.offsetHeight || 54;
        freePos.left = clamp(freePos.left, m, Math.max(m, window.innerWidth - w - m));
        freePos.top = clamp(freePos.top, m, Math.max(m, window.innerHeight - h - m));
        applyPos(freePos);
    }

    // مختصاتِ «خانه» را از روی خودِ CSS می‌خوانیم، نه با عددِ ثابت: جایگاهِ خانه
    // در موبایل با media query فرق دارد و هر صفحه هم می‌تواند بازنویسی‌اش کند.
    function homePoint() {
        const wasFree = bubble.classList.contains('dn-free');
        const prevLeft = bubble.style.left, prevTop = bubble.style.top;
        bubble.classList.add('dn-no-anim');
        applyPos(null);
        const r = bubble.getBoundingClientRect();
        if (wasFree) {
            bubble.classList.add('dn-free');
            bubble.style.left = prevLeft;
            bubble.style.top = prevTop;
        }
        bubble.getBoundingClientRect();       // تخلیهٔ چیدمان، تا پرش دیده نشود
        bubble.classList.remove('dn-no-anim');
        return {left: Math.round(r.left), top: Math.round(r.top)};
    }

    function makeDraggable() {
        let pid = null, sx = 0, sy = 0, ox = 0, oy = 0, moved = false;

        bubble.addEventListener('pointerdown', function (e) {
            if (e.button) return;                      // فقط کلیکِ اصلی
            pid = e.pointerId;
            moved = false;
            const r = bubble.getBoundingClientRect();
            sx = e.clientX; sy = e.clientY; ox = r.left; oy = r.top;
            try { bubble.setPointerCapture(pid); } catch (err) {}
            bubble.classList.add('dn-dragging');
        });

        bubble.addEventListener('pointermove', function (e) {
            if (pid === null || e.pointerId !== pid) return;
            const dx = e.clientX - sx, dy = e.clientY - sy;
            if (!moved && Math.sqrt(dx * dx + dy * dy) < DRAG_THRESHOLD) return;
            moved = true;
            freePos = {left: ox + dx, top: oy + dy};
            keepInView();
        });

        function end(e) {
            if (pid === null || (e.pointerId !== undefined && e.pointerId !== pid)) return;
            try { bubble.releasePointerCapture(pid); } catch (err) {}
            pid = null;
            bubble.classList.remove('dn-dragging');
            bubble.dataset.dragged = moved ? '1' : '';
            if (moved) savePos();
        }

        bubble.addEventListener('pointerup', end);
        bubble.addEventListener('pointercancel', end);
        window.addEventListener('resize', keepInView);
    }

    function toggle() {
        opened = !opened;
        if (opened) {
            // حباب می‌آید پایین تا پنجره از کنارِ خودش باز شود؛ اگر وسطِ صفحه
            // مانده بود، پنجره از آن جدا می‌افتاد.
            if (freePos) {
                applyPos(homePoint());                 // با انیمیشن تا خانه
                setTimeout(function () { if (opened) applyPos(null); }, 300);
            } else {
                applyPos(null);
            }
        } else if (freePos) {
            applyPos(freePos);                         // برگشت به جایی که کاربر انتخاب کرده بود
        }
        panel.style.display = opened ? 'flex' : 'none';
        if (opened && !started) start();
    }

    function setState(text) {
        const box = panel.querySelector('[data-state]');
        box.textContent = text;          // ← عمداً textContent
        box.style.display = text ? 'block' : 'none';
    }

    async function start() {
        setState('در حال اتصال…');
        let session;
        try {
            const res = await fetch(`${API}/session`, {credentials: 'include'});
            if (res.status === 401) {
                // بدونِ ورود نمی‌شود چت کرد؛ ولی ساعتِ کاری را می‌شود نشان داد.
                await showLoggedOut();
                return;
            }
            if (!res.ok) throw new Error();
            session = await res.json();
        } catch (e) {
            setState('اتصال به پشتیبانی برقرار نشد. کمی بعد دوباره تلاش کنید.');
            return;
        }

        // حسابِ کارکنان گفت‌وگوی مشتریِ خودش را ندارد، پس /session عمداً conversation
        // برنمی‌گرداند. بدونِ این شاخه، ویجت گفت‌وگوی نداشته را باز می‌کرد، شناسه خالی
        // می‌ماند و اولین ارسال با «شناسهٔ گفت‌وگو لازم است» رد می‌شد — خطایی که هیچ
        // ربطی به کارِ کاربر نداشت و فهمیدنش سخت بود.
        if (session.isAgent) {
            setState('شما با حسابِ کارکنان وارد شده‌اید. گفت‌وگو با مشتریان در پنلِ فروش، تبِ «پشتیبانی مشتریان» است.');
            const link = document.createElement('a');
            link.href = '/SalesPanel.html';
            link.textContent = 'رفتن به پنل فروش';
            link.style.cssText = 'display:inline-block;margin-top:8px;color:#4338ca;font-weight:bold;';
            panel.querySelector('[data-state]').appendChild(link);
            return;
        }

        started = true;
        setState('');
        const threadHost = panel.querySelector('[data-thread]');
        threadHost.style.display = 'block';

        ChatCore.mountThread(threadHost, {isAgent: false, selfId: session.userId});
        ChatCore.connect();
        await ChatCore.openConversation(session.conversation.id);
        ChatCore.requestNotificationPermission();

        applyBusinessHours(session.businessHours);
        showAgentName(session.conversation);
        await sendProductContext();
    }

    /**
     * نامِ کارشناسِ گفت‌وگو، بالای پنجرهٔ چت.
     * <p>
     * نام از خودِ سرور می‌آید و خطابش («آقای/خانم») همان‌جا ساخته می‌شود، پس اینجا
     * فقط نمایش داده می‌شود. تا وقتی کسی گفت‌وگو را برنداشته خالی می‌ماند — نوشتنِ
     * نامِ حدسی بدتر از ننوشتن است.
     */
    function showAgentName(conversation) {
        const el = panel && panel.querySelector('[data-agent]');
        if (!el) return;
        const name = conversation && conversation.assignedAgentName;
        el.textContent = name ? 'کارشناس: ' + name : '';   // ← textContent، نه innerHTML
        el.style.display = name ? 'block' : 'none';
    }

    /** قفلِ خارج از ساعتِ کاری — ورودی بسته، تاریخچه خوانا. */
    function applyBusinessHours(status) {
        if (!status || status.open) {
            ChatCore.setComposerEnabled(true);
            return;
        }
        const when = status.schedule ? ' (' + status.schedule + ')' : '';
        ChatCore.setComposerEnabled(false, (status.message || '') + when);
    }

    async function showLoggedOut() {
        let hint = '';
        try {
            const res = await fetch('/api/v1/settings/chat-hours');
            if (res.ok) {
                const data = await res.json();
                const s = data.status || {};
                if (!s.open && s.schedule) hint = '\n' + s.message + ' ' + s.schedule;
            }
        } catch (e) { /* بی‌اهمیت */ }
        setState('برای گفت‌وگو با پشتیبانی ابتدا وارد حساب کاربری خود شوید.' + hint);
    }

    /**
     * زمینهٔ خودکار: اگر چت از صفحهٔ محصول باز شده، همان‌جا ثبت می‌شود.
     * فقط یک‌بار در هر بازدید، وگرنه با هر بار بازوبسته‌کردن تاریخچه پر می‌شود.
     */
    async function sendProductContext() {
        if (contextSent) return;
        const ctx = window.supportChatContext;
        if (!ctx || !ctx.productName) return;
        contextSent = true;
        // توکنِ CSRF — صفحه‌های عمومی رَپرِ fetch ندارند (توضیح در chat-core.js)
        const m = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
        const headers = {'Content-Type': 'application/json'};
        if (m) headers['X-XSRF-TOKEN'] = decodeURIComponent(m[1]);
        try {
            await fetch(`${API}/context`, {
                method: 'POST',
                credentials: 'include',
                headers: headers,
                body: JSON.stringify({
                    productName: ctx.productName,
                    productUrl: ctx.productUrl || location.href
                })
            });
        } catch (e) { /* زمینه اختیاری است؛ نبودش نباید چت را بشکند */ }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', build);
    } else {
        build();
    }
})();
