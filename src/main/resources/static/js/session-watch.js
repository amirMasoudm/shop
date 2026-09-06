/**
 * نگهبانِ سشن — پنل را بدونِ نیازِ به رفرشِ دستی از باطل‌شدنِ سشن باخبر می‌کند.
 *
 * وقتی ادمین نقشِ کسی را عوض می‌کند، UserService.changeUserRole همه‌ی سشن‌هایِ آن
 * کاربر را (رویِ هر دستگاهی) از مانگو پاک می‌کند. ولی صفحه‌ی بازِ او تا وقتی
 * درخواستی نفرستد از این خبر ندارد؛ این فایل همان درخواستِ کوچک را می‌فرستد.
 *
 * انتخابِ کم‌هزینه‌ترین راه (به‌جایِ WebSocket/SSE که برایِ هر کاربر یک اتصالِ باز
 * نگه می‌دارند): یک GETِ خالی به /api/users/api/session-check که هیچ کوئریِ
 * دیتابیسی ندارد، آن هم فقط:
 *   - هر ۶۰ ثانیه، و تنها وقتی تب واقعاً دیده می‌شود (تبِ پنهان صفر هزینه دارد)
 *   - بلافاصله وقتی کاربر به تب برمی‌گردد (visibilitychange) — یعنی در عمل
 *     تغییرِ دسترسی همان لحظه‌ی بازگشت به تب اعمال می‌شود، نه بعدِ یک دقیقه
 */
(function () {
    const CHECK_URL = '/api/users/api/session-check';
    const INTERVAL_MS = 60000;

    let timer = null;
    let kicked = false;

    async function check() {
        if (kicked) return;
        let res;
        try {
            res = await fetch(CHECK_URL, {credentials: 'include'});
        } catch (e) {
            // قطعیِ موقتِ شبکه یا ری‌استارتِ سرور نباید کاربر را بیرون بیندازد
            return;
        }
        if (res.status === 401 || res.status === 403) kickOut();
    }

    function kickOut() {
        kicked = true;
        if (timer) clearInterval(timer);
        window.location.href = '/AdminLogin.html';
    }

    timer = setInterval(() => { if (!document.hidden) check(); }, INTERVAL_MS);
    document.addEventListener('visibilitychange', () => { if (!document.hidden) check(); });
})();
