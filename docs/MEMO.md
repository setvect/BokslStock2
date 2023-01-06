# 복슬스톡 메모

## 포트폴리오 비주얼라이저

- 샤프지수 설명 및 공식: https://www.portfoliovisualizer.com/faq  (Terms and Definitions 참고)

### 듀얼모엔텀

- 월단위 매매로 했을 경우 매매가 일어나는 시점은 직전월 종가 기준
- MDD 계산은 월 단위로 함
    - 예를 들어
        - 7월 31일 종가 100원
        - 8월 1일 종가 90원
        - 8월 15일 종가 70원
        - 8월 31일 종가 100원
        - 이면 MDD는 0%

## 개발하다 발생한 의문점

### 없는 존재하지 않는 dependencies를 등록했는데도 에러가 나지 않음 ㅡㅡ;

```
dependencies {
    ...
    
    # 아래 처럼 존재하지 않은 dependencies 에러 안남 ㅡㅡ;
    # 이러 
    developmentOnly("org.springframework.boot:spring-boot-dssssssssssevtools")
    ... 
}

```

- gradle 명령어로 빌드 했을때 에러남. IDEA에서 에러 보여 줘야되는거 아니야?
- 더 우낀건 gradle reload는 했을때 오류가 나지 않음.. ㅡㅡ

## 찾다 찾다 포기한거

### static resource hot reload

구글링해서 찾은건데 다 안된다.

- https://jojoldu.tistory.com/48
- https://devwithpug.github.io/spring/about-hot-swap-in-spring-boot/
- https://docs.spring.io/spring-boot/docs/2.0.x/reference/html/using-boot-devtools.html
- https://mkyong.com/spring-boot/intellij-idea-spring-boot-template-reload-is-not-working/

찾았다!

- https://velog.io/@jodawooooon/IntelliJ-%EC%9E%90%EB%8F%99-%EB%B9%8C%EB%93%9C-%EC%84%A4%EC%A0%95-%EB%B0%A9%EB%B2%95-Registry%EC%97%90-compiler.automake.allow.when.app.running%EC%9D%B4-%EC%97%86%EB%8A%94-%EA%B2%BD%EC%9A%B0
  > Setting > Advanced Settings<br>
  > Allow auto-make to start even if developed application 체크

참고로 `spring-boot-devtools` 추가할 필요도 없다. 

## crontab 설정
```shell
40 8 * * 1-5 /home/setvect/stock-restart.sh >> /home/setvect/restart.log 2>&1
* 9-15 * * * cd /home/setvect/BokslStock2/bin && ./BokslStock2.sh startNotRunning >> startNotRunning.log
```