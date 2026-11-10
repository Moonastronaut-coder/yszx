package com.yunshu.zhixue.platform.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public final class TimePolicy {
    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter UTC_MILLIS = new DateTimeFormatterBuilder().appendInstant(3).toFormatter();
    private TimePolicy() {}
    public static String formatUtc(Instant instant) { return UTC_MILLIS.format(instant); }
    public static LocalDate businessDate(Instant instant) { return instant.atZone(BUSINESS_ZONE).toLocalDate(); }
}
