package org.example.shop1.model.service.analytics;

import org.example.shop1.model.enums.Channel;

/**
 * بستهٔ منبعِ ورودِ یک بازدید — همان چیزی که روی {@code SESSION_START} می‌نشیند و
 * {@code channel}/{@code campaign}ِ آن روی <b>هر</b> رویدادِ آن بازدید تکرار می‌شود.
 */
public record TrafficSource(
        Channel channel,
        String source,
        String medium,
        String campaign,
        String term,
        String content,
        String referrerHost,
        String landingPath) {

    public static TrafficSource direct(String landingPath) {
        return new TrafficSource(Channel.DIRECT, null, null, null, null, null, null, landingPath);
    }
}
