# 6단계: 아키텍처 & 분산 시스템 — 하루 단위 커리큘럼

> **기간:** 8주 (Week 26–33)
> **선수 과정:** 1~5단계
> **이 단계의 의의:** 시니어 백엔드의 차별점. "코드를 짠다"에서 "시스템을 설계한다"로 도약.
> **하루:** 평일 2~3h / 토 5~6h / 일 2.5h (오후 휴식)
> **코딩 장소:** java-lab (실험) / trader-bot (적용)
> **매주 필수:** PR 1개 + 테스트 + 설계 문서 + 블로그 1편

> **이 단계는 책의 비중이 커진다.**
> - "데이터 중심 애플리케이션 설계" (마틴 클레프만) ← 백엔드의 성경, 분산 시스템 파트 정독
> - "가상 면접 사례로 배우는 대규모 시스템 설계 기초" 1, 2권 (Alex Xu) ← 시스템 디자인 필독
> - "도메인 주도 설계 핵심" (반 버논) ← DDD 입문서
> - "마이크로서비스 패턴" (크리스 리처드슨) ← MSA 실전
> - "카프카 핵심 가이드" (Confluent) ← Kafka 깊이
> - "클린 아키텍처" (로버트 마틴) ← 아키텍처 사고

---

# Part 1: 아키텍처 패턴 (Week 26–27)

---

## Week 26 — 레이어드 한계 & 헥사고날 아키텍처

### Day 176 (월) — 레이어드 아키텍처 한계 체감

**이해 (2h)**
- 현재 trader-bot 구조 분석 (`domain` / `application` / `infra` / `presentation`)
  - 각 레이어의 의존 방향 화살표 그려보기
  - 어디서 어디로 import가 발생하는가?
- 다음 변경을 가정하고 영향 범위 측정
  - 거래 알고리즘 정책 1개 변경 → 몇 개 파일 수정?
  - DB를 PostgreSQL → MongoDB로 변경 → 도메인까지 영향이 가는가?
  - 외부 거래소 API를 다른 회사로 교체 → 비즈니스 로직이 바뀌는가?
- **왜 질문:** 레이어드에서 도메인이 JPA 어노테이션(`@Entity`, `@Column`)을 갖고 있으면 왜 문제인가?
- **왜 질문:** "의존성이 아래로만 흐른다"는 규칙이 있는데, 왜 여전히 변경 파급이 큰가?
- **왜 질문:** 테스트할 때 DB 없이 비즈니스 로직을 테스트할 수 있는가? 못 한다면 왜?

**참고:** "클린 아키텍처" 22장 (The Clean Architecture)

### Day 177 (화) — 헥사고날 아키텍처 이론

**이해 (2.5h)**
- [ ] 헥사고날(Ports & Adapters) 아키텍처 전체 그림 그리기
  ```
  domain/             ← 순수 비즈니스 로직 (외부 의존 없음)
  application/
    port/in/          ← UseCase 인터페이스 (입력)
    port/out/         ← Repository 인터페이스 (출력)
    service/          ← UseCase 구현
  adapter/
    in/web/           ← REST 컨트롤러
    out/persistence/  ← JPA 구현
    out/external/     ← 외부 API 구현
  ```
- [ ] 의존 방향 규칙: 바깥 → 안쪽으로만. 도메인은 아무것도 import하지 않음
- **왜 질문:** Port와 Adapter의 차이는? Port는 "뭘 할 수 있는지", Adapter는 "어떻게 하는지"
- **왜 질문:** 왜 인터페이스(Port)를 application 안에 두는가? 도메인에 두면 안 되는가?
- **왜 질문:** 헥사고날이라는 이름의 유래는? 왜 육각형인가? (각 변이 포트를 의미, 사실 변 수는 무관)
- **왜 질문:** Input Port(UseCase)와 Output Port(Repository)를 분리하는 이유는?
- [ ] 레이어드 vs 헥사고날 비교표 작성
  - 테스트 용이성, 변경 영향 범위, 복잡도, 학습 곡선

### Day 178 (수) — 헥사고날 아키텍처 적용 (1)

**코드 (2.5h) — trader-bot**
- [ ] 도메인 레이어 리팩토링
  - 도메인 엔티티에서 JPA, Spring 어노테이션 모두 제거
  - 순수 Java 객체로 도메인 모델 재작성
  - 비즈니스 규칙은 도메인 메서드 안에만 존재
- [ ] Input Port 정의
  - `CreateOrderUseCase` 인터페이스
  - `GetOrderUseCase` 인터페이스
  - **왜 질문:** UseCase를 인터페이스로 만드는 이유는? 구현체가 하나뿐인데 왜?
- [ ] Output Port 정의
  - `SaveOrderPort` 인터페이스
  - `LoadOrderPort` 인터페이스
  - `SendExchangeOrderPort` 인터페이스
  - **왜 질문:** Repository 인터페이스를 application 패키지에 두면, JPA를 MongoDB로 바꿔도 도메인/application은 수정 없음. 이게 핵심.

### Day 179 (목) — 헥사고날 아키텍처 적용 (2)

**코드 (2.5h) — trader-bot**
- [ ] Adapter 구현
  - `in/web/OrderController` — Input Port를 호출
  - `out/persistence/OrderJpaAdapter` — Output Port 구현, JPA Entity 별도 정의
  - `out/external/KisExchangeAdapter` — Output Port 구현
- [ ] 매핑 레이어 작성
  - Domain Entity ↔ JPA Entity 변환 Mapper
  - **왜 질문:** 매핑 코드가 보일러플레이트처럼 느껴지는데, 이 비용을 감수하는 이유는?
  - **왜 질문:** MapStruct 같은 라이브러리를 쓰면 이 비용을 줄일 수 있나?
- [ ] 테스트 용이성 확인
  - Output Port를 Mock으로 교체 → 도메인 로직만 순수하게 테스트
  - DB 없이 UseCase 테스트가 되는지 확인
  - **왜 질문:** 아키텍처 바꾸기 전에는 이 테스트가 왜 어려웠는가?

### Day 180 (금) — DDD 전술적 패턴 (1): Entity, VO, Aggregate

**이해 + 코드 (2.5h)**
- [ ] Entity vs Value Object 구분
  - Entity: 식별자(ID)로 구분, 상태 변화 가능. `Order`, `User`
  - Value Object: 값으로 동등성 판단, 불변. `Money`, `Address`, `StockCode`
  - **왜 질문:** VO를 불변으로 만드는 이유는? 공유해도 안전하려면?
  - **왜 질문:** `equals()`를 ID로 비교(Entity) vs 모든 필드로 비교(VO) — 왜 다르게?
- [ ] Aggregate와 Aggregate Root
  - Aggregate = 일관성 경계. 외부에서는 Root를 통해서만 접근
  - `Order`가 Aggregate Root, `OrderLine`은 내부 엔티티
  - **왜 질문:** 왜 Aggregate 단위로 트랜잭션을 묶는가?
  - **왜 질문:** Aggregate를 너무 크게 잡으면 무슨 문제가 생기는가? (락 경합, 성능 저하)
  - **왜 질문:** Aggregate 간 참조는 ID로만 하라는 규칙의 이유는?
- [ ] trader-bot에 적용
  - `Money` VO 구현 (금액 + 통화, 사칙연산 메서드, 불변)
  - `StockCode` VO 구현 (6자리 숫자 검증, equals/hashCode)

### Day 181 (토) — DDD 전술적 패턴 (2): Domain Service, Domain Event

**코드 + 이해 (5h)**

오전 (2.5h) — Domain Service & Repository
- [ ] Domain Service 이해
  - 여러 Aggregate에 걸친 로직은 어디에 넣는가?
  - **왜 질문:** "도메인 서비스"와 "애플리케이션 서비스"의 차이는?
    - 도메인 서비스: 비즈니스 규칙 (가격 검증, 잔고 확인)
    - 애플리케이션 서비스: 유스케이스 오케스트레이션 (트랜잭션, 이벤트 발행)
  - **왜 질문:** Entity에 넣을 수 없는 로직이 왜 존재하는가? (두 Aggregate에 동시 접근)
- [ ] Repository는 Aggregate 단위
  - `OrderRepository`는 Order Aggregate만 다룬다
  - `OrderLine`을 직접 저장하는 Repository는 만들지 않는다
  - **왜 질문:** JPA의 `CascadeType.ALL`이 Aggregate 개념과 어떻게 매핑되는가?

오후 (2.5h) — Domain Event
- [ ] Domain Event 개념 이해
  - "과거에 일어난 사실" — `OrderCreatedEvent`, `OrderExecutedEvent`
  - 이벤트는 불변, 과거형 이름
  - **왜 질문:** 도메인 이벤트와 Spring ApplicationEvent의 차이는?
  - **왜 질문:** 이벤트를 발행하면 동기? 비동기? `@TransactionalEventListener`는?
- [ ] 구현
  - `AbstractDomainEvent` 추상 클래스 (eventId, occurredAt)
  - `OrderExecutedEvent` 작성
  - Aggregate Root에서 이벤트 등록 → Application Service에서 발행
  - `@TransactionalEventListener(phase = AFTER_COMMIT)` 활용
- [ ] 이벤트 리스너로 알림/로그 처리 분리

### Day 182 (일) — Week 26 정리 + 블로그

**오전 (2.5h)**
- [ ] 레이어드 → 헥사고날 전환 전/후 비교 정리
- [ ] DDD 전술적 패턴 용어 요약 카드 작성 (Entity, VO, Aggregate, Domain Event, Domain Service)
- [ ] **블로그 작성:** "레이어드 아키텍처의 한계를 체감하고 헥사고날로 바꿔본 기록"
- [ ] 다음 주 예습: "도메인 주도 설계 핵심" 4~6장 훑기
- 오후: 휴식

**Week 26 PR:** trader-bot 헥사고날 리팩토링 + DDD 전술적 패턴 적용 (Money VO, Domain Event)

---

## Week 27 — 전략적 DDD & 아키텍처 심화

### Day 183 (월) — Bounded Context & Context Map

**이해 (2h)**
- [ ] Bounded Context 식별
  - trader-bot을 다음으로 나누기
    - 사용자 관리 Context
    - 주문 Context
    - 시세 Context
    - 거래 분석 Context
  - 같은 "User"라도 Context마다 다른 의미 (주문 Context에서의 User ≠ 분석 Context에서의 User)
  - **왜 질문:** 왜 하나의 "User" 모델로 통일하면 안 되는가? (God Object 문제)
  - **왜 질문:** Bounded Context와 마이크로서비스는 1:1인가? 항상 그런가?
- [ ] Context Map 그리기
  - 각 Context 간 관계 표현
  - Upstream/Downstream, Conformist, Anti-Corruption Layer, Shared Kernel
  - **왜 질문:** Anti-Corruption Layer가 왜 필요한가? 외부 모델이 내 도메인을 오염시키는 예시는?
- [ ] trader-bot에서 ACL 적용 대상 식별
  - 외부 거래소(KIS) API 응답 모델 → 우리 도메인 모델로 변환하는 레이어

### Day 184 (화) — Anti-Corruption Layer 구현

**코드 (2.5h) — trader-bot**
- [ ] ACL 구현
  - KIS API 응답 DTO는 `adapter/out/external/kis/dto/` 패키지에 격리
  - 변환 로직: KIS 응답 → 도메인 모델 (별도 Translator 클래스)
  - **왜 질문:** 외부 API가 필드명을 바꾸면 우리 도메인은 영향받는가? ACL 있으면?
  - **왜 질문:** ACL 없이 외부 DTO를 직접 Service에서 쓰면 어떤 문제가 시간이 지나며 커지는가?
- [ ] Context 간 통신 방식 결정
  - 같은 프로세스: 직접 호출 or 도메인 이벤트
  - 별도 프로세스: REST API or 메시지 큐
  - **왜 질문:** Context 간 결합도를 낮추려면 이벤트가 REST보다 나은 이유는?
- [ ] 패키지 구조 재정리
  - Context별 패키지 분리가 명확한지 확인

### Day 185 (수) — 클린 아키텍처 & 의존성 규칙 검증

**이해 + 코드 (2.5h)**
- [ ] 클린 아키텍처 동심원 그리기
  - Entities → Use Cases → Interface Adapters → Frameworks & Drivers
  - 의존성은 항상 안쪽으로만!
  - **왜 질문:** 클린 아키텍처와 헥사고날 아키텍처의 공통점과 차이점은?
  - **왜 질문:** DIP(의존성 역전 원칙)가 아키텍처 전체에 어떻게 적용되는가?
- [ ] ArchUnit으로 의존성 규칙 자동 검증
  - `domain` 패키지는 `adapter`를 import하면 안 됨
  - `application` 패키지는 `adapter`를 import하면 안 됨
  - 테스트 코드 작성 → CI에서 자동 검증
  - **왜 질문:** 코드 리뷰로만 의존성 규칙을 지키면 왜 부족한가? (사람은 실수한다)
- [ ] ArchUnit 테스트 실행 → 위반 사항 수정

### Day 186 (목) — CQRS 기초 개념

**이해 (2.5h)**
- [ ] CQRS(Command Query Responsibility Segregation) 이해
  - Command: 상태 변경 (쓰기). 반환값 없음 (혹은 ID만)
  - Query: 상태 조회 (읽기). 상태 변경 없음
  - **왜 질문:** 왜 읽기와 쓰기를 분리하는가? 같은 모델로 하면 안 되는가?
    - 조회 최적화 ≠ 쓰기 최적화 (인덱스, 정규화 등)
    - 읽기 부하 >> 쓰기 부하인 경우가 대부분
  - **왜 질문:** CQRS를 도입하면 Eventual Consistency를 받아들여야 하는 이유는?
  - **왜 질문:** 모든 서비스에 CQRS를 적용해야 하는가? 언제 적용하면 과도한가?
- [ ] 단순 CQRS 적용 (같은 DB, 모델만 분리)
  - Command 모델: `Order` (쓰기 최적화, 정규화)
  - Query 모델: `OrderSummaryView` (읽기 최적화, 비정규화)
  - `CommandHandler`와 `QueryHandler` 분리
- [ ] Event Sourcing과의 관계
  - CQRS ≠ Event Sourcing (별개 개념이지만 잘 어울림)
  - **왜 질문:** Event Sourcing을 하면 CQRS가 거의 필수인 이유는?

### Day 187 (금) — 아키텍처 결정 기록 (ADR)

**이해 + 코드 (2.5h)**
- [ ] ADR(Architecture Decision Record) 작성법
  - 제목, 상태, 컨텍스트, 결정, 결과
  - **왜 질문:** 왜 아키텍처 결정을 문서로 남기는가? 코드만으로 충분하지 않은가?
  - **왜 질문:** 3개월 뒤 팀원이 "왜 이렇게 했어요?"라고 물으면 답할 수 있는가?
- [ ] trader-bot ADR 3개 작성
  - ADR-001: 헥사고날 아키텍처 채택 (vs 레이어드 유지)
  - ADR-002: Domain Event 발행 방식 (ApplicationEvent vs Kafka)
  - ADR-003: Aggregate 크기 결정 기준
- [ ] 아키텍처 패턴 trade-off 정리표
  - 레이어드 / 헥사고날 / 클린 / 이벤트 드리븐
  - 각각: 복잡도, 테스트 용이성, 변경 대응력, 팀 학습 비용
- [ ] **왜 질문:** "이 프로젝트에 적합한 아키텍처"를 고르는 기준은 무엇인가? (팀 규모, 도메인 복잡도, 변경 빈도)

### Day 188 (토) — 종합 리팩토링 + 통합 테스트

**코드 (5h)**

오전 (2.5h) — 전체 구조 마무리
- [ ] trader-bot 전체 패키지 구조 최종 정리
  - 헥사고날 + Bounded Context + ACL 반영
  - ArchUnit 테스트 전체 통과 확인
- [ ] Aggregate 일관성 테스트
  - Order Aggregate 내 불변식(invariant) 테스트 작성
  - 잔고 부족 시 주문 생성 실패하는지
  - OrderLine 없는 Order는 생성 불가한지

오후 (2.5h) — 통합 테스트
- [ ] 유스케이스 통합 테스트
  - `CreateOrderUseCase` 호출 → DB 저장 → Event 발행 확인
  - `@SpringBootTest` + `@Transactional`
  - **왜 질문:** 통합 테스트에서 `@Transactional`을 붙이면 이벤트 리스너 테스트에 문제가 생기는 이유는?
- [ ] 헥사고날 전환 전/후 테스트 실행 시간 비교
  - 도메인 단위 테스트: DB 없이 실행 → 속도 차이 측정
- [ ] `jstat`, 빌드 시간 기록

### Day 189 (일) — Week 27 정리 + 블로그

**오전 (2.5h)**
- [ ] Bounded Context, ACL, CQRS 핵심 개념 다이어그램 정리
- [ ] ADR 작성 회고 — 문서화의 가치 체감
- [ ] **블로그 작성:** "DDD 전략적 패턴: Bounded Context와 Anti-Corruption Layer 실전 적용"
- [ ] 다음 주 예습: "카프카 핵심 가이드" 1~3장 훑기
- 오후: 휴식

**Week 27 PR:** Bounded Context 분리 + ACL 구현 + ArchUnit 검증 + CQRS 기초 적용

---

# Part 2: 메시징 & 이벤트 기반 (Week 28–29)

---

## Week 28 — Kafka 기초 & Spring Kafka

### Day 190 (월) — Kafka 아키텍처 이해

**이해 (2h)**
- [ ] Kafka 전체 아키텍처 그림 그리기
  - Producer → Broker (Cluster) → Consumer
  - Zookeeper (혹은 KRaft) 역할
- [ ] 핵심 개념 정리
  - **Topic**: 메시지 카테고리 (DB 테이블과 비교)
  - **Partition**: Topic의 물리적 분할. 순서 보장 단위
  - **Offset**: 파티션 내 메시지의 고유 위치 (자동 증가)
  - **Consumer Group**: 파티션을 나눠 가짐. 같은 그룹 내 소비자는 겹치지 않음
  - **Replication**: ISR(In-Sync Replica) — Leader + Follower
- **왜 질문:** 왜 Kafka는 메시지를 디스크에 저장하는데도 빠른가? (순차 I/O, Zero-copy, 배치)
- **왜 질문:** Partition 수를 늘리면 처리량이 올라가는 이유는? (병렬 소비)
- **왜 질문:** Partition 수를 줄일 수 없는 이유는? (offset 재배치 불가)
- **왜 질문:** Consumer Group 내 Consumer 수 > Partition 수이면 놀고 있는 Consumer가 생기는 이유는?

**참고:** "카프카 핵심 가이드" 1~2장

### Day 191 (화) — Kafka 로컬 실행 & CLI 실험

**코드 (2.5h) — java-lab**
- [ ] docker-compose로 Kafka 클러스터 띄우기
  - KRaft 모드 (Zookeeper 없이) 사용
  - Broker 3개로 구성
- [ ] CLI 실험
  - `kafka-topics.sh --create --topic test-orders --partitions 3 --replication-factor 2`
  - `kafka-console-producer.sh` 로 메시지 발행
  - `kafka-console-consumer.sh --group test-group` 으로 소비
  - Consumer 2개 띄우고 파티션 분배 확인
- [ ] Rebalancing 실험
  - Consumer 하나 종료 → 파티션 재분배 관찰
  - **왜 질문:** Rebalancing이 일어나는 동안 메시지 처리가 멈추는 이유는?
  - **왜 질문:** Cooperative Rebalancing이 Stop-the-World 방식보다 나은 이유는?
- [ ] Producer acks 설정 실험
  - `acks=0`: 브로커 응답 안 기다림 (유실 가능)
  - `acks=1`: Leader만 확인
  - `acks=all`: 모든 ISR 확인 (가장 안전)
  - **왜 질문:** acks=all이면 왜 처리량이 떨어지는가?

### Day 192 (수) — Kafka 내부 동작 깊이

**이해 (2.5h)**
- [ ] Kafka 저장 구조
  - Segment 파일 (.log, .index, .timeindex)
  - **왜 질문:** 왜 파일을 Segment로 나누는가? (삭제/압축 단위, 검색 효율)
  - Log Compaction이란? 언제 쓰는가?
- [ ] Producer 내부 동작
  - Serializer → Partitioner → RecordAccumulator → Sender Thread → Broker
  - **왜 질문:** `batch.size`와 `linger.ms`가 처리량에 미치는 영향은?
  - **왜 질문:** Sticky Partitioner가 Round Robin보다 나은 이유는?
- [ ] Consumer 내부 동작
  - `poll()` 루프의 의미
  - `max.poll.interval.ms` 초과 시 Rebalancing 발생
  - **왜 질문:** Consumer가 죽었는지 브로커가 아는 방법은? (heartbeat + session.timeout)
  - **왜 질문:** `auto.offset.reset = earliest vs latest` — 각각 언제 쓰는가?
- [ ] Offset 커밋 전략
  - Auto commit vs Manual commit
  - **왜 질문:** Auto commit에서 메시지 유실 또는 중복 처리가 발생하는 시나리오는?

### Day 193 (목) — Spring Kafka Producer 구현

**코드 (2.5h) — trader-bot**
- [ ] `spring-kafka` 의존성 추가
- [ ] KafkaTemplate 설정
  - `ProducerConfig` — bootstrap servers, serializer, acks
  - JSON 직렬화 (Jackson2JsonSerializer)
  - **왜 질문:** Key serializer와 Value serializer를 분리하는 이유는?
- [ ] 시세 이벤트 발행 구현
  - `QuotePriceChangedEvent` 정의
  - `KafkaTemplate.send(topic, key, event)` 호출
  - Key = 종목코드 → 같은 종목은 같은 파티션으로 (순서 보장)
  - **왜 질문:** Key를 종목코드로 설정하면 왜 순서가 보장되는가? (같은 키 → 같은 파티션)
- [ ] 발행 실패 처리
  - `ListenableFuture` / `CompletableFuture` 콜백
  - 실패 시 재시도 전략 (`retries`, `retry.backoff.ms`)
  - **왜 질문:** 재시도하면 순서가 바뀔 수 있는가? `max.in.flight.requests.per.connection=1`은?

### Day 194 (금) — Spring Kafka Consumer 구현

**코드 (2.5h) — trader-bot**
- [ ] `@KafkaListener` 구현
  - `@KafkaListener(topics = "quote-events", groupId = "trade-algorithm")`
  - JSON 역직렬화 (JsonDeserializer + Trusted Packages)
  - **왜 질문:** Consumer Group ID를 같게 하면 왜 메시지를 나눠 가지는가?
- [ ] Manual Acknowledge 구현
  - `AckMode.MANUAL_IMMEDIATE`
  - 처리 성공 후에만 `acknowledgment.acknowledge()` 호출
  - **왜 질문:** acknowledge 안 하면 어떻게 되는가? (다음 poll에서 같은 메시지 다시 옴)
- [ ] DLQ(Dead Letter Queue) 구현
  - 3번 재시도 실패 → `order-events.DLT` 토픽으로 이동
  - `DefaultErrorHandler` + `DeadLetterPublishingRecoverer`
  - **왜 질문:** DLQ에 쌓인 메시지는 어떻게 처리하는가? (모니터링 + 수동 재처리)
- [ ] Consumer 에러 처리 전략 정리
  - Retriable 에러 (네트워크 일시 장애) vs Non-retriable 에러 (역직렬화 실패)

### Day 195 (토) — EDA(Event-Driven Architecture) 설계

**코드 + 이해 (5h)**

오전 (2.5h) — EDA 설계
- [ ] trader-bot을 EDA로 재설계 (다이어그램)
  ```
  [사용자 주문]
     ↓ OrderCreated
  [주문 검증 서비스]
     ↓ OrderValidated
  [잔고 차감 서비스]
     ↓ BalanceDeducted
  [거래 실행 서비스]
     ↓ TradeExecuted
  [알림 / 분석 / 통계 서비스] (다수 구독)
  ```
- [ ] EDA의 장점 검증
  - 각 서비스는 독립 배포 가능한가?
  - 한 서비스 장애가 전체 마비로 이어지지 않는가?
  - 새 구독자(예: 통계 서비스) 추가 시 기존 서비스 변경 없는가?
- **왜 질문:** 동기 호출(REST)과 비동기 이벤트의 근본적 차이는? (시간 결합 vs 시간 비결합)
- **왜 질문:** 이벤트 순서가 보장 안 되면 어떤 문제가 생기는가?
- **왜 질문:** Event Notification vs Event-Carried State Transfer 차이는?

오후 (2.5h) — 이벤트 스키마 설계
- [ ] 이벤트 스키마 버전 관리
  - 이벤트 필드 추가 시 하위호환(backward compatibility)
  - Avro + Schema Registry vs JSON Schema
  - **왜 질문:** Producer가 이벤트에 필드를 추가하면 기존 Consumer가 깨지는가? (forward compatibility)
- [ ] 이벤트 네이밍 규칙 정의
  - 과거형: `OrderCreated`, `PaymentCompleted`
  - Context prefix: `order.OrderCreated`
- [ ] Kafka 토픽 설계
  - 토픽 이름 규칙: `{context}.{event-type}`
  - 파티션 수 결정 기준 (소비자 수, 처리량)
  - Retention 정책 (7일 vs 무기한)

### Day 196 (일) — Week 28 정리 + 블로그

**오전 (2.5h)**
- [ ] Kafka 핵심 개념 요약 카드 (Topic, Partition, Consumer Group, ISR, Offset)
- [ ] Spring Kafka Producer/Consumer 설정 체크리스트
- [ ] **블로그 작성:** "Kafka 내부 동작 원리: 왜 디스크에 쓰는데 빠른가?"
- [ ] 다음 주 예습: "마이크로서비스 패턴" 4장 (Saga) 훑기
- 오후: 휴식

**Week 28 PR:** Kafka 클러스터 docker-compose + Spring Kafka Producer/Consumer + DLQ + EDA 설계 문서

---

## Week 29 — Saga, Outbox, 이벤트 기반 심화

### Day 197 (월) — 분산 트랜잭션 문제 이해

**이해 (2h)**
- [ ] 왜 분산 트랜잭션이 필요한가?
  - 주문 생성 = 주문 저장 + 잔고 차감 + 외부 거래소 호출
  - 이것들이 다른 DB(또는 다른 서비스)에 있으면 하나의 DB 트랜잭션으로 못 묶음
  - **왜 질문:** 2PC(Two-Phase Commit)는 왜 마이크로서비스에서 쓰지 않는가? (성능, 가용성, 단일 장애점)
- [ ] Saga 패턴 개요
  - 로컬 트랜잭션의 연쇄 + 실패 시 보상 트랜잭션
  - **왜 질문:** Saga는 ACID를 보장하는가? (아니다. ACD는 보장, Isolation은 포기)
  - **왜 질문:** Isolation 없이 어떤 문제가 생기는가? (Dirty Read, Lost Update → Countermeasure 필요)
- [ ] 두 가지 구현 방식
  - **Choreography**: 이벤트 체인. 중앙 조율자 없음
  - **Orchestration**: 중앙 Orchestrator가 단계 관리
  - **왜 질문:** Choreography가 단순한데 왜 복잡한 Saga에서는 Orchestration을 선호하는가?

### Day 198 (화) — Choreography Saga 구현

**코드 (2.5h) — trader-bot**
- [ ] 시나리오 정의
  - Step 1: 주문 생성 (`OrderCreated`)
  - Step 2: 잔고 차감 (`BalanceDeducted`)
  - Step 3: 거래소 주문 실행 (`TradeExecuted`)
- [ ] 보상 트랜잭션 정의
  - Step 3 실패 → `BalanceRefunded` 이벤트 → 잔고 복구
  - Step 2 실패 → `OrderCancelled` 이벤트 → 주문 취소
- [ ] Kafka 토픽으로 이벤트 연결
  - 각 서비스가 이전 단계의 이벤트를 구독 → 처리 → 다음 이벤트 발행
- [ ] 실패 시나리오 테스트
  - 거래소 API 호출 실패 → 잔고 복구 확인
  - **왜 질문:** 보상 트랜잭션이 실패하면 어떻게 하는가? (재시도 + 수동 개입)
  - **왜 질문:** Choreography에서 전체 Saga 상태를 추적하기 어려운 이유는?

### Day 199 (수) — Orchestration Saga 구현

**코드 (2.5h) — trader-bot**
- [ ] Saga Orchestrator 구현
  - `OrderSagaOrchestrator` 클래스
  - 상태 머신: STARTED → BALANCE_DEDUCTED → TRADE_EXECUTED → COMPLETED
  - 각 상태에서 실패 시 → 보상 단계로 역주행
- [ ] 구현 방식 선택
  - Spring StateMachine 또는 직접 enum + switch 구현
  - **왜 질문:** StateMachine 라이브러리를 쓰면 좋은 점은? (상태 전이 시각화, 검증)
- [ ] Saga 상태 저장
  - `saga_instances` 테이블 — sagaId, currentStep, status, createdAt
  - **왜 질문:** Saga 상태를 DB에 저장하는 이유는? (서버 재시작 시 복구)
- [ ] 비교 정리
  - Choreography: 단순, 느슨한 결합, 추적 어려움
  - Orchestration: 명시적 흐름, 중앙 집중, 단일 장애점 가능

### Day 200 (목) — Outbox 패턴 이론 & 구현 (1)

**이해 + 코드 (2.5h)**
- [ ] Outbox 패턴이 필요한 이유
  - 문제: DB save 성공 → Kafka publish 실패 → 데이터 불일치
  - 해결: 이벤트를 같은 DB 트랜잭션에서 Outbox 테이블에 저장
  - **왜 질문:** "DB 저장과 이벤트 발행의 원자성"을 왜 보장해야 하는가?
  - **왜 질문:** Kafka에 직접 쓰면서 트랜잭션을 보장할 수 없는 이유는? (다른 시스템이라 2PC 필요)
- [ ] Outbox 테이블 설계
  ```sql
  CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100),
    aggregate_id VARCHAR(100),
    event_type VARCHAR(100),
    payload JSONB,
    created_at TIMESTAMP,
    published BOOLEAN DEFAULT FALSE
  );
  ```
- [ ] 비즈니스 로직에서 Outbox에 저장
  - `@Transactional` 안에서 Order 저장 + OutboxEvent 저장 (같은 트랜잭션)
  - **왜 질문:** 왜 같은 트랜잭션이 핵심인가? (둘 다 성공하거나 둘 다 실패)

### Day 201 (금) — Outbox 패턴 구현 (2): Polling Publisher

**코드 (2.5h) — trader-bot**
- [ ] Polling Publisher 구현
  - 별도 스케줄러 스레드가 주기적으로 Outbox 테이블 조회
  - `published = false`인 이벤트를 Kafka로 발행
  - 발행 성공 시 `published = true` 업데이트 (또는 삭제)
  - **왜 질문:** Polling 주기가 짧으면? (DB 부하) 길면? (이벤트 지연)
- [ ] 발행 실패 시 재시도
  - 멱등한 Consumer가 있으면 같은 이벤트 2번 보내도 안전
  - **왜 질문:** Outbox + Polling은 At-least-once인가? At-most-once인가? (At-least-once)
- [ ] (고급 선택) Debezium CDC 방식 소개
  - PostgreSQL WAL(Write-Ahead Log) → Debezium → Kafka
  - **왜 질문:** CDC가 Polling보다 나은 점은? (지연 최소, DB 부하 없음)
  - **왜 질문:** CDC의 단점은? (인프라 복잡도 증가, Debezium 운영 부담)
- [ ] Outbox 패턴 흐름 전체 다이어그램 정리

### Day 202 (토) — Saga + Outbox 통합 & 장애 테스트

**코드 (5h)**

오전 (2.5h) — 통합
- [ ] Orchestration Saga + Outbox 결합
  - 각 Saga Step에서 Outbox로 이벤트 저장
  - Polling Publisher가 발행 → 다음 Step 진행
  - **왜 질문:** Saga without Outbox면 어떤 시나리오에서 불일치가 발생하는가?
- [ ] 전체 흐름 테스트
  - 주문 생성 → 잔고 차감 → 거래 실행 → 완료

오후 (2.5h) — 장애 시나리오 테스트
- [ ] 장애 시뮬레이션
  - 거래소 API 호출 중 timeout → 보상 트랜잭션 작동 확인
  - Kafka 브로커 일시 다운 → Outbox에 쌓임 → 복구 후 발행 확인
  - Application 재시작 → 미발행 Outbox 이벤트 처리 확인
- [ ] 데이터 일관성 검증
  - 100건 주문 중 30건 실패 시키기 → 잔고 합계 일치 확인
  - **왜 질문:** "결과적 일관성"은 얼마나 기다려야 일관성이 맞춰지는가? 측정!
- [ ] 모니터링
  - Outbox 미발행 건수 메트릭
  - Saga 실패/보상 횟수 메트릭

### Day 203 (일) — Week 29 정리 + 블로그

**오전 (2.5h)**
- [ ] Saga (Choreography vs Orchestration) 비교표
- [ ] Outbox 패턴 전체 흐름 다이어그램 최종 정리
- [ ] **블로그 작성:** "분산 트랜잭션은 왜 어려운가? Saga + Outbox로 해결한 경험"
- [ ] 다음 주 예습: "데이터 중심 애플리케이션 설계" 5장, 9장 훑기
- 오후: 휴식

**Week 29 PR:** Choreography Saga + Orchestration Saga + Outbox 패턴 + 장애 테스트

---

# Part 3: 분산 시스템 깊이 (Week 30–31)

---

## Week 30 — CAP, 일관성 모델, 멱등성

### Day 204 (월) — CAP 정리 & PACELC

**이해 (2h)**
- [ ] CAP 정리 이해
  - **Consistency**: 모든 노드가 같은 데이터를 본다
  - **Availability**: 모든 요청에 응답한다 (에러 아닌 응답)
  - **Partition Tolerance**: 네트워크 분할에도 시스템이 동작
  - **왜 질문:** 왜 3개 중 2개만 선택 가능한가? 네트워크 파티션이 발생하면?
  - **왜 질문:** "CA 시스템"은 현실에서 존재하는가? (분산 환경에서는 P를 포기할 수 없다)
- [ ] PACELC 확장
  - 파티션 발생 시: A vs C 선택
  - 파티션 없을 때: Latency vs Consistency 선택
  - **왜 질문:** 파티션이 없어도 일관성 vs 지연 trade-off가 있는 이유는? (복제 동기화 비용)
- [ ] trader-bot 컴포넌트별 분류
  - PostgreSQL: CP (트랜잭션 일관성 우선)
  - Redis: AP (캐시 용도, 일부 유실 허용)
  - Kafka: CP (acks=all 기준, ISR 만족 못하면 거부)
  - **왜 질문:** Redis를 CP로 운영하려면 어떻게 해야 하는가? (RedLock? 한계는?)

### Day 205 (화) — 일관성 모델 깊이

**이해 + 코드 (2.5h)**
- [ ] 일관성 모델 종류
  - **Strong Consistency (선형 일관성)**: 쓰기 직후 모든 읽기에서 반영
  - **Eventual Consistency (결과적 일관성)**: 시간이 지나면 일관성 수렴
  - **Causal Consistency (인과적 일관성)**: 인과 관계 있는 연산만 순서 보장
  - **Read-your-writes**: 내가 쓴 건 내가 즉시 읽음
  - **Monotonic Reads**: 한 번 읽은 값보다 과거 값을 읽지 않음
- **왜 질문:** Strong Consistency를 분산 환경에서 달성하면 왜 느려지는가? (합의 프로토콜 비용)
- **왜 질문:** Eventual Consistency의 "eventually"는 얼마나 걸리는가? (수 ms ~ 수 초, 상황 의존)
- **왜 질문:** Read-your-writes를 구현하는 방법은? (쓰기 후 Leader에서 읽기, 또는 버전 태그)
- [ ] trader-bot 기능별 일관성 수준 분류
  - 잔고 조회: Strong (내가 입금하면 즉시 보여야)
  - 주문 상태: Read-your-writes (내가 낸 주문은 바로 보여야)
  - 시세 데이터: Eventual (약간의 지연 허용)
  - 거래 내역 검색: Eventual (비정규화 뷰 갱신 지연 허용)

### Day 206 (수) — 멱등성 & Exactly-Once 시맨틱

**이해 + 코드 (2.5h)**
- [ ] 전달 보장 수준 이해
  - **At-most-once**: 최대 1번. 유실 가능. 재시도 안 함
  - **At-least-once**: 최소 1번. 중복 가능. 재시도 있음
  - **Exactly-once**: 정확히 1번. 가장 어렵고 비쌈
  - **왜 질문:** 네트워크 환경에서 진짜 Exactly-once가 가능한가? (일반적으로 불가능. "효과적 exactly-once" = at-least-once + 멱등성)
- [ ] Kafka의 Exactly-once semantics
  - Producer: `enable.idempotence=true` (PID + Sequence Number)
  - Consumer: Transactional Consumer + 멱등한 처리
  - **왜 질문:** Kafka Producer의 idempotence는 어떻게 동작하는가? (브로커가 시퀀스 추적)
  - **왜 질문:** Consumer 쪽 exactly-once는 왜 Kafka만으로 안 되는가? (외부 시스템 사이드이펙트)
- [ ] Idempotent Consumer 구현 (trader-bot)
  - `processed_events` 테이블 — event_id (UNIQUE)
  - 메시지 수신 시: event_id 존재하면 skip, 없으면 처리 + event_id 저장
  - **같은 트랜잭션**에서 처리 + event_id 저장
  - **왜 질문:** event_id 체크와 비즈니스 로직이 다른 트랜잭션이면 왜 안전하지 않은가?

### Day 207 (목) — Idempotent Consumer 구현 & 테스트

**코드 (2.5h) — trader-bot**
- [ ] 멱등성 키 생성 전략
  - 이벤트 자체 ID (UUID)
  - 또는 비즈니스 키 (orderId + eventType)
  - **왜 질문:** 어떤 키를 쓰느냐에 따라 "같은 메시지"의 정의가 달라지는 이유는?
- [ ] 중복 메시지 테스트
  - 같은 이벤트 3번 발행 → Consumer에서 1번만 처리되는지 확인
  - DB 조회로 중복 insert 안 됐는지 검증
- [ ] API 멱등성 (HTTP 레벨)
  - `Idempotency-Key` 헤더 기반 중복 요청 방지
  - 클라이언트가 같은 주문을 2번 POST해도 1건만 생성
  - **왜 질문:** GET은 왜 기본적으로 멱등한가? POST는 왜 아닌가?
  - **왜 질문:** `Idempotency-Key`의 유효 기간은 얼마로 설정해야 하는가?
- [ ] 멱등성 보장 범위 정리
  - DB 쓰기: 멱등 (upsert or duplicate check)
  - 외부 API 호출: 거래소가 멱등 API를 제공하는가?
  - 알림 발송: 중복 알림은 사용자 경험 문제

### Day 208 (금) — 분산 합의 & Leader Election 개념

**이해 (2.5h)**
- [ ] 분산 합의(Consensus) 기초
  - 왜 필요한가? 여러 노드가 "하나의 값"에 동의해야 할 때
  - Paxos → Raft (이해하기 쉬운 쪽)
  - **왜 질문:** Raft에서 Leader가 죽으면 어떻게 새 Leader를 뽑는가? (Term + 투표)
  - **왜 질문:** Split Brain이란? 왜 위험한가? (두 Leader가 동시에 존재)
- [ ] Kafka의 Leader Election
  - 파티션마다 Leader Broker가 존재
  - Leader 죽으면 ISR 중에서 새 Leader 선출
  - **왜 질문:** `unclean.leader.election.enable=true`면 데이터 유실이 가능한 이유는?
- [ ] ZooKeeper / etcd의 역할
  - 분산 코디네이션: 설정 관리, 서비스 디스커버리, 분산 락
  - **왜 질문:** Kafka가 ZooKeeper를 벗어나 KRaft로 간 이유는? (의존성 제거, 운영 복잡도)
- [ ] 참고: "데이터 중심 애플리케이션 설계" 9장 (일관성과 합의)

### Day 209 (토) — Circuit Breaker & Resilience4j

**코드 + 이해 (5h)**

오전 (2.5h) — Circuit Breaker 이해 & 구현
- [ ] Circuit Breaker 상태 머신
  - CLOSED → (실패율 임계치 초과) → OPEN → (대기 시간 경과) → HALF_OPEN → (성공) → CLOSED
  - **왜 질문:** 왜 즉시 재시도하지 않고 OPEN 상태로 기다리는가? (장애 서비스에 부하 주지 않기 위해)
  - **왜 질문:** HALF_OPEN에서 일부만 통과시키는 이유는? (서비스 복구 확인)
- [ ] Resilience4j 적용 (trader-bot)
  - `resilience4j-spring-boot3` 의존성 추가
  - `@CircuitBreaker` 어노테이션으로 외부 거래소 API 호출 보호
  - 설정: `failureRateThreshold=50`, `waitDurationInOpenState=30s`, `slidingWindowSize=10`
  - Fallback 메서드 정의 (캐시된 시세 반환 or 에러 응답)
- [ ] Retry 적용
  - `@Retry(name = "exchangeApi", fallbackMethod = "...")`
  - `maxAttempts=3`, `waitDuration=500ms`, exponential backoff
  - **왜 질문:** Retry와 Circuit Breaker를 같이 쓸 때 순서가 중요한 이유는? (Retry가 안쪽, CB가 바깥)

오후 (2.5h) — Rate Limiter, Bulkhead, 조합
- [ ] Rate Limiter
  - `@RateLimiter` — 초당 요청 수 제한
  - Token Bucket vs Sliding Window 알고리즘
  - **왜 질문:** Rate Limiter는 누구를 보호하는가? (내 서비스? 상대 서비스?)
- [ ] Bulkhead
  - Semaphore Bulkhead vs ThreadPool Bulkhead
  - **왜 질문:** 왜 "격벽"이라고 부르는가? (배의 격벽처럼 한 부분 침수가 전체로 퍼지지 않게)
  - 외부 API 호출에 스레드 풀 격리 → 느린 API가 다른 기능 먹통 안 만듦
- [ ] TimeLimiter
  - 비동기 호출 타임아웃 설정
- [ ] 모두 조합: Retry → CircuitBreaker → RateLimiter → Bulkhead → TimeLimiter 순서
  - **왜 질문:** 이 순서를 바꾸면 동작이 어떻게 달라지는가?
- [ ] Actuator 엔드포인트로 CB 상태 모니터링

### Day 210 (일) — Week 30 정리 + 블로그

**오전 (2.5h)**
- [ ] CAP/PACELC 요약 + 컴포넌트별 분류표
- [ ] 일관성 모델 비교 다이어그램
- [ ] Resilience4j 조합 순서 정리
- [ ] **블로그 작성:** "멱등성이란 무엇이고 왜 분산 시스템에서 필수인가?"
- [ ] 다음 주 예습: Chaos Engineering 도구 조사 (Toxiproxy, Chaos Monkey)
- 오후: 휴식

**Week 30 PR:** Idempotent Consumer + Resilience4j (CB, Retry, RateLimiter, Bulkhead) 적용

---

## Week 31 — 카오스 엔지니어링 & 분산 추적

### Day 211 (월) — Chaos Engineering 이론

**이해 (2h)**
- [ ] Chaos Engineering 원칙
  - "정상 상태를 정의하고, 가설을 세우고, 실험으로 검증"
  - 프로덕션에서 하는 것이 이상적이지만, 스테이징부터 시작
  - **왜 질문:** 왜 의도적으로 장애를 만드는가? (예방 가능한 장애를 미리 발견)
  - **왜 질문:** "내 시스템은 잘 만들었으니 괜찮다"가 왜 위험한 생각인가?
- [ ] 장애 유형 분류
  - 네트워크: 지연, 패킷 유실, 파티션
  - 서버: 프로세스 다운, CPU 과부하, 메모리 부족
  - 의존성: 외부 API 다운, 느린 응답
  - 데이터: 디스크 가득 참, DB 커넥션 고갈
- [ ] 도구 조사
  - **Toxiproxy**: 네트워크 프록시로 지연/에러 주입
  - **Chaos Monkey for Spring Boot**: 런타임에 예외/지연 주입
  - **Litmus/Chaos Mesh**: K8s 레벨 카오스

### Day 212 (화) — Toxiproxy로 네트워크 장애 시뮬레이션

**코드 (2.5h) — trader-bot**
- [ ] Toxiproxy 설정
  - docker-compose에 Toxiproxy 추가
  - 외부 거래소 API 앞에 Proxy 배치
  - DB 커넥션 앞에 Proxy 배치
- [ ] 장애 시나리오 실험
  - 시나리오 1: 외부 API 응답 3초 지연 → Circuit Breaker OPEN 확인
  - 시나리오 2: 외부 API 완전 차단 → Fallback 동작 확인
  - 시나리오 3: DB 연결 지연 500ms → 커넥션 풀 고갈 여부 확인
  - 시나리오 4: 패킷 50% 유실 → 재시도 동작 확인
- [ ] 각 시나리오에서 시스템 동작 기록
  - **왜 질문:** Circuit Breaker 없이 외부 API가 느려지면 내 서비스 스레드가 어떻게 되는가? (모두 블로킹 → 전체 마비)
  - **왜 질문:** 이것을 "Cascading Failure(연쇄 장애)"라고 하는 이유는?

### Day 213 (수) — 분산 추적 (Distributed Tracing)

**이해 + 코드 (2.5h)**
- [ ] 분산 추적이 필요한 이유
  - 요청 하나가 여러 서비스를 거침 → 어디서 느린지 어떻게 아는가?
  - Trace, Span, Span Context 개념
  - **왜 질문:** 서비스 A → B → C를 거치는 요청에서, C에서 에러가 나면 A의 로그에서 어떻게 추적하는가?
- [ ] OpenTelemetry 기초
  - Trace = 요청의 전체 여정
  - Span = 하나의 작업 단위 (HTTP 호출, DB 쿼리 등)
  - Context Propagation = Trace ID를 서비스 간 전달
  - **왜 질문:** TraceId는 어떻게 서비스를 넘나드는가? (HTTP 헤더, Kafka 헤더에 주입)
- [ ] Micrometer Tracing + Zipkin 설정 (trader-bot)
  - `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-zipkin`
  - Zipkin docker-compose에 추가
  - 요청 처리 후 Zipkin UI에서 Trace 확인
- [ ] Kafka 메시지에 Trace 전파
  - Producer에서 Trace Context를 Kafka 헤더에 삽입
  - Consumer에서 Trace Context 추출 → 같은 Trace로 연결

### Day 214 (목) — 메트릭 & 알림 설계

**이해 + 코드 (2.5h)**
- [ ] RED 메서드 (서비스 모니터링)
  - **Rate**: 초당 요청 수
  - **Errors**: 에러율
  - **Duration**: 지연 시간 (p50, p95, p99)
- [ ] USE 메서드 (인프라 모니터링)
  - **Utilization**: 사용률
  - **Saturation**: 포화도 (큐 길이)
  - **Errors**: 에러 수
- [ ] 비즈니스 메트릭 정의
  - 주문 처리 성공/실패 수
  - Saga 보상 트랜잭션 횟수
  - Outbox 미발행 건수
  - Circuit Breaker 상태 변경 횟수
  - **왜 질문:** 기술 메트릭만으로 충분한가? 비즈니스 메트릭이 왜 필요한가?
- [ ] Grafana 대시보드 구성
  - Prometheus + Grafana docker-compose
  - 주요 메트릭 대시보드 패널 구성
  - 알림 규칙 설정 (에러율 > 5%, p99 > 500ms)

### Day 215 (금) — 장애 대응 런북 작성

**이해 + 코드 (2.5h)**
- [ ] 장애 시나리오별 대응 절차 작성
  - 외부 거래소 API 다운 → CB 확인 → 수동 Fallback → 복구 확인
  - Kafka 브로커 1대 다운 → ISR 상태 확인 → 자동 복구 대기
  - DB 커넥션 풀 고갈 → 원인 파악 (느린 쿼리?) → 커넥션 수 조정
  - OOM 발생 → Heap dump 분석 → 메모리 누수 원인 찾기
- [ ] 각 시나리오에 대한 판단 기준
  - **왜 질문:** 자동 복구 vs 수동 개입의 기준은 무엇인가?
  - **왜 질문:** 장애 발생 시 가장 먼저 확인해야 할 것은? (영향 범위 파악)
- [ ] 포스트모템(Post-mortem) 템플릿
  - 타임라인, 영향 범위, 근본 원인, 재발 방지
  - **왜 질문:** "누가 잘못했나"가 아니라 "시스템이 왜 이걸 방지하지 못했나"를 묻는 이유는?
- [ ] Chaos 실험 결과 → 런북에 반영

### Day 216 (토) — 종합 장애 시뮬레이션 & 복구 훈련

**코드 (5h)**

오전 (2.5h) — 복합 장애 시나리오
- [ ] 시나리오 1: Kafka 브로커 1대 + 외부 API 동시 장애
  - Outbox에 이벤트 적체 → Kafka 복구 후 순서대로 발행 확인
  - CB OPEN → 일정 시간 후 HALF_OPEN → 복구
- [ ] 시나리오 2: DB 지연 + 트래픽 급증
  - 커넥션 풀 소진 → Bulkhead로 격리 → 핵심 기능은 동작
  - Rate Limiter로 과도한 트래픽 차단
- [ ] 각 시나리오에서 데이터 일관성 검증
  - 장애 전 잔고 합계 = 장애 후 잔고 합계

오후 (2.5h) — 성능 측정 & 개선
- [ ] k6 또는 JMeter로 부하 테스트
  - 정상 상태: 초당 100건 주문 처리 확인
  - 장애 상태: 초당 몇 건까지 처리 가능한지
  - 복구 후: 정상 처리량 회복 시간 측정
- [ ] 병목 지점 식별
  - Zipkin 트레이스에서 가장 느린 Span
  - DB 커넥션 대기 시간
  - Kafka Consumer lag
- [ ] 개선 포인트 정리 → Week 32에서 적용

### Day 217 (일) — Week 31 정리 + 블로그

**오전 (2.5h)**
- [ ] Chaos Engineering 실험 결과 요약
- [ ] 분산 추적 설정 가이드 정리
- [ ] **블로그 작성:** "의도적으로 시스템을 부수기: Toxiproxy + Resilience4j로 장애 대응 검증"
- [ ] 다음 주 예습: "가상 면접 사례로 배우는 대규모 시스템 설계 기초" 1~3장
- 오후: 휴식

**Week 31 PR:** Toxiproxy 장애 테스트 + OpenTelemetry 분산 추적 + Grafana 대시보드 + 런북

---

# Part 4: MSA & 성능 (Week 32–33)

---

## Week 32 — MSA 분해 & 캐싱 심화

### Day 218 (월) — MSA 분해 전략

**이해 (2h)**
- [ ] 모놀리스 → MSA 분해 기준
  - 비즈니스 역량(Business Capability) 기준
  - 하위 도메인(Subdomain) 기준 — DDD의 Bounded Context
  - **왜 질문:** "모놀리스 first"가 원칙인 이유는? (초기에는 경계를 모름)
  - **왜 질문:** MSA로 분해하면 뭐가 좋아지고 뭐가 나빠지는가?
    - 좋아짐: 독립 배포, 기술 이기종, 장애 격리, 팀 자율성
    - 나빠짐: 네트워크 통신 비용, 분산 트랜잭션, 운영 복잡도, 디버깅 어려움
- [ ] trader-bot MSA 분해 설계
  - User Service, Order Service, Quote Service, Trade Service, Notification Service
  - 각 서비스별 책임 정의
  - **왜 질문:** "이 기능은 어느 서비스에 속하는가?"를 결정하는 기준은?
- [ ] DB per Service 원칙
  - 각 서비스는 자기 DB만 소유
  - 다른 서비스 DB에 직접 접근 금지
  - **왜 질문:** DB를 공유하면 왜 MSA의 장점이 사라지는가? (스키마 결합, 독립 배포 불가)

### Day 219 (화) — API Gateway & Service Discovery

**이해 + 코드 (2.5h)**
- [ ] API Gateway 역할
  - 라우팅, 인증/인가, Rate Limiting, 로깅
  - Spring Cloud Gateway 기본 설정
  - **왜 질문:** Gateway 없이 클라이언트가 각 서비스를 직접 호출하면 무슨 문제가 생기는가?
  - **왜 질문:** Gateway가 SPOF(Single Point of Failure)가 되지 않으려면?
- [ ] Service Discovery
  - Client-side Discovery vs Server-side Discovery
  - Spring Cloud Netflix Eureka 또는 K8s Service
  - **왜 질문:** 서비스 인스턴스가 동적으로 늘어나고 줄어들 때 IP를 하드코딩하면 왜 안 되는가?
  - **왜 질문:** Health Check가 왜 Service Discovery에 필수인가?
- [ ] 간단한 Gateway 설정 (java-lab)
  - Spring Cloud Gateway Route 설정
  - 경로 기반 라우팅: `/api/orders/**` → Order Service
  - 필터: 요청 로깅, JWT 검증

### Day 220 (수) — 캐싱 전략 심화 (1)

**이해 + 코드 (2.5h)**
- [ ] 캐싱 패턴 4가지 비교
  - **Cache Aside**: 애플리케이션이 캐시 관리 (가장 일반적)
  - **Read Through**: 캐시가 없으면 캐시 레이어가 DB 조회
  - **Write Through**: 쓰기 시 캐시와 DB 동시 갱신
  - **Write Behind (Write Back)**: 캐시에만 쓰고 나중에 DB에 반영
  - **왜 질문:** 각 패턴은 언제 적합한가? (읽기 많으면 Cache Aside, 쓰기 많으면 Write Behind)
  - **왜 질문:** Write Behind에서 캐시가 죽으면 데이터 유실이 발생하는 이유는?
- [ ] Multi-level Caching 구현 (trader-bot)
  - L1: Caffeine (로컬 캐시, 초고속, JVM 내)
  - L2: Redis (분산 캐시, 서버 간 공유)
  - 조회 순서: L1 → L2 → DB
  - **왜 질문:** 로컬 캐시만 쓰면 왜 안 되는가? (서버 간 불일치)
  - **왜 질문:** Redis만 쓰면 왜 안 되는가? (네트워크 왕복 비용)
- [ ] TTL 전략
  - L1: 짧게 (10초~1분). 서버 간 불일치 최소화
  - L2: 중간 (5분~1시간). DB 부하 감소
  - **왜 질문:** TTL을 너무 길게 잡으면? 너무 짧게 잡으면?

### Day 221 (목) — 캐싱 전략 심화 (2): Cache Stampede

**코드 (2.5h) — trader-bot**
- [ ] Cache Stampede (Thundering Herd) 이해
  - 인기 키의 캐시 만료 → 수백 요청이 동시에 DB 조회 → DB 과부하
  - **왜 질문:** 왜 하필 "인기 키"에서 문제가 심한가?
- [ ] 해결법 1: 분산 락
  - 캐시 miss 시 락 획득한 1개 스레드만 DB 조회 → 나머지는 대기
  - Redis `SET NX EX` 기반 분산 락 구현
  - **왜 질문:** 분산 락의 단점은? (대기 시간, 락 서버 장애)
- [ ] 해결법 2: Probabilistic Early Recomputation
  - 캐시 만료 전에 확률적으로 미리 갱신
  - `currentTime + random * beta * computeTime > expiry` 이면 갱신
  - **왜 질문:** 이 방식이 분산 락보다 나은 상황은?
- [ ] 해결법 3: 캐시 만료를 서로 다르게 (jitter)
  - TTL = baseTTL + random(0, jitterRange)
  - **왜 질문:** 모든 키가 같은 시간에 만료되면 왜 위험한가?
- [ ] 실제 부하 테스트로 Stampede 재현 & 해결 확인

### Day 222 (금) — 데이터베이스 확장: Read Replica

**이해 + 코드 (2.5h)**
- [ ] Read Replica 이해
  - Master: 쓰기 담당
  - Replica: 읽기 담당 (비동기 복제)
  - **왜 질문:** 비동기 복제면 Replica에서 옛날 데이터를 읽을 수 있는가? (Yes → Replication Lag)
  - **왜 질문:** 동기 복제를 하면 해결되지만 왜 안 하는가? (쓰기 지연 증가, 가용성 하락)
- [ ] Spring AbstractRoutingDataSource로 읽기/쓰기 분기
  - `@Transactional(readOnly = true)` → Replica
  - `@Transactional(readOnly = false)` → Master
  - **왜 질문:** `readOnly = true`가 단순히 라우팅 힌트만 주는 건가? JPA/DB 최적화도 되는가?
- [ ] Replication Lag 문제 대응
  - 쓰기 직후 읽기는 Master에서 (Read-your-writes)
  - 시간 기반: 쓰기 후 N초간은 Master 읽기
  - **왜 질문:** Lag이 길어지면 사용자가 체감하는 문제는? ("방금 주문했는데 목록에 안 보여요")

### Day 223 (토) — 데이터베이스 확장: 샤딩 & CQRS 심화

**코드 + 이해 (5h)**

오전 (2.5h) — 샤딩
- [ ] 샤딩 전략
  - Range Sharding: 범위 기반 (시간별, ID 구간별)
  - Hash Sharding: 해시 기반 (userId % shardCount)
  - **왜 질문:** Range Sharding에서 핫스팟이 생기는 이유는? (최근 데이터에 집중)
  - **왜 질문:** 샤드 수를 늘릴 때(resharding) 왜 어려운가? (데이터 재분배)
- [ ] Consistent Hashing
  - 노드 추가/제거 시 최소한의 데이터만 이동
  - **왜 질문:** 일반 해시(mod N)보다 Consistent Hashing이 나은 이유는?
- [ ] ShardingSphere 소개 (읽기만)
  - Sharding-JDBC: 애플리케이션 레벨 샤딩
  - 설정 예시 읽고 이해

오후 (2.5h) — CQRS 심화 (별도 저장소)
- [ ] CQRS 심화: 쓰기 DB ≠ 읽기 DB
  - 쓰기: PostgreSQL (정규화, 트랜잭션)
  - 읽기: Elasticsearch (검색 최적화, 비정규화)
  - **왜 질문:** 같은 DB에서 인덱스만 추가하면 안 되는가? 왜 별도 저장소?
  - **왜 질문:** 읽기 모델 업데이트가 늦어지면 사용자에게 어떤 영향?
- [ ] 이벤트 기반 읽기 모델 동기화
  - Domain Event 발행 → Consumer가 Elasticsearch 인덱스 갱신
  - Eventual Consistency 수용
  - **왜 질문:** 이벤트 순서가 바뀌면 읽기 모델이 깨지는가? 어떻게 방지?
- [ ] 간단한 CQRS 구현 (java-lab)
  - PostgreSQL(쓰기) + H2(읽기 뷰) 로 축소판 구현
  - 이벤트 발행 → 읽기 뷰 갱신 → 조회 API는 읽기 뷰에서

### Day 224 (일) — Week 32 정리 + 블로그

**오전 (2.5h)**
- [ ] MSA 분해 기준 정리 (언제 분해하고 언제 모놀리스 유지)
- [ ] 캐싱 패턴 비교표 (Cache Aside / Read Through / Write Through / Write Behind)
- [ ] DB 확장 전략 정리 (Replica, Sharding, CQRS)
- [ ] **블로그 작성:** "Cache Stampede 문제와 3가지 해결법"
- [ ] 다음 주 예습: "가상 면접 사례로 배우는 대규모 시스템 설계" 4~8장
- 오후: 휴식

**Week 32 PR:** Multi-level Cache + Cache Stampede 방지 + Read Replica 라우팅 + CQRS 구현

---

## Week 33 — 시스템 디자인 연습 & 종합 과제

### Day 225 (월) — 시스템 디자인 방법론

**이해 (2h)**
- [ ] 시스템 디자인 4단계 프레임워크
  1. **요구사항 정리** (기능 + 비기능: QPS, 지연, 가용성)
  2. **추정** (트래픽, 데이터 크기, 대역폭)
  3. **아키텍처 설계** (고수준 다이어그램)
  4. **핵심 컴포넌트 상세 + 확장** (병목 식별 → 해결)
- [ ] 추정 연습
  - DAU 100만, 초당 주문 1000건일 때 DB 저장량?
  - 시세 데이터: 종목 3000개 x 1초 간격 x 1년 → 몇 GB?
  - **왜 질문:** 추정이 왜 중요한가? (설계의 방향이 바뀜. 초당 100건 vs 10만 건 = 완전 다른 아키텍처)
- [ ] Back-of-the-envelope 계산 핵심 수치 암기
  - QPS: 1일 = 86400초, 피크 = 평균 x 2~3
  - 저장: 1문자 = 2바이트, 1행 ≈ 100~500바이트
  - 네트워크: 1Gbps ≈ 125MB/s

### Day 226 (화) — 시스템 디자인 연습 1: URL 단축 서비스

**이해 + 설계 (2.5h)**
- [ ] 요구사항 정의
  - 기능: 긴 URL → 짧은 URL, 짧은 URL → 리다이렉트
  - 비기능: 1초 생성 1000건, 읽기 10000건, 99.9% 가용성
- [ ] 핵심 설계
  - 고유 키 생성: Base62 인코딩, 해시(MD5/SHA256 앞 7자리), 카운터
  - **왜 질문:** 해시 충돌이 발생하면 어떻게 처리하는가?
  - **왜 질문:** 순차 카운터를 쓰면 예측 가능해지는 보안 문제는?
- [ ] 확장 전략
  - DB 샤딩 (키의 첫 글자 기반? 해시 기반?)
  - 캐시: 인기 URL은 Redis에 캐싱
  - 만료된 URL 정리: TTL or lazy deletion
- [ ] 아키텍처 다이어그램 종이에 그리기

### Day 227 (수) — 시스템 디자인 연습 2: 실시간 채팅

**이해 + 설계 (2.5h)**
- [ ] 요구사항 정의
  - 기능: 1:1 채팅, 그룹 채팅 (최대 100명), 온라인 상태 표시
  - 비기능: 동시접속 10만, 메시지 지연 < 100ms, 메시지 영구 저장
- [ ] 핵심 설계
  - 프로토콜: WebSocket (양방향 실시간)
  - **왜 질문:** HTTP polling vs Long polling vs WebSocket — 각각의 trade-off는?
  - 메시지 저장: 시계열 특성 → 어떤 DB? (Cassandra? MongoDB? 시계열 DB?)
  - 메시지 순서: 서버 타임스탬프 vs Lamport Clock
- [ ] 확장
  - 서버 여러 대: 사용자 A가 서버1, B가 서버2에 연결. 어떻게 전달?
  - Pub/Sub (Redis) or 메시지 큐 (Kafka)
  - **왜 질문:** WebSocket은 stateful인데 로드밸런서 뒤에서 어떻게 동작하는가? (Sticky Session or 별도 라우팅)
- [ ] 아키텍처 다이어그램 그리기

### Day 228 (목) — 시스템 디자인 연습 3: 거래소 매칭 엔진

**이해 + 설계 (2.5h)**
- [ ] 요구사항 정의
  - 기능: 매수/매도 주문 접수, 가격-시간 우선순위 매칭, 체결 통보
  - 비기능: 초당 100만 주문, 매칭 지연 < 1ms, 무중단
- [ ] 핵심 설계
  - 오더북 자료구조: TreeMap<가격, Queue<주문>> (매수: 내림차순, 매도: 오름차순)
  - **왜 질문:** 왜 HashMap이 아니라 TreeMap인가? (가격 순서가 중요)
  - **왜 질문:** Lock-free 자료구조가 필요한 이유는? (초당 100만 건에서 락은 병목)
  - 매칭 알고리즘: 가격 우선 → 시간 우선
- [ ] 확장
  - 종목별 파티셔닝 (종목별 독립 매칭 엔진)
  - 이벤트 소싱: 모든 주문/체결을 이벤트로 기록 → 재현 가능
  - **왜 질문:** 매칭 엔진에서 Exactly-once가 얼마나 중요한가? (돈이 걸려 있으므로 critical)
- [ ] 아키텍처 다이어그램 그리기

### Day 229 (금) — 시스템 디자인 연습 4: 뉴스피드

**이해 + 설계 (2.5h)**
- [ ] 요구사항 정의
  - 기능: 포스트 작성, 팔로워 피드에 노출, 좋아요/댓글
  - 비기능: DAU 1000만, 피드 로딩 < 200ms
- [ ] 핵심 설계: Fan-out 전략
  - **Fan-out on Write (Push 모델)**: 포스트 작성 시 팔로워 피드에 미리 기록
  - **Fan-out on Read (Pull 모델)**: 피드 조회 시 팔로잉 목록에서 조합
  - **왜 질문:** 팔로워 1000만 명인 셀럽이 글을 쓰면 Push 모델에서 무슨 일이 벌어지는가?
  - **왜 질문:** 하이브리드(셀럽=Pull, 일반=Push)가 최적인 이유는?
- [ ] 캐싱 & 저장소
  - 피드 캐시: Redis Sorted Set (timestamp score)
  - 미디어: Object Storage (S3) + CDN
  - **왜 질문:** 피드에 보이는 데이터가 다 최신이어야 하는가? Eventual Consistency 허용 범위는?
- [ ] 아키텍처 다이어그램 그리기

### Day 230 (토) — 종합 과제: trader-bot 분산 시스템화 설계

**코드 + 설계 (5h)**

오전 (2.5h) — 아키텍처 설계
- [ ] 요구사항 정리
  - 동시 사용자 1만 명
  - 초당 1000건 주문 처리
  - 시세 실시간 push (지연 100ms 이내)
  - 99.9% 가용성
- [ ] 전체 아키텍처 다이어그램 그리기
  - [x] 헥사고날 아키텍처
  - [x] 도메인 이벤트 + Kafka
  - [x] Outbox 패턴
  - [x] Saga로 분산 트랜잭션
  - [x] CQRS (쓰기/읽기 분리)
  - [x] Read Replica
  - [x] Multi-level 캐싱
  - [x] Circuit Breaker
  - [x] 분산 추적 (OpenTelemetry)
- [ ] 컴포넌트별 기술 선택 근거 정리 (ADR)
  - **왜 질문:** 각 기술 선택에서 "왜 이것인가? 대안은?"을 답할 수 있는가?

오후 (2.5h) — 검증 계획 & 부하 테스트 설계
- [ ] 부하 테스트 시나리오 설계
  - k6 스크립트: 동시 사용자 1만, 초당 주문 1000건
  - 목표: p99 < 200ms
- [ ] 장애 시뮬레이션 시나리오
  - 외부 API 장애 → 자동 복구
  - Kafka 브로커 1대 다운 → 무중단
  - 서비스 인스턴스 1개 다운 → 트래픽 재분배
- [ ] 데이터 일관성 검증 계획
  - 10만 건 거래 후 잔고 합계 일치 확인
  - Saga 보상 트랜잭션 후 상태 정합성
- [ ] 병목 예측 & 확장 전략
  - 어디가 먼저 터질까? (DB? Kafka? 외부 API?)
  - 각 병목에 대한 수평/수직 확장 계획

### Day 231 (일) — Week 33 정리 + 6단계 졸업 회고

**오전 (2.5h)**
- [ ] 시스템 디자인 4개 설계 요약 비교표
- [ ] 종합 과제 설계 문서 최종 정리
- [ ] **블로그 작성:** "8주간 아키텍처 & 분산 시스템을 공부하며 배운 것들"
- [ ] 6단계 졸업 체크리스트 자기 평가
- 오후: 휴식

**Week 33 PR:** 시스템 디자인 4개 설계 문서 + 종합 과제 아키텍처 설계 + 부하 테스트 시나리오

---

## 졸업 체크리스트

이 단계까지 완주하면 다음이 가능해야 한다.

- [ ] "이 기능, 어떻게 설계하시겠어요?" → 30분 안에 화이트보드에 그릴 수 있다
- [ ] 트래픽 10배 증가 시나리오에서 병목과 해결책을 말할 수 있다
- [ ] 분산 시스템의 일관성/가용성 trade-off를 상황별로 판단할 수 있다
- [ ] 장애 발생 시 트레이스/로그/메트릭을 조합해 근본 원인을 찾을 수 있다
- [ ] 코드 리뷰에서 "왜 그렇게 짰는지"를 설계 관점에서 설명할 수 있다
- [ ] Saga, Outbox, CQRS를 직접 구현하고 장단점을 설명할 수 있다
- [ ] Circuit Breaker, Retry, Rate Limiter를 적절히 조합할 수 있다
- [ ] 시스템 디자인 면접에서 추정 → 설계 → 확장까지 체계적으로 답할 수 있다

여기까지 오면 미드 시니어 ~ 시니어 수준입니다. 화이팅!

---

## 추천 학습 자료 ★★★ 필독

| 주제 | 자료 |
|---|---|
| **시스템 설계 입문** | **"가상 면접 사례로 배우는 대규모 시스템 설계 기초" 1, 2권 (Alex Xu)** ← 강추 |
| **분산 시스템 정석** | **"데이터 중심 애플리케이션 설계" (마틴 클레프만)** ← 백엔드의 성경 |
| DDD | "도메인 주도 설계" (에릭 에반스), "도메인 주도 설계 핵심" (반 버논) — 후자가 쉬움 |
| MSA | "마이크로서비스 패턴" (크리스 리처드슨) |
| Kafka | "카프카 핵심 가이드" (Confluent) |
| 클린 아키텍처 | "클린 아키텍처" (로버트 마틴) |

---

## 진도 체크
- [ ] Week 26: 레이어드 한계 & 헥사고날 아키텍처 & DDD 전술적 패턴
- [ ] Week 27: 전략적 DDD & 클린 아키텍처 & CQRS 기초
- [ ] Week 28: Kafka 기초 & Spring Kafka & EDA 설계
- [ ] Week 29: Saga (Choreography + Orchestration) & Outbox 패턴
- [ ] Week 30: CAP/PACELC & 일관성 모델 & 멱등성 & Resilience4j
- [ ] Week 31: Chaos Engineering & 분산 추적 & 모니터링
- [ ] Week 32: MSA 분해 & 캐싱 심화 & DB 확장
- [ ] Week 33: 시스템 디자인 연습 & 종합 과제 설계
