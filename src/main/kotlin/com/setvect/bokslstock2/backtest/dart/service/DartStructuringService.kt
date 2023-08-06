package com.setvect.bokslstock2.backtest.dart.service

import com.setvect.bokslstock2.backtest.dart.model.*
import com.setvect.bokslstock2.crawl.dart.DartConstants
import com.setvect.bokslstock2.crawl.dart.model.ReportCode
import com.setvect.bokslstock2.util.NumberUtil.comma
import okhttp3.internal.toImmutableList
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Service
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * DART 통해서 수집한 자료 구조화
 *
 * DART에서 수집한 데이터를 메모리에 올려 놓고 데이터를 조회하기 때문에 Scope를 prototype으로 설정
 */
@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class DartStructuringService {
    companion object {
        val PATTERN: Pattern = Pattern.compile("(\\d{4})_(QUARTER\\d|HALF_ANNUAL|ANNUAL)_(\\d{6})_.+\\.json")
    }

    private val financialStatementList = mutableListOf<FinancialStatement>()
    private val stockQuantityStatementList = mutableListOf<StockQuantityStatement>()

    private val dividendStatementList = mutableListOf<DividendStatement>()

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * DART에서 수집한 데이터를 메모리에 올려 놓음
     */
    fun loadFinancial(filter: DartFilter) {
        financialStatementList.clear()
        val result = loader(DartConstants.FINANCIAL_PATH, filter, FinancialStatement::loader)
        financialStatementList.addAll(result)
    }

    fun loadStockQuantity(filter: DartFilter) {
        stockQuantityStatementList.clear()
        val result = loader(DartConstants.QUANTITY_PATH, filter, StockQuantityStatement::loader)
        stockQuantityStatementList.addAll(result)
    }

    fun loadDividend(filter: DartFilter) {
        dividendStatementList.clear()
        val result = loader(DartConstants.DIVIDEND_PATH, filter, DividendStatement::loader)
        dividendStatementList.addAll(result)
    }

    private fun <T> loader(
        jsonSaveDir: File,
        filter: DartFilter,
        loader: (jsonFile: File, year: Int, reportCode: ReportCode, stockCode: String) -> List<T>
    ): List<T> {
        val countAtom = AtomicInteger()
        val r = jsonSaveDir.walk()
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

    fun searchFinancial(filter: Map<String, Any>): List<FinancialStatement> {
        return financialStatementList.filter { filter(it, filter) }.toImmutableList()

    }


    private fun filter(target: Any, filter: Map<String, Any>): Boolean {
        return filter.entries.all { it ->
            val fieldNames = it.key.split(".")
            var currentObj: Any? = target

            fieldNames.let { names ->
                var result = true
                for (name in names) {
                    val prop: KProperty1<Any, *> = currentObj?.javaClass?.kotlin?.memberProperties?.find { it.name == name }
                        ?: throw IllegalArgumentException("No such field: $name in the class ${currentObj?.javaClass?.kotlin}")

                    currentObj = prop.get(currentObj!!)

                    if (name == names.last()) {
                        result = currentObj == it.value
                    }
                }
                result
            }
        }
    }

    /**
     * 데이터를 로드한 상태에서 본 메소드 사용
     * - loadFinancial() 호출 후 사용
     * @param stockCode 종목코드
     * @return 회사의 회계 마감 기준 제공
     */
    fun getAccountClose(stockCode: String): AccountClose {
        if (financialStatementList.isEmpty()) {
            throw IllegalStateException("loadFinancial() 호출 후 사용")
        }

        val condition: Map<String, Any> = mapOf(
            "commonStatement.reportCode" to ReportCode.ANNUAL,
            "commonStatement.stockCode" to stockCode,
            "accountNm" to "영업이익", // 고정값
            "fsDiv" to FinancialStatement.FinancialStatementFs.CFS, // TODO 연결재무가 없는 회사가 있음, OFS 사용
        )

        val financialList = searchFinancial(condition)
        if (financialList.isEmpty()) {
            throw IllegalArgumentException("매출액정보가 없는 회사 $stockCode")
        }
        val financialStatement = financialList[0]

        return when (financialStatement.thstrmDtEnd!!.month.value) {
            3 -> AccountClose.Q1
            6 -> AccountClose.Q2
            9 -> AccountClose.Q3
            12 -> AccountClose.Q4
            else -> throw IllegalArgumentException("매출액정보가 없는 회사 $stockCode")
        }
    }

    /**
     * 손익계산서 항목을 분기별로 변환
     * - 기업 회계 마감 분기를 우리가 흔히 사용하고 있는 분기 기준으로 변환
     *
     * 데이터를 로드한 상태에서 본 메소드 사용
     * @param stockCode 종목코드
     * @param year 연도
     * @param accountNm 재무제표 항목명
     */
    fun getIncomeStatement(stockCode: String, year: Int, accountNm: String): IncomeStatement {
        val accountClose = getAccountClose(stockCode)

        val condition: Map<String, Any> = mapOf(
            "commonStatement.stockCode" to stockCode,
            "accountNm" to accountNm,
            "fsDiv" to FinancialStatement.FinancialStatementFs.CFS,
        )

        val financialList = searchFinancial(condition)
        var currentIncomeStatement = IncomeStatement(
            stockCode = stockCode,
            year = year,
            itemName = accountNm,
            q1Value = 0,
            q2Value = 0,
            q3Value = 0,
            q4Value = 0,
        )

        when (accountClose) {
            // ReportCode.ANNUAL: 작년4 ~ 3월
            // ReportCode.QUARTER1: 4 ~ 6월
            // ReportCode.HALF_ANNUAL: 4 ~ 9월
            // ReportCode.QUARTER3: 10 ~ 12월
            AccountClose.Q1 -> {
                val beforeQ4FS = findFinancialStatement(financialList, ReportCode.QUARTER3, year - 1) ?: return currentIncomeStatement

                val q1FS = findFinancialStatement(financialList, ReportCode.ANNUAL, year) ?: return currentIncomeStatement
                currentIncomeStatement = currentIncomeStatement.copy(q1Value = q1FS.thstrmAmount!! - beforeQ4FS.thstrmAddAmount!!)

                val q2FS = findFinancialStatement(financialList, ReportCode.QUARTER1, year) ?: return currentIncomeStatement
                currentIncomeStatement = currentIncomeStatement.copy(q2Value = q2FS.thstrmAmount ?: 0)

                val q3FS = findFinancialStatement(financialList, ReportCode.HALF_ANNUAL, year) ?: return currentIncomeStatement
                currentIncomeStatement = currentIncomeStatement.copy(q3Value = q3FS.thstrmAmount!!)

                val q4FS = findFinancialStatement(financialList, ReportCode.QUARTER3, year) ?: return currentIncomeStatement
                currentIncomeStatement = currentIncomeStatement.copy(q4Value = q4FS.thstrmAmount!!)

                return currentIncomeStatement
            }

            // ReportCode.QUARTER3: 1 ~ 3월
            // ReportCode.ANNUAL: 작년7 ~ 6월
            // ReportCode.QUARTER1: 7 ~ 9월
            // ReportCode.HALF_ANNUAL: 7 ~ 12월
            AccountClose.Q2 -> {
                val q1FS = findFinancialStatement(financialList, ReportCode.QUARTER3, year) ?: return currentIncomeStatement
                currentIncomeStatement = currentIncomeStatement.copy(q1Value = q1FS.thstrmAmount!!)

                val beforeH = findFinancialStatement(financialList, ReportCode.HALF_ANNUAL, year - 1) ?: return currentIncomeStatement

                val q2FS = findFinancialStatement(financialList, ReportCode.ANNUAL, year) ?: return currentIncomeStatement
                currentIncomeStatement =
                    currentIncomeStatement.copy(q2Value = q2FS.thstrmAmount!! - beforeH.thstrmAddAmount!! - currentIncomeStatement.q1Value)

                val q3FS = findFinancialStatement(financialList, ReportCode.QUARTER1, year) ?: return currentIncomeStatement
                currentIncomeStatement = currentIncomeStatement.copy(q3Value = q3FS.thstrmAmount!!)

                val q4FS = findFinancialStatement(financialList, ReportCode.HALF_ANNUAL, year) ?: return currentIncomeStatement
                currentIncomeStatement = currentIncomeStatement.copy(q4Value = q4FS.thstrmAmount!!)

                return currentIncomeStatement
            }

            // ReportCode.HALF_ANNUAL: 작년10 ~ 3월
            // ReportCode.QUARTER3: 4 ~ 6월
            // ReportCode.ANNUAL: 작년10 ~ 9월
            // ReportCode.QUARTER1: 10 ~ 12월
            AccountClose.Q3 -> {
                val q1FS = findFinancialStatement(financialList, ReportCode.HALF_ANNUAL, year) ?: return currentIncomeStatement
                currentIncomeStatement = currentIncomeStatement.copy(q1Value = q1FS.thstrmAmount!!)

                val q2FS = findFinancialStatement(financialList, ReportCode.QUARTER3, year) ?: return currentIncomeStatement
                currentIncomeStatement = currentIncomeStatement.copy(q2Value = q2FS.thstrmAmount ?: 0)

                val q3FS = findFinancialStatement(financialList, ReportCode.ANNUAL, year) ?: return currentIncomeStatement
                currentIncomeStatement = currentIncomeStatement.copy(q3Value = q3FS.thstrmAmount!! - q1FS.thstrmAddAmount!! - q2FS.thstrmAmount!!)

                val q4FS = findFinancialStatement(financialList, ReportCode.QUARTER1, year) ?: return currentIncomeStatement
                currentIncomeStatement = currentIncomeStatement.copy(q4Value = q4FS.thstrmAmount!!)
                return currentIncomeStatement
            }

            // ReportCode.QUARTER1: 1 ~ 3월
            // ReportCode.HALF_ANNUAL: 1 ~ 6월
            // ReportCode.QUARTER3: 7 ~ 9월
            // ReportCode.ANNUAL: 1 ~ 12월
            AccountClose.Q4 -> {
                val q1FS = findFinancialStatement(financialList, ReportCode.QUARTER1, year) ?: return currentIncomeStatement
                currentIncomeStatement = currentIncomeStatement.copy(q1Value = q1FS.thstrmAmount ?: 0)

                val q2FS = findFinancialStatement(financialList, ReportCode.HALF_ANNUAL, year) ?: return currentIncomeStatement
                currentIncomeStatement = currentIncomeStatement.copy(q2Value = q2FS.thstrmAmount!!)

                val q3FS = findFinancialStatement(financialList, ReportCode.QUARTER3, year) ?: return currentIncomeStatement
                currentIncomeStatement = currentIncomeStatement.copy(q3Value = q3FS.thstrmAmount!!)

                val q4FS = findFinancialStatement(financialList, ReportCode.ANNUAL, year) ?: return currentIncomeStatement
                currentIncomeStatement =
                    currentIncomeStatement.copy(q4Value = q4FS.thstrmAmount!! - currentIncomeStatement.q1Value - currentIncomeStatement.q2Value - currentIncomeStatement.q3Value)

                return currentIncomeStatement
            }
        }
    }

    /**
     * @param reportCode 분기보고서
     */
    private fun findFinancialStatement(financialList: List<FinancialStatement>, reportCode: ReportCode, year: Int): FinancialStatement? {
        return financialList.find { it.commonStatement.reportCode == reportCode && it.commonStatement.year == year }
    }
}