# 복슬스톡(kotlin Ver.)

- 각종 매매전략을 테스트하기 위한 기능 제공

## 설치

### 설정 파일

### 설정 파일

- [application.yml](src/main/resources/application.yml) 참고

### 인스톨

TODO

### 실행

TODO

### 빌드 & 실행

TODO

## 수집

- 미국 주식 시세 크롤링
- https://stooq.com/q/d/?s=spy.us
- `dividends` 항목 체크하지 않음 

## 백테스트 전략

### 이동평균돌파 전략 

해당 알고리즘을 요약 설명하면 단기 이동평균이 장기 이동평균을 돌파(정배열) 했을 때 매수, 단기 이동 평균이 장기 이동평균 아래로 내려(역배열)가면 매도함.

- 메인 소스: [MabsBacktestService.kt](src/main/kotlin/com/setvect/bokslstock2/analysis/service/MabsBacktestService.kt)
- 알고리즘
    1. 오늘 종가 기준 단기 이동평균 값과 장기 이동평균 값을 구함
    2. `(단기 이동평균 / 장기 이동평균 - 1) > 상승매수률` 조건이 만족하면 다음날 시초가 매수
    3. `(단기 이동평균 / 장기 이동평균 - 1) * -1 > 하락매도률 )` 조건이 만족하면 다음날 시초가 매도
    4. 매도가 발생한 주기는 매수하지 않음, 다음 주기로 넘어갔을 때 매수 활성화

`상승매수률`과 `하락매도률`를 둔 이유는 매수가와 매도가의 차이를 두어 매수가 이러난 직후 매도하지 않게 하기 위함 

### 변동성 돌파 전략 
- 메인 소스:[VbsBacktestService.kt](src/main/kotlin/com/setvect/bokslstock2/analysis/service/vbs/VbsBacktestService.kt)
- 매수 조건
  - 목표가 < 오늘 주가
  - 목표가 산출 방법: 목표가 = 오늘 시가 + (어제 고가 - 어제 저가) * k
- 매도 조건
  - 매수 다음날 시가 매도

### 듀얼모멘텀 
- 매수/매도 시점: 매월 마지막 거래일
  - 만약 2018년 1월에 기준이면 2017년 12월 마지막 거래일에서 매수/매도를 함 
   
- 매수 조건
  - 월 종가 매수
- 매도 조건
  - 월 종가 매도
- [포트폴리오 비주얼라이저](https://www.portfoliovisualizer.com/)
- [예시](https://www.portfoliovisualizer.com/test-market-timing-model?s=y&coreSatellite=false&timingModel=6&timePeriod=4&startYear=2018&firstMonth=1&endYear=2019&lastMonth=12&calendarAligned=true&includeYTD=false&initialAmount=10000&periodicAdjustment=0&adjustmentAmount=0&inflationAdjusted=true&adjustmentPercentage=0.0&adjustmentFrequency=4&symbols=SPY&singleAbsoluteMomentum=false&volatilityTarget=9.0&downsideVolatility=false&outOfMarketStartMonth=5&outOfMarketEndMonth=10&outOfMarketAssetType=2&outOfMarketAsset=TLT&movingAverageSignal=1&movingAverageType=1&multipleTimingPeriods=true&periodWeighting=2&windowSize=1&windowSizeInDays=105&movingAverageType2=1&windowSize2=10&windowSizeInDays2=105&excludePreviousMonth=false&normalizeReturns=false&volatilityWindowSize=0&volatilityWindowSizeInDays=0&assetsToHold=1&allocationWeights=1&riskControlType=0&riskWindowSize=10&riskWindowSizeInDays=0&stopLossMode=0&stopLossThreshold=2.0&stopLossAssetType=1&rebalancePeriod=1&separateSignalAsset=false&tradeExecution=0&leverageType=0&leverageRatio=0.0&debtAmount=0&debtInterest=0.0&maintenanceMargin=25.0&leveragedBenchmark=false&comparedAllocation=0&benchmark=VFINX&timingPeriods%5B0%5D=1&timingUnits%5B0%5D=2&timingWeights%5B0%5D=100&timingUnits%5B1%5D=2&timingUnits%5B2%5D=2&timingUnits%5B3%5D=2&timingWeights%5B3%5D=0&timingUnits%5B4%5D=2&timingWeights%5B4%5D=0&volatilityPeriodUnit=2&volatilityPeriodWeight=0)