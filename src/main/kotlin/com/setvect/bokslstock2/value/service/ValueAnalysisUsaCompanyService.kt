package com.setvect.bokslstock2.value.service

import com.google.gson.GsonBuilder
import com.setvect.bokslstock2.util.NumberUtil
import com.setvect.bokslstock2.value.dto.UsaCompanyDetail
import org.apache.commons.io.FileUtils
import org.modelmapper.TypeToken
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File


@Service
/**
 * 미국 기업 주식 가치 평가 전략
 */
class ValueAnalysisUsaCompanyService(
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val gson = GsonBuilder().setPrettyPrinting().create()

    companion object {
        private val USA_COMPANY_VALUE_FILE = File("crawl/finviz.com/finviz.json")
    }

    /**
     * 미국 기업 주식 가치 평가 전략
     */
    fun analysis() {
        log.info("미국 기업 주식 가치 평가 전략")
        val companyList = loadCompanyInfo()
        val usaCompanyDetailList = loadCompanyDetail(companyList)
        println(usaCompanyDetailList)

    }

    private fun loadCompanyDetail(companyList: List<Map<String, String>>): List<UsaCompanyDetail> {
        val usaCompanyDetailList = mutableListOf<UsaCompanyDetail>()
        for (company in companyList) {
            val ticker = company["Ticker"]!!
            val name = company["Company"]!!
            val sector = company["Sector"]!!
            val industry = company["Industry"]!!
            val country = company["Country"]!!
            val index = company["Index"]!!.split(",").map { it.trim() }.toSet()
            val marketCap = company["Market Cap"]?.let { NumberUtil.unitToNumber(it) }

            val currentIndicator = UsaCompanyDetail.CurrentIndicator(
                per = company["P/E"]?.takeIf { it != "-" }?.toDouble(),
                eps = company["EPS"]?.takeIf { it != "-" }?.toDouble(),
                pbr = company["P/B"]?.takeIf { it != "-" }?.toDouble(),
                dvr = company["Dividend"]?.takeIf { it != "-" }?.let { NumberUtil.percentToNumber(it) },
            )
            val usaCompanyDetail = UsaCompanyDetail(
                ticker = ticker,
                name = name,
                sector = sector,
                industry = industry,
                country = country,
                index = index,
                marketCap = marketCap,
                currentIndicator = currentIndicator,
            )
            usaCompanyDetailList.add(usaCompanyDetail)
        }
        return usaCompanyDetailList
    }

    /**
     * 중복제거된 기업 정보
     */
    private fun loadCompanyInfo(): List<Map<String, String>> {
        val json = FileUtils.readFileToString(USA_COMPANY_VALUE_FILE, "UTF-8")
        val type = object : TypeToken<List<Map<String, String>>>() {}.type
        val loadCompanyDetails: List<Map<String, String>> = gson.fromJson(json, type)
        return loadCompanyDetails.distinctBy { it["Ticker"] }
    }
}