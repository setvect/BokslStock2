package com.setvect.bokslstock2.analysis.rb.repository

import com.setvect.bokslstock2.analysis.rb.entity.RbConditionEntity
import com.setvect.bokslstock2.analysis.rb.entity.RbTradeEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface RbTradeRepository : JpaRepository<RbTradeEntity, Long> {

    @Modifying
    @Query("delete from VB_RB_TRADE x where x.rbConditionEntity = :rbConditionEntity")
    fun deleteByCondition(@Param("rbConditionEntity") rbConditionEntity: RbConditionEntity): Int

}