package com.setvect.bokslstock2.analysis.vbs.model

import com.setvect.bokslstock2.analysis.common.model.AnalysisResult

/**
 * VBS 분석 조건과 결과
 */
data class VbsAnalysisConditionAndResult(
    val vbsAnalysisCondition: VbsAnalysisCondition,
    val analysisResult: AnalysisResult,
)
