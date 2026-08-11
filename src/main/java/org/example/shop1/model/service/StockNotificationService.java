package org.example.shop1.model.service;

import org.example.shop1.exeption.ApiException;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.entity.StockNotification;
import org.example.shop1.model.entity.User;
import org.example.shop1.model.reposritory.ProductRepository;
import org.example.shop1.model.reposritory.StockNotificationRepository;
import org.example.shop1.model.reposritory.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockNotificationService {

    private static final Logger log = LoggerFactory.getLogger(StockNotificationService.class);

    // آستانه‌ای که بالاتر از آن، تعدادِ مشترکین (هزینهٔ پیامک) را برجسته لاگ می‌کنیم
    private static final int HIGH_VOLUME_THRESHOLD = 50;

    private final StockNotificationRepository repo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;
    private final SmsService smsService;

    public StockNotificationService(StockNotificationRepository repo, ProductRepository productRepo,
                                    UserRepository userRepo, SmsService smsService) {
        this.repo = repo;
        this.productRepo = productRepo;
        this.userRepo = userRepo;
        this.smsService = smsService;
    }

    // ثبتِ درخواستِ اطلاع‌رسانی توسط کاربرِ لاگین‌کرده (موبایل از سشن، نه ورودی)
    public void register(String productId) {
        String mobile = currentUserMobile();
        Product p = productRepo.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "محصول یافت نشد"));

        // اگر محصول موجود است، ثبتِ اطلاع‌رسانی بی‌معنی است
        if (p.getStock() > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "این محصول هم‌اکنون موجود است");
        }

        // جلوگیری از ثبتِ تکراری (همان کاربر/محصول که هنوز اطلاع نگرفته) — بی‌صدا موفق
        if (repo.existsByProductIdAndMobileAndNotifiedFalse(productId, mobile)) {
            return;
        }
        repo.save(new StockNotification(productId, mobile));
    }

    /**
     * تریگرِ موجودشدن: async تا ذخیرهٔ محصول را کند/شکننده نکند.
     * برای هر مشترکِ اطلاع‌نداده‌شده پیامک می‌فرستد و در صورتِ موفقیت notified=true می‌کند
     * (خطای یک نفر بقیه را متوقف نمی‌کند؛ ناموفق‌ها notified=false می‌مانند تا بعداً دوباره تلاش شود).
     */
    @Async
    public void notifyBackInStock(String productId, String productName) {
        List<StockNotification> subs = repo.findByProductIdAndNotifiedFalse(productId);
        if (subs.isEmpty()) return;

        if (subs.size() >= HIGH_VOLUME_THRESHOLD) {
            log.warn("⚠️ تعدادِ زیادِ مشترکِ اطلاع‌رسانی ({}) برای «{}» — هزینهٔ پیامک را در نظر بگیرید",
                    subs.size(), productName);
        }

        int sent = 0, failed = 0;
        for (StockNotification n : subs) {
            try {
                smsService.sendBackInStockNotification(n.getMobile(), productName);
                n.setNotified(true);
                repo.save(n);
                sent++;
            } catch (Exception e) {
                failed++;
                log.error("ارسالِ پیامکِ موجودی به {} ناموفق بود: {}", n.getMobile(), e.getMessage());
            }
        }
        log.info("اطلاع‌رسانیِ موجودشدنِ «{}»: {} موفق، {} ناموفق", productName, sent, failed);
    }

    private String currentUserMobile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            User u = userRepo.findByUsername(auth.getName())
                    .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "کاربر یافت نشد"));
            String mobile = (u.getPhoneNumber() != null && !u.getPhoneNumber().isBlank())
                    ? u.getPhoneNumber() : u.getUsername();
            if (mobile == null || mobile.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "شمارهٔ موبایلِ کاربر ثبت نشده است");
            }
            return mobile;
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "برای ثبتِ اطلاع‌رسانی ابتدا وارد شوید");
    }
}
