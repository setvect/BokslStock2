package com.setvect.bokslstock2.analysis.repository.vbs

import com.setvect.bokslstock2.analysis.entity.vbs.VbsConditionEntity
import com.setvect.bokslstock2.analysis.entity.vbs.VbsTradeEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface VbsTradeRepository : JpaRepository<VbsTradeEntity, Int> {

    @Modifying
    @Query("delete from WB_VBS_TRADE x where x.vbsConditionEntity = :vbsConditionEntity")
    fun deleteByCondition(@Param("vbsConditionEntity") vbsConditionEntity: VbsConditionEntity): Int

}