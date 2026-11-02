package net.laper.lapermod.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShopScheduleTest {
    private final ShopSchedule schedule = new ShopSchedule("Asia/Jakarta", List.of("12:00", "20:00"), 120);

    @Test
    void activeInsideWindow() {
        var w = schedule.activeAt(Instant.parse("2026-10-31T06:30:00Z")); // 13:30 WIB
        assertTrue(w.isPresent());
        assertEquals(Instant.parse("2026-10-31T05:00:00Z"), w.get().start());
        assertEquals(Instant.parse("2026-10-31T07:00:00Z"), w.get().end());
    }

    @Test
    void inactiveOutsideWindow() {
        assertFalse(schedule.activeAt(Instant.parse("2026-10-31T04:59:59Z")).isPresent()); // 11:59:59 WIB
        assertFalse(schedule.activeAt(Instant.parse("2026-10-31T07:00:00Z")).isPresent()); // 14:00:00 WIB
    }

    @Test
    void windowCrossingMidnight() {
        var s = new ShopSchedule("Asia/Jakarta", List.of("23:00"), 120);
        assertTrue(s.activeAt(Instant.parse("2026-10-31T17:30:00Z")).isPresent()); // 00:30 WIB besok
    }

    @Test
    void durationIsCappedAtTwoHours() {
        var s = new ShopSchedule("UTC", List.of("10:00"), 999);
        assertEquals(120, s.duration().toMinutes());
    }

    @Test
    void nextStart() {
        var next = schedule.nextStart(Instant.parse("2026-10-31T08:00:00Z")); // 15:00 WIB
        assertTrue(next.isPresent());
        assertEquals(20, next.get().getHour());
    }
}
