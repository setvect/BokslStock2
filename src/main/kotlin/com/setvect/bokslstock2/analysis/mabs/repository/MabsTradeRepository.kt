package com.setvect.bokslstock2.analysis.mabs.repository

import com.setvect.bokslstock2.analysis.mabs.entity.MabsConditionEntity
import com.setvect.bokslstock2.analysis.mabs.entity.MabsTradeEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MabsTradeRepository : JpaRepository<MabsTradeEntity, Long> {

    @Modifying
    @Query("delete from HB_MABS_TRADE x where x.mabsConditionEntity = :mabsConditionEntity")
    fun deleteByCondition(@Param("mabsConditionEntity") mabsConditionEntity: MabsConditionEntity): Int


    @Query("select w from HB_MABS_TRADE w where w.mabsConditionEntity = :mabsConditionEntity")
    fun findByCondition(@Param("mabsConditionEntity") mabsConditionEntity: MabsConditionEntity): List<MabsTradeEntity>

}