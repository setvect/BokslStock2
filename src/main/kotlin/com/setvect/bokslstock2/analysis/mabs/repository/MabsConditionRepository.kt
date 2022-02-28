package com.setvect.bokslstock2.analysis.mabs.repository

import com.setvect.bokslstock2.analysis.mabs.entity.MabsConditionEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface MabsConditionRepository : JpaRepository<MabsConditionEntity, Long>{

    @Query("select x from HA_MABS_CONDITION x where x.conditionSeq in :mabsConditionSeqs")
    fun listBySeq(@Param("mabsConditionSeqs") mabsConditionSeqs: Collection<Long>): List<MabsConditionEntity>

}