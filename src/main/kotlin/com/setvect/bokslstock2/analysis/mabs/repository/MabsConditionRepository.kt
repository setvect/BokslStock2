package com.setvect.bokslstock2.analysis.mabs.repository

import com.setvect.bokslstock2.analysis.mabs.entity.MabsConditionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MabsConditionRepository : JpaRepository<MabsConditionEntity, Int>{

    @Query("select x from XA_MABS_CONDITION x where x.mabsConditionSeq in :mabsConditionSeqs")
    fun listBySeq(@Param("mabsConditionSeqs") mabsConditionSeqs: Collection<Int>): List<MabsConditionEntity>

}