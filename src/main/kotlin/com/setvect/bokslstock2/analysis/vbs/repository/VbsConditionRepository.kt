package com.setvect.bokslstock2.analysis.vbs.repository

import com.setvect.bokslstock2.analysis.vbs.entity.VbsConditionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface VbsConditionRepository : JpaRepository<VbsConditionEntity, Long> {

    @Query("select x from GA_VBS_CONDITION x where x.conditionSeq in :vbsConditionSeqs")
    fun listBySeq(@Param("vbsConditionSeqs") vbsConditionSeqs: Collection<Long>): List<VbsConditionEntity>

}