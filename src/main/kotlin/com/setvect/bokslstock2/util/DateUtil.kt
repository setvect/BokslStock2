package com.setvect.bokslstock2.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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
    @JvmOverloads
    fun format(localDateTime: LocalDateTime, pattern: String? = yyyy_MM_dd): String {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return localDateTime.format(formatter)
    }

    @JvmStatic
    fun formatDateTime(localDateTime: LocalDateTime): String {
        return format(localDateTime, yyyy_MM_dd_HH_mm_ss)
    }

    fun format(localTime: LocalTime, pattern: String?): String {
        val formatter = DateTimeFormatter.ofPattern(pattern)
        return localTime.format(formatter)
    }

    @JvmStatic
    fun convert(timeInMillis: Long): LocalDateTime {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timeInMillis), TimeZone.getDefault().toZoneId())
    }
}