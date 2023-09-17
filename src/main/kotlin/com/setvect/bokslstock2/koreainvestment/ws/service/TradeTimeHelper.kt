package com.setvect.bokslstock2.koreainvestment.ws.service

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoField


/**
 * 매매 시간과 관련된 상수
 */
object TradeTimeHelper {
    /** 매매 시작*/
    private val START_TIME = LocalTime.of(8, 41, 0).get(ChronoField.MILLI_OF_DAY)

    /** 동시호가 매도 판단 시작 시간*/
    private val SELL_OPEN_TIME = LocalTime.of(8, 59, 59).get(ChronoField.MILLI_OF_DAY)

    /** 장 시작*/
    private val OPEN_TIME = LocalTime.of(9, 0, 0).get(ChronoField.MILLI_OF_DAY)

    /** 매수 시작 시간*/
    private val BUY_TIME = LocalTime.of(9, 0, 20).get(ChronoField.MILLI_OF_DAY)

    /** 장 종료(동시호가 제외)*/
    private val CLOSE_TIME = LocalTime.of(15, 20, 0).get(ChronoField.MILLI_OF_DAY)

    /**
     * @return 동시호가 매도 시간 범위 이면 true
     */
    fun isOpenPriceSellTime(): Boolean {
        val now = LocalTime.now().get(ChronoField.MILLI_OF_DAY)
        return now in (SELL_OPEN_TIME + 1) until OPEN_TIME
    }

    /**
     * @return 오전 동시호가
     */
    fun isMorningSimultaneity(): Boolean {
        val now = LocalTime.now().get(ChronoField.MILLI_OF_DAY)
        return now in (START_TIME + 1) until OPEN_TIME
    }

    /**
     * @return 매수 가능 시간
     */
    fun isBuyTimeRange(): Boolean {
        val now = LocalTime.now().get(ChronoField.MILLI_OF_DAY)
        return now in (BUY_TIME + 1) until CLOSE_TIME
    }

    /**
     * @return 갭 상승 종목 매도 가능 시간
     */
    fun isStayGapRiseTimeRange(): Boolean {
        val now = LocalTime.now().get(ChronoField.MILLI_OF_DAY)
        return now in (BUY_TIME) until CLOSE_TIME
    }

    /**
     * @return 매매 가능 시간이면 true
     */
    fun isTimeToTrade(): Boolean {
        val now = LocalTime.now().get(ChronoField.MILLI_OF_DAY)
        if (isHoliday()) {
            return false
        }
        if (now < START_TIME || CLOSE_TIME < now) {
            return false
        }
        return true
    }

    /**
     * 장 오픈 이후 시간
     */
    fun isAfterOpen(): Boolean {
        val now = LocalTime.now().get(ChronoField.MILLI_OF_DAY)
        return now > OPEN_TIME
    }

    private fun isHoliday(): Boolean {
        val dayOfWeek = LocalDate.now().dayOfWeek
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
    }
}
