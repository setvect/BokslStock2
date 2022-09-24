package com.setvect.bokslstock2.koreainvestment.trade.repository

import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import com.setvect.bokslstock2.koreainvestment.trade.entity.QTradeEntity.tradeEntity
import com.setvect.bokslstock2.koreainvestment.trade.model.TradeDto
import com.setvect.bokslstock2.koreainvestment.trade.model.web.TradeSearchForm
import org.apache.commons.lang3.StringUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

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
            .where(
                eqCode(searchForm.code),
                containsAccount(searchForm.account),
                range(searchForm.from, searchForm.to)
            )
            .orderBy(tradeEntity.tradeSeq.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()


        val count = queryFactory.from(tradeEntity)
            .select(tradeEntity.count())
            .where(
                eqCode(searchForm.code),
                containsAccount(searchForm.account),
                range(searchForm.from, searchForm.to)
            )
            .fetchOne()

        return PageImpl(result, pageable, count!!)
    }

    private fun eqCode(code: String?): BooleanExpression? {
        return if (StringUtils.isEmpty(code)) {
            null
        } else tradeEntity.code.eq(code)
    }

    private fun containsAccount(account: String?): BooleanExpression? {
        return if (StringUtils.isEmpty(account)) {
            null
        } else tradeEntity.account.contains(account)
    }

    private fun range(from: LocalDateTime?, to: LocalDateTime?): BooleanExpression? {
        return if (from == null || to == null) {
            null
        } else tradeEntity.regDate.between(from, to)
    }

}