/**
 * تبدیلِ تاریخِ شمسی ↔ میلادی — بدونِ هیچ کتابخانه‌ای.
 *
 * 🔴 <b>چرا الگوریتم و نه Intl:</b> مرورگر با
 * {@code toLocaleDateString('fa-IR')} تاریخ را شمسی <b>نشان</b> می‌دهد، ولی راهی
 * برای <b>خواندنِ</b> «۱۴۰۵/۰۶/۲۵» و برگرداندنش به میلادی ندارد. فیلدِ تاریخ هر دو
 * را لازم دارد: نمایش و ورودی. پس همان حسابِ استانداردِ تقویمِ جلالی اینجا نوشته
 * شده — چند خط ریاضیِ قطعی، بی‌وابستگی و بی‌نیاز به CDN.
 *
 * ⚠️ ورودیِ کاربر ممکن است ارقامِ فارسی یا عربی داشته باشد و با «/» یا «-» جدا شده
 * باشد؛ همه نرمال می‌شوند.
 */
(function () {

    function div(a, b) { return Math.floor(a / b); }

    /** میلادی → شمسی. ورودی: سالِ کامل، ماهِ ۱..۱۲، روزِ ۱..۳۱ */
    function toJalali(gy, gm, gd) {
        const gDaysInMonth = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
        let gy2 = (gm > 2) ? (gy + 1) : gy;
        let days = 355666 + (365 * gy) + div(gy2 + 3, 4) - div(gy2 + 99, 100)
            + div(gy2 + 399, 400) + gd;
        for (let i = 0; i < gm - 1; i++) days += gDaysInMonth[i];

        let jy = -1595 + (33 * div(days, 12053));
        days %= 12053;
        jy += 4 * div(days, 1461);
        days %= 1461;
        if (days > 365) {
            jy += div(days - 1, 365);
            days = (days - 1) % 365;
        }
        const jm = (days < 186) ? 1 + div(days, 31) : 7 + div(days - 186, 30);
        const jd = 1 + ((days < 186) ? (days % 31) : ((days - 186) % 30));
        return [jy, jm, jd];
    }

    /** شمسی → میلادی. */
    function toGregorian(jy, jm, jd) {
        jy += 1595;
        let days = -355668 + (365 * jy) + (div(jy, 33) * 8) + div((jy % 33) + 3, 4) + jd
            + ((jm < 7) ? (jm - 1) * 31 : ((jm - 7) * 30) + 186);
        let gy = 400 * div(days, 146097);
        days %= 146097;
        if (days > 36524) {
            gy += 100 * div(--days, 36524);
            days %= 36524;
            if (days >= 365) days++;
        }
        gy += 4 * div(days, 1461);
        days %= 1461;
        if (days > 365) {
            gy += div(days - 1, 365);
            days = (days - 1) % 365;
        }
        let gd = days + 1;
        const leap = ((gy % 4 === 0 && gy % 100 !== 0) || gy % 400 === 0) ? 29 : 28;
        const gDaysInMonth = [31, leap, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
        let gm = 0;
        for (; gm < 12 && gd > gDaysInMonth[gm]; gm++) gd -= gDaysInMonth[gm];
        return [gy, gm + 1, gd];
    }

    const FA_DIGITS = '۰۱۲۳۴۵۶۷۸۹';

    function toFaDigits(s) {
        return String(s).replace(/[0-9]/g, d => FA_DIGITS[+d]);
    }

    /** ارقامِ فارسی و عربی → لاتین، تا بشود عدد خواندشان. */
    function toLatinDigits(s) {
        return String(s)
            .replace(/[۰-۹]/g, d => String(d.charCodeAt(0) - 0x06F0))
            .replace(/[٠-٩]/g, d => String(d.charCodeAt(0) - 0x0660));
    }

    const pad2 = n => (n < 10 ? '0' + n : String(n));

    /** «YYYY-MM-DD»ِ میلادی → «۱۴۰۵/۰۶/۲۵»ِ شمسی. ورودیِ نامعتبر → رشتهٔ خالی. */
    function isoToJalali(iso) {
        if (!iso) return '';
        const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(iso).trim());
        if (!m) return '';
        const [jy, jm, jd] = toJalali(+m[1], +m[2], +m[3]);
        return toFaDigits(jy + '/' + pad2(jm) + '/' + pad2(jd));
    }

    /** «۱۴۰۵/۰۶/۲۵» (یا با «-» و ارقامِ لاتین) → «YYYY-MM-DD»ِ میلادی، یا null. */
    function jalaliToIso(text) {
        if (!text) return null;
        const parts = toLatinDigits(text).trim().split(/[\/\-.]/).map(x => parseInt(x, 10));
        if (parts.length !== 3 || parts.some(isNaN)) return null;
        const [jy, jm, jd] = parts;
        if (jy < 1200 || jy > 1700 || jm < 1 || jm > 12 || jd < 1 || jd > 31) return null;
        const [gy, gm, gd] = toGregorian(jy, jm, jd);
        return gy + '-' + pad2(gm) + '-' + pad2(gd);
    }

    window.DnJalali = {toJalali, toGregorian, isoToJalali, jalaliToIso, toFaDigits, toLatinDigits};
})();
