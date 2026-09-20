package org.example.shop1.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.shop1.model.entity.Campaign;
import org.example.shop1.model.enums.Channel;
import org.example.shop1.model.enums.EventType;
import org.example.shop1.model.reposritory.CampaignRepository;
import org.example.shop1.model.service.analytics.AnalyticsContext;
import org.example.shop1.model.service.analytics.CampaignService;
import org.example.shop1.model.service.analytics.ChannelResolver;
import org.example.shop1.model.service.analytics.CrawlerDetector;
import org.example.shop1.model.service.analytics.TrafficSource;
import org.example.shop1.model.service.analytics.UserEventRecorder;
import org.example.shop1.model.service.analytics.VisitorSessionWriter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

/**
 * لینکِ کوتاهِ کارزار — {@code GET /l/{code}}.
 *
 * <h3>🔴 چرا مقصد فقط از دیتابیس خوانده می‌شود</h3>
 * هیچ پارامترِ کوئری‌ای در تعیینِ مقصد نقش ندارد. اگر مقصد از آدرس گرفته می‌شد،
 * {@code dadehnama.com/l/x?to=https://phish.example} یک ریدایرکتِ بازِ تمام‌عیار بود و
 * اعتبارِ دامنهٔ ما خرجِ سایتِ جعلیِ کسِ دیگری می‌شد. مقصد هنگامِ <b>ساخت</b> هم
 * اعتبارسنجی می‌شود ({@code CampaignService.normalizeLandingPath})، پس حتی سندِ
 * دستکاری‌شدهٔ دیتابیس هم دوباره از همان صافی رد می‌شود.
 *
 * <h3>چرا انتساب همین‌جا و سمتِ سرور</h3>
 * بازدید <b>پیش از</b> ریدایرکت شروع می‌شود. اگر منتظرِ صفحهٔ مقصد می‌ماندیم، هر
 * کلیکی که صفحه‌اش باز نمی‌شود — بستنِ زودهنگام، اینترنتِ کند، ربات — گم می‌شد.
 * اختلافِ {@code LINK_CLICK} و {@code SESSION_START}ِ صفحهٔ مقصد دقیقاً همین را
 * قابلِ اندازه‌گیری می‌کند.
 *
 * <h3>سئو</h3>
 * ریدایرکت عمداً <b>۳۰۲</b> است نه ۳۰۱ — لینکِ کارزار آدرسِ دائمیِ چیزی نیست و
 * نباید جای مقصد را در ایندکس بگیرد. {@code X-Robots-Tag: noindex} هم روی پاسخ
 * هست و {@code Disallow: /l/} در {@code robots.txt}.
 */
@RestController
public class ShortLinkController {

    private final CampaignRepository repo;
    private final CampaignService campaigns;
    private final UserEventRecorder analytics;
    private final VisitorSessionWriter sessions;
    private final ChannelResolver channelResolver;
    private final CrawlerDetector crawlers;

    public ShortLinkController(CampaignRepository repo, CampaignService campaigns,
                               UserEventRecorder analytics, VisitorSessionWriter sessions,
                               ChannelResolver channelResolver, CrawlerDetector crawlers) {
        this.repo = repo;
        this.campaigns = campaigns;
        this.analytics = analytics;
        this.sessions = sessions;
        this.channelResolver = channelResolver;
        this.crawlers = crawlers;
    }

    @GetMapping("/l/{code}")
    public ResponseEntity<Void> follow(@PathVariable String code,
                                       HttpServletRequest request, HttpServletResponse response) {

        Campaign campaign = repo.findByCode(code)
                .filter(c -> c.isLive(Instant.now()))
                // لینکِ غیرفعال یا منقضی ۴۰۴ِ عادیِ سایت می‌گیرد، نه خطای خام —
                // همان چیزی که بازدیدکننده از هر آدرسِ اشتباهِ دیگری می‌بیند.
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "لینک پیدا نشد"));

        // ⚠️ مقصد دوباره از همان صافیِ ساخت رد می‌شود. سندِ دیتابیس منبعِ معتبرتری از
        // کوئری است، ولی «معتبرتر» یعنی معتبرتر، نه معتبر.
        String target = CampaignService.normalizeLandingPath(campaign.getLandingPath());

        // 🔴 این مسیر تنها جایی است که گاردِ خزندهٔ VisitorIdentityFilter را دور
        // می‌زند: فیلتر برایِ ربات زمینه‌ای نمی‌سازد، ولی startSession وقتی زمینه‌ای
        // نبیند خودش یکی می‌سازد و کوکیِ ناشناس را هم می‌نویسد. پس اتکا به «زمینهٔ
        // نال» اینجا جواب نمی‌دهد و قضاوت باید صریح تکرار شود.
        //
        // ریدایرکت خودش دست‌نخورده انجام می‌شود — فقط شمارشِ کلیک انجام نمی‌شود.
        if (!crawlers.isCrawler(request.getHeader("User-Agent"))) {
            AnalyticsContext ctx = sessions.startSession(request, response,
                    channelOf(campaign), campaign.getSlug());

            // سندِ بازدیدکننده هم همین‌جا ساخته/به‌روز می‌شود تا کلیک در «کاربران» دیده شود.
            analytics.touchVisitor(ctx, new TrafficSource(ctx.channel(), campaign.getSource(),
                    campaign.getMedium(), campaign.getSlug(), campaign.getTerm(), campaign.getContent(),
                    null, target));

            analytics.record(ctx, EventType.LINK_CLICK, "/l/" + code,
                    "CAMPAIGN", campaign.getId(), campaign.getName(),
                    Map.of("code", code, "landingPath", target), null);
            analytics.record(ctx, EventType.SESSION_START, target,
                    "CAMPAIGN", campaign.getId(), campaign.getName(),
                    Map.of("landingPath", target), null);
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, campaigns.taggedPath(campaign))
                .header("X-Robots-Tag", "noindex, nofollow")
                .header(HttpHeaders.CACHE_CONTROL, "no-store, private")
                .build();
    }

    /**
     * کانالِ کارزار از خودِ برچسبش درمی‌آید، نه از هدرِ ارجاع‌دهنده — چون لینکِ کوتاه
     * را خودمان ساخته‌ایم و برچسبش قابلِ اتکاترین چیزی است که داریم.
     */
    private Channel channelOf(Campaign campaign) {
        return channelResolver.classifyCampaign(campaign.getSource(), campaign.getMedium());
    }
}
