package com.setvect.bokslstock2.value.service

import com.google.gson.GsonBuilder
import com.setvect.bokslstock2.value.dto.CompanyDetail
import com.setvect.bokslstock2.value.dto.Rank
import org.apache.commons.io.FileUtils
import org.springframework.stereotype.Service


@Service
/**
 * 가치 평가 전략
 */
class ValueAnalysisService(
    val valueCommonService: ValueCommonService
) {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val excludeIndustry = listOf("기타금융", "생명보험", "손해보험", "은행", "증권", "창업투자")
    private val upperRatio = .7
    private val lowerRatio = .9


    /**
     *
     */
    fun analysis() {
        val detailFile = valueCommonService.getDetailListFile()
        val listJson = FileUtils.readFileToString(detailFile, "utf-8")
        val companyAllList = gson.fromJson(listJson, Array<CompanyDetail>::class.java).asList()
        println("전체 Size: " + companyAllList.size)
        val companyFilterList = companyAllList
            .filter { !excludeIndustry.contains(it.industry) }
            .filter { it.currentIndicator.per != null && it.currentIndicator.pbr != null && it.currentIndicator.dvr != null }
            .sortedByDescending { it.summary.capitalization }
            .toList()
        println("필터 Size: " + companyFilterList.size)

        val fromIndex = (companyFilterList.size * upperRatio).toInt()
        val toIndex = (companyFilterList.size * lowerRatio).toInt()
        val targetList = companyFilterList.subList(fromIndex, toIndex)

        val targetByRank = targetList.map { Pair(it, Rank()) }

        targetByRank.sortedByDescending { 1 / it.first.currentIndicator.per!! }
            .forEachIndexed { index, pair ->
                pair.second.per = index + 1
            }

        targetByRank.sortedByDescending { 1 / it.first.currentIndicator.pbr!! }
            .forEachIndexed { index, pair ->
                pair.second.pbr = index + 1
            }

        targetByRank.sortedByDescending { it.first.currentIndicator.dvr!! }
            .forEachIndexed { index, pair ->
                pair.second.dvr = index + 1
            }

        val listByRanking = targetByRank.sortedBy { it.second.total() }

        listByRanking.forEach { println("${it.first.summary.name} - ${it.first.summary.capitalization} - ${it.first.industry}") }

    }
}