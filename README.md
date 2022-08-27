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

- 한국 및 미국주식 크롤링
  - 한국주식: https://finance.naver.com
  - 미국주식: https://query1.finance.yahoo.com
- 메인 소스: [CrawlService.kt](src/main/kotlin/com/setvect/bokslstock2/index/service/CrawlStockPriceService.kt)
- 실행 소스: [CrawlStockPriceTest.kt](src/test/kotlin/com/setvect/bokslstock2/crawl/CrawlStockPriceTest.kt)

## 백테스트 전략

### 이동평균돌파 전략

해당 알고리즘을 요약 설명하면 단기 이동평균이 장기 이동평균을 돌파(정배열) 했을 때 매수, 단기 이동 평균이 장기 이동평균 아래로 내려(역배열)가면 매도함.

- 메인 소스: [MabsBacktestService.kt](src/main/kotlin/com/setvect/bokslstock2/analysis/mabs/service/MabsBacktestService.kt)
- 알고리즘
  1. 오늘 종가 기준 단기 이동평균 값과 장기 이동평균 값을 구함
  2. `(단기 이동평균 / 장기 이동평균 - 1) > 상승매수률` 조건이 만족하면 다음날 시초가 매수
  3. `(단기 이동평균 / 장기 이동평균 - 1) * -1 > 하락매도률 )` 조건이 만족하면 다음날 시초가 매도
  4. 매도가 발생한 주기는 매수하지 않음, 다음 주기로 넘어갔을 때 매수 활성화

`상승매수률`과 `하락매도률`를 둔 이유는 매수가와 매도가의 차이를 두어 매수가 이러난 직후 매도하지 않게 하기 위함

### 변동성 돌파 전략

- 메인 소스:[VbsBacktestService.kt](src/main/kotlin/com/setvect/bokslstock2/analysis/vbs/service/VbsBacktestService.kt)
- 매수 조건
  - 목표가 < 오늘 주가
  - 목표가 산출 방법: 목표가 = 오늘 시가 + (어제 고가 - 어제 저가) * k
- 매도 조건
  - 매수 다음날 시가 매도

### 듀얼모멘텀

- 메인 소스:[DmAnalysisService.kt](src/main/kotlin/com/setvect/bokslstock2/analysis/dm/service/DmAnalysisService.kt)
- 절대 모멘텀과 상대 모멘텀 결합
- 매매 전략 전략
  - 직전월 종가 기준 n월 평균 종가보다 높은순으로 정렬
  - 1보다 큰 종목이 있으면 매수
  - 0보다 작으면 Hold 종목 매수
- 포트폴리오 비주얼라이저와 차이점
  - 복슬스톡2: 현재 월 시가 기준 매매
  - 포트폴리오 비주얼라이저: 직전 월 종가 기준 매매
- 모멘텀 계산 예시
  - 조건
    - 모멘텀 가중치: 1개월 전: 33%, 3개월 전: 33%, 6개월 전: 34%
    - 현재 날짜: 2022년 5월 1일
    - 2022년 04월 종가: 105
    - 2022년 03월 종가: 110
    - 2022년 01월 종가: 100
    - 2021년 10월 종가: 95
  - 모멘텀 스코어 = 105 / (110 * 0.33 + 100 * 0.33 + 95 * 0.34) = 1.033464567
- [포트폴리오 비주얼라이저](https://www.portfoliovisualizer.com/)
- [예시](https://www.portfoliovisualizer.com/test-market-timing-model?s=y&coreSatellite=false&timingModel=6&timePeriod=4&startYear=2018&firstMonth=1&endYear=2019&lastMonth=12&calendarAligned=true&includeYTD=false&initialAmount=10000&periodicAdjustment=0&adjustmentAmount=0&inflationAdjusted=true&adjustmentPercentage=0.0&adjustmentFrequency=4&symbols=SPY&singleAbsoluteMomentum=false&volatilityTarget=9.0&downsideVolatility=false&outOfMarketStartMonth=5&outOfMarketEndMonth=10&outOfMarketAssetType=2&outOfMarketAsset=TLT&movingAverageSignal=1&movingAverageType=1&multipleTimingPeriods=true&periodWeighting=2&windowSize=1&windowSizeInDays=105&movingAverageType2=1&windowSize2=10&windowSizeInDays2=105&excludePreviousMonth=false&normalizeReturns=false&volatilityWindowSize=0&volatilityWindowSizeInDays=0&assetsToHold=1&allocationWeights=1&riskControlType=0&riskWindowSize=10&riskWindowSizeInDays=0&stopLossMode=0&stopLossThreshold=2.0&stopLossAssetType=1&rebalancePeriod=1&separateSignalAsset=false&tradeExecution=0&leverageType=0&leverageRatio=0.0&debtAmount=0&debtInterest=0.0&maintenanceMargin=25.0&leveragedBenchmark=false&comparedAllocation=0&benchmark=VFINX&timingPeriods%5B0%5D=1&timingUnits%5B0%5D=2&timingWeights%5B0%5D=100&timingUnits%5B1%5D=2&timingUnits%5B2%5D=2&timingUnits%5B3%5D=2&timingWeights%5B3%5D=0&timingUnits%5B4%5D=2&timingWeights%5B4%5D=0&volatilityPeriodUnit=2&volatilityPeriodWeight=0)

### ~~RB~~ 사용안함 삭제예정

- 메인 소스:[RbAnalysisService.kt](src/main/kotlin/com/setvect/bokslstock2/analysis/rb/setvice/RbAnalysisService.kt)
- 리밸런싱 매매 분석
- 일정주기마다 리벨런싱 적용
- 사용가능한 매매전략
  - 영구포트폴리오
  - 올웨더포트폴리오
  - 등...

### 리벨런싱

- 메인 소스: [RebalanceAnalysisService.kt](src/main/kotlin/com/setvect/bokslstock2/analysis/rebalance/service/RebalanceAnalysisService.kt)
- 리밸런싱 매매 분석
- 일정주기마다 리벨런싱 적용
- 사용가능한 매매전략
  - 영구포트폴리오
  - 올웨더포트폴리오
  - 등...

### LAA

#### 수정 주가 (adjusted stock price)

- 영어로된 설명
  - Close price adjusted for splits
  - Adjusted close price adjusted for splits and dividend and/or capital gain distributions
- 서로 값이 조금씩 달라 뭐가 맞는지 모르겠다 ㅡㅡ;
  - https://stooq.com/q/d/?s=spy.us&c=0&i=m
  - 야후 파이낸스: https://finance.yahoo.com/quote/SPY/history?period1=1627731136&period2=1659267136&interval=1mo&filter=history&frequency=1mo&includeAdjustedClose=true
    - 다운로드 예시: https://query1.finance.yahoo.com/v7/finance/download/SPY?period1=757382400&period2=1659267136
  - https://kr.tradingview.com/chart/Y75mi3ck/?symbol=SPY
- 상황: 2022년 7월 31일 기준 SPY 2022년 1월 종가

| 사이트          | 수정주가 여부 | 추가 설명                   | 주가            |
| --------------- | ------------- | --------------------------- | --------------- |
| stooq           | X             | 'Skip dividends' 체크       | 449.90744983156 |
| stooq           | O             | 'Skip dividends' 체크 안함  | 448.51          |
| yahoo finance   | X             | 'close' 값                  | 449.91          |
| yahoo finance   | O             | 'Adj Close' 값              | 446.59          |
| tradingview.com | X             | '조정' 체크 안함            | 449.91          |
| tradingview.com | O             | tradingview.com '조정' 체크 | 446.59          |

## 전략

### 가치평가 전략

- 메인 소스: [ValueAnalysisService.kt](src/main/kotlin/com/setvect/bokslstock2/value/service/ValueAnalysisService.kt)
- 매수조건
  - 필터
    - 시총 순위 70% ~ 90% 사이 기업
    - 기타금융, 생명보험, 손해보험, 은행, 증권, 창업투자
    - `홀딩스` 포함 기업제외
  - 순위 매김
    - 1/PER
    - 1/PBR
    - 배당수익률
  - 각각의 등수를 더해 오름차순 정렬
  - 후처리(수동)
    - 중국기업 제외
    - 지주회사 제외
  - 상위 20개 기업 매수
- 매도 조건
  - 1년에 한 번 또는 두 번 리벨런싱

