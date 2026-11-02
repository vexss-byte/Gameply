package net.laper.lapermod.shop;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Jadwal event berdasarkan waktu nyata (tanpa state, otomatis benar setelah restart). */
public final class ShopSchedule {
    public record Window(Instant start, Instant end) {}

    private final ZoneId zone;
    private final List<LocalTime> times = new ArrayList<>();
    private final Duration duration;

    public ShopSchedule(String zoneId, List<String> startTimes, int durationMinutes) {
        ZoneId z;
        try {
            z = ZoneId.of(zoneId);
        } catch (Exception e) {
            z = ZoneId.of("UTC");
        }
        this.zone = z;
        for (String s : startTimes) {
            try {
                times.add(LocalTime.parse(s.trim()));
            } catch (Exception ignored) {
                // jam tidak valid dilewati
            }
        }
        this.duration = Duration.ofMinutes(Math.max(1, Math.min(120, durationMinutes)));
    }

    public ZoneId zone() {
        return zone;
    }

    public Duration duration() {
        return duration;
    }

    /** Window yang sedang aktif (jika beberapa tumpang tindih, ambil yang mulai paling akhir). */
    public Optional<Window> activeAt(Instant now) {
        LocalDate today = now.atZone(zone).toLocalDate();
        Window best = null;
        for (int d = -1; d <= 0; d++) {
            LocalDate date = today.plusDays(d);
            for (LocalTime t : times) {
                Instant start = ZonedDateTime.of(date, t, zone).toInstant();
                Instant end = start.plus(duration);
                if (!now.isBefore(start) && now.isBefore(end) && (best == null || start.isAfter(best.start()))) {
                    best = new Window(start, end);
                }
            }
        }
        return Optional.ofNullable(best);
    }

    /** Waktu mulai berikutnya setelah `now`. */
    public Optional<ZonedDateTime> nextStart(Instant now) {
        LocalDate today = now.atZone(zone).toLocalDate();
        ZonedDateTime best = null;
        for (int d = 0; d <= 1; d++) {
            for (LocalTime t : times) {
                ZonedDateTime s = ZonedDateTime.of(today.plusDays(d), t, zone);
                if (s.toInstant().isAfter(now) && (best == null || s.isBefore(best))) best = s;
            }
        }
        return Optional.ofNullable(best);
    }
}
