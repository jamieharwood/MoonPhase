package org.iHarwood;

import java.util.Calendar;
import java.util.logging.Logger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class MoonPhase {
    private static final Logger logger = Logger.getLogger(MoonPhase.class.getName());

    private static final String[] NEW_MOON = {
            "       _..._     ",
            "     .:::::::.   ",
            "    :::::::::::  ",
            "    :::::::::::  ",
            "    `:::::::::'  ",
            "      `':::''    "
    };

    private static final String[] WAXING_CRESCENT = {
            "       _..._     ",
            "     .::::. `.   ",
            "    :::::::.  :  ",
            "    ::::::::  :  ",
            "    `::::::' .'  ",
            "      `'::'-'    "
    };

    private static final String[] FIRST_QUARTER = {
            "       _..._     ",
            "     .::::  `.   ",
            "    ::::::    :  ",
            "    ::::::    :  ",
            "    `:::::   .'  ",
            "      `'::.-'    "
    };

    private static final String[] WAXING_GIBBOUS = {
            "       _..._     ",
            "     .::'   `.   ",
            "    :::       :  ",
            "    :::       :  ",
            "    `::.     .'  ",
            "      `':..-'    "
    };

    private static final String[] FULL_MOON = {
            "       _..._     ",
            "     .'     `.   ",
            "    :         :  ",
            "    :         :  ",
            "    `.       .'  ",
            "      `-...-'    "
    };

    private static final String[] WANING_GIBBOUS = {
            "       _..._     ",
            "     .'   `::.   ",
            "    :       :::  ",
            "    :       :::  ",
            "    `.     .::'  ",
            "      `-..:''    "
    };

    private static final String[] LAST_QUARTER = {
            "       _..._     ",
            "     .'  ::::.   ",
            "    :    ::::::  ",
            "    :    ::::::  ",
            "    `.   :::::'  ",
            "      `-.::''    "
    };

    private static final String[] WANING_CRESCENT = {
            "       _..._     ",
            "     .' .::::.   ",
            "    :  ::::::::  ",
            "    :  ::::::::  ",
            "    `. '::::::'  ",
            "      `-.::''    "
    };

    // Example usage: Map them to an array based on your indices
    private static final String[][] PHASES = {
            NEW_MOON,         // 0
            WAXING_CRESCENT,  // 1
            FIRST_QUARTER,    // 2
            WAXING_GIBBOUS,   // 3
            FULL_MOON,        // 4
            WANING_GIBBOUS,   // 5
            LAST_QUARTER,     // 6
            WANING_CRESCENT   // 7
    };
    private static double ip; // Phase fraction (0 to 1)

    public int julianDate(int d, int m, int y) {
        int mm, yy;
        int k1, k2, k3;
        int j;

        yy = y - ((12 - m) / 10);
        mm = m + 9;
        if (mm >= 12) {
            mm = mm - 12;
        }
        k1 = (int)(365.25 * (yy + 4712));
        k2 = (int)(30.6001 * mm + 0.5);
        k3 = (int)(((yy / 100.0) + 49.0) * 0.75) - 38;
        j = k1 + k2 + d + 59;
        if (j > 2299160) {
            j = j - k3; // For Gregorian calendar
        }
        return j;
    }

    public double moonAge(int d, int m, int y) {
        int j = julianDate(d, m, y);
        ip = (j + 4.867) / 29.53059;
        ip = ip - Math.floor(ip);
        double ag;
        if (ip < 0.5) {
            ag = ip * 29.53059 + 29.53059 / 2;
        } else {
            ag = ip * 29.53059 - 29.53059 / 2;
        }
        ag = Math.floor(ag) + 1;

        return ag;
    }

    public String getPhaseName() {
        String[] names = {
                "New Moon",         // 0
                "Waxing Crescent",  // 1
                "First Quarter",    // 2
                "Waxing Gibbous",   // 3
                "Full Moon",        // 4
                "Waning Gibbous",   // 5
                "Last Quarter",     // 6
                "Waning Crescent"   // 7
        };
        int index = (int) (ip * 8);
        return names[index];
    }

    public static void main(String[] args) {
        Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH) + 1; // January is 0
        int year = cal.get(Calendar.YEAR);

        // Print current date, day of week and time to the console
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEEE");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        System.out.println("Date: " + now.format(dateFmt) + " | Day: " + now.format(dayFmt) + " | Time: " + now.format(timeFmt));

        MoonPhase mp = new MoonPhase();
        mp.moonAge(day, month, year); // Sets ip
        String phase = mp.getPhaseName();

        System.out.println("Current moon phase is " + phase + ".");

        for (String row : PHASES[(int) (ip * 8.0)]) {
            System.out.println(row);
            //logger.info(row);
        }
    }
}