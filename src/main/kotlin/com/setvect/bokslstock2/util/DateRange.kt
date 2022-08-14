package com.setvect.bokslstock2.util

import com.setvect.bokslstock2.util.DateUtil.convert
import com.setvect.bokslstock2.util.DateUtil.format
import com.setvect.bokslstock2.util.DateUtil.formatDateTime
import com.setvect.bokslstock2.util.DateUtil.getLocalDateTime
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * 날짜의 범위를 나타내줌 <br></br>
 * 날짜 범위 검색에 필요한 파라미터 역활을 할 수 있음
 */
class DateRange {
    companion object {
        /**
         * 기간 제한 없는 날짜 시작일
         */
        private const val UNLIMITED_DATE_START = "1990-01-01T00:00:00"

        /**
         * 기간 제한 없는 날짜 종료일
         */
        private const val UNLIMITED_DATE_END = "2100-12-31T00:00:00"

        /**
         * @return 1990-01-01 ~ 2100-12-31 날짜 범위 리턴
         * @see .UNLIMITE_DATE_START
         * @see .UNLIMITE_DATE_END
         */
        val maxRange: DateRange get() = DateRange(UNLIMITED_DATE_START, UNLIMITED_DATE_END)
    }

    // TODO LocalDate로 변경
    /**
     * 시작 날짜
     */
    val from: LocalDateTime

    /**
     * 종료 날짜
     */
    val to: LocalDateTime

    /**
     * 오늘 날짜를 기준으로 해서 차이 값을 생성 한다.
     *
     * @param diff
     */
    constructor(diff: Int) {
        // 양수()
        if (diff > 0) {
            from = LocalDateTime.now()
            to = from.plusDays(diff.toLong())
        } else {
            to = LocalDateTime.now()
            from = to.minusDays(diff.toLong())
        }
    }

    /**
     * 날짜 범위를 해당 년도의 달에 1부터 그달의 마지막으로 한다.
     *
     * @param year 년도
     */
    constructor(year: Int, month: Int) {
        val cal = Calendar.getInstance()
        cal[Calendar.YEAR] = year
        cal[Calendar.MONTH] = month

        // 해당 달의 맨 끝에 날짜로 가기위해서
        cal[Calendar.DATE] = 1
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.DATE, -1)
        to = convert(cal.timeInMillis)
        cal[Calendar.DATE] = 1
        from = convert(cal.timeInMillis)
    }

    /**
     * 날짜영역 객체 생성. 기본 날짜 포맷 (yyyy-MM-dd'T'HH:mm:ss)으로 날짜 변환
     *
     * @param from   시작날짜
     * @param to     종료날짜
     * @param format 날짜 패턴 "yyyy, MM, dd, HH, mm, ss and more"
     */
    @JvmOverloads
    constructor(from: String, to: String, format: String = "yyyy-MM-dd'T'HH:mm:ss") {
        this.from = getLocalDateTime(from, format)
        this.to = getLocalDateTime(to, format)
    }

    /**
     * 날짜영역 객체 생성.
     *
     * @param from 시작일
     * @param to   종료일
     */
    constructor(from: LocalDateTime, to: LocalDateTime) {
        this.from = from
        this.to = to
    }


    /**
     * 날짜영역 객체 생성.
     *
     * [from] 시작일
     * [to]   종료일
     */
    constructor(from: LocalDate, to: LocalDate) {
        this.from = from.atTime(0, 0)
        this.to = to.atTime(0, 0)
    }

    /**
     * @return 종료날짜를 "yyyy-MM-dd" 형태로 리턴합니다.
     */
    val toDateFormat: String
        get() = format(to, "yyyy-MM-dd")

    /**
     * @return 시작날짜를 "yyyy-MM-dd" 형태로 리턴합니다.
     */
    val fromDateFormat: String
        get() = format(from, "yyyy-MM-dd")

    /**
     * @return 종료날짜를 "yyyy-MM-dd HH:mm:ss" 형태로 리턴합니다.
     */
    val toDateTimeFormat: String
        get() = format(to, "yyyy-MM-dd HH:mm:ss")

    /**
     * @return 시작날짜를 "yyyy-MM-dd HH:mm:ss" 형태로 리턴합니다.
     */
    val fromDateTimeFormat: String
        get() = format(from, "yyyy-MM-dd HH:mm:ss")

    val fromDate: LocalDate
        get() = from.toLocalDate()

    val toDate: LocalDate
        get() = to.toLocalDate()

    /**
     * @param format 날짜 패턴 "yyyy, MM, dd, HH, mm, ss and more"
     * @return 종료날짜를 포맷 형태로 리턴합니다.
     */
    fun getToDateTimeFormat(format: String): String {
        return format(to, format)
    }

    /**
     * @param format 날짜 패턴 "yyyy, MM, dd, HH, mm, ss and more"
     * @return 종료날짜를 포맷 형태로 리턴합니다.
     */
    fun getFromDateTimeFormat(format: String): String {
        return format(from, format)
    }

    /**
     * 시작과 끝을 포함한 between 검사
     *
     * @param dateTime 검사할 날짜시간
     * @return 두 날짜 사이에 있는지에 있으면 true
     */
    fun isBetween(dateTime: LocalDateTime): Boolean {
        val before = from.isBefore(dateTime)
        val after = to.isAfter(dateTime)
        val fromEqual = from.isEqual(dateTime)
        val toEqual = to.isEqual(dateTime)
        return before && after || fromEqual || toEqual
    }

    override fun toString(): String {
        return formatDateTime(from) + " ~ " + formatDateTime(to)
    }

    val diffMinute: Long
        get() {
            val dur = Duration.between(from, to)
            return dur.seconds / 60
        }

    /**
     * @return 날짜 차이
     */
    val diffDays: Long
        get() = ChronoUnit.DAYS.between(from, to)
}