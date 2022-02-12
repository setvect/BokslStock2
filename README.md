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

TODO

## 백테스트

### 이동평균돌파 전략 매매 알고리즘

해당 알고리즘을 요약 설명하면 단기 이동평균이 장기 이동평균을 돌파(정배열) 했을 때 매수, 단기 이동 평균이 장기 이동평균 아래로 내려(역배열)가면 매도함.

- 메인 소스: [MabsBacktestService.kt](src/main/kotlin/com/setvect/bokslstock2/analysis/service/MabsBacktestService.kt)
- 알고리즘
    1. 오늘 종가 기준 단기 이동평균 값과 장기 이동평균 값을 구함
    2. `(단기 이동평균 / 장기 이동평균 - 1) > 상승매수률` 조건이 만족하면 다음날 시초가 매수
    3. `(단기 이동평균 / 장기 이동평균 - 1) * -1 > 하락매도률 )` 조건이 만족하면 다음날 시초가 매도
    4. 매도가 발생한 주기는 매수하지 않음, 다음 주기로 넘어갔을 때 매수 활성화

`상승매수률`과 `하락매도률`를 둔 이유는 매수가와 매도가의 차이를 두어 매수가 이러난 직후 매도하지 않게 하기 위함 

### 변동성 돌파 전략 매매 알고리즘
- 메인 소스:[VbsBacktestService.kt](src/main/kotlin/com/setvect/bokslstock2/analysis/service/vbs/VbsBacktestService.kt)
- 매수 조건
  - 목표가 < 오늘 주가
  - 목표가 산출 방법: 목표가 = 오늘 시가 + (어제 고가 - 어제 저가) * k
- 매도 조건
  - 매수 다음날 시가 매도
