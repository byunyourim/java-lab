# 1단계: 현재 스택 깊이 다지기 — 하루 단위 커리큘럼

> **기간:** 8주 (M1: Java/Spring + M2: JPA)  
> **목표:** "왜 이렇게 동작하는가"를 설명할 수 있는 수준  
> **하루:** 평일 2~3h / 토 5~6h / 일 2~3h (오후 휴식)  
> **코딩 장소:** java-lab (실험) / trader-bot (적용)  
> **매주 필수:** PR 1개 + 테스트 + 측정 + 블로그 1편  

> **책은 정독하지 않는다.** "왜" 질문에 답이 안 될 때 해당 챕터만 찾아본다.
> 1단계 끝나고 리팩토링 주간(Week 13)에 몰아 읽으면 흡수력이 다르다.
> - "자바 성능 튜닝 이야기" — JVM/GC 막힐 때 사전식 참조
> - "컴퓨터 밑바닥 파헤치기" — CPU 캐시, 메모리 계층 배경지식 필요할 때
> - "이펙티브 자바" — 예외(Item 69~77) 등 특정 주제만
> - "자바 ORM 표준 JPA 프로그래밍" — **유일하게 정독 권장** (M2 시작 전)

---

# M1: Java/Spring 원리 (Week 1–4)

---

## Week 1 — JVM 메모리 & 동시성 + Gradle 기초

### Day 1 (5/25 월) — JVM 메모리 구조 이해

**이해 (2h)**
- JVM 아키텍처 전체 그림 그려보기 (종이에 직접)
  - Class Loader → Runtime Data Area → Execution Engine
- Runtime Data Area 5가지 영역 구분
  - **Heap**: 객체 인스턴스 저장. Young(Eden + S0 + S1) / Old 구조
  - **Stack**: 스레드마다 생성. 프레임(지역변수 + 오퍼랜드 스택 + 프레임 데이터)
  - **Metaspace**: 클래스 메타데이터 (Java 8 이전 PermGen과 차이는?)
  - **PC Register**: 스레드마다 현재 실행 중인 JVM 명령 주소
  - **Native Method Stack**: JNI 호출용
- **질문하며 읽기:**
  - `new Object()`를 호출하면 메모리에서 정확히 무슨 일이 일어나는가?
  - 지역변수 `int x = 10`과 `Integer x = 10`은 어디에 저장되는가? 왜?
  - String 리터럴 `"hello"`와 `new String("hello")`의 저장 위치 차이는?
  - 왜 Heap은 공유이고 Stack은 스레드별인가?

**참고 자료**
- "자바 성능 튜닝 이야기" 1~2장
- Oracle 공식: "The Structure of the Java Virtual Machine"

### Day 2 (5/26 화) — Heap OOM + Stack Overflow 재현

**코드 (2.5h) — java-lab**
- [ ] Heap OOM 재현
  - `List<byte[]>`에 1MB씩 무한 추가
  - `-Xmx128m`으로 제한 → 터지는 시점 관찰
  - **왜 질문:** OOM이 터질 때 GC는 뭘 하고 있었나? `jstat -gcutil <pid> 1000`으로 확인
- [ ] StackOverflowError 재현
  - 무한 재귀 호출 → 몇 번째 프레임에서 터지나?
  - `-Xss256k` vs `-Xss1m`으로 프레임 수 차이 확인
  - **왜 질문:** 스택 프레임 하나의 크기는 뭐가 결정하는가? (지역변수 수, 파라미터 수)
- [ ] 각 OOM 발생 시 에러 메시지 정확히 기록

### Day 3 (5/27 수) — Metaspace OOM + GC 기초

**코드 + 이해 (2.5h)**
- [ ] Metaspace OOM 재현 (java-lab)
  - ByteBuddy로 런타임에 클래스 1만 개 동적 생성
  - `-XX:MaxMetaspaceSize=64m` 제한
  - **왜 질문:** 왜 Java 8에서 PermGen을 버리고 Metaspace로 바꿨는가?
  - **왜 질문:** 클래스 언로딩은 언제 일어나는가? GC와 관계는?
- [ ] GC 기초 이해
  - Mark-Sweep-Compact 알고리즘 종이에 그려보기
  - Minor GC(Young) vs Major GC(Old) vs Full GC 차이
  - **왜 질문:** 왜 Young 영역을 Eden + Survivor 두 개로 나눴는가? (복사 수집기의 원리)
  - **왜 질문:** 객체가 Old로 넘어가는 조건은? (`-XX:MaxTenuringThreshold`)

**참고:** "자바 성능 튜닝 이야기" 3장

### Day 4 (5/28 목) — G1 GC vs ZGC 깊이 파기

**이해 + 코드 (2.5h)**
- [ ] G1 GC 동작 원리
  - Region 기반 구조 그려보기 (2048개 region)
  - Young-only phase → Space Reclamation phase
  - Remembered Set, SATB(Snapshot-At-The-Beginning) 마킹
  - **왜 질문:** G1이 "Garbage-First"인 이유는? (가비지 비율 높은 Region부터 수집)
  - **왜 질문:** Region 크기는 어떻게 결정되나? (`-XX:G1HeapRegionSize`)
- [ ] ZGC 동작 원리
  - Colored Pointer와 Load Barrier
  - **왜 질문:** ZGC가 10ms 미만 pause를 달성하는 원리는?
  - **왜 질문:** ZGC는 왜 Generational이 아니었다가 Java 21에서 Generational로 바뀌었나?
- [ ] GC 로그 활성화 코드 (java-lab)
  - G1: `-Xlog:gc*=info:file=g1.log:time,uptime`
  - ZGC: `-XX:+UseZGC -XX:+ZGenerational -Xlog:gc*=info:file=zgc.log`
  - 같은 부하(byte[] 1만 개 생성+삭제 반복)로 두 로그 비교

### Day 5 (5/29 금) — 동시성 기초: synchronized와 volatile

**이해 + 코드 (2.5h)**
- [ ] Java Memory Model (JMM) 핵심 이해
  - **왜 질문:** 왜 스레드마다 CPU 캐시가 있어서 문제가 되는가? (가시성 문제)
  - happens-before 관계란?
  - **왜 질문:** `volatile`을 붙이면 왜 다른 스레드가 최신 값을 보는가? (메모리 배리어)
- [ ] synchronized 깊이 파기
  - Monitor Lock(모니터 락)의 구조: Entry Set → Owner → Wait Set
  - **왜 질문:** synchronized는 Heap의 객체 헤더(Mark Word)에 어떻게 저장되는가?
  - 편향 락(Biased Lock) → 경량 락(Lightweight) → 중량 락(Heavyweight) 업그레이드 과정
- [ ] 코드 실험 (java-lab)
  - 공유 카운터 `count++`를 1000 스레드로 돌려서 race condition 재현
  - synchronized로 수정 → 정확히 1000 나오는지 확인
  - volatile만 붙이면 race condition이 해결되는가? 안 되는가? **왜?**

### Day 6 (5/30 토) — AtomicInteger, LongAdder, Virtual Thread 벤치마크

**코드 + 측정 (5h)**
- [ ] Atomic 클래스 깊이 이해
  - CAS(Compare-And-Swap) 연산이란? CPU 레벨에서 어떻게 동작하는가?
  - **왜 질문:** AtomicInteger는 락을 안 쓰는데 왜 스레드 안전한가? (CAS 루프)
  - **왜 질문:** LongAdder가 AtomicLong보다 빠른 이유는? (Cell 배열 + 스트라이핑)
  - **왜 질문:** LongAdder는 정확한 합계를 항상 보장하는가?
- [ ] 벤치마크 코드 (java-lab — JMH 사용)
  - synchronized vs AtomicInteger vs LongAdder
  - 스레드 수: 1, 4, 16, 64로 각각 측정
  - 처리량(ops/sec) + 평균 지연 시간 기록
- [ ] Virtual Thread 실험
  - `Executors.newVirtualThreadPerTaskExecutor()`로 10만 개 태스크 실행
  - 일반 스레드풀(`newFixedThreadPool(200)`)과 비교
  - **왜 질문:** Virtual Thread가 빠른 게 아니라 "많이 만들 수 있는" 거다. 왜?
  - **왜 질문:** Virtual Thread에서 synchronized를 쓰면 왜 pinning이 발생하는가?
  - **왜 질문:** `ReentrantLock`은 pinning이 안 생기는 이유는?
- [ ] `jstat`, `jmap -histo` 결과 캡처

### Day 7 (5/31 일) — Gradle 기초 + 정리

**오전 (2.5h)**
- [ ] Gradle 빌드 라이프사이클
  - 초기화(settings.gradle) → 설정(build.gradle) → 실행(태스크)
  - **왜 질문:** Maven은 XML인데 Gradle은 왜 코드(Groovy/Kotlin)인가? 장단점은?
  - **왜 질문:** `implementation`과 `api`의 차이는? 왜 `compile`이 deprecated 됐나?
- [ ] trader-bot 멀티모듈 구조 분석
  - `settings.gradle` 읽기 → 모듈 간 의존 관계 파악
  - `./gradlew dependencies --configuration compileClasspath` 실행 → 트리 읽기
- [ ] `./gradlew build --scan` 한 번 돌려보기 → 빌드 타임 분석
- [ ] **Week 1 블로그 소재 정리** (저녁에 초안)
- 오후: 휴식

**Week 1 PR:** java-lab에 OOM 3종 + 동시성 비교 + JMH 벤치마크 머지

---

## Week 2 — Spring IoC/AOP 내부 동작

### Day 8 (6/1 월) — IoC 컨테이너가 하는 일

**이해 (2h)**
- Spring IoC 컨테이너 전체 흐름 그려보기
  1. `@ComponentScan`으로 클래스 탐색
  2. `BeanDefinition` 생성 (메타데이터)
  3. `BeanFactory`가 인스턴스 생성
  4. 의존성 주입 (생성자/세터/필드)
  5. `BeanPostProcessor` 실행 (프록시 생성 등)
  6. `@PostConstruct` 호출
- **왜 질문:** BeanFactory와 ApplicationContext의 차이는? 왜 ApplicationContext를 쓰는가?
- **왜 질문:** 생성자 주입을 권장하는 진짜 이유는? (불변성 + 순환참조 방지 + 테스트 용이)
- **왜 질문:** `@Autowired`를 필드에 붙이면 왜 리플렉션이 필요한가?
- Spring 소스 코드 30분 읽기: `DefaultListableBeanFactory.preInstantiateSingletons()`

**참고:** 김영한 "스프링 핵심 원리 기본편" 섹션 1~3

### Day 9 (6/2 화) — 빈 스코프와 라이프사이클

**이해 + 코드 (2.5h)**
- [ ] 빈 스코프 5가지 이해
  - singleton / prototype / request / session / application
  - **왜 질문:** 기본이 왜 singleton인가? 매번 new하면 안 되는 이유는?
  - **왜 질문:** singleton 빈에 prototype 빈을 주입하면 왜 문제인가? → `ObjectProvider`로 해결
- [ ] 빈 라이프사이클 실험 (java-lab 또는 trader-bot)
  - `@PostConstruct` → `InitializingBean` → `@Bean(initMethod)` 호출 순서 확인
  - `@PreDestroy` → `DisposableBean` 호출 순서 확인
  - **왜 질문:** 왜 이렇게 여러 가지 초기화 방법이 있는가? (역사적 이유)
- [ ] 순환참조 실험
  - A → B → A 순환참조 만들어서 에러 확인
  - **왜 질문:** 생성자 주입에서는 순환참조가 왜 불가능한가?
  - **왜 질문:** 세터 주입에서는 왜 가능했는가? (3단계 캐시 — singletonFactories)
  - Spring Boot 2.6+에서 기본적으로 순환참조를 금지한 이유는?

### Day 10 (6/3 수) — 미니 IoC 컨테이너 직접 구현

**코드 (2.5h) — java-lab**
- [ ] 직접 만드는 IoC 컨테이너 (100~150줄)
  1. `@MyComponent` 어노테이션 정의
  2. `@MyAutowired` 어노테이션 정의
  3. 패키지 스캔 → `@MyComponent` 붙은 클래스 찾기 (리플렉션)
  4. 인스턴스 생성 → 싱글톤 맵에 저장
  5. `@MyAutowired` 필드에 의존성 주입
  6. `getBean(Class<T>)` 메서드
- [ ] 만들면서 느끼는 것들 메모
  - **왜 질문:** 인터페이스에 구현체가 2개면 어떻게 구분하지? → `@Qualifier`가 왜 필요한지 체감
  - **왜 질문:** 생성 순서를 어떻게 결정하지? → 의존성 그래프 + 위상 정렬
  - Spring은 이걸 어떻게 해결하는가? → `DefaultListableBeanFactory` 다시 보기

### Day 11 (6/4 목) — AOP 내부 동작 원리

**이해 + 코드 (2.5h)**
- [ ] AOP 프록시 원리 이해
  - JDK Dynamic Proxy vs CGLIB Proxy
  - **왜 질문:** JDK Proxy는 인터페이스가 필요한데 CGLIB은 왜 필요 없는가? (상속 기반)
  - **왜 질문:** Spring Boot는 왜 기본으로 CGLIB을 쓰는가? (`proxyTargetClass=true`)
  - **왜 질문:** `final` 클래스/메서드에 AOP가 안 걸리는 이유는?
- [ ] 프록시 객체 확인 실험 (trader-bot)
  - `@Service` 빈을 주입받아서 `getClass()` 출력 → `$$SpringCGLIB$$` 확인
  - 디버거로 프록시 내부 구조 탐색
- [ ] Filter vs Interceptor vs AOP 비교
  - 각각의 실행 시점 다이어그램 그리기
  - **왜 질문:** Filter는 Servlet 스펙, Interceptor는 Spring MVC, AOP는 Spring Core. 왜 3개나?
  - 언제 뭘 써야 하는가? (인증: Filter, 로깅: AOP, 권한: Interceptor or AOP)

### Day 12 (6/5 금) — @TradeLog AOP 구현

**코드 (2.5h) — trader-bot**
- [ ] `@TradeLog` 커스텀 어노테이션 작성
- [ ] `@Around` 어드바이스 구현
  - 메서드 진입/종료 시간, 소요 시간 기록
  - 파라미터 로깅 (API 키/비밀번호는 마스킹)
  - 예외 발생 시 에러 로그 + 원래 예외 재throw
- [ ] 적용 대상: application 레이어 Handler 메서드
- [ ] AOP 적용 전/후 메서드 호출 비용 측정
  - **왜 질문:** AOP 프록시가 성능에 미치는 영향은 실제로 얼마나 되나?

### Day 13 (6/6 토) — Auto Configuration + MDC traceId

**이해 + 코드 (5h)**

오전 (2.5h) — Auto Configuration 분석
- [ ] `spring-boot-autoconfigure` jar 열기
  - `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 파일 읽기
  - `DataSourceAutoConfiguration` 소스 코드 따라가기
  - **왜 질문:** `@ConditionalOnClass`, `@ConditionalOnMissingBean`은 무슨 원리인가?
  - **왜 질문:** 왜 "자동" 설정인데 내가 직접 빈을 등록하면 자동 설정이 꺼지는가?
- [ ] `--debug`로 실행 → Auto Configuration Report 분석
  - Positive matches / Negative matches 구분
  - trader-bot에서 실제로 적용된 Auto Configuration 목록 정리

오후 (2.5h) — MDC traceId 적용
- [ ] MDC(Mapped Diagnostic Context) 이해
  - **왜 질문:** 로그에 traceId가 왜 필요한가? 멀티스레드 환경에서 요청 추적
  - **왜 질문:** MDC는 ThreadLocal 기반인데 Virtual Thread에서는?
- [ ] Filter에서 `MDC.put("traceId", UUID)` 설정
- [ ] logback 패턴에 `%X{traceId}` 추가
- [ ] 에러 응답에 traceId 포함 → 로그 추적 시연

### Day 14 (6/7 일) — Week 2 정리 + 블로그

**오전 (2.5h)**
- [ ] 미니 IoC vs Spring 차이점 표로 정리
- [ ] AOP 프록시 동작 다이어그램 그리기
- [ ] **블로그 작성:** "Spring DI는 어떻게 동작할까? 100줄로 직접 만들어봤다"
- [ ] 다음 주 예습: "이펙티브 자바" Item 69~77 훑어보기
- 오후: 휴식

**Week 2 PR:** java-lab에 미니 IoC 컨테이너 + trader-bot에 @TradeLog AOP + MDC traceId

---

## Week 3 — 예외 처리 전략 + Bean Validation

### Day 15 (6/8 월) — 자바 예외 체계 깊이 이해

**이해 (2h)**
- Throwable 계층 구조 그려보기
  - `Error` (OutOfMemoryError 등 — 잡지 않음)
  - `Exception`
    - Checked Exception (`IOException`, `SQLException`)
    - Unchecked Exception = RuntimeException (`NullPointerException`, `IllegalArgumentException`)
- **왜 질문:** Checked vs Unchecked, 자바는 왜 둘 다 만들었나?
- **왜 질문:** Spring은 왜 대부분 Unchecked를 쓰는가? (선언부 오염, 트랜잭션 롤백 기본 정책)
- **왜 질문:** `catch (Exception e)` vs `catch (RuntimeException e)` — 왜 넓게 잡으면 위험한가?
- **왜 질문:** try-with-resources가 왜 필요한가? `finally` 블록의 문제점은?
- "이펙티브 자바" Item 69~77 핵심만 메모

### Day 16 (6/9 화) — 도메인 예외 계층 설계

**코드 (2.5h) — trader-bot**
- [ ] 예외 계층 설계 (기존 `ApplicationException` 기반 확장)
  ```
  ApplicationException (RuntimeException)
   ├── DomainException (비즈니스 규칙 위반)
   │    ├── InsufficientBalanceException
   │    ├── InvalidOrderException
   │    └── DailyLimitExceededException
   ├── ExternalApiException (외부 시스템 장애)
   │    └── KisResponseException (기존)
   └── NotFoundException
  ```
- [ ] 각 예외에 `ErrorStatus` 매핑
  - **왜 질문:** 예외 클래스마다 HTTP 상태 코드를 갖게 하는 게 맞나? 도메인이 HTTP를 알아야 하나?
  - **왜 질문:** 예외 메시지에 사용자 입력값을 그대로 넣으면 왜 위험한가? (로그 인젝션, 민감정보 노출)
- [ ] 기존 코드에서 예외를 `throw`하는 곳 찾아서 새 계층으로 교체

### Day 17 (6/10 수) — @RestControllerAdvice 깊이 파기

**이해 + 코드 (2.5h)**
- [ ] `@ControllerAdvice` 동작 원리
  - **왜 질문:** 예외가 Controller → DispatcherServlet → HandlerExceptionResolver → @ControllerAdvice 순서로 전파되는 이유는?
  - **왜 질문:** `@ExceptionHandler`의 우선순위는 어떻게 결정되는가? (구체적 예외 > 부모 예외)
  - **왜 질문:** Filter에서 터진 예외는 `@ControllerAdvice`가 잡는가? 못 잡는가? 왜?
- [ ] 기존 `ExceptionController` 리팩토링
  - `ApplicationException` 핸들러: 예외의 ErrorStatus에서 code/httpStatus 추출
  - `MethodArgumentNotValidException` 핸들러: Bean Validation 에러 상세 응답
  - `Exception` 핸들러 (최후의 보루): 500 + 로그에 stack trace
  - 모든 에러 응답에 `traceId` 포함
- [ ] 컨트롤러에서 try-catch 전부 제거 → 예외는 throw만

### Day 18 (6/11 목) — Bean Validation 깊이 이해

**이해 + 코드 (2.5h)**
- [ ] Bean Validation 동작 원리
  - **왜 질문:** `@Valid`와 `@Validated`의 차이는? 왜 둘 다 있는가?
  - **왜 질문:** Validation은 어느 레이어에서 해야 하는가? Controller? Service? Domain?
    - 입력 형식 검증 → Controller (Bean Validation)
    - 비즈니스 규칙 검증 → Domain (도메인 메서드 내)
  - **왜 질문:** `@NotNull` vs `@NotBlank` vs `@NotEmpty` 차이는?
- [ ] Request DTO에 Validation 적용
  - 주문 요청: 수량 > 0, 종목코드 6자리, 가격 > 0
  - 그룹(`groups`)으로 생성/수정 시 다른 규칙 적용
- [ ] 커스텀 Validator 작성
  - `@ValidStockCode` — 한국 종목코드 형식 검증 (6자리 숫자)
  - `ConstraintValidator` 인터페이스 구현
  - **왜 질문:** 커스텀 Validator가 빈으로 등록되어야 하는 이유는? (DI 받기 위해)

### Day 19 (6/12 금) — 에러 로깅 전략 + Logback 설정

**이해 + 코드 (2.5h)**
- [ ] 로깅 프레임워크 구조
  - SLF4J(Facade) → Logback(Implementation) 관계
  - **왜 질문:** 왜 직접 Logback을 쓰지 않고 SLF4J를 통하는가? (추상화, 교체 용이)
  - **왜 질문:** `log.info("user: " + userId)` vs `log.info("user: {}", userId)` — 왜 후자가 나은가? (문자열 결합 비용)
- [ ] 로그 레벨 전략 정리
  - ERROR: 즉시 대응 필요 (외부 API 장애, 데이터 불일치)
  - WARN: 주의 필요 (재시도 성공, 잔고 부족 등)
  - INFO: 주요 흐름 (요청 수신, KIS API 호출, 주문 체결)
  - DEBUG: 디버깅용 (파싱 상세, 조건 분기)
- [ ] logback-spring.xml 설정
  - 프로필별 로그 레벨 분리 (local: DEBUG, prod: INFO)
  - JSON 포맷 로그 (운영 환경용)
  - 파일 롤링 정책 (일별, 30일 보관)
- [ ] 민감정보 마스킹 확인 — 계좌번호, API 키 로그 출력 안 되는지

### Day 20 (6/13 토) — 테스트로 예외 처리 검증 + 측정

**코드 + 측정 (5h)**

오전 (3h) — 예외 처리 테스트
- [ ] 단위 테스트 작성
  - 각 도메인 예외가 올바른 ErrorStatus를 반환하는지
  - ExceptionController가 예외별로 올바른 HTTP 상태/응답 구조 반환하는지 (`MockMvc`)
  - Bean Validation 실패 시 응답 구조 검증
  - traceId가 에러 응답에 포함되는지
- [ ] 통합 테스트
  - 실제 API 호출 → 잘못된 파라미터 → Validation 에러 응답 확인
  - 존재하지 않는 리소스 → NotFoundException → 404 확인

오후 (2h) — 측정 + 정리
- [ ] 에러 발생 시 traceId로 로그 추적 시연 (스크린샷)
- [ ] `--debug` Auto Config Report에서 Validation 관련 자동 설정 확인
- [ ] Week 3 학습 노트 정리

### Day 21 (6/14 일) — 블로그 + 다음 주 예습

**오전 (2.5h)**
- [ ] **블로그 작성:** "Spring Boot 예외 처리, 제대로 설계하기 — @ControllerAdvice부터 MDC까지"
- [ ] Spring Security 공식 문서 Architecture 섹션 미리 읽기
  - SecurityFilterChain 개념만 훑기
- 오후: 휴식

**Week 3 PR:** 도메인 예외 계층 + ExceptionController 정비 + Bean Validation + 테스트

---

## Week 4 — Spring Security + 종합

### Day 22 (6/15 월) — Spring Security 아키텍처 이해

**이해 (2h)**
- Spring Security 전체 흐름 그려보기
  1. 요청 → `DelegatingFilterProxy` → `FilterChainProxy`
  2. `SecurityFilterChain` (15~20개 필터 체인)
  3. 핵심 필터들:
     - `SecurityContextPersistenceFilter` → 인증 정보 로드
     - `UsernamePasswordAuthenticationFilter` → 폼 로그인
     - `BasicAuthenticationFilter` → Basic 인증
     - `ExceptionTranslationFilter` → 인증/인가 예외 처리
     - `FilterSecurityInterceptor` → 최종 인가 결정
- **왜 질문:** 왜 Servlet Filter 체인을 쓰는가? Spring MVC Interceptor가 아니라?
- **왜 질문:** `SecurityContext`는 어디에 저장되나? (SecurityContextHolder → ThreadLocal)
- **왜 질문:** JWT 인증에서는 세션이 필요 없는데, Spring Security가 기본으로 세션을 만드는 이유는?

**참고:** 인프런 정수원 "스프링 시큐리티" 또는 공식 문서 Architecture 섹션

### Day 23 (6/16 화) — JWT 발급 구현

**코드 (2.5h) — trader-bot**
- [ ] 의존성 추가: `jjwt-api`, `jjwt-impl`, `jjwt-jackson`
- [ ] JWT 토큰 구조 이해
  - Header(알고리즘) + Payload(claims) + Signature
  - **왜 질문:** JWT는 암호화가 아니라 서명이다. Base64로 디코딩하면 내용이 다 보인다. 그런데 왜 안전한가?
  - **왜 질문:** Access Token과 Refresh Token을 분리하는 이유는?
  - **왜 질문:** Refresh Token은 왜 DB/Redis에 저장하는가? JWT가 stateless라며?
- [ ] `JwtProvider` 구현
  - `generateAccessToken(userId)` — 만료 30분
  - `generateRefreshToken(userId)` — 만료 7일
  - `validateToken(token)` → Claims 추출
  - 서명 키는 환경변수로 (`JWT_SECRET`)
- [ ] Refresh Token Redis 저장 (`jwt:refresh:{userId}`)

### Day 24 (6/17 수) — JWT 검증 필터 + SecurityFilterChain

**코드 (2.5h) — trader-bot**
- [ ] `JwtAuthenticationFilter extends OncePerRequestFilter` 구현
  - `Authorization: Bearer {token}` 헤더에서 토큰 추출
  - 토큰 검증 → `UsernamePasswordAuthenticationToken` 생성
  - `SecurityContextHolder`에 인증 정보 저장
  - **왜 질문:** `OncePerRequestFilter`는 왜 필요한가? 일반 Filter와 차이는?
  - **왜 질문:** 포워드/리다이렉트 시 필터가 두 번 타는 문제
- [ ] `SecurityFilterChain` 설정
  - 세션 정책: `STATELESS` (JWT니까)
  - CSRF 비활성화 (왜 비활성화해도 되는가? — JWT는 쿠키가 아니라 헤더이므로)
  - URL별 권한: `/api/auth/**` → permitAll, 나머지 → authenticated
  - `JwtAuthenticationFilter`를 `UsernamePasswordAuthenticationFilter` 앞에 등록

### Day 25 (6/18 목) — 로그인/회원가입 API + Refresh

**코드 (2.5h) — trader-bot**
- [ ] 회원가입 API
  - 비밀번호 `BCryptPasswordEncoder`로 해싱
  - **왜 질문:** 왜 SHA-256이 아니라 BCrypt인가? (Salt + 의도적 느림 + cost factor)
  - **왜 질문:** Rainbow Table 공격이란?
- [ ] 로그인 API
  - 이메일 + 비밀번호 → Access Token + Refresh Token 발급
- [ ] 토큰 갱신 API
  - Refresh Token 검증 → 새 Access Token 발급
  - **Refresh Token Rotation**: 갱신 시 Refresh Token도 새로 발급 + 기존 것 폐기
  - **왜 질문:** Rotation을 안 하면 어떤 공격이 가능한가?
- [ ] 로그아웃 API
  - Redis에서 Refresh Token 삭제
  - (선택) Access Token 블랙리스트

### Day 26 (6/19 금) — @PreAuthorize + 권한 체계

**코드 (2.5h) — trader-bot**
- [ ] 역할(Role) 기반 권한
  - `ROLE_USER`, `ROLE_ADMIN`
  - **왜 질문:** `hasRole('ADMIN')`은 내부적으로 `ROLE_ADMIN`을 찾는다. 왜 접두사가 필요한가?
- [ ] `@PreAuthorize` 메서드 레벨 보안
  - 관리자만: 매매 정지/재개 API
  - 본인만: 자기 계좌 조회
  - `@PreAuthorize("#userId == authentication.principal.id")` — SpEL 표현식
  - **왜 질문:** `@PreAuthorize`는 AOP 기반이다. Week 2에서 배운 프록시가 여기서 다시 나온다
- [ ] 인증/인가 실패 처리
  - `AuthenticationEntryPoint` — 401 커스텀 응답
  - `AccessDeniedHandler` — 403 커스텀 응답

### Day 27 (6/20 토) — 종합 테스트 + 부하 측정

**코드 + 측정 (5h)**

오전 (3h) — Security 테스트
- [ ] 단위 테스트
  - JwtProvider: 토큰 생성/검증/만료 테스트
  - 만료된 토큰 → 적절한 에러 응답
  - 잘못된 서명 → 적절한 에러 응답
- [ ] 통합 테스트 (`@SpringBootTest` + `MockMvc`)
  - 인증 없이 보호된 API 호출 → 401
  - 유효한 토큰으로 호출 → 200
  - 권한 부족 → 403
  - `@WithMockUser`로 인증 상태 시뮬레이션
- [ ] Swagger에서 JWT 인증 테스트 가능하도록 설정
  - `@SecurityScheme(type = HTTP, scheme = "bearer")`

오후 (2h) — 종합 측정
- [ ] M1 전체 학습 내용이 trader-bot에 적용된 상태 점검
  - AOP 로깅 동작 확인
  - 예외 → 표준 에러 응답 + traceId 확인
  - JWT 인증 흐름 동작 확인
- [ ] k6 또는 JMeter로 인증된 API 100 RPS 부하 테스트
  - 응답 시간 p50/p95/p99 기록
  - JWT 검증이 성능에 미치는 영향 측정

### Day 28 (6/21 일) — M1 회고 + 블로그

**오전 (2.5h)**
- [ ] **블로그 작성:** "Spring Boot 백엔드, 1단계 완성 — 4주 학습 회고"
  - JVM → IoC/AOP → 예외 → Security 흐름 정리
  - 가장 인상 깊었던 "왜" 3가지
- [ ] M1 코드 셀프 리뷰 — 네이밍, 구조, 테스트 커버리지 확인
- [ ] "자바 ORM 표준 JPA 프로그래밍" 1~2장 읽기 (M2 예습)
- 오후: 휴식

**Week 4 PR:** Spring Security + JWT 인증/인가 + 테스트

---

# M2: JPA 심화 (Week 5–8)

---

## Week 5 — 영속성 컨텍스트 + 연관관계

### Day 29 (6/22 월) — 영속성 컨텍스트 개념

**이해 (2h)**
- 영속성 컨텍스트란? — 엔티티를 관리하는 1차 캐시
- 엔티티 상태 4가지 그려보기
  - **비영속 (new/transient):** `new Member()` — 아직 persist 안 함
  - **영속 (managed):** `em.persist()` 또는 `em.find()` 후
  - **준영속 (detached):** `em.detach()`, `em.clear()`, 트랜잭션 종료 후
  - **삭제 (removed):** `em.remove()`
- **왜 질문:** 영속성 컨텍스트가 왜 필요한가? 바로 DB에 쓰면 안 되나?
  - 1차 캐시로 같은 트랜잭션 내 반복 조회 최적화
  - 쓰기 지연(write-behind): flush 시점까지 SQL 모아서 실행
  - 변경 감지(dirty checking): set만 해도 update 자동 발생
  - 동일성(identity) 보장: 같은 ID → 같은 객체 참조
- **왜 질문:** `EntityManager`와 `EntityManagerFactory`의 관계는? 왜 분리?

**참고:** "자바 ORM 표준 JPA 프로그래밍" 3장

### Day 30 (6/23 화) — Dirty Checking + flush 실험

**코드 (2.5h) — trader-bot 또는 java-lab**
- [ ] Dirty Checking 확인
  - `findById()`로 엔티티 조회 → set으로 값 변경 → `save()` 안 불러도 update 발생
  - `spring.jpa.show-sql=true` + `org.hibernate.orm.jdbc.bind=trace`로 SQL 확인
  - **왜 질문:** Hibernate는 어떻게 변경을 감지하는가? (스냅샷 비교 — 최초 조회 시 복사본 저장)
  - **왜 질문:** 필드가 100개인 엔티티면 스냅샷 비교 비용이 크지 않나? (`@DynamicUpdate`는?)
- [ ] flush 타이밍 실험
  - `em.flush()` 직접 호출 vs 트랜잭션 커밋 시 자동 flush
  - JPQL 쿼리 실행 전 자동 flush 되는 것 확인
  - **왜 질문:** JPQL 전에 왜 자동 flush하는가? (DB와 영속성 컨텍스트 동기화)
  - FlushMode: `AUTO` vs `COMMIT` 차이
- [ ] 준영속 상태 실험
  - `em.detach(entity)` 후 변경 → update 안 나가는 것 확인
  - `em.clear()` 후 같은 ID 조회 → SQL 다시 나가는 것 확인 (1차 캐시 초기화)

### Day 31 (6/24 수) — merge vs persist + 1차 캐시

**코드 (2.5h)**
- [ ] `persist()` vs `merge()` 차이
  - `persist()`: 비영속 → 영속 (ID 자동 생성)
  - `merge()`: 준영속/비영속 → 영속 복사본 반환 (원본은 여전히 준영속)
  - **왜 질문:** `merge()`가 새 객체를 반환하는 이유는? 왜 원본을 영속화하지 않는가?
  - **왜 질문:** Spring Data JPA의 `save()`는 내부적으로 persist와 merge를 어떻게 구분하는가? → `SimpleJpaRepository.save()` 소스 읽기 (`isNew()` 체크)
- [ ] 1차 캐시 동작 확인
  - 같은 트랜잭션 내에서 같은 ID로 2번 `findById()` → SQL 1번만 나가는지
  - `==` 비교 → `true` (동일성 보장)
  - **왜 질문:** 1차 캐시의 수명은? (트랜잭션 or EntityManager 범위)
  - **왜 질문:** OSIV(Open Session In View)가 뭔가? 왜 Spring Boot에서 기본 true인가? 왜 논란인가?

### Day 32 (6/25 목) — 연관관계 매핑 기초

**이해 + 코드 (2.5h)**
- [ ] 연관관계 종류 이해
  - @ManyToOne (가장 중요, 가장 많이 씀)
  - @OneToMany (mappedBy)
  - @OneToOne (주의: 지연 로딩 안 먹는 경우)
  - @ManyToMany (쓰지 마라 — 왜?)
- [ ] **왜 질문 모음:**
  - 단방향 vs 양방향: 양방향은 왜 필요한가? 언제 써야 하는가?
  - `mappedBy`는 뭔가? 왜 연관관계의 "주인"을 정해야 하는가?
  - 외래키가 있는 쪽이 왜 주인인가?
  - `@JoinColumn`을 생략하면 어떤 일이 생기는가? (중간 테이블 생성)
- [ ] trader-bot 엔티티 연관관계 설계
  - User ← Account ← Order ← Trade (필요한 것만)
  - **단방향 @ManyToOne 위주로 설계** — 양방향은 진짜 필요할 때만
  - cascade, orphanRemoval 결정

### Day 33 (6/26 금) — 지연 로딩 vs 즉시 로딩

**이해 + 코드 (2.5h)**
- [ ] FetchType.LAZY vs EAGER
  - **왜 질문:** 기본이 왜 `@ManyToOne`은 EAGER이고 `@OneToMany`는 LAZY인가?
  - **왜 질문:** 왜 실무에서는 전부 LAZY로 하라고 하는가?
  - **왜 질문:** LAZY 로딩은 어떻게 구현되는가? (프록시 객체 — Hibernate가 엔티티를 상속한 프록시 생성)
- [ ] 프록시 동작 확인
  - LAZY 연관관계 접근 시 SQL 발생하는 시점 확인
  - `Hibernate.isInitialized()` / `PersistenceUnitUtil.isLoaded()` 사용
  - **왜 질문:** 트랜잭션 밖에서 LAZY 접근 → `LazyInitializationException` — 왜?
  - **왜 질문:** OSIV를 끄면 이 예외가 자주 터지는 이유는?
- [ ] 프록시 vs 실제 객체 `getClass()` 비교 → `$$HibernateProxy$$` 확인
  - **왜 질문:** `entity1.getClass() == entity2.getClass()`가 `false`일 수 있다. 왜? → `instanceof` 사용 권장

### Day 34 (6/27 토) — N+1 문제 재현 + 4가지 해결법

**코드 + 측정 (5h)**

오전 (3h) — N+1 재현 + 해결
- [ ] 시드 데이터 준비: User 100명, 각각 Order 10건
- [ ] N+1 재현: `findAll()` → User 100명 조회 → 각 User의 Orders 조회 = SQL 101번
  - `show-sql=true`로 SQL 카운트 → 충격 체감
- [ ] 해결법 4가지 (각각 별도 커밋)
  1. **JPQL fetch join:** `SELECT u FROM User u JOIN FETCH u.orders`
     - **왜 질문:** fetch join은 inner join인가 left join인가? 주의할 점은?
  2. **@EntityGraph:** `@EntityGraph(attributePaths = {"orders"})`
     - **왜 질문:** EntityGraph와 fetch join의 차이는?
  3. **@BatchSize(size = 100):** IN 절로 모아서 조회
     - **왜 질문:** 배치 사이즈가 너무 크면? 너무 작으면?
  4. **QueryDSL fetchJoin:** `.join(user.orders).fetchJoin()`

오후 (2h) — 카르테시안 곱 + 측정
- [ ] **카르테시안 곱 문제 재현**
  - User → Orders + User → Addresses 동시 fetch join → 데이터 뻥튀기
  - **왜 질문:** 왜 컬렉션 2개를 동시에 fetch join하면 안 되는가?
  - 해결: `default_batch_fetch_size` + `Set` 사용 또는 쿼리 분리
- [ ] 각 방법별 SQL 발행 수 + 응답 시간 표로 정리
- [ ] EXPLAIN ANALYZE로 실행 계획 비교

### Day 35 (6/28 일) — 블로그 + Week 5 정리

**오전 (2.5h)**
- [ ] **블로그 작성:** "N+1 문제 4가지 해법, SQL 로그로 직접 비교"
- [ ] 영속성 컨텍스트 상태 전이 다이어그램 최종 정리
- [ ] "자바 ORM 표준 JPA 프로그래밍" 9~10장 (JPQL, QueryDSL) 훑기
- 오후: 휴식

**Week 5 PR:** 엔티티 연관관계 설계 + N+1 해결 + 테스트

---

## Week 6 — QueryDSL + 트랜잭션 심화

### Day 36 (6/29 월) — JPQL 깊이 이해

**이해 + 코드 (2.5h)**
- [ ] JPQL vs SQL 차이
  - JPQL은 엔티티 대상, SQL은 테이블 대상
  - **왜 질문:** JPQL이 왜 필요한가? 그냥 SQL 쓰면 안 되나? (DB 벤더 독립, 엔티티 그래프 탐색)
- [ ] JPQL 주요 문법 실습
  - 조인: `JOIN`, `LEFT JOIN`, `JOIN FETCH`
  - 서브쿼리: WHERE 절, SELECT 절
  - 프로젝션: DTO 직접 조회 (`new com.xxx.Dto(m.name, m.age)`)
  - 벌크 연산: `UPDATE ... SET ... WHERE ...`
  - **왜 질문:** 벌크 연산 후 영속성 컨텍스트를 왜 초기화해야 하는가? (`em.clear()`)
- [ ] 네이티브 쿼리는 언제 쓰는가?
  - PostgreSQL 전용 기능 (JSONB, Window Function 등)
  - **왜 질문:** 네이티브 쿼리의 단점은? (DB 벤더 종속, 타입 안전성 없음)

### Day 37 (6/30 화) — QueryDSL 설정 + 기본 쿼리

**코드 (2.5h) — trader-bot**
- [ ] Gradle에 QueryDSL 설정
  - `annotationProcessor`로 Q클래스 생성
  - Q클래스 생성 경로 확인 (`build/generated/sources/annotationProcessor`)
  - **왜 질문:** Q클래스는 왜 필요한가? 컴파일 타임 타입 체크를 위해
- [ ] 기본 쿼리 작성
  - `JPAQueryFactory` 빈 등록
  - `selectFrom()`, `where()`, `orderBy()`, `fetch()`
  - **왜 질문:** QueryDSL이 JPQL보다 나은 점은? (컴파일 타임 검증, 동적 쿼리, 자동 완성)
  - **왜 질문:** QueryDSL이 JPQL로 변환되는 과정은?
- [ ] 거래 내역 기본 조회 쿼리 작성
  - 종목별, 날짜별 조회

### Day 38 (7/1 수) — QueryDSL 동적 쿼리 + 페이지네이션

**코드 (2.5h) — trader-bot**
- [ ] 동적 검색 쿼리
  - 거래 내역 필터: 날짜 범위 + 종목 + 매수/매도 + 금액 범위
  - `BooleanBuilder` 방식 구현
  - `BooleanExpression` 메서드 분리 방식 구현
  - **왜 질문:** `BooleanExpression` 방식이 왜 더 좋은가? (재사용, 조합, null 안전)
- [ ] 페이지네이션
  - `offset()`, `limit()` 기본 방식
  - count 쿼리 분리 최적화
  - **왜 질문:** offset 페이지네이션의 문제점은? (깊은 페이지에서 느림)
  - **왜 질문:** cursor 페이지네이션(keyset)은 왜 빠른가?
- [ ] DTO 프로젝션
  - `Projections.constructor()` vs `@QueryProjection`
  - **왜 질문:** 엔티티 전체를 가져오지 않고 DTO로 가져오면 왜 성능이 좋은가?

### Day 39 (7/2 목) — 트랜잭션 전파 속성 실험

**이해 + 코드 (2.5h)**
- [ ] `@Transactional` 프록시 동작 이해
  - **왜 질문:** `@Transactional`이 AOP 프록시 기반이라는 건 Week 2 AOP와 같은 원리다. 어떻게?
  - `TransactionInterceptor` → `PlatformTransactionManager` 호출 흐름
- [ ] 전파 속성 실험 (중요한 것 위주)
  - **REQUIRED** (기본): 기존 트랜잭션 있으면 참여, 없으면 새로 생성
  - **REQUIRES_NEW**: 항상 새 트랜잭션. 기존 트랜잭션 일시 중단
    - 용도: 주문 실패해도 로그는 남기고 싶을 때
  - **NESTED**: Savepoint 사용. 내부 롤백해도 외부는 유지
    - **왜 질문:** NESTED와 REQUIRES_NEW의 차이는? (NESTED는 같은 물리 트랜잭션)
  - **SUPPORTS**: 트랜잭션 있으면 참여, 없으면 없이 실행
  - **NOT_SUPPORTED**: 트랜잭션 없이 실행 (있으면 일시 중단)
- [ ] 각 속성 동작을 로그로 확인 (`logging.level.org.springframework.transaction=TRACE`)

### Day 40 (7/3 금) — 자기호출 함정 + readOnly

**코드 (2.5h)**
- [ ] **자기호출(self-invocation) 함정** 재현
  - 같은 클래스 내에서 `this.method()` 호출 → `@Transactional` 안 먹힘
  - **왜 질문:** 왜 안 먹히나? 프록시를 거치지 않고 직접 호출하니까!
  - 해결법 3가지:
    1. 별도 클래스로 분리 (가장 깔끔)
    2. `AopContext.currentProxy()` (비추)
    3. `ApplicationContext.getBean()` (비추)
  - **왜 질문:** 해결법 2, 3이 왜 비추인가?
- [ ] `@Transactional(readOnly = true)` 효과
  - **왜 질문:** readOnly가 왜 성능에 좋은가?
    - Hibernate: 스냅샷 저장 안 함 → 메모리 절약 + dirty checking 스킵
    - DB: 일부 DB에서 읽기 전용 최적화
  - **왜 질문:** readOnly 트랜잭션에서 save()를 호출하면 어떻게 되나?
- [ ] 트랜잭션 격리 수준 간단 실험
  - PostgreSQL의 기본 격리 수준은? (Read Committed)
  - **왜 질문:** Repeatable Read에서 Phantom Read가 발생하는가? PostgreSQL에서는? (MVCC)

### Day 41 (7/4 토) — 낙관적/비관적 락 + Auditing

**코드 + 측정 (5h)**

오전 (3h) — 락 실험
- [ ] 낙관적 락 (`@Version`) 적용
  - 엔티티에 `@Version Long version` 추가
  - 동시 수정 시 `OptimisticLockException` 재현
  - **왜 질문:** 낙관적 락이 "낙관적"인 이유는? (충돌이 적을 거라 가정 → 커밋 시점에 검증)
  - `@Retryable`로 재시도 로직 추가
- [ ] 비관적 락 실험
  - `@Lock(LockModeType.PESSIMISTIC_WRITE)` → `SELECT ... FOR UPDATE`
  - **왜 질문:** 비관적 락이 "비관적"인 이유는? (충돌 가능성 높다고 가정 → 먼저 락 획득)
  - **왜 질문:** 어떤 상황에서 낙관적, 어떤 상황에서 비관적을 쓰는가?
- [ ] 동시 요청 테스트: 같은 주문을 2개 스레드가 동시에 수정

오후 (2h) — JPA Auditing + 테스트
- [ ] `@EnableJpaAuditing` + BaseEntity
  - `createdAt`, `updatedAt`, `createdBy`, `updatedBy`
  - `AuditorAware` 구현 (SecurityContext에서 현재 사용자)
  - **왜 질문:** `@PrePersist`, `@PreUpdate` 콜백과 Auditing의 관계는?
- [ ] Soft Delete (선택)
  - `@SQLDelete(sql = "UPDATE ... SET deleted = true ...")`
  - `@Where(clause = "deleted = false")`
  - **왜 질문:** Hard Delete 대신 Soft Delete를 쓰는 이유는? 단점은?
- [ ] Week 6 테스트 코드 작성
  - QueryDSL 동적 쿼리 결과 검증
  - 낙관적 락 충돌 시나리오 테스트
  - Auditing 필드 자동 채워지는지 테스트

### Day 42 (7/5 일) — 블로그 + Week 6 정리

**오전 (2.5h)**
- [ ] **블로그 작성:** "트랜잭션 전파 + 자기호출 함정 — 코드로 재현하고 이해하기"
- [ ] 또는: "낙관적 vs 비관적 락, 동시 요청으로 직접 비교"
- [ ] Week 7 예습: "자바 ORM 표준 JPA 프로그래밍" 관련 챕터 훑기
- 오후: 휴식

**Week 6 PR:** QueryDSL 동적 쿼리 + 트랜잭션 전파 실험 + 낙관적 락 + Auditing

---

## Week 7 — 성능 최적화 + 내부 동작

### Day 43 (7/6 월) — JPA 성능 문제 패턴 정리

**이해 (2h)**
- JPA에서 성능 문제가 생기는 대표적 원인 5가지
  1. N+1 (Week 5에서 해결)
  2. 불필요한 컬럼 전체 조회 (SELECT * 효과)
  3. 변경 감지의 비용 (대량 데이터)
  4. OSIV로 인한 커넥션 점유
  5. 잘못된 ID 전략 (`IDENTITY` vs `SEQUENCE` vs `TABLE`)
- **왜 질문:** `GenerationType.IDENTITY`는 왜 배치 INSERT가 안 되는가?
  - INSERT 해야 ID를 알 수 있음 → 쓰기 지연 불가
  - **왜 질문:** `SEQUENCE` 전략은 어떻게 배치가 되는가? (allocationSize로 미리 확보)
- **왜 질문:** OSIV를 끄면 어떤 문제가 생기고, 어떻게 해결하는가?
  - 트랜잭션 밖에서 LAZY 로딩 불가 → 서비스에서 필요한 데이터 미리 로딩
  - `spring.jpa.open-in-view=false` 설정 후 테스트

### Day 44 (7/7 화) — 벌크 연산 + DTO 조회 최적화

**코드 (2.5h) — trader-bot**
- [ ] 벌크 연산 최적화
  - JPQL 벌크 UPDATE로 대량 상태 변경
  - `em.clear()` 후 조회 → 영속성 컨텍스트 동기화
  - **왜 질문:** `@Modifying(clearAutomatically = true)`는 왜 필요한가?
  - Spring Data JPA의 `@Modifying` + `@Query` 사용
- [ ] DTO 직접 조회로 성능 개선
  - 엔티티 조회 vs DTO 프로젝션 비교
  - **왜 질문:** 엔티티를 조회하면 영속성 컨텍스트에 올라가는데, DTO는 안 올라간다. 왜 이게 중요한가?
  - 인터페이스 기반 프로젝션 (`Closed Projection`) 실험
- [ ] `@BatchSize` 글로벌 설정
  - `spring.jpa.properties.hibernate.default_batch_fetch_size=100`
  - 효과 측정 (SQL 수 비교)

### Day 45 (7/8 수) — Hibernate 2차 캐시

**이해 + 코드 (2.5h)**
- [ ] 1차 캐시 vs 2차 캐시 차이
  - 1차 캐시: EntityManager 범위 (트랜잭션)
  - 2차 캐시: SessionFactory 범위 (애플리케이션 전체)
  - **왜 질문:** 2차 캐시는 왜 기본 비활성인가? (동시성 이슈, 데이터 정합성)
  - **왜 질문:** 어떤 데이터에 2차 캐시를 쓰면 좋은가? (변경 적고 조회 많은 데이터 — 종목 마스터 등)
- [ ] 2차 캐시 적용 (선택)
  - `@Cacheable` 엔티티에 적용
  - Ehcache 또는 Caffeine 연동
  - 캐시 hit/miss 로그 확인
- [ ] Spring Cache vs Hibernate 2차 캐시 차이
  - **왜 질문:** 둘 다 캐시인데 왜 다른가? (레이어가 다름 — 애플리케이션 vs ORM)

### Day 46 (7/9 목) — Spring Data JPA 내부 동작

**이해 + 코드 (2.5h)**
- [ ] Spring Data JPA가 인터페이스에서 구현체를 만드는 원리
  - `JpaRepositoryFactoryBean` → `SimpleJpaRepository` 프록시 생성
  - **왜 질문:** 인터페이스만 선언했는데 어떻게 구현체가 생기는가? (JDK Dynamic Proxy + 메서드 이름 파싱)
  - **왜 질문:** `findByNameAndAge`같은 메서드명이 어떻게 JPQL로 변환되는가? → `PartTree` 클래스
- [ ] `SimpleJpaRepository` 소스 읽기 (30분)
  - `save()` — `isNew()` 판단 → persist or merge
  - `findById()` — `em.find()` 호출
  - `findAll()` — JPQL `SELECT x FROM Entity x`
  - **왜 질문:** `save()`에서 `isNew()`는 어떻게 판단하는가? (ID가 null이면 new, `Persistable` 인터페이스)
- [ ] 커스텀 Repository 구현
  - `TradeRepositoryCustom` + `TradeRepositoryCustomImpl`
  - **왜 질문:** 왜 이름 규칙을 지켜야 하는가? (`Impl` 접미사)

### Day 47 (7/10 금) — OSIV + 커넥션 풀

**이해 + 코드 (2.5h)**
- [ ] OSIV(Open Session In View) 깊이 이해
  - OSIV ON: 요청 시작~응답 끝까지 영속성 컨텍스트 유지
  - OSIV OFF: 트랜잭션 범위까지만
  - **왜 질문:** OSIV ON이면 편하지만 왜 위험한가? (DB 커넥션 오래 점유 → 커넥션 풀 고갈)
  - **왜 질문:** API 서버에서는 왜 OSIV OFF를 권장하는가?
- [ ] `spring.jpa.open-in-view=false` 설정 후 테스트
  - `LazyInitializationException` 발생 지점 찾아서 fetch join으로 해결
- [ ] HikariCP 커넥션 풀 이해
  - `maximum-pool-size`, `minimum-idle`, `connection-timeout`
  - **왜 질문:** 커넥션 풀이 왜 필요한가? (DB 커넥션 생성 비용이 비쌈)
  - **왜 질문:** 풀 크기를 어떻게 결정하는가? (스레드 수 + 동시 쿼리 수)
  - HikariCP 메트릭 확인 (active, idle, waiting)

### Day 48 (7/11 토) — 종합 PR + 부하 테스트

**코드 + 측정 (5h)**

오전 (3h) — 종합 코드
- [ ] M2 학습 내용을 trader-bot에 통합
  - 모든 엔티티: LAZY 로딩 + fetch join 최적화
  - 동적 검색: QueryDSL
  - 변경이 필요 없는 조회: `readOnly = true`
  - Auditing: BaseEntity 적용
  - 낙관적 락: 동시성 필요한 엔티티
  - OSIV OFF + 서비스에서 필요한 데이터 로딩
- [ ] 테스트 코드 보강
  - Repository 레이어 테스트 (`@DataJpaTest`)
  - 동시성 테스트 (낙관적 락 충돌)

오후 (2h) — 부하 테스트 + 측정
- [ ] k6로 주요 API 부하 테스트 (100~500 RPS)
  - 응답 시간 p50/p95/p99
  - SQL 발행 수 확인
  - 커넥션 풀 사용량 모니터링
- [ ] EXPLAIN ANALYZE로 주요 쿼리 실행 계획 확인
- [ ] 성능 개선 전후 비교 표 작성

### Day 49 (7/12 일) — M2 회고 + 블로그

**오전 (2.5h)**
- [ ] **블로그 작성:** "JPA 4주 심화 — 내가 몰랐던 것들"
  - 가장 충격적이었던 동작 3가지
  - 성능 측정 결과
- [ ] 1단계 전체 (M1+M2) 회고
  - 8주간 만든 PR 목록
  - "왜"에 대답할 수 있게 된 것들 리스트
  - 아직 모호한 것들 → 2단계에서 더 깊이
- 오후: 휴식

**Week 7 PR:** JPA 성능 최적화 + OSIV OFF + 벌크 연산 + 테스트

---

## Week 8 — 1단계 종합 정리 + 졸업 과제

### Day 50~51 (7/13~14 월~화) — 종합 과제: "거래 주문 처리 API"

학습한 모든 것을 하나의 API에 통합:

- [ ] JWT 인증 필수
- [ ] Bean Validation으로 입력 검증
- [ ] 도메인 예외 → `@ControllerAdvice` → 표준 에러 응답 + traceId
- [ ] `@TradeLog` AOP 로깅
- [ ] 트랜잭션: 주문 저장은 메인, 외부 알림은 REQUIRES_NEW
- [ ] 낙관적 락으로 동시 주문 충돌 처리
- [ ] N+1 없이 관련 데이터 fetch join 조회
- [ ] QueryDSL 동적 검색 (거래 내역 필터)
- [ ] JPA Auditing createdBy 자동 기록

### Day 52~53 (7/15~16 수~목) — 테스트 + 문서화

- [ ] 단위 테스트: 도메인 로직, 예외 케이스
- [ ] 통합 테스트: `@SpringBootTest` + Testcontainers PostgreSQL
  - 성공 시나리오 + 실패 시나리오 (잔고 부족, 한도 초과, 토큰 만료, 동시 수정)
- [ ] Swagger 문서 완성: `@Operation`, `@Schema`, JWT 인증 설정

### Day 54 (7/17 금) — 부하 테스트 + 프로파일링

- [ ] k6로 100 RPS 부하 테스트
- [ ] SQL 쿼리 수 최종 확인 (N+1 없음)
- [ ] JVM 메모리/CPU 프로파일 (VisualVM 또는 IntelliJ Profiler)
- [ ] 병목 지점 식별 + 가능하면 개선

### Day 55 (7/18 토) — 셀프 코드 리뷰 + 리팩토링

- [ ] 8주간 작성한 모든 PR 셀프 리뷰
- [ ] 네이밍, 메서드 길이, 클래스 책임 점검
- [ ] 테스트 커버리지 확인 (80% 이상)
- [ ] 리팩토링 필요한 곳 정리 + 수정

### Day 56 (7/19 일) — 1단계 졸업

**오전 (2.5h)**
- [ ] **블로그 작성:** "8주 학습 회고 — Java/Spring/JPA를 다시 배웠다"
- [ ] 면접 대비: 1단계에서 배운 "왜" 질문 20개 스스로 답해보기
- [ ] 2단계(DB & SQL) 책 준비: "Real MySQL 8.0" 1장 읽기
- 오후: 완전 휴식 (번아웃 방지)

**Week 8 PR:** 종합 거래 주문 API + 테스트 + 성능 측정 = **1단계 졸업 PR**

---

## 1단계 완료 체크리스트

### PR 목록 (8개)
- [ ] W1: JVM OOM + 동시성 벤치마크 (java-lab)
- [ ] W2: 미니 IoC + @TradeLog AOP + MDC (java-lab + trader-bot)
- [ ] W3: 예외 계층 + Bean Validation + 로깅 (trader-bot)
- [ ] W4: Spring Security + JWT (trader-bot)
- [ ] W5: 엔티티 연관관계 + N+1 해결 (trader-bot)
- [ ] W6: QueryDSL + 트랜잭션 전파 + 락 (trader-bot)
- [ ] W7: JPA 성능 최적화 + OSIV OFF (trader-bot)
- [ ] W8: 종합 졸업 과제 (trader-bot)

### 블로그 (8편)
- [ ] W1: Virtual Thread 벤치마크
- [ ] W2: Spring DI 100줄로 직접 만들기
- [ ] W3: Spring Boot 예외 처리 설계
- [ ] W4: Spring Boot 1단계 완성 회고
- [ ] W5: N+1 4가지 해법 비교
- [ ] W6: 트랜잭션 전파 + 자기호출 함정
- [ ] W7: JPA 4주 심화 회고
- [ ] W8: 8주 학습 종합 회고

### 참고 도서 활용법
- **정독:** "자바 ORM 표준 JPA 프로그래밍" (M2 시작 전)
- **사전식 참조:** "자바 성능 튜닝 이야기" — JVM/GC 막힐 때만
- **사전식 참조:** "컴퓨터 밑바닥 파헤치기" — CPU 캐시, 메모리 배경지식 필요할 때
- **사전식 참조:** "이펙티브 자바" — 예외(Item 69~77) 등 특정 주제만
- **1단계 끝나고 리팩토링 주간(Week 13)에 몰아 읽기** — 실험 경험 있어서 흡수력 2배

### "왜"에 답할 수 있어야 하는 것들 (면접 대비)
- [ ] JVM 메모리 영역 5가지와 각 역할
- [ ] G1 GC가 Region 기반인 이유
- [ ] Virtual Thread가 빠른 게 아니라 많이 만들 수 있는 이유
- [ ] CAS 연산 원리와 AtomicInteger의 스레드 안전성
- [ ] Spring IoC 컨테이너의 빈 생성~주입 과정
- [ ] 생성자 주입을 권장하는 3가지 이유
- [ ] AOP 프록시(CGLIB)의 동작 원리
- [ ] Checked vs Unchecked Exception, Spring이 Unchecked를 쓰는 이유
- [ ] @ControllerAdvice의 예외 처리 흐름
- [ ] JWT의 구조와 서명 vs 암호화 차이
- [ ] BCrypt를 쓰는 이유
- [ ] 영속성 컨텍스트의 4가지 상태
- [ ] Dirty Checking의 원리 (스냅샷 비교)
- [ ] N+1 문제와 4가지 해결법
- [ ] fetch join의 카르테시안 곱 문제
- [ ] 트랜잭션 전파 속성 (특히 REQUIRED vs REQUIRES_NEW)
- [ ] 자기호출 시 @Transactional이 안 먹히는 이유
- [ ] 낙관적 vs 비관적 락의 차이와 사용 시점
- [ ] OSIV의 장단점
- [ ] Spring Data JPA가 인터페이스에서 구현체를 만드는 원리
