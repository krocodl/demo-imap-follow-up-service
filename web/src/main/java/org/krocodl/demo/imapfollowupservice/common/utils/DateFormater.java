package org.krocodl.demo.imapfollowupservice.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Introduced for concurrent usage of SimpleDateFormat
 */
public class DateFormater {

    public static final String DD_MM_YYYY_HH_MM_SS = "dd-MM-yyyy HH/mm/ss";
    private static final String DD_MM_YYYY = "dd-MM-yyyy";


    private static final ThreadLocal<DateFormat> dfTime = ThreadLocal.withInitial(() -> new SimpleDateFormat(DD_MM_YYYY_HH_MM_SS));
    private static final ThreadLocal<DateFormat> dfDate = ThreadLocal.withInitial(() -> new SimpleDateFormat(DD_MM_YYYY));

    public static Date parse(String dateString) {
        try {
            if (StringUtils.isEmpty(dateString)) {
                return null;
            }
            return dateString.contains("/") ? dfTime.get().parse(dateString) : dfDate.get().parse(dateString);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Can't parse " + dateString, ex);
        }
    }

    public static String formatTime(Date date) {
        try {
            return date == null ? null : dfTime.get().format(date);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Can't formatTime ", ex);
        }
    }
}
