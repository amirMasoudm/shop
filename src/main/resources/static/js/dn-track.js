/**
 * بیکنِ ردگیریِ رفتار — فقط برایِ چیزی که سرور نمی‌بیند.
 *
 * ⚠️ بیشترِ رویدادها سروری ثبت می‌شوند (مشاهدهٔ صفحه، سفارش، پرداخت، استعلام، چت،
 * ورود). این فایل فقط ناوبریِ داخلیِ SPA، فیلتر، مرتب‌سازی، عمقِ مطالعه و سبدِ
 * سمتِ مرورگر را می‌فرستد — چیزهایی که هیچ درخواستی به سرور نمی‌زنند.
 *
 * هویت اینجا نیست: anonId و sessionId در کوکیِ HttpOnly هستند و userId از سشنِ
 * سرور خوانده می‌شود. این فایل عمداً هیچ شناسه‌ای نمی‌فرستد — اگر می‌فرستاد، هرکسی
 * می‌توانست تاریخچهٔ جعلی برای هر کاربری بسازد.
 *
 * گوگل آنالیتیکس سرِ جایش می‌ماند؛ dnTrack کنارِ gaEvent می‌نشیند نه به‌جایش.
 */
(function () {
    const ENDPOINT = '/api/v1/track';
    const FLUSH_MS = 5000;
    const MAX_BATCH = 20;

    let queue = [];
    let timer = null;

    function dnTrack(type, payload) {
        if (!type) return;
        payload = payload || {};
        queue.push({
            type: type,
            // زمانِ خودِ رویداد همراهش می‌رود: اگر سرور زمانِ دریافت را بگذارد، بیست
            // رویدادِ یک بسته همه یک زمان می‌گیرند و ترتیبِ «سفرِ کاربر» از بین می‌رود.
            at: Date.now(),
            path: location.pathname,
            entityType: payload.entityType || null,
            entityId: payload.entityId || null,
            entityName: payload.entityName || null,
            props: payload.props || null
        });
        if (queue.length >= MAX_BATCH) flush();
        else schedule();
    }

    function schedule() {
        if (timer) return;
        timer = setTimeout(flush, FLUSH_MS);
    }

    function flush(useBeacon) {
        clearTimeout(timer);
        timer = null;
        if (!queue.length) return;

        const batch = queue.splice(0, MAX_BATCH);
        const body = JSON.stringify({events: batch});

        // sendBeacon موقعِ بسته‌شدن/پنهان‌شدنِ تب هم تحویل می‌دهد، جایی که fetch لغو می‌شود.
        if (useBeacon && navigator.sendBeacon) {
            navigator.sendBeacon(ENDPOINT, new Blob([body], {type: 'application/json'}));
            return;
        }
        fetch(ENDPOINT, {
            method: 'POST',
            credentials: 'include',
            headers: {'Content-Type': 'application/json'},
            body: body,
            keepalive: true
        }).catch(() => { /* ردگیری هرگز نباید خطای کاربر بسازد */ });
    }

    // تبِ پنهان‌شده یعنی شاید دیگر برنگردد — همان‌جا تخلیه شود.
    document.addEventListener('visibilitychange', function () {
        if (document.hidden) flush(true);
    });
    window.addEventListener('pagehide', function () { flush(true); });

    window.dnTrack = dnTrack;
})();
