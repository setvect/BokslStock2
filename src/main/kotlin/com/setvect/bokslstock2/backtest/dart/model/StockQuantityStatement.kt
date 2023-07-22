package com.setvect.bokslstock2.backtest.dart.model

import com.fasterxml.jackson.core.type.TypeReference
import com.setvect.bokslstock2.crawl.dart.model.ReportCode
import com.setvect.bokslstock2.crawl.dart.model.ResStockQuantity
import com.setvect.bokslstock2.util.ApplicationUtil.convertToLong
import com.setvect.bokslstock2.util.JsonUtil
import org.slf4j.LoggerFactory
import java.io.File

data class StockQuantityStatement(
    val commonStatement: CommonStatement, // 사업연도(4자리)

    val rceptNo: String, // 접수번호(14자리)
    val corpCls: CorpCls, // 법인구분 : Y(유가), K(코스닥), N(코넥스), E(기타)
    val corpCode: String, // 공시대상회사의 고유번호(8자리)
    val corpName: String, // 공시대상회사명
    val se: String, // 구분(증권의종류, 합계, 비고)
    val isuStockTotqy: Long?, // Ⅰ. 발행할 주식의 총수, 9,999,999,999
    val nowToIsuStockTotqy: Long?, // Ⅱ. 현재까지 발행한 주식의 총수, 9,999,999,999
    val nowToDcrsStockTotqy: Long?, // Ⅲ. 현재까지 감소한 주식의 총수, 9,999,999,999
    val redc: String?, // Ⅲ. 현재까지 감소한 주식의 총수(1. 감자), 9,999,999,999
    val profitIncnr: String?, // Ⅲ. 현재까지 감소한 주식의 총수(2. 이익소각), 9,999,999,999
    val rdmstkRepy: String?, // Ⅲ. 현재까지 감소한 주식의 총수(3. 상환주식의 상환), 9,999,999,999
    val etc: String?, // Ⅲ. 현재까지 감소한 주식의 총수(4. 기타), 9,999,999,999
    val istcTotqy: Long?, // Ⅳ. 발행주식의 총수 (Ⅱ-Ⅲ), 9,999,999,999
    val tesstkCo: String?, // Ⅴ. 자기주식수, 9,999,999,999
    val distbStockCo: Long? // Ⅵ. 유통주식수 (Ⅳ-Ⅴ), 9,999,999,999
) {
    companion object {
        private val log = LoggerFactory.getLogger(StockQuantityStatement::class.java)

        fun loader(jsonFile: File, year: Int, reportCode: ReportCode, stockCode: String): List<StockQuantityStatement> {
            val typeRef = object : TypeReference<List<ResStockQuantity>>() {}
            val dartFinancialList = JsonUtil.mapper.readValue(jsonFile, typeRef)
            return dartFinancialList.map { resFinancialStatement -> create(year, reportCode, stockCode, resFinancialStatement) }
        }

        private fun create(year: Int, reportCode: ReportCode, stockCode: String, resStockQuantity: ResStockQuantity): StockQuantityStatement {
            try {
                return StockQuantityStatement(
                    commonStatement = CommonStatement(year, reportCode, stockCode),
                    rceptNo = resStockQuantity.rceptNo,
                    corpCls = CorpCls.valueOf(resStockQuantity.corpCls),
                    corpCode = resStockQuantity.corpCode,
                    corpName = resStockQuantity.corpName,
                    se = resStockQuantity.se,
                    isuStockTotqy = convertToLong(resStockQuantity.isuStockTotqy),
                    nowToIsuStockTotqy = convertToLong(resStockQuantity.nowToIsuStockTotqy),
                    nowToDcrsStockTotqy = convertToLong(resStockQuantity.nowToDcrsStockTotqy),
                    redc = resStockQuantity.redc,
                    profitIncnr = resStockQuantity.profitIncnr,
                    rdmstkRepy = resStockQuantity.rdmstkRepy,
                    etc = resStockQuantity.etc,
                    istcTotqy = convertToLong(resStockQuantity.istcTotqy),
                    tesstkCo = resStockQuantity.tesstkCo,
                    distbStockCo = convertToLong(resStockQuantity.distbStockCo)
                )
            } catch (e: Exception) {
                log.info("error: ${e.message}\n------ json ------\n ${JsonUtil.mapper.writeValueAsString(resStockQuantity)}");
                throw e
            }
        }
    }
}