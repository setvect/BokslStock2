package com.setvect.bokslstock2.index.model

enum class PeriodType {
    /**
     * 일봉
     */
    PERIOD_DAY,

    /**
     * 주봉
     */
    PERIOD_WEEK,

    /**
     * 월봉
     */
    PERIOD_MONTH,

    /**
     * 분기봉
     */
    PERIOD_QUARTER,

    /**
     * 반기봉
     */
    PERIOD_HALF,

    /**
     * 년봉
     */
    PERIOD_YEAR;

    fun getDeviceMonth(): Int {
        return when (this) {
            PERIOD_MONTH -> 1
            PERIOD_QUARTER -> 3
            PERIOD_HALF -> 6
            PERIOD_YEAR -> 12
            else -> -1
        }
    }
}
