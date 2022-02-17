package com.setvect.bokslstock2.analysis.vbs.repository

import com.setvect.bokslstock2.analysis.vbs.entity.VbsConditionEntity
import com.setvect.bokslstock2.analysis.vbs.entity.VbsTradeEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface VbsTradeRepository : JpaRepository<VbsTradeEntity, Int> {

    @Modifying
    @Query("delete from WB_VBS_TRADE x where x.vbsConditionEntity = :vbsConditionEntity")
    fun deleteByCondition(@Param("vbsConditionEntity") vbsConditionEntity: VbsConditionEntity): Int

}