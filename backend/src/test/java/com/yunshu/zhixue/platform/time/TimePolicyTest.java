package com.yunshu.zhixue.platform.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TimePolicyTest {
    @Test
    void businessDayChangesAtShanghaiMidnightWhileTimestampRemainsUtc() {
        var instant = Instant.parse("2026-10-30T16:00:00Z");
        assertEquals(LocalDate.of(2026, 10, 31), TimePolicy.businessDate(instant));
        assertEquals("2026-10-30T16:00:00.000Z", TimePolicy.formatUtc(instant));
    }
}
