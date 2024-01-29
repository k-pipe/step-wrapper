package org.kpipe;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

public class TimeFormatter {

    static Supplier<String> parseFormat() {
        String timeformat = Parameters.TIME_FORMAT;
        if (timeformat == null) {
            return null;
        }
        if (timeformat.equals(Constants.RELATIVE_TIME_FORMAT)) {
            return TimeFormatter::relativeTimeStamp;
        }
        if (timeformat.equals(Constants.MILLIS)) {
            return () -> Long.toString(System.currentTimeMillis());
        }
        if (timeformat.equals(Constants.ISO)) {
            return () -> DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now());
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timeformat);
            return () -> formatter.format(LocalDateTime.now());
        } catch(IllegalArgumentException e) {
            Main.fail("Could not parse time format: "+timeformat, e);
            return null;
        }
    }

    private static String relativeTimeStamp() {
        long time = System.currentTimeMillis() - Main.getStartTime();
        int secs = (int) (time / 1000);
        int milis = (int) (time % 1000);
        int digits;
        if (secs >= 1000) {
            digits = 0;
        } else if (secs >= 100) {
            digits = 1;
        } else if (secs >= 10) {
            digits = 2;
        } else {
            digits = 3;
        }
        StringBuilder timestr = new StringBuilder(Integer.toString(milis));
        while (timestr.length() < 3) {
            timestr.insert(0, "0");
        }
        timestr = new StringBuilder(secs + "." + timestr.substring(0, digits));
        while (timestr.length() < 5) {
            timestr.insert(0, " ");
        }
        return timestr.toString();
    }

}
