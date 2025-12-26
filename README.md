복슬스톡(kotlin Ver.)
==================================

---

**경고: 본 소스코드는 누구나 사용할 수 있으며 그로 인해 발생하는 모든 문제의 책임은 사용자에게 있습니다.**

---

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
   $ sudo timedatectl set-timezone Asia/Seoul
   $ sudo rdate -s time.bora.net
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
    - 목표가 <= 오늘 주가
      - 여기서 주가는 현재가 또는 매도1호가를 기준으로 목표가 돌파 여부를 판단함
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
- 메인 소스: [CrawlerStockPriceService.kt](src/main/kotlin/com/setvect/bokslstock2/crawl/stockprice/service/CrawlerStockPriceService.kt)

#### 5.1.1 수정 주가 (adjusted stock price)

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

### 5.2. 한국주식 기업 가치 평가 정보

- 메인 소스: [NaverCompanyValueCrawlerService.kt](src/main/kotlin/com/setvect/bokslstock2/crawl/naver/service/NaverCompanyValueCrawlerService.kt)
- 실행 소스: [NaverCompanyValueCrawlerServiceTest.kt](src/testDependency/kotlin/com/setvect/bokslstock2/crawl/naver/service/NaverCompanyValueCrawlerServiceTest.kt)
- 수집 소스
  - Naver API: `https://stock.naver.com/api/domestic/market/stock/default`
  - 수집 조건: tradeType=KRX, marketType=ALL, orderType=marketSum, pageSize=5000
- 결과 파일
  - `crawl/stock.naver.com/detail-list.json`
- 수집 데이터(요약)
  - summary: 종목코드, 종목명, 마켓, 시총, 현재가
  - currentIndicator: 상장주식수, PER, EPS, PBR, 배당수익률
  - historyData는 수집하지 않음


## 5.3. finviz.com 수집
### 5.3.1. 수집 방법

- [https://finviz.com](https://finviz.com/screener.ashx?v=152&ft=4&c=0,1,2,3,4,5,6,7,67,65,66) 에서 수집
- 페이지 이동하면서 데이터 수집
- 수집 결과는 `crawl/finviz.com`에 저장됨
- 메인 소스: [CrawlerFinvizCompanyService.kt](src/main/kotlin/com/setvect/bokslstock2/crawl/finviz/service/CrawlerFinvizCompanyService.kt)
- 실행 소스: [CrawlerFinvizCompanyServiceTest.kt](src/testDependency/kotlin/com/setvect/bokslstock2/crawl/finviz/service/CrawlerFinvizCompanyServiceTest.kt)

### 5.3.2. 수집 용어
[수집항목_용어](docs/주식_용어.md) 참고

## 6. 백테스트 전략

### 6.1. 변동성 돌파 전략

- 메인 소스:[VbsBacktestService.kt](src/main/kotlin/com/setvect/bokslstock2/backtest/vbs/service/VbsBacktestService.kt)
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

### 6.2. 리벨런싱

- 메인 소스: [RebalanceAnalysisService.kt](src/main/kotlin/com/setvect/bokslstock2/backtest/rebalance/service/RebalanceBacktestService.kt)
- 실행 소스:[RebalanceBacktest.kt](src/testDependency/kotlin/com/setvect/bokslstock2/backtest/RebalanceBacktest.kt)
- 리밸런싱 매매 분석
- 일정주기마다 리벨런싱 적용
- 사용가능한 매매전략
    - 영구포트폴리오
    - 올웨더포트폴리오
    - 등...

## 7. 전략

### 7.1. 가치평가 전략(한국 주식)

- 메인 소스: [ValueAnalysisKoreanCompanyService.kt](src/main/kotlin/com/setvect/bokslstock2/strategy/companyvalue/service/ValueAnalysisKoreanCompanyService.kt)
- 실행 소스: [ValueAnalysisKoreanCompanyServiceTest.kt](src/testDependency/kotlin/com/setvect/bokslstock2/strategy/companyvalue/service/ValueAnalysisKoreanCompanyServiceTest.kt)
- 매수조건
    - 필터
       1. 기타금융, 생명보험, 손해보험, 은행, 증권, 창업투자
       2. 시총 순위 70% ~ 90% 사이 기업
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
       - 전년도 영업이익 적자 제외 OR 직전 2분기 연속 영업이익 적자 제외
    - 상위 20개 기업 매수
- 매도 조건
    - 1년에 한 번 또는 두 번 리벨런싱

### 7.2. 가치평가 전략(미국 주식)
- 메인 소스: [ValueAnalysisUsaCompanyService.kt](src/main/kotlin/com/setvect/bokslstock2/strategy/companyvalue/service/ValueAnalysisUsaCompanyService.kt)
- 실행 소스: [ValueAnalysisUsaCompanyServiceTest.kt](src/testDependency/kotlin/com/setvect/bokslstock2/strategy/companyvalue/service/ValueAnalysisUsaCompanyServiceTest.kt)
- 매수조건
  - 필터
    1. 시총 순위 70% ~ 90% 사이 기업
    1. 국가: USA
    1. 제외 색터: Real Estate, Financial, Energy
    1. PTP 종목 제외 [PTP1.txt, PTP2.txt](src/main/resources/assets) 참고
  - 순위 매김
      - 1/PER
      - 1/PBR
      - 배당수익률
  - 각각의 등수를 더해 오름차순 정렬
  - 후처리(수동)
      - 업종당 3종목 이하
    - 전년도 영업이익 적자 제외 OR 직전 2분기 연속 영업이익 적자 제외
  - 상위 20개 기업 매수


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
$ vi ./ssh/authorized_keys                                   <== 공개키 복사
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
