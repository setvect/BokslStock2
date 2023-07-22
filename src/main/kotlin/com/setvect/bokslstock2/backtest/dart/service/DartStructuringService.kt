package com.setvect.bokslstock2.backtest.dart.service

import com.setvect.bokslstock2.backtest.dart.model.DividendStatement
import com.setvect.bokslstock2.backtest.dart.model.FilterCondition
import com.setvect.bokslstock2.backtest.dart.model.FinancialStatement
import com.setvect.bokslstock2.backtest.dart.model.StockQuantityStatement
import com.setvect.bokslstock2.crawl.dart.DartConstants
import com.setvect.bokslstock2.crawl.dart.model.ReportCode
import com.setvect.bokslstock2.util.NumberUtil.comma
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern

/**
 * DART 통해서 수집한 자료 구조화
 *
 * DART에서 수집한 데이터를 메모리에 올려 놓고 데이터를 조회하기 때문에 Scope를 prototype으로 설정
 */
@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class DartStructuringService {

    val financialStatementList = mutableListOf<FinancialStatement>()
    val stockQuantityStatementList = mutableListOf<StockQuantityStatement>()
    val dividendStatementList = mutableListOf<DividendStatement>()

    val log = LoggerFactory.getLogger(javaClass)

    /**
     * DART에서 수집한 데이터를 메모리에 올려 놓음
     */
    fun loadFinancial(filter: FilterCondition) {
        val r = loader(DartConstants.FINANCIAL_PATH, filter, FinancialStatement::loader)
        financialStatementList.addAll(r)
    }

    fun loadStockQuantity(filter: FilterCondition) {
        val r = loader(DartConstants.QUANTITY_PATH, filter, StockQuantityStatement::loader)
        stockQuantityStatementList.addAll(r)
    }

    fun loadDividend(filter: FilterCondition) {
        val r = loader(DartConstants.DIVIDEND_PATH, filter, DividendStatement::loader)
        dividendStatementList.addAll(r)
    }

    private fun <T> loader(
        jsonSaveDir: File,
        filter: FilterCondition,
        loader: (jsonFile: File, year: Int, reportCode: ReportCode, stockCode: String) -> List<T>
    ): List<T> {
        val countAtom = AtomicInteger()
        val r =jsonSaveDir.walk()
            .filter { it.isFile && it.extension == "json" }
            .mapNotNull { financialFile ->
                val matcher = PATTERN.matcher(financialFile.name)
                if (!matcher.find()) {
                    return@mapNotNull null
                }
                val year = matcher.group(1).toInt()
                val reportCode = ReportCode.valueOf(matcher.group(2))
                val stockCode = matcher.group(3)

                val yearFilter = filter.year.isEmpty() || filter.year.contains(year)
                val quarterFilter = filter.quarter.isEmpty() || filter.quarter.contains(reportCode)
                val stockCodesFilter = filter.stockCodes.isEmpty() || filter.stockCodes.contains(stockCode)
                val find = !(yearFilter && quarterFilter && stockCodesFilter)
                if (find) {
                    return@mapNotNull null
                }

                val result = loader(financialFile, year, reportCode, stockCode)
                val count = countAtom.incrementAndGet()
                if (count % 200 == 0) {
                    log.info("load: ${comma(count)}, ${financialFile.name}")
                }
                result
            }
            .flatten()
            .toList()

        log.info("loadFinancial size: ${comma(r.size)}")
        return r
    }

    companion object {
        val PATTERN: Pattern = Pattern.compile("(\\d{4})_(QUARTER\\d|HALF_ANNUAL|ANNUAL)_(\\d{6})_.+\\.json")
    }

}