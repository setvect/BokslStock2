package com.setvect.bokslstock2.util

import java.io.PrintStream
import java.text.DecimalFormat

/**
 * 시간을 체크
 * 프로그램 코드 진행 시간을 체크 할때 사용
 */
class LapTimeChecker(
    /**
     * 출력 스트림
     */
    private val out: PrintStream,
    /**
     * 이름
     */
    private val name: String
) {
    /**
     * 체크 시작 시간
     */
    private val startTime: Long

    /**
     * 체크 카운트
     */
    private var checkCount = 0

    /**
     * 현재 진행 시간 대비 전체 카운트를 계산하여 총 예상 시간을 구함
     */
    var totalCount = 0

    /**
     * 처리된 건수
     */
    /**
     * 처리된 건수
     */
    var processCount = 0

    /**
     * 기본 생성자<br></br>
     * 메세지 출력은 System.out 에서 함
     *
     * [name] 구분 이름
     */
    constructor(name: String) : this(System.out, name)

    init {
        startTime = System.currentTimeMillis()
        out.println("$name start [$startTime]")
    }
    /**
     * 시간 체크
     *
     * [message] 출력 메시지
     */
    /**
     * 시간 체크
     */
    @JvmOverloads
    fun check(message: String = "") {
        out.println(getCheckMessage(message))
    }

    /**
     * [message] 출력 메시지
     * @return 시간 체크 출력 메세지를 문자열
     */
    fun getCheckMessage(message: String): String {
        checkCount++
        val cur = System.currentTimeMillis()
        return (name + ", " + NUMBER_FORMAT.format(checkCount.toLong()) + ", " + NUMBER_COMMA_FORMAT.format(cur - startTime)
                + "ms, " + message)
    }

    /**
     * [processCount] 처리된 건수
     * @return 예상 남은 시간을 ms 단위로 리턴
     */
    fun getRemainTime(processCount: Int): Long {
        val running = System.currentTimeMillis() - startTime
        return if (processCount == 0) {
            0
        } else running * totalCount / processCount - running
    }

    /**
     * @return 예상 남은 시간을 정해진 포맷에 의해 문자열로 제공
     */
    val remainTimeFormat: String
        get() = getRemainTimeFormat(processCount)

    /**
     * [processCount] 처리된 건수
     * @return 예상 남은 시간을 정해진 포맷에 의해 문자열로 제공
     */
    fun getRemainTimeFormat(processCount: Int): String {
        val time = getRemainTime(processCount)
        return getTimeToDayFormat(time)
    }

    /**
     * @return 수행 시간을 날짜/시간 포맷으로
     */
    val runningTimeFormat: String
        get() {
            val runningTime = System.currentTimeMillis() - startTime
            return getTimeToDayFormat(runningTime)
        }

    /**
     * [t] 진행 시간
     * @return 진행 시간를 포맷팅해 표현
     */
    private fun getTimeToDayFormat(t: Long): String {
        var time = t
        val day = time / DAY_FOR_MS
        time %= DAY_FOR_MS
        val hour = time / HOUR_FOR_MS
        time %= HOUR_FOR_MS
        val min = time / MIN_FOR_MS
        time %= MIN_FOR_MS
        val sec = time / SEC_FOR_MS
        val rtnValue = StringBuffer()
        if (day != 0L) {
            rtnValue.append(NUMBER_TIME_FORMAT.format(day) + "day(s) ")
        }
        rtnValue.append(NUMBER_TIME_FORMAT.format(hour) + ":")
        rtnValue.append(NUMBER_TIME_FORMAT.format(min) + ":")
        rtnValue.append(NUMBER_TIME_FORMAT.format(sec))
        return rtnValue.toString()
    }

    companion object {
        private const val SEC_FOR_MS = 1000
        private const val MIN_FOR_MS = SEC_FOR_MS * 60
        private const val HOUR_FOR_MS = MIN_FOR_MS * 60
        private const val DAY_FOR_MS = HOUR_FOR_MS * 24
        private val NUMBER_FORMAT = DecimalFormat("0000")
        private val NUMBER_COMMA_FORMAT = DecimalFormat(",###")
        private val NUMBER_TIME_FORMAT = DecimalFormat("00")
    }
}