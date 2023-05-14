복슬스톡(kotlin Ver.)
==================================
각종 매매전략을 테스트하기 위한 기능 제공

## 1. 실행

### 1.1. 설정 파일

- [application.yml](src/main/resources/application.yml) 참고

### 1.2. IDE 환경에서 실행

1. 프로그램 실행 시 업비트에서 받은 `엑세스키`, `보안키값`을 환경변수에 설정
    - Intellij 경우 `Run/Debug Configurations` ->  `Environment variables`
        ```
        APPKEY=앱키;APPSECRET=앱시크릿키;ACCOUNTNO=계좌번호
        ```
1. Active Profiles: `local`
1. `BokslStock2Application` 실행

## 2. 빌드

### 2.1. 빌드 실행

```bash
gradlew clean
gradlew makeInstallFile
```

`build/generated/source/kapt/main/dist`에 실행 파일 만들어짐

### 2.2. 빌드 파일 설명

- conf/BokslStock2.yml: 설정 파일
- conf/logback-spring.xml: logback 설정
- lib/BokslStock2-0.0.2.jar: 복슬스톡2 프로그램
- bin/BokslStock2.sh: Linux 실행 스크립트
- bin/BokslStock2.bat: Windows에서 실행 스크립트

## 3. 배포

1. 서버시간 동기화
   ```sh
   $ rdate -s time.bora.net
   ```
1. `BokslStock2.yml` 설정 변경
    - appkey, appsecret, accountNo 값 등록
    - 알고리즘 상수값 변경

1. `BokslStock2.sh` 실행권한 부여
    ```shell
    $ chmod u+x BokslStock2.sh
    ```
1. `BokslStock2.sh` 실행
    ```shell
    $ ./BokslStock2.sh
    ```

## 4. 매매알고리즘

[한국투자증권 Open API](https://apiportal.koreainvestment.com/intro)를 사용하여 자동매매를 수행함

### 4.1. 변동성돌파전략

`변동성 돌파` 전략을 사용하여 매매

- 메인소스: [VbsService.kt](src/main/kotlin/com/setvect/bokslstock2/koreainvestment/vbs/service/VbsService.kt)
- 매수 조건
    - 목표가 < 오늘 주가
    - 목표가 산출 방법: 목표가 = 오늘 시가 + (어제 고가 - 어제 저가) * k
- 매도 조건
    - 매수 다음 영업일 매도
    - `stayGapRise` 설정에 따라 매도 방식이 결정됨 
      - false
        - 동시호가 시간에 매도. 동시호가 직전 예상 체결가 보다 낮게 매도 가격을 작성해 항상 메도 될 수 있또록 함
      - true
        - 매 5분마다 직전 5분봉 판단해 '시가 >= 종가' 이며 매도, 아니면 유지
        - 만약 실시간 시세가 그날 목표 매수가 이상이 되면 그날 매도를 하지 않음. 즉 매수 유지

## 5. 수집

### 5.1. 시세 크롤링

- 한국 및 미국주식
    - 한국주식: https://finance.naver.com
    - 미국주식: https://query1.finance.yahoo.com
    - 원달러환율: https://spot.wooribank.com/pot/Dream?withyou=FXXRT0014
- 메인 소스: [CrawlService.kt](src/main/kotlin/com/setvect/bokslstock2/index/service/CrawlStockPriceService.kt)
- 실행 소스: [CrawlStockPriceTest.kt](src/test/kotlin/com/setvect/bokslstock2/crawl/CrawlStockPriceTest.kt)

### 5.2. 가치 평가 정보

- 메인 소스: [CrawlerCompanyValueService.kt](src/main/kotlin/com/setvect/bokslstock2/value/service/CrawlerCompanyValueService.kt)
- 실행 소스: [CrawlCompanyValueTest.kt](src/test/kotlin/com/setvect/bokslstock2/crawl/CrawlCompanyValueTest.kt)

## 6. 백테스트 전략

### 6.1. 이동평균돌파 전략

해당 알고리즘을 요약 설명하면 단기 이동평균이 장기 이동평균을 돌파(정배열) 했을 때 매수, 단기 이동 평균이 장기 이동평균 아래로 내려(역배열)가면 매도함.

- 메인 소스: [MabsBacktestService.kt](src/main/kotlin/com/setvect/bokslstock2/analysis/mabs/service/MabsBacktestService.kt)
- 알고리즘
    1. 오늘 종가 기준 단기 이동평균 값과 장기 이동평균 값을 구함
    2. `(단기 이동평균 / 장기 이동평균 - 1) > 상승매수률` 조건이 만족하면 다음날 시초가 매수
    3. `(단기 이동평균 / 장기 이동평균 - 1) * -1 > 하락매도률 )` 조건이 만족하면 다음날 시초가 매도
    4. 매도가 발생한 주기는 매수하지 않음, 다음 주기로 넘어갔을 때 매수 활성화

`상승매수률`과 `하락매도률`를 둔 이유는 매수가와 매도가의 차이를 두어 매수가 이러난 직후 매도하지 않게 하기 위함

### 6.2. 변동성 돌파 전략

- 메인 소스:[VbsBacktestService.kt](src/main/kotlin/com/setvect/bokslstock2/analysis/vbs/service/VbsBacktestService.kt)
- 매수 조건
    - 목표가 < 오늘 주가
    - 목표가 산출 방법: 목표가 = 오늘 시가 + (어제 고가 - 어제 저가) * k
- 매도 조건
    - 매수 다음날 시가 매도

**주의사항**

- 변동성 돌파전략은 5분봉 을 사용하고 있음 
- 본 프로그램에서는 수정주가를 수집하고 있음 
- 크레온에서는 5분봉 데이터는 5년치만 수집가능
- 수집 시점마다 주가 데이터는 달라짐
- 이 때문에 수집데이터 여부에 따라서 백테스는 결과가 달라짐
  - 특히 MDD 계산이 달라지는데 이유는 종가 데이터를 기준으로 해당 일에 평가금액을 구함
  - 그래서 `KODEX 은행` 같은 배당이 있는 종목은 매매 조건중 `stayGapRise` false로 하기 바람
- 참고로 레버리지 종목은 애초에 배당이 없기 때문에 수정주가 이슈는 없음

### 6.3. 듀얼모멘텀

- 메인 소스:[DmAnalysisService.kt](src/main/kotlin/com/setvect/bokslstock2/analysis/dm/service/DmAnalysisService.kt)
- 실행 소스:[DmBacktest.kt](src/test/kotlin/com/setvect/bokslstock2/analysis/DmBacktest.kt)
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

### 6.4. ~~RB~~ 사용안함 삭제예정

- 메인 소스:[RbAnalysisService.kt](src/main/kotlin/com/setvect/bokslstock2/analysis/rb/setvice/RbAnalysisService.kt)
- 리밸런싱 매매 분석
- 일정주기마다 리벨런싱 적용
- 사용가능한 매매전략
    - 영구포트폴리오
    - 올웨더포트폴리오
    - 등...

### 6.5. 리벨런싱

- 메인 소스: [RebalanceAnalysisService.kt](src/main/kotlin/com/setvect/bokslstock2/analysis/rebalance/service/RebalanceAnalysisService.kt)
- 실행 소스:[RebalanceBacktest.kt](src/test/kotlin/com/setvect/bokslstock2/analysis/RebalanceBacktest.kt)
- 리밸런싱 매매 분석
- 일정주기마다 리벨런싱 적용
- 사용가능한 매매전략
    - 영구포트폴리오
    - 올웨더포트폴리오
    - 등...

### 6.6. LAA

#### 6.6.1. 수정 주가 (adjusted stock price)

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

## 7. 전략

### 7.1. 가치평가 전략

- 메인 소스: [ValueAnalysisService.kt](src/main/kotlin/com/setvect/bokslstock2/value/service/ValueAnalysisService.kt)
- 실행 소스:[ValueStrategyTest.kt](src/test/kotlin/com/setvect/bokslstock2/analysis/ValueStrategyTest.kt)
- 매수조건
    - 필터
       1. 기타금융, 생명보험, 손해보험, 은행, 증권, 창업투자
       1. 시총 순위 70% ~ 90% 사이 기업
    - 순위 매김
       - 1/PER
       - 1/PBR
       - 배당수익률
    - 각각의 등수를 더해 오름차순 정렬
    - 후처리(수동)
       - 중국기업 제외
       - 지주회사 제외
       - 기업명에 `홀딩스` 포함 기업제외
       - 우선주 제외
    - 상위 20개 기업 매수
- 매도 조건
    - 1년에 한 번 또는 두 번 리벨런싱

## 8. 기타
### 8.1. 인증서 연결
1. 접속하는 서버에서 작업
```shell
$ ssh-keygen -t rsa -C '아이디@주소.com'
The key''s randomart image is:
+---[RSA 3072]----+
|                 |
|     +           |
|      O          |
|..   O o         |
|                 |
|+X.o .           |
|B*@o.            |
|                 |
|OO%E.            |
+----[SHA256]-----+

$ ls -la
-rw-------  1 setvect setvect 1679  6월 14 19:34 id_rsa      <== 비밀키
-rw-r--r--  1 setvect setvect  390  6월 14 19:34 id_rsa.pub  <== 공개키

2. 접속할 서버에서 작업
```shell 
$ mkdir .ssh
$ chmod 700 .ssh/                                            <== 꼭 700 퍼미션 가져야됨.
$ vi ./ssh/id_rsa.pub                                        <== 공개키 복사
$ chmod 644 .ssh/authorized_keys                             <== 꼭 644 퍼미션 가져야됨.
```

3. 접속하는 쪽에서 확인
```shell
$ ssh id@대상_아이피
```

4. 접속 과정 상세 디버깅
```shell
$ ssh -v id@대상_아이피
$ ssh -vv id@대상_아이피
$ ssh -vvv id@대상_아이피
```

### 8.2. 오류 대응
#### com.jcraft.jsch.JSchException: invalid privatekey
오류 내용
```
com.jcraft.jsch.JSchException: invalid privatekey: [B@60f8a7c0
```

해결방법
```shell
$ ssh-keygen -p -f .ssh/id_rsa -m pem
```