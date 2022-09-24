package com.setvect.bokslstock2.koreainvestment.trade.repository

import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import com.setvect.bokslstock2.koreainvestment.trade.entity.QTradeEntity.tradeEntity
import com.setvect.bokslstock2.koreainvestment.trade.model.TradeDto
import com.setvect.bokslstock2.koreainvestment.trade.model.web.TradeSearchForm
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class HistoryRepository(
    private val queryFactory: JPAQueryFactory
) {
    fun list(searchForm: TradeSearchForm, pageable: Pageable): Page<TradeDto> {
        val result = queryFactory.from(tradeEntity)
            .select(
                Projections.fields(
                    TradeDto::class.java,
                    tradeEntity.tradeSeq,
                    tradeEntity.account,
                    tradeEntity.code,
                    tradeEntity.tradeType,
                    tradeEntity.qty,
                    tradeEntity.unitPrice,
                    tradeEntity.yield,
                    tradeEntity.memo,
                    tradeEntity.regDate
                )
            )
            .fetch()

        return PageImpl(result, pageable, result.size.toLong())
    }
}