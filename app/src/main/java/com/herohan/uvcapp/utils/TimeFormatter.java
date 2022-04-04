package com.herohan.uvcapp.utils;

import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

public class TimeFormatter {
    public static final SimpleDateFormat yyyy_MM_dd = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    public static final SimpleDateFormat yyyy_MM_ddHH_mm = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    public static final SimpleDateFormat yyyy_MM_ddHH_mm_ss = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    public static final SimpleDateFormat yyyy_MM_dd_HH_mm_ss = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
    public static final SimpleDateFormat yyyyMMdd = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
    public static final SimpleDateFormat yyyyMMddHHmmss = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());

    public static final SimpleDateFormat yyyy_M_d_cn = new SimpleDateFormat("yyyy年M月d日", Locale.getDefault());
    public static final SimpleDateFormat yyyy_M_d_HH_mm_cn = new SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.getDefault());

    public enum DateType {
        YEAR,
        MONTH,
        DAY,
        HOUR,
        MIN,
        SEC,
        TIME
    }

    public static String format_yyyy_MM_dd(long time) {
        return format_yyyy_MM_dd(new Date(time));
    }

    public static String format_yyyy_MM_dd(Date date) {
        return yyyy_MM_dd.format(date);
    }

    public static String format_yyyy_MM_dd(Calendar calendar) {
        return format_yyyy_MM_dd(calendar.getTime());
    }

    public static String format_yyyy_MM_dd_HH_mm(long time) {
        return format_yyyy_MM_dd_HH_mm(new Date(time));
    }

    public static String format_yyyy_MM_dd_HH_mm(Date date) {
        return yyyy_MM_ddHH_mm.format(date);
    }

    public static String format_yyyy_MM_dd_HH_mm(Calendar calendar) {
        return format_yyyy_MM_dd_HH_mm(calendar.getTime());
    }

    public static String format_yyyy_MM_ddHH_mm_ss(long time) {
        return format_yyyy_MM_ddHH_mm_ss(new Date(time));
    }

    public static String format_yyyy_MM_ddHH_mm_ss(Date date) {
        return yyyy_MM_ddHH_mm_ss.format(date);
    }

    public static String format_yyyy_MM_ddHH_mm_ss(Calendar calendar) {
        return format_yyyy_MM_ddHH_mm_ss(calendar.getTime());
    }

    public static String format_yyyy_MM_dd_HH_mm_ss(long time) {
        return format_yyyy_MM_dd_HH_mm_ss(new Date(time));
    }

    public static String format_yyyy_MM_dd_HH_mm_ss(Date date) {
        return yyyy_MM_dd_HH_mm_ss.format(date);
    }

    public static String format_yyyy_MM_dd_HH_mm_ss(Calendar calendar) {
        return format_yyyy_MM_dd_HH_mm_ss(calendar.getTime());
    }

    public static String format_yyyyMMdd(long time) {
        return format_yyyyMMdd(new Date(time));
    }

    public static String format_yyyyMMdd(Date date) {
        return yyyyMMdd.format(date);
    }

    public static String format_yyyyMMdd(Calendar calendar) {
        return format_yyyyMMdd(calendar.getTime());
    }

    public static String format_yyyyMMddHHmmss(long time) {
        return format_yyyyMMddHHmmss(new Date(time));
    }

    public static String format_yyyyMMddHHmmss(Date date) {
        return yyyyMMddHHmmss.format(date);
    }

    public static String getTimeFormatValue(long time) {
        return MessageFormat.format("{0,number,00}:{1,number,00}", Long.valueOf(time / 60), Long.valueOf(time % 60));
    }

    public static String getHHMMSSFormatValue(int time) {
        long t = (long) (time / 1000);
        return MessageFormat.format("{0,number,00}:{1,number,00}:{2,number,00}", Long.valueOf((t / 60) / 60), Long.valueOf((t / 60) % 60), Long.valueOf(t % 60));
    }

    public static String getFormattedDateString(int timeZoneOffset) {
        TimeZone timeZone;
        if (timeZoneOffset > 13 || timeZoneOffset < -12) {
            timeZoneOffset = 0;
        }
        String[] ids = TimeZone.getAvailableIDs(timeZoneOffset * 60 * 60 * 1000);
        if (ids.length == 0) {
            timeZone = TimeZone.getDefault();
        } else {
            timeZone = new SimpleTimeZone(timeZoneOffset * 60 * 60 * 1000, ids[0]);
        }
        SimpleDateFormat sdf = yyyy_MM_ddHH_mm_ss;
        sdf.setTimeZone(timeZone);
        return sdf.format(new Date());
    }

    public static String getDateTime(String date, SimpleDateFormat simpleDateFormat, DateType type) {
        String result = null;
        if (date == null || date.isEmpty() || simpleDateFormat == null) {
            return null;
        }
        Date newDate = null;
        try {
            newDate = simpleDateFormat.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (newDate != null) {
            switch (type) {
                case YEAR:
                    result = String.valueOf(newDate.getYear() + 1900);
                    break;
                case MONTH:
                    result = String.valueOf(newDate.getMonth() + 1);
                    break;
                case DAY:
                    result = String.valueOf(newDate.getDate());
                    break;
                case HOUR:
                    result = String.valueOf(newDate.getHours());
                    break;
                case MIN:
                    result = String.valueOf(newDate.getMinutes());
                    break;
                case SEC:
                    result = String.valueOf(newDate.getSeconds());
                    break;
                case TIME:
                    result = getInt2TwoByte(newDate.getHours()) + ":" + getInt2TwoByte(newDate.getMinutes()) + ":" + getInt2TwoByte(newDate.getSeconds());
                    break;
                default:
                    break;
            }
        }
        return result;
    }

    public static String getTime_mm_ss(long ms) {
        SimpleDateFormat formatter = new SimpleDateFormat("mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        return formatter.format(Long.valueOf(ms));
    }

    public static String getFormattedDateTime(SimpleDateFormat format, long dateTime) {
        if (format == null || dateTime < 0) {
            return null;
        }
        return format.format(new Date(dateTime));
    }

    private static String getInt2TwoByte(int num) {
        String str = String.valueOf(num);
        if (num < 10) {
            return "0" + num;
        }
        return str;
    }
}
