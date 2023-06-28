package com.setvect.bokslstock2.koreainvestment.trade.repository.query

import com.querydsl.core.types.ExpressionUtils
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import com.setvect.bokslstock2.backtest.common.model.StockCode
import com.setvect.bokslstock2.koreainvestment.trade.entity.QAssetHistoryEntity.assetHistoryEntity
import com.setvect.bokslstock2.koreainvestment.trade.model.dto.AssetHistoryDto
import com.setvect.bokslstock2.koreainvestment.trade.model.dto.AssetPeriodHistoryDto
import com.setvect.bokslstock2.koreainvestment.trade.model.web.AssetHistorySearchForm
import com.setvect.bokslstock2.koreainvestment.trade.model.web.AssetPeriodHistorySearchForm
import com.setvect.bokslstock2.util.ApplicationUtil
import org.apache.commons.lang3.StringUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class AssetHistorySelectRepository(
    private val queryFactory: JPAQueryFactory
) {
    fun pageAssetHistory(searchForm: AssetHistorySearchForm, pageable: Pageable): Page<AssetHistoryDto> {
        val result = queryFactory
            .select(
                Projections.fields(
                    AssetHistoryDto::class.java,
                    assetHistoryEntity.assetHistorySeq,
                    assetHistoryEntity.account,
                    assetHistoryEntity.assetCode,
                    assetHistoryEntity.investment,
                    assetHistoryEntity.yield,
                    assetHistoryEntity.memo,
                    assetHistoryEntity.regDate
                )
            )
            .from(assetHistoryEntity)
            .where(
                eqCode(searchForm.assetCode),
                containsAccount(searchForm.account),
                range(searchForm.from, searchForm.to),
            )
            .orderBy(assetHistoryEntity.regDate.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        result.toList().forEach {
            it.name = StockCode.findByCodeOrNull(it.assetCode!!)?.desc
        }

        val count = queryFactory
            .select(assetHistoryEntity.count())
            .from(assetHistoryEntity)
            .where(
                eqCode(searchForm.assetCode),
                containsAccount(searchForm.account),
                range(searchForm.from, searchForm.to)
            )
            .fetchOne()

        return PageImpl(result, pageable, count!!)
    }

    fun pageAssetPeriodHistory(
        searchForm: AssetPeriodHistorySearchForm,
        pageable: Pageable
    ): Page<AssetPeriodHistoryDto> {
        val result = queryFactory
            .from(assetHistoryEntity)
            .select(
                Projections.fields(
                    AssetPeriodHistoryDto::class.java,
                    assetHistoryEntity.account,
                    assetHistoryEntity.investment.sum().`as`("investment"),
                    ExpressionUtils.`as`(
                        assetHistoryEntity.investment.multiply(
                            Expressions.asNumber(1.0).add(assetHistoryEntity.yield.coalesce(0.0))
                        ).sum(), "evlPrice"
                    ),

                    // TODO AssetHistoryEntity.DEPOSIT 가 아닌 ROW만 카운트 해야 됨. 어떻게 해야될지 감이 잡히지 않음 ㅡㅡ;
                    assetHistoryEntity.assetCode.count().`as`("stockCount"),
                    assetHistoryEntity.regDate
                )
            )
            .where(
                containsAccount(searchForm.account),
                range(searchForm.from, searchForm.to),
            )
            .groupBy(assetHistoryEntity.regDate)
            .orderBy(assetHistoryEntity.regDate.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        result.toList().forEach {
            it.yield = ApplicationUtil.getYield(it.investment, it.evlPrice)
        }

        val count = queryFactory
            .select(
                assetHistoryEntity.regDate.countDistinct()
            )
            .from(assetHistoryEntity)
            .where(
                containsAccount(searchForm.account),
                range(searchForm.from, searchForm.to),
            )
            .fetchOne()

        return PageImpl(result, pageable, count!!)
    }

    private fun eqCode(assetCode: String?): BooleanExpression? {
        return if (StringUtils.isEmpty(assetCode)) {
            null
        } else assetHistoryEntity.assetCode.eq(assetCode)
    }

    private fun containsAccount(account: String?): BooleanExpression? {
        return if (StringUtils.isEmpty(account)) {
            null
        } else assetHistoryEntity.account.contains(account)
    }

    private fun range(from: LocalDateTime?, to: LocalDateTime?): BooleanExpression? {
        return if (from == null || to == null) {
            null
        } else assetHistoryEntity.regDate.between(from, to)
    }
}