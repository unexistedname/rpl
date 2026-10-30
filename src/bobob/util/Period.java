package bobob.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public final class Period {

    public static final String HARIAN = "Harian";
    public static final String MINGGUAN = "Mingguan";
    public static final String BULANAN = "Bulanan";

    private Period() {
    }

    public static LocalDate start(LocalDate anchor, String mode) {
        if (MINGGUAN.equals(mode)) {
            return anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        if (BULANAN.equals(mode)) {
            return anchor.withDayOfMonth(1);
        }
        return anchor;
    }

    public static LocalDate end(LocalDate anchor, String mode) {
        if (MINGGUAN.equals(mode)) {
            return start(anchor, mode).plusDays(6);
        }
        if (BULANAN.equals(mode)) {
            return anchor.withDayOfMonth(anchor.lengthOfMonth());
        }
        return anchor;
    }

    public static String label(LocalDate anchor, String mode) {
        LocalDate start = start(anchor, mode);
        LocalDate end = end(anchor, mode);

        if (BULANAN.equals(mode)) {
            return Format.month(anchor);
        }
        if (MINGGUAN.equals(mode)) {
            return Format.date(start) + " s/d " + Format.date(end);
        }
        return Format.date(anchor);
    }
}
