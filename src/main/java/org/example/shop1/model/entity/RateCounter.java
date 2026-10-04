package org.example.shop1.model.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * شمارندهٔ پنجره‌ایِ محدودیتِ نرخ. در مونگو، نه در حافظه، تا ری‌استارت شمارش را صفر
 * نکند — وگرنه هر استقرار سقفِ پیامک را از نو باز می‌کرد. {@code expireAt} هم پایانِ
 * پنجره است و هم کلیدِ ایندکسِ TTL.
 */
@Document(collection = "rate_counters")
public class RateCounter {

    @Id
    private String id;
    private long count;
    private Instant expireAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
    public Instant getExpireAt() { return expireAt; }
    public void setExpireAt(Instant expireAt) { this.expireAt = expireAt; }
}
