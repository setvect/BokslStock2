package com.setvect.bokslstock2.backtest.dart.model

import com.fasterxml.jackson.core.type.TypeReference
import com.setvect.bokslstock2.backtest.dart.common.DartUtil
import com.setvect.bokslstock2.crawl.dart.model.ReportCode
import com.setvect.bokslstock2.crawl.dart.model.ResFinancialDetailStatement
import com.setvect.bokslstock2.util.ApplicationUtil.convertToLong
import com.setvect.bokslstock2.util.DateUtil
import com.setvect.bokslstock2.util.JsonUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDate

data class FinancialDetailStatement(
    val commonStatement: CommonStatement,

    val rceptNo: String,   // 접수번호(14자리)
    val bsnsYear: String,  // 사업연도(4자리)
    val corpCode: String,  // 상장회사의 종목코드(6자리)
    val fsDiv: FinancialStatementFs,  // CFS:연결재무제표, OFS:재무제표
    val fsNm: String,  // 연결재무제표 또는 재무제표 출력
    val sjDiv: FinancialStatementSj,  // BS:재무상태표, IS:손익계산서
    val sjNm: String,  // 재무상태표 또는 손익계산서 출력
    val accountNm: String,  // 유동자산, 비유동자산
    val thstrmNm: String,  // 당기명
    val thstrmDtStart: LocalDate,  // 당기일자, ex) 2018.09.30 현재
    val thstrmDtEnd: LocalDate?,  // 당기일자, ex) 2018.09.30 현재
    val thstrmAmount: Long?,  // 당기금액
    val thstrmAddAmount: Long?,  // 당기누적금액
    val frmtrmNm: String?,  // 전기명, ex) 제 12 기말
    val frmtrmDtStart: LocalDate?,  // 전기일자
    val frmtrmDtEnd: LocalDate?,  // 전기일자
    val frmtrmAmount: Long?,  // 전기금액
    val frmtrmAddAmount: Long?,  // 전기누적금액
    val bfefrmtrmNm: String?,  // 전전기명
    val bfefrmtrmDtStart: LocalDate?,  // 전전기일자
    val bfefrmtrmDtEnd: LocalDate?,  // 전전기일자
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
            val thstrmDt =  DartUtil.parsingDate(resFinancialDetailStatement.thstrmDt)
            val frmtrmDt = DartUtil.parsingDate(resFinancialDetailStatement.frmtrmDt)
            val bfefrmtrmDt =  DartUtil.parsingDate(resFinancialDetailStatement.bfefrmtrmDt)

            try {
                return FinancialDetailStatement(
                    commonStatement = CommonStatement(year, reportCode, stockCode),
                    rceptNo = resFinancialDetailStatement.rceptNo,
                    bsnsYear = resFinancialDetailStatement.bsnsYear,
                    corpCode = resFinancialDetailStatement.corpCode,
                    fsDiv = FinancialStatementFs.valueOf(resFinancialDetailStatement.fsDiv),
                    fsNm = resFinancialDetailStatement.fsNm,
                    sjDiv = FinancialStatementSj.valueOf(resFinancialDetailStatement.sjDiv),
                    sjNm = resFinancialDetailStatement.sjNm,
                    accountNm = resFinancialDetailStatement.accountNm,
                    thstrmNm = resFinancialDetailStatement.thstrmNm,
                    thstrmDtStart = thstrmDt.first!!,
                    thstrmDtEnd = thstrmDt.second,
                    thstrmAmount = convertToLong(resFinancialDetailStatement.thstrmAmount),
                    thstrmAddAmount = convertToLong(resFinancialDetailStatement.thstrmAddAmount),
                    frmtrmNm = resFinancialDetailStatement.frmtrmNm,
                    frmtrmDtStart = frmtrmDt.first,
                    frmtrmDtEnd = frmtrmDt.second,
                    frmtrmAmount = convertToLong(resFinancialDetailStatement.frmtrmAmount),
                    frmtrmAddAmount = convertToLong(resFinancialDetailStatement.frmtrmAddAmount),
                    bfefrmtrmNm = resFinancialDetailStatement.bfefrmtrmNm,
                    bfefrmtrmDtStart = bfefrmtrmDt.first,
                    bfefrmtrmDtEnd = bfefrmtrmDt.second,
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