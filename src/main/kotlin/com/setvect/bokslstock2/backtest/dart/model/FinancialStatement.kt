package com.setvect.bokslstock2.backtest.dart.model

import com.fasterxml.jackson.core.type.TypeReference
import com.setvect.bokslstock2.crawl.dart.model.ReportCode
import com.setvect.bokslstock2.crawl.dart.model.ResFinancialStatement
import com.setvect.bokslstock2.util.ApplicationUtil.convertToLong
import com.setvect.bokslstock2.util.DateUtil
import com.setvect.bokslstock2.util.JsonUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDate

data class FinancialStatement(
    val commonStatement: CommonStatement, // 사업연도(4자리)

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
        private val log = LoggerFactory.getLogger(FinancialStatement::class.java)

        fun loader(jsonFile: File, year: Int, reportCode: ReportCode, stockCode: String): List<FinancialStatement> {
            val typeRef = object : TypeReference<List<ResFinancialStatement>>() {}
            val dartFinancialList = JsonUtil.mapper.readValue(jsonFile, typeRef)
            return dartFinancialList.map { resFinancialStatement -> create(year, reportCode, stockCode, resFinancialStatement) }
        }

        private fun create(year: Int, reportCode: ReportCode, stockCode: String, resFinancialStatement: ResFinancialStatement): FinancialStatement {
            val thstrmDt = parsingDate(resFinancialStatement.thstrmDt)
            val frmtrmDt = parsingDate(resFinancialStatement.frmtrmDt)
            val bfefrmtrmDt = parsingDate(resFinancialStatement.bfefrmtrmDt)

            try {
                return FinancialStatement(
                    commonStatement = CommonStatement(year, reportCode, stockCode),
                    rceptNo = resFinancialStatement.rceptNo,
                    bsnsYear = resFinancialStatement.bsnsYear,
                    corpCode = resFinancialStatement.corpCode,
                    fsDiv = FinancialStatementFs.valueOf(resFinancialStatement.fsDiv),
                    fsNm = resFinancialStatement.fsNm,
                    sjDiv = FinancialStatementSj.valueOf(resFinancialStatement.sjDiv),
                    sjNm = resFinancialStatement.sjNm,
                    accountNm = resFinancialStatement.accountNm,
                    thstrmNm = resFinancialStatement.thstrmNm,
                    thstrmDtStart = thstrmDt.first!!,
                    thstrmDtEnd = thstrmDt.second,
                    thstrmAmount = convertToLong(resFinancialStatement.thstrmAmount),
                    thstrmAddAmount = convertToLong(resFinancialStatement.thstrmAddAmount),
                    frmtrmNm = resFinancialStatement.frmtrmNm,
                    frmtrmDtStart = frmtrmDt.first,
                    frmtrmDtEnd = frmtrmDt.second,
                    frmtrmAmount = convertToLong(resFinancialStatement.frmtrmAmount),
                    frmtrmAddAmount = convertToLong(resFinancialStatement.frmtrmAddAmount),
                    bfefrmtrmNm = resFinancialStatement.bfefrmtrmNm,
                    bfefrmtrmDtStart = bfefrmtrmDt.first,
                    bfefrmtrmDtEnd = bfefrmtrmDt.second,
                    bfefrmtrmAmount = convertToLong(resFinancialStatement.bfefrmtrmAmount),
                    ord = resFinancialStatement.ord.toInt(),
                    currency = "",
                )
            } catch (e: Exception) {
                log.info("error: ${e.message}\n------ json ------\n ${JsonUtil.mapper.writeValueAsString(resFinancialStatement)}");
                throw e
            }
        }

        private fun parsingDate(dateStr: String?): Pair<LocalDate?, LocalDate?> {
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

        fun convertToDateType1(dateStr: String): LocalDate {
            val cleanedStr = dateStr.replace(" 현재", "")
            return DateUtil.getLocalDate(cleanedStr, "yyyy.MM.dd")
        }

        fun convertToDateType2(dateStr: String): Pair<LocalDate, LocalDate> {
            val splitStr = dateStr.split(" ~ ")
            val start = DateUtil.getLocalDate(splitStr[0], "yyyy.MM.dd")
            val end = DateUtil.getLocalDate(splitStr[1], "yyyy.MM.dd")
            return Pair(start, end)
        }
    }

    enum class FinancialStatementFs(val code: String) {
        CFS("CFS"), // 연결재무제표
        OFS("OFS")  // 재무제표
    }

    enum class FinancialStatementSj(val code: String) {
        BS("BS"), // 재무상태표
        IS("IS")  // 손익계산서
    }
}