# 6단계 실습 가이드: 아키텍처 & 분산 시스템

> **기간:** 6~8주
> **선수 과정:** 1~5단계
> **이 단계의 의의:** 시니어 백엔드의 차별점. "코드를 짠다"에서 "시스템을 설계한다"로 도약.

---

## 학습 원칙

1. **책 + 실습** 병행 — 이 단계는 책의 비중이 커진다
2. **trade-off 사고** — 정답이 없다. "이 상황에선 A가 낫다"를 말할 수 있어야 함
3. **시스템 디자인 연습** — 면접에도 직결

---

## Week 1-2: 아키텍처 패턴

### 과제 1-1. 레이어드 아키텍처의 한계 체감 (난이도 ★★)

**할 일**
1. 현재 trader-bot 구조 분석 (`domain` / `application` / `infra` / `presentation`)
2. 다음 변경을 시도해보며 영향 범위 측정
   - 거래 알고리즘 정책 1개 변경
   - DB를 PostgreSQL → MongoDB로 변경 (가정)
   - 외부 거래소 API를 다른 회사로 교체
3. 어디가 변경 파급이 큰지 정리

---

### 과제 1-2. 헥사고날 아키텍처 적용 (난이도 ★★★)

**목표:** "도메인이 외부에 의존하지 않게"

**할 일**
1. **Port & Adapter** 구조로 재설계
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
2. 도메인에 JPA, Spring 어노테이션 없게
3. 테스트하기 쉬워졌는지 확인 — Port를 Mock으로 갈아끼우기

---

### 과제 1-3. DDD 전술적 패턴 (난이도 ★★★)

**할 일**
1. **Entity vs Value Object** — `Order`는 Entity, `Money`는 VO
2. **Aggregate Root** 결정 — `Order`인가 `Account`인가
3. **Domain Service** — 여러 Aggregate에 걸친 로직
4. **Domain Event** — 주문 체결 시 `OrderExecutedEvent` 발행
5. **Repository는 Aggregate 단위** — `OrderRepository`는 Order Aggregate만

---

### 과제 1-4. 전략적 DDD (난이도 ★★★)

**할 일**
1. **Bounded Context** 식별
   - 사용자 관리 / 주문 / 시세 / 거래 분석
2. **Context Map** 그리기 — 각 context 간 관계
3. **Anti-Corruption Layer** — 외부 거래소 모델을 우리 도메인에 침투 못하게

---

## Week 3-4: 메시징 & 이벤트 기반

### 과제 2-1. Kafka 기초 (난이도 ★★)

**할 일**
1. docker-compose로 Kafka + Zookeeper(또는 KRaft) 띄우기
2. CLI로 topic 생성, producer/consumer 실행
3. **개념 이해**
   - Topic / Partition / Offset
   - Consumer Group / Rebalancing
   - Replication / ISR
   - Producer 의미: acks=0/1/all
4. 파티션 수 변경 시 영향 실험

---

### 과제 2-2. Spring Kafka 적용 (난이도 ★★★)

**할 일**
1. `spring-kafka` 의존성 추가
2. **Producer** — `KafkaTemplate`로 시세 이벤트 발행
3. **Consumer** — `@KafkaListener`로 거래 알고리즘 실행
4. **JSON 직렬화** — Jackson 또는 Avro + Schema Registry
5. **Manual Acknowledge** — 처리 실패 시 재시도
6. **DLQ (Dead Letter Queue)** — 영구 실패 메시지 격리

---

### 과제 2-3. Event-Driven Architecture (난이도 ★★★)

**할 일**
trader-bot을 EDA로 재설계

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

**체크리스트**
- [ ] 각 서비스는 독립 배포 가능
- [ ] 한 서비스 장애가 전체 마비로 안 이어짐
- [ ] 새 구독자 추가 시 기존 서비스 변경 없음

---

### 과제 2-4. 분산 트랜잭션: Saga 패턴 (난이도 ★★★)

**시나리오:** 주문 생성 = 주문 저장 + 잔고 차감 + 외부 거래소 호출 — DB 트랜잭션 하나로 못 묶음

**할 일**
1. **Choreography Saga** — 이벤트 체인
   - 실패 시 보상 트랜잭션(Compensating Action)
2. **Orchestration Saga** — Orchestrator가 단계 관리
   - `Spring StateMachine` 또는 직접 구현
3. **실패 시나리오 테스트** — 각 단계마다 일부러 실패시켜서 보상 동작 확인

---

### 과제 2-5. Outbox 패턴 (난이도 ★★★)

**목표:** "DB 저장과 이벤트 발행을 원자적으로"

**문제:** DB save 성공 → Kafka publish 실패 → 데이터 불일치

**할 일**
1. **Outbox 테이블** 만들기 — 이벤트를 같은 트랜잭션에서 저장
2. **Polling Publisher** — 별도 스레드가 Outbox 조회 후 Kafka 발행
3. (고급) **Debezium**으로 PostgreSQL WAL 읽어서 자동 발행

---

## Week 5-6: 분산 시스템 깊이

### 과제 3-1. CAP / PACELC (난이도 ★★)

**할 일** — 이론 정리 + 실험
1. **CAP 정리** — Consistency / Availability / Partition tolerance
2. **PACELC** — 파티션 없을 때도 Latency vs Consistency 선택
3. trader-bot 컴포넌트별 분류
   - PostgreSQL: CP
   - Redis: AP (옵션에 따라 다름)
   - Kafka: CP (acks=all 기준)

---

### 과제 3-2. 일관성 모델 (난이도 ★★★)

**할 일**
1. **Strong Consistency** — DB 트랜잭션
2. **Eventual Consistency** — 이벤트 기반 시스템
3. **Read-your-writes Consistency** — 내가 쓴 건 내가 즉시 읽음
4. trader-bot에서 어디는 강한 일관성이 필요하고, 어디는 결과적 일관성으로 충분한지 분류

---

### 과제 3-3. 멱등성 & 정확히 한 번 처리 (난이도 ★★★)

**할 일**
1. **At-most-once / At-least-once / Exactly-once** 차이
2. Kafka **Exactly-once** 의미와 한계
3. **Idempotent Consumer** 구현
   - 메시지 ID를 DB에 저장 → 중복 처리 방지
4. trader-bot 주문 처리에 적용

---

### 과제 3-4. Circuit Breaker & Resilience (난이도 ★★★)

**할 일**
1. **Resilience4j** 적용
   - Circuit Breaker
   - Retry
   - Rate Limiter
   - Bulkhead
   - TimeLimiter
2. 모두 조합 — 외부 거래소 API 호출에 적용
3. **Chaos Engineering** — 일부러 외부 API 죽이고 시스템 동작 확인
4. **Toxiproxy**로 네트워크 지연/단절 시뮬레이션

---

## Week 7-8: MSA & 성능

### 과제 4-1. MSA 분해 (난이도 ★★★)

**할 일**
1. trader-bot을 다음 서비스로 분해
   - User Service
   - Order Service
   - Quote Service (시세)
   - Trade Service
   - Notification Service
2. **DB per Service** — 각 서비스 전용 DB
3. **API Gateway** (Spring Cloud Gateway)
4. **Service Discovery** (Eureka 또는 K8s Service)

> ⚠️ **주의:** 처음부터 MSA는 비추. "모놀리스 first"가 원칙. 학습 목적으로만.

---

### 과제 4-2. 캐싱 전략 심화 (난이도 ★★)

**할 일**
1. **Multi-level Caching** — Caffeine (로컬) + Redis (분산)
2. **Cache Aside vs Read Through vs Write Through vs Write Behind**
3. **Cache Stampede** 방지
   - Probabilistic Early Recomputation
   - 분산 락
4. **CDN 캐싱** — CloudFront로 정적 자산

---

### 과제 4-3. 데이터베이스 확장 (난이도 ★★★)

**할 일**
1. **Read Replica** — 읽기 분산
   - Spring `AbstractRoutingDataSource`로 master/slave 분기
2. **샤딩**
   - 사용자 ID 기반 수평 샤딩
   - **ShardingSphere** 적용
3. **CQRS** — 명령(쓰기)과 조회(읽기) 분리
   - 쓰기: PostgreSQL
   - 읽기: Elasticsearch (검색용 인덱스)

---

### 과제 4-4. 시스템 디자인 연습 (난이도 ★★★)

**목표:** 면접 + 실제 설계 역량

매주 1개씩 설계해보기 (가상 시나리오)

1. **URL 단축 서비스** — bit.ly 같은
2. **실시간 채팅** — 동시접속 10만
3. **거래소 매칭 엔진** — 초당 100만 주문 처리
4. **뉴스피드** — 인스타그램/X
5. **검색 자동완성** — 1억 검색어
6. **분산 락 서비스** — Redis보다 안정적인

각각에 대해 다음 작성
- 요구사항 (기능 + 비기능)
- 추정 (QPS, 데이터 크기)
- 아키텍처 다이어그램
- 핵심 컴포넌트 상세
- bottleneck 식별 + 확장 전략

---

## 종합 과제

### "trader-bot 분산 시스템화"

기존 trader-bot을 아래 요구사항을 모두 만족하도록 재구축

**기능 요구사항**
- 동시 사용자 1만 명
- 초당 1000건 주문 처리
- 시세 실시간 push (지연 100ms 이내)
- 99.9% 가용성

**아키텍처 요구사항**
- [x] 헥사고날 아키텍처
- [x] 도메인 이벤트 + Kafka
- [x] Outbox 패턴
- [x] Saga로 분산 트랜잭션
- [x] CQRS (쓰기/읽기 분리)
- [x] Read Replica
- [x] Multi-level 캐싱
- [x] Circuit Breaker
- [x] 분산 추적 (OpenTelemetry)
- [x] HPA로 자동 스케일

**검증**
- k6로 부하 테스트 → p99 < 200ms
- 외부 API 장애 시뮬레이션 → 자동 복구
- 1개 노드 다운 → 무중단
- 데이터 일관성 검증 (10만 건 거래 후 잔고 합계 일치)

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
- [ ] Week 1-2: 아키텍처 패턴 (헥사고날, DDD)
- [ ] Week 3-4: 메시징 & 이벤트 (Kafka, Saga, Outbox)
- [ ] Week 5-6: 분산 시스템 이론 + Resilience
- [ ] Week 7-8: MSA + 성능 + 시스템 디자인
- [ ] 종합 과제: trader-bot 분산 시스템화

---

## 졸업 체크리스트

이 단계까지 완주하면 다음이 가능해야 한다.

- [ ] "이 기능, 어떻게 설계하시겠어요?" → 30분 안에 화이트보드에 그릴 수 있다
- [ ] 트래픽 10배 증가 시나리오에서 병목과 해결책을 말할 수 있다
- [ ] 분산 시스템의 일관성/가용성 trade-off를 상황별로 판단할 수 있다
- [ ] 장애 발생 시 트레이스/로그/메트릭을 조합해 근본 원인을 찾을 수 있다
- [ ] 코드 리뷰에서 "왜 그렇게 짰는지"를 설계 관점에서 설명할 수 있다

여기까지 오면 미드 시니어 ~ 시니어 수준입니다. 화이팅!
