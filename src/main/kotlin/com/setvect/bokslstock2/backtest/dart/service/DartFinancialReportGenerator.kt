package com.setvect.bokslstock2.backtest.dart.service

import com.setvect.bokslstock2.backtest.dart.model.TotalCapitalCondition
import org.springframework.stereotype.Service

/**
 * 재무 관련 리포트를 만듦
 */
@Service
class DartFinancialReportGenerator {
    private var dartStructuringService: DartStructuringService? = null

    fun init(dartStructuringService: DartStructuringService) {
        this.dartStructuringService = dartStructuringService
    }

    fun makeTotalCapital(condition: TotalCapitalCondition) {
        val structuringService = getDartStructuringService()

    }

    private fun getDartStructuringService(): DartStructuringService {
        if (dartStructuringService == null) {
            throw IllegalStateException("dartStructuringService is null")
        }
        return dartStructuringService!!
    }
}