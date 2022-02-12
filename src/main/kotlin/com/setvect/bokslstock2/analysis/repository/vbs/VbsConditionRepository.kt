package com.setvect.bokslstock2.analysis.repository.vbs

import com.setvect.bokslstock2.analysis.entity.MabsConditionEntity
import com.setvect.bokslstock2.analysis.entity.vbs.VbsConditionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface VbsConditionRepository : JpaRepository<VbsConditionEntity, Int>{

    @Query("select x from WA_VBS_CONDITION x where x.vbsConditionSeq in :vbsConditionSeqs")
    fun listBySeq(@Param("vbsConditionSeqs") vbsConditionSeqs: Collection<Int>): List<MabsConditionEntity>

}