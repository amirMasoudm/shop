package org.example.shop1.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.shop1.model.service.LegacyPathFallback;
import org.example.shop1.model.service.LegacyRedirectService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

/**
 * کنترلرِ خطا، فقط برایِ اینکه ۴۰۴ آخرین فرصتش را بگیرد.
 * <p>
 * 🔴 <b>چرا این‌جا و نه در یک فیلتر:</b> شرطِ اصلیِ تسک این بود که فالبک «فقط روی
 * ۴۰۴» اجرا شود و هرگز جلوی مسیرِ واقعی را نگیرد. تنها جایی که این تضمین رایگان
 * است همین‌جاست: کدِ ۴۰۴ از قبل تصمیم‌گیری شده و همهٔ کنترلرها شکست خورده‌اند.
 * فیلتر باید پاسخ را بپیچد و وضعیت را حدس بزند.
 * <p>
 * از {@link BasicErrorController} ارث می‌برد تا وقتی فالبک جوابی ندارد، رفتارِ
 * پیش‌فرضِ خطا عیناً حفظ شود — نه بازنویسیِ صفحهٔ خطا.
 */
@Controller
public class LegacyPathErrorController extends BasicErrorController {

    private final LegacyPathFallback fallback;

    public LegacyPathErrorController(ErrorAttributes errorAttributes, ServerProperties serverProperties,
                                     ObjectProvider<ErrorViewResolver> errorViewResolvers,
                                     LegacyPathFallback fallback) {
        super(errorAttributes, serverProperties.getError(), errorViewResolvers.orderedStream().toList());
        this.fallback = fallback;
    }

    @Override
    public ModelAndView errorHtml(HttpServletRequest request, HttpServletResponse response) {
        String target = fallback.resolve(request);
        if (target == null) return super.errorHtml(request, response);

        sendMovedPermanently(response, target);
        // null یعنی «پاسخ را خودم دادم» — صفحهٔ خطا رندر نمی‌شود
        return null;
    }

    @Override
    public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
        String target = fallback.resolve(request);
        if (target == null) return super.error(request);

        // مسیرِ غیرِHTML (مثلاً curlِ خام) هم باید همان ۳۰۱ را بگیرد، وگرنه رفتارِ
        // سایت به هدرِ Accept گره می‌خورد و تستِ خودکار چیزِ دیگری می‌بیند.
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                .header(HttpHeaders.LOCATION, LegacyRedirectService.toLocationHeader(target))
                .build();
    }

    /**
     * ⚠️ {@code Location} با {@code toLocationHeader} ساخته می‌شود: مقصدها فارسی‌اند و
     * هدرِ HTTP نویسهٔ غیرِ لاتین-۱ نمی‌پذیرد — تامکت هدر را بی‌صدا حذف می‌کند و نتیجه
     * ۳۰۱ِ بدونِ مقصد می‌شود.
     */
    private void sendMovedPermanently(HttpServletResponse response, String target) {
        response.setStatus(HttpStatus.MOVED_PERMANENTLY.value());
        response.setHeader(HttpHeaders.LOCATION, LegacyRedirectService.toLocationHeader(target));
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    }
}
