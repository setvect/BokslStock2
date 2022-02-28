package com.setvect.bokslstock2.analysis.vbs.repository

import com.setvect.bokslstock2.analysis.vbs.entity.VbsConditionEntity
import com.setvect.bokslstock2.analysis.vbs.entity.VbsTradeEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface VbsTradeRepository : JpaRepository<VbsTradeEntity, Long> {

    @Modifying
    @Query("delete from GB_VBS_TRADE x where x.vbsConditionEntity = :vbsConditionEntity")
    fun deleteByCondition(@Param("vbsConditionEntity") vbsConditionEntity: VbsConditionEntity): Int

    @Query("select w from GB_VBS_TRADE w where w.vbsConditionEntity = :vbsConditionEntity")
    fun findByCondition(@Param("vbsConditionEntity") vbsConditionEntity: VbsConditionEntity): List<VbsTradeEntity>
}