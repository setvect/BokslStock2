package com.setvect.bokslstock2.analysis.mabs.repository

import com.setvect.bokslstock2.analysis.mabs.entity.MabsConditionEntity
import com.setvect.bokslstock2.analysis.mabs.entity.MabsTradeEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MabsTradeRepository : JpaRepository<MabsTradeEntity, Int> {

    @Modifying
    @Query("delete from XB_MABS_TRADE x where x.mabsConditionEntity = :mabsConditionEntity")
    fun deleteByCondition(@Param("mabsConditionEntity") mabsConditionEntity: MabsConditionEntity): Int

}