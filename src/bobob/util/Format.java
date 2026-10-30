package bobob.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Format {

    private static final DateTimeFormatter DATE =
        DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private static final DateTimeFormatter MONTH =
        DateTimeFormatter.ofPattern("MMMM yyyy", new Locale("id", "ID"));

    private Format() {
    }

    public static String date(LocalDate date) {
        return DATE.format(date);
    }

    public static String date(String isoDate) {
        return DATE.format(LocalDate.parse(isoDate));
    }

    public static String month(LocalDate date) {
        return MONTH.format(date);
    }

    public static String money(double value) {
        return "Rp " + String.format(Locale.GERMANY, "%,.0f", value);
    }
}
