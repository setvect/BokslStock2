package com.setvect.bokslstock2.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

object DateUtil {
    const val yyyyMMdd = "yyyyMMdd"
    const val yyyy_MM_dd = "yyyy-MM-dd"
    const val yyyy_MM_dd_HH_mm_ss = "yyyy-MM-dd HH:mm:ss"
    const val yyyy_MM_ddTHH_mm_ss = "yyyy-MM-dd'T'HH:mm:ss"
    const val yyyy_MM_ddTHH_mm_ssZ = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    const val HHmmss = "HHmmss"
    const val HH_mm_ss = "HH:mm:ss"
    val NUMBER_FORMAT: NumberFormat = DecimalFormat("#.############")
    fun getLocalDateTime(text: String, pattern: String): LocalDateTime {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return LocalDateTime.parse(text, formatter)
    }

    /**
     *
     * @param dateStr yyyy-MM-dd 형태
     * @return LocalDAte
     */
    fun getLocalDate(dateStr: String?): LocalDate {
        return getLocalDate(dateStr, yyyy_MM_dd)
    }

    fun getLocalDate(dateStr: String?, pattern: String?): LocalDate {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return LocalDate.parse(dateStr, formatter)
    }

    fun getLocalDateTime(dateStr: String?): LocalDateTime {
        val formatter = DateTimeFormatter.ofPattern(yyyy_MM_ddTHH_mm_ss)
        return LocalDateTime.parse(dateStr, formatter)
    }

    fun getLocalTime(timeStr: String?): LocalTime {
        val formatter = DateTimeFormatter.ofPattern(HH_mm_ss)
        return LocalTime.parse(timeStr, formatter)
    }

    fun getLocalTime(timeStr: String?, pattern: String?): LocalTime {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return LocalTime.parse(timeStr, formatter)
    }

    /**
     * @param localDateTime 날짜시간
     * @return yyyy-MM-dd 형태로 반환
     */
    fun format(localDateTime: LocalDateTime, pattern: String = yyyy_MM_dd): String {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return localDateTime.format(formatter)
    }

    fun formatDateTime(localDateTime: LocalDateTime): String {
        return format(localDateTime, yyyy_MM_dd_HH_mm_ss)
    }

    fun format(localTime: LocalDate, pattern: String): String {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return localTime.format(formatter)
    }

    fun format(localTime: LocalTime, pattern: String): String {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return localTime.format(formatter)
    }

    fun convert(timeInMillis: Long): LocalDateTime {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timeInMillis), TimeZone.getDefault().toZoneId())
    }

    /**
     * 어떻게 설명해야될지 모르겠다. ㅡㅡ;
     * 분기나 반기로 시작하는 날짜를 맞추기 위함
     * 예시)
     * - [date] = 2021-02-10, [deviceMonth] = 3 => 2021-01-10
     * - [date] = 2021-11-21, [deviceMonth] = 3 => 2021-10-21
     * - [date] = 2021-08-15, [deviceMonth] = 6 => 2021-07-15
     *
     * @return 입력 기간을 [deviceMonth]로 나눠 제일 작은 날짜로 반환
     */
    fun fitMonth(date: LocalDate, deviceMonth: Int): LocalDate {
        if (deviceMonth < 1 || deviceMonth > 12) {
            throw RuntimeException("입력값은 1~12 사이 입니다.")
        }
        return date.withMonth((date.monthValue - 1) / deviceMonth * deviceMonth + 1)
    }

    fun fitMonth(date: LocalDateTime, deviceMonth: Int): LocalDate {
        return fitMonth(date.toLocalDate(), deviceMonth)
    }

    fun getUnixTime(date: LocalDate): Long {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().epochSecond
    }

    fun getUnixTimeCurrent(): Long {
        return LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().epochSecond
    }

    /**
     * [current]을 기준으로 가장 앞에 있는 월요일 날짜 반환
     * 예) 2022-08-12(금) -> 2022-08-08(월)
     */
    fun convertDateOfMonday(current: LocalDateTime): LocalDateTime {
        var week = current.dayOfWeek
        return current.minusDays(week.value.toLong() - 1);
    }
}