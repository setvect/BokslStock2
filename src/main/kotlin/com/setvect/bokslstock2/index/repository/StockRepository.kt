package com.setvect.bokslstock2.index.repository

import com.setvect.bokslstock2.index.entity.StockEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface StockRepository : JpaRepository<StockEntity, Long> {


    @Query("select s from StockEntity s where s.code = :code")
    fun findByCode(@Param("code") code: String): Optional<StockEntity>

}