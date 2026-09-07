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

    function build() {
        bubble = document.createElement('button');
        bubble.type = 'button';
        bubble.setAttribute('aria-label', 'پشتیبانی');
        bubble.textContent = '💬';
        bubble.style.cssText = 'position:fixed;bottom:20px;left:20px;z-index:9998;width:54px;height:54px;'
            + 'border-radius:50%;border:0;background:#4338ca;color:#fff;font-size:24px;cursor:pointer;'
            + 'box-shadow:0 6px 20px rgba(0,0,0,.25);';
        bubble.addEventListener('click', toggle);
        document.body.appendChild(bubble);

        panel = document.createElement('div');
        panel.style.cssText = 'position:fixed;bottom:84px;left:20px;z-index:9999;width:340px;max-width:calc(100vw - 40px);'
            + 'height:460px;max-height:calc(100vh - 120px);background:#fff;border-radius:14px;display:none;'
            + 'flex-direction:column;overflow:hidden;box-shadow:0 12px 40px rgba(0,0,0,.28);'
            + 'font-family:inherit;direction:rtl;';
        panel.innerHTML = `
            <div style="background:#4338ca;color:#fff;padding:10px 12px;display:flex;justify-content:space-between;align-items:center;">
                <div style="font-weight:bold;font-size:13px;">پشتیبانی</div>
                <button type="button" data-close style="background:transparent;border:0;color:#fff;font-size:20px;cursor:pointer;line-height:1;">✕</button>
            </div>
            <div data-state style="padding:14px;font-size:13px;color:#4b5563;line-height:2;"></div>
            <div data-thread style="flex:1;min-height:0;display:none;"></div>`;
        panel.querySelector('[data-close]').addEventListener('click', toggle);
        document.body.appendChild(panel);
    }

    function toggle() {
        opened = !opened;
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

        started = true;
        setState('');
        const threadHost = panel.querySelector('[data-thread]');
        threadHost.style.display = 'block';

        ChatCore.mountThread(threadHost, {isAgent: false, selfId: session.userId});
        ChatCore.connect();
        await ChatCore.openConversation(session.conversation.id);
        ChatCore.requestNotificationPermission();

        applyBusinessHours(session.businessHours);
        await sendProductContext();
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
        try {
            await fetch(`${API}/context`, {
                method: 'POST',
                credentials: 'include',
                headers: {'Content-Type': 'application/json'},
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
