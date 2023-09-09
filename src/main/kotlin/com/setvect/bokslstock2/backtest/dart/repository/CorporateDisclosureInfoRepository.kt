package com.setvect.bokslstock2.backtest.dart.repository

import com.setvect.bokslstock2.backtest.dart.entity.CorporateDisclosureInfoEntity
import com.setvect.bokslstock2.backtest.dart.model.FinancialDetailMetric
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface CorporateDisclosureInfoRepository : JpaRepository<CorporateDisclosureInfoEntity, Long> {
    fun findByCode(code: String): List<CorporateDisclosureInfoEntity>

    @Query("SELECT c FROM CorporateDisclosureInfoEntity c WHERE c.code = :code AND c.financialDetailMetric = :financialDetailMetric AND c.year = :year")
    fun findByMetric(
        code: String,
        financialDetailMetric: FinancialDetailMetric,
        year: Int
    ): Optional<CorporateDisclosureInfoEntity>
}