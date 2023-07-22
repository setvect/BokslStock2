package com.setvect.bokslstock2.backtest.dart.model

import com.fasterxml.jackson.core.type.TypeReference
import com.setvect.bokslstock2.crawl.dart.model.ReportCode
import com.setvect.bokslstock2.crawl.dart.model.ResDividend
import com.setvect.bokslstock2.util.JsonUtil
import org.slf4j.LoggerFactory
import java.io.File

data class DividendStatement(
    val commonStatement: CommonStatement, // 사업연도(4자리)

    val rceptNo: String, // 접수번호(14자리)
    val corpCls: CorpCls, // 법인구분 : Y(유가), K(코스닥), N(코넥스), E(기타)
    val corpCode: String, // 공시대상회사의 고유번호(8자리)
    val corpName: String, // 법인명
    val se: String, // 유상증자(주주배정), 전환권행사 등
    val stockKnd: DividendStatementStockKnd?, // 주식 종류 : 보통주, 우선주
    val thstrm: String, // 당기, 9,999,999,999
    val frmtrm: String, // 전기, 9,999,999,999
    val lwfr: String // 전전기, 9,999,999,999
) {
    companion object {
        private val log = LoggerFactory.getLogger(DividendStatement::class.java)

        fun loader(jsonFile: File, year: Int, reportCode: ReportCode, stockCode: String): List<DividendStatement> {
            val typeRef = object : TypeReference<List<ResDividend>>() {}
            val dartFinancialList = JsonUtil.mapper.readValue(jsonFile, typeRef)
            return dartFinancialList.map { resFinancialStatement -> create(year, reportCode, stockCode, resFinancialStatement) }
        }

        private fun create(year: Int, reportCode: ReportCode, stockCode: String, resDividend: ResDividend): DividendStatement {
            try {
                return DividendStatement(
                    commonStatement = CommonStatement(year, reportCode, stockCode),
                    rceptNo = resDividend.rceptNo,
                    corpCls = CorpCls.valueOf(resDividend.corpCls),
                    corpCode = resDividend.corpCode,
                    corpName = resDividend.corpName,
                    se = resDividend.se,
                    stockKnd = DividendStatementStockKnd.valueOfCode(resDividend.stockKnd),
                    thstrm = resDividend.thstrm,
                    frmtrm = resDividend.frmtrm,
                    lwfr = resDividend.lwfr,
                )
            } catch (e: Exception) {
                log.info("error: ${e.message}\n------ json ------\n ${JsonUtil.mapper.writeValueAsString(resDividend)}");
                throw e
            }
        }
    }

    enum class DividendStatementStockKnd(val code: String) {
        ORD("보통주"),
        PRF("우선주");

        companion object {
            fun valueOfCode(code: String?): DividendStatementStockKnd? {
                return values().find { it.code == code }
            }
        }
    }

}