package com.qcsy.autocreatetable.core.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Description:
 *
 * @author qcsy
 * @version 2025/2/18
 */
public class DateTimeUtil {
    /**
     * 将Date转换为LocalDateTime
     * @param date
     * @return
     */
    public static LocalDateTime dateToLocalDateTime(Date date) {
        return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
    }
    /**
     * 将LocalDateTime转换为Date
     * @param localDateTime 目标数据
     * @return
     */
    public static Date localDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant());
    }

    /**
     *  get format date between startDate and endDate
     * @param startDate startDate
     * @param endDate endDate
     * @param pattern reesult date format
     * @param unit unit
     * @return List<String>
     */
    public static List<String> getDateStrsBetween(LocalDateTime startDate, LocalDateTime endDate, String pattern, ChronoUnit unit) {
        List<String> times = new ArrayList<>();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
        LocalDateTime tempStartDateTime=startDate;
        // add an hour to startDate
        times.add(startDate.format(dateTimeFormatter));
        while (tempStartDateTime.compareTo(endDate)<0) {
            tempStartDateTime=tempStartDateTime.plus(1, unit);
            if(tempStartDateTime.compareTo(endDate)<0){
                times.add(tempStartDateTime.format(dateTimeFormatter));
            }
        };
        if(!times.contains(startDate.format(dateTimeFormatter))){
            times.add(startDate.format(dateTimeFormatter));
        }
        if(!times.contains(endDate.format(dateTimeFormatter))){
            times.add(endDate.format(dateTimeFormatter));
        }
        return times;
    }
}
