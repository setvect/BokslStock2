package com.setvect.bokslstock2.analysis.rb.repository

import com.setvect.bokslstock2.analysis.rb.entity.RbConditionEntity
import com.setvect.bokslstock2.analysis.rb.entity.RbTradeEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

@Deprecated("삭제할 백테스트")
interface RbTradeRepository : JpaRepository<RbTradeEntity, Long> {

    @Modifying
    @Query("delete from FB_RB_TRADE x where x.rbConditionEntity = :rbConditionEntity")
    fun deleteByCondition(@Param("rbConditionEntity") rbConditionEntity: RbConditionEntity): Int

    @Query("select w from FB_RB_TRADE  w where w.rbConditionEntity = :rbConditionEntity")
    fun findByCondition(@Param("rbConditionEntity") condition: RbConditionEntity): List<RbTradeEntity>

}