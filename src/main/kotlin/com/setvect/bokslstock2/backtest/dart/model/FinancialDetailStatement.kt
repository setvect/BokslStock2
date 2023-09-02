package com.setvect.bokslstock2.backtest.dart.model

import com.fasterxml.jackson.core.type.TypeReference
import com.setvect.bokslstock2.crawl.dart.model.ReportCode
import com.setvect.bokslstock2.crawl.dart.model.ResFinancialDetailStatement
import com.setvect.bokslstock2.util.ApplicationUtil.convertToLong
import com.setvect.bokslstock2.util.JsonUtil
import org.slf4j.LoggerFactory
import java.io.File

data class FinancialDetailStatement(
    val commonStatement: CommonStatement,

    val rceptNo: String,   // 접수번호(14자리)
    val bsnsYear: String,  // 사업연도(4자리)
    val corpCode: String,  // 상장회사의 종목코드(6자리)
    val stockCode: String,  // 주식종목코드
    val fsDiv: FinancialFs,  // CFS:연결재무제표, OFS:재무제표
    val sjDiv: FinancialSj,  // BS:재무상태표, IS:손익계산서
    val sjNm: String,  // 재무상태표 또는 손익계산서 출력
    val accountId: String,  // 계정ID
    val accountNm: String,  // 유동자산, 비유동자산
    val accountDetail: String,  // 계정상세
    val thstrmNm: String,  // 당기명
    val thstrmAmount: Long?,  // 당기금액
    val thstrmAddAmount: Long?,  // 당기누적금액
    val frmtrmNm: String?,  // 전기명, ex) 제 12 기말
    val frmtrmAmount: Long?,  // 전기금액
    val frmtrmAddAmount: Long?,  // 전기누적금액
    val bfefrmtrmNm: String?,  // 전전기명
    val bfefrmtrmAmount: Long?,  // 전전기금액
    val ord: Int,  // 계정과목 정렬순서
    val currency: String  // 통화 단위
) {
    companion object {
        private val log = LoggerFactory.getLogger(FinancialDetailStatement::class.java)

        fun loader(jsonFile: File, year: Int, reportCode: ReportCode, stockCode: String): List<FinancialDetailStatement> {
            val typeRef = object : TypeReference<List<ResFinancialDetailStatement>>() {}
            val dartFinancialDetailList = JsonUtil.mapper.readValue(jsonFile, typeRef)
            return dartFinancialDetailList.map { resFinancialDetailStatement -> create(year, reportCode, stockCode, resFinancialDetailStatement) }
        }

        private fun create(
            year: Int,
            reportCode: ReportCode,
            stockCode: String,
            resFinancialDetailStatement: ResFinancialDetailStatement
        ): FinancialDetailStatement {
            try {
                return FinancialDetailStatement(
                    commonStatement = CommonStatement(stockCode, year, reportCode),
                    rceptNo = resFinancialDetailStatement.rceptNo,
                    bsnsYear = resFinancialDetailStatement.bsnsYear,
                    corpCode = resFinancialDetailStatement.corpCode,
                    stockCode = resFinancialDetailStatement.stockCode!!,
                    fsDiv = FinancialFs.valueOf(resFinancialDetailStatement.fsDiv!!),
                    sjDiv = FinancialSj.valueOf(resFinancialDetailStatement.sjDiv),
                    sjNm = resFinancialDetailStatement.sjNm,
                    accountId = resFinancialDetailStatement.accountId,
                    accountNm = resFinancialDetailStatement.accountNm,
                    accountDetail = resFinancialDetailStatement.accountDetail,
                    thstrmNm = resFinancialDetailStatement.thstrmNm,
                    thstrmAmount = convertToLong(resFinancialDetailStatement.thstrmAmount),
                    thstrmAddAmount = convertToLong(resFinancialDetailStatement.thstrmAddAmount),
                    frmtrmNm = resFinancialDetailStatement.frmtrmNm,
                    frmtrmAmount = convertToLong(resFinancialDetailStatement.frmtrmAmount),
                    frmtrmAddAmount = convertToLong(resFinancialDetailStatement.frmtrmAddAmount),
                    bfefrmtrmNm = resFinancialDetailStatement.bfefrmtrmNm,
                    bfefrmtrmAmount = convertToLong(resFinancialDetailStatement.bfefrmtrmAmount),
                    ord = resFinancialDetailStatement.ord.toInt(),
                    currency = resFinancialDetailStatement.currency
                )
            } catch (e: Exception) {
                log.info("error: ${e.message}\n------ json ------\n ${JsonUtil.mapper.writeValueAsString(resFinancialDetailStatement)}")
                throw e
            }
        }
    }
}