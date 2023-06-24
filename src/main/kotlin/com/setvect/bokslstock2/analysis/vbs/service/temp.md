```kotlin
val conditionByMovingAverageCandle = condition.conditionList.associateWith { conditionItem ->
    if (conditionItem.stayGapRise) {
        movingAverageService.getMovingAverage(
            conditionItem.stock.convertStockCode(),
            PeriodType.PERIOD_MINUTE_5,
            PeriodType.PERIOD_DAY,
            listOf(conditionItem.maPeriod),
            condition.range
        )
    } else {
        movingAverageService.getMovingAverage(
            conditionItem.stock.convertStockCode(),
            PeriodType.PERIOD_DAY,
            PeriodType.PERIOD_DAY,
            listOf(conditionItem.maPeriod),
            condition.range
        )
    }
}

```