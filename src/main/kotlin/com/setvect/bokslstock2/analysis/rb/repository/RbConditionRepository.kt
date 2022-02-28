package com.setvect.bokslstock2.analysis.rb.repository

import com.setvect.bokslstock2.analysis.rb.entity.RbConditionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface RbConditionRepository : JpaRepository<RbConditionEntity, Long>{

    @Query("select x from FA_RB_CONDITION x where x.conditionSeq in :rbConditionSeqs")
    fun listBySeq(@Param("rbConditionSeqs") rbConditionSeqs: Collection<Long>): List<RbConditionEntity>

}