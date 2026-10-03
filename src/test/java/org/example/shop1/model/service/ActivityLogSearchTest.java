package org.example.shop1.model.service;

import org.bson.Document;
import org.example.shop1.model.entity.ActivityLog;
import org.example.shop1.model.reposritory.ActivityLogRepository;
import org.example.shop1.model.reposritory.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * نگهبانِ فیلترهایِ تاریخچهٔ تغییرات.
 * <p>
 * دو چیز اینجا آزموده می‌شود که شکستنشان بی‌صدا است:
 * <ul>
 *   <li><b>فیلترِ رویداد واقعاً به کوئری می‌رسد</b> — اگر نرسد، سلکت کار می‌کند
 *       ولی همان نتیجهٔ قبلی برمی‌گردد و کاربر فکر می‌کند رویدادی ثبت نشده.</li>
 *   <li><b>ورودیِ ناشناخته وارد کوئری نمی‌شود</b> — رشتهٔ دلخواهِ کاربر هرگز نباید
 *       مستقیم به شرطِ مونگو تبدیل شود.</li>
 * </ul>
 * ⚠️ سندِ کوئری عمداً با {@code toJson()} خوانده نمی‌شود: مقدارِ بازهٔ زمانی
 * {@code Instant} است و کدکِ BSON ندارد تا لحظهٔ ذخیره — سریالایزکردنش همین‌جا
 * می‌ترکد. پس شرط‌ها مستقیم از خودِ سند برداشته می‌شوند.
 */
class ActivityLogSearchTest {

    private MongoOperations mongo;
    private ActivityLogService service;

    @BeforeEach
    void setUp() {
        mongo = mock(MongoOperations.class);
        service = new ActivityLogService(mock(ActivityLogRepository.class),
                mock(UserRepository.class), mongo);
        when(mongo.count(any(Query.class), eq(ActivityLog.class))).thenReturn(0L);
        when(mongo.find(any(Query.class), eq(ActivityLog.class))).thenReturn(List.of());
    }

    @SuppressWarnings("unchecked")
    private List<Document> conditions() {
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(captor.capture(), eq(ActivityLog.class));
        Document where = captor.getValue().getQueryObject();
        Object and = where.get("$and");
        if (and instanceof List<?> list) {
            List<Document> out = new ArrayList<>();
            list.forEach(d -> out.add((Document) d));
            return out;
        }
        return List.of(where);
    }

    /** مقدارِ یک شرط در کوئری، یا {@code null} اگر اصلاً آن شرط ساخته نشده باشد. */
    private Object condition(String key) {
        for (Document d : conditions()) {
            if (d.containsKey(key)) return d.get(key);
        }
        return null;
    }

    @Test
    void actionFilterReachesTheQuery() {
        service.search(null, null, "CHAT_TRANSFER", null, null, 0, 50);

        assertEquals(ActivityLog.Action.CHAT_TRANSFER, condition("action"),
                "فیلترِ رویداد باید در شرطِ کوئری بنشیند");
    }

    @Test
    void lowerCaseActionIsAccepted() {
        service.search(null, null, "chat_release", null, null, 0, 50);

        assertEquals(ActivityLog.Action.CHAT_RELEASE, condition("action"));
    }

    @Test
    void unknownActionIsIgnoredNotInjected() {
        service.search(null, null, "{$ne: null}", null, null, 0, 50);

        assertNull(condition("action"), "رویدادِ ناشناخته یعنی بی‌فیلتر، نه شرطِ دلخواهِ کاربر");
    }

    @Test
    void allThreeFiltersCombine() {
        service.search("ali", "CONVERSATION", "CHAT_CLAIM",
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-02-01T00:00:00Z"), 0, 50);

        assertEquals("ali", condition("username"));
        assertEquals("CONVERSATION", condition("entityType"));
        assertEquals(ActivityLog.Action.CHAT_CLAIM, condition("action"));
        assertNotNull(condition("at"), "بازهٔ زمانی هم باید سرِ جایش باشد");
    }

    @Test
    void emptyFiltersLeaveOnlyTheDateRange() {
        service.search(null, null, null, null, null, 0, 50);

        assertNotNull(condition("at"), "بازهٔ زمانی همیشه هست");
        assertNull(condition("username"));
        assertNull(condition("entityType"));
        assertNull(condition("action"));
    }
}
