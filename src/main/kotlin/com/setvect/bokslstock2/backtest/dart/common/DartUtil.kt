package com.setvect.bokslstock2.backtest.dart.common

import com.setvect.bokslstock2.backtest.dart.model.TotalCapitalCondition
import com.setvect.bokslstock2.util.DateUtil
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * 재무 관련 리포트를 만듦
 */
class DartUtil {
    companion object {
        fun parsingDate(dateStr: String?): Pair<LocalDate?, LocalDate?> {
            if (dateStr.isNullOrBlank()) {
                return Pair(null, null)
            }

            var start: LocalDate? = null
            var end: LocalDate? = null
            if (determineType(dateStr) == 1) {
                start = convertToDateType1(dateStr)
            }
            if (determineType(dateStr) == 2) {
                val (s, e) = convertToDateType2(dateStr)
                start = s
                end = e
            }
            return Pair(start, end)
        }

        fun determineType(dateStr: String): Int {
            return if (dateStr.contains("~")) 2 else 1
        }

        private fun convertToDateType1(dateStr: String): LocalDate {
            val cleanedStr = dateStr.replace(" 현재", "")
            return DateUtil.getLocalDate(cleanedStr, "yyyy.MM.dd")
        }

        private fun convertToDateType2(dateStr: String): Pair<LocalDate, LocalDate> {
            val splitStr = dateStr.split(" ~ ")
            val start = DateUtil.getLocalDate(splitStr[0], "yyyy.MM.dd")
            val end = DateUtil.getLocalDate(splitStr[1], "yyyy.MM.dd")
            return Pair(start, end)
        }
    }
}