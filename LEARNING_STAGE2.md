# 2단계 실습 가이드: 데이터베이스 & SQL

> **기간:** 4주
> **선수 과정:** [1단계](./LEARNING_STAGE1.md) 완료 (특히 JPA N+1, 트랜잭션 전파/격리)
> **현재 DB 스택:** PostgreSQL · Redis · JPA

---

## 학습 원칙

1. **"JPA가 만든 SQL"을 항상 본다** — `show-sql=true`로 시작, 점차 실행 계획까지 읽기
2. **EXPLAIN 없이는 인덱스 얘기하지 않는다** — 추측 금지, 측정만
3. **재현 데이터** — 부하 테스트용 더미 데이터 100만 건 이상 미리 만들어 두기
4. **실험은 격리된 DB에서** — Testcontainers 또는 Docker로 일회용 PostgreSQL 사용

---

## Week 1: SQL 기본기 & 실행 계획

### 과제 1-1. 더미 데이터 100만 건 생성 (난이도 ★)

**목표:** 모든 실험의 베이스 데이터 구축. 데이터가 적으면 인덱스 효과가 안 보임.

**할 일**
1. `User` 1만 명, `Account` 3만 개, `Order` 100만 건, `Trade` 500만 건 생성
2. PostgreSQL `generate_series`로 SQL 한 방에 생성
   ```sql
   INSERT INTO orders (user_id, symbol, price, quantity, created_at)
   SELECT
     (random() * 10000)::int + 1,
     (ARRAY['005930','000660','035720'])[floor(random()*3+1)],
     (random() * 100000)::numeric(10,2),
     (random() * 100)::int,
     now() - (random() * interval '365 days')
   FROM generate_series(1, 1000000);
   ```
3. `ANALYZE` 실행해서 통계 갱신
4. 데이터 분포가 한쪽에 쏠리지 않게 (인덱스 실험용)

**산출물**: `/backend/docs/db-seed.sql`

---

### 과제 1-2. EXPLAIN 읽기 (난이도 ★★)

**목표:** 실행 계획 4종 세트 해석

**할 일**
1. 다음 4가지 명령 모두 써보고 차이 이해
   - `EXPLAIN` — 예측만
   - `EXPLAIN ANALYZE` — 실제 실행 + 시간
   - `EXPLAIN (ANALYZE, BUFFERS)` — 버퍼 사용량까지
   - `EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)` — 도구로 시각화 (explain.dalibo.com)
2. 다음 스캔 방식을 모두 코드로 재현
   - Seq Scan
   - Index Scan
   - Index Only Scan (커버링 인덱스)
   - Bitmap Heap Scan
3. **Hash Join / Merge Join / Nested Loop** 셋 다 발생시키기 — `enable_hashjoin = off` 등으로 강제

**산출물**: 실행 계획 캡처와 해석 노트

---

### 과제 1-3. 느린 쿼리 만들고 튜닝 (난이도 ★★)

**목표:** "실행시간 → EXPLAIN → 인덱스 → 재측정" 사이클 체득

**할 일**
다음 쿼리들을 인덱스 없이 실행 시간 측정 → 인덱스 추가 → 다시 측정

1. 특정 사용자의 최근 30일 주문 조회 — `(user_id, created_at DESC)` 인덱스
2. 종목별 매도 거래 합계 — 집계 쿼리 인덱스 전략
3. 부분 일치 검색 (`LIKE 'BTC%'`) vs (`LIKE '%BTC%'`) — 인덱스 사용 여부
4. `IN (1000개)` 절 — 인덱스 사용되는가? 성능 어떤가?

**산출물**: Before/After 응답시간 표

---

## Week 2: 인덱스 깊이 파기

### 과제 2-1. B-Tree 인덱스 동작 이해 (난이도 ★★)

**목표:** "왜 첫 컬럼만 효과 있는가" 같은 질문에 답할 수 있게

**할 일**
1. 복합 인덱스 `(user_id, symbol, created_at)` 만든 후 다음 쿼리들의 인덱스 사용 여부 확인
   - `WHERE user_id = ?` ✓
   - `WHERE user_id = ? AND symbol = ?` ✓
   - `WHERE symbol = ?` ✗ (왜?)
   - `WHERE user_id = ? AND created_at > ?` (부분 사용)
   - `WHERE user_id = ? ORDER BY created_at` (정렬에 인덱스 활용)
2. **카디널리티** 실험 — `gender` 같은 낮은 카디널리티 컬럼에 인덱스 걸어보고 효과 측정
3. PostgreSQL의 **부분 인덱스(Partial Index)** — 활성 주문만 인덱싱
   ```sql
   CREATE INDEX idx_active_orders ON orders (user_id) WHERE status = 'OPEN';
   ```

**산출물**: 복합 인덱스 컬럼 순서 결정 가이드

---

### 과제 2-2. 커버링 인덱스 (난이도 ★★)

**목표:** Index Only Scan으로 테이블 접근 자체를 없애기

**할 일**
1. `SELECT symbol, price FROM orders WHERE user_id = ?` 쿼리
2. 일반 인덱스 `(user_id)` → Index Scan + 테이블 lookup
3. `INCLUDE` 절로 커버링 인덱스
   ```sql
   CREATE INDEX idx_orders_covering ON orders (user_id) INCLUDE (symbol, price);
   ```
4. Index Only Scan 발생 확인 + 성능 비교

---

### 과제 2-3. 다양한 인덱스 타입 (난이도 ★★)

PostgreSQL이 제공하는 인덱스 타입 직접 써보기

| 타입 | 용도 | 과제 |
|---|---|---|
| B-Tree | 기본 | 위에서 다 함 |
| Hash | 등호 비교만 | 만들어보고 B-Tree와 비교 (대부분 B-Tree가 나음 — 이유?) |
| **GIN** | 배열, JSONB, 전문검색 | 주문 metadata JSONB 컬럼에 GIN 적용 |
| **BRIN** | 시계열 거대 테이블 | 1억 건 거래 로그에 BRIN — 크기와 성능 비교 |
| GiST | 공간/범위 | (선택) |

---

### 과제 2-4. 인덱스의 비용 측정 (난이도 ★★)

**목표:** "인덱스는 무조건 좋다"는 환상 깨기

**할 일**
1. 100만 건 INSERT 시간 측정 — 인덱스 0개 / 5개 / 10개일 때
2. UPDATE 시간 비교 — 인덱스 컬럼 vs 비인덱스 컬럼
3. 디스크 사용량 비교: `\di+` 명령으로 인덱스 크기 확인
4. `pg_stat_user_indexes`로 **안 쓰이는 인덱스** 찾기

**산출물**: 인덱스 추가 시 고려할 체크리스트

---

## Week 3: 트랜잭션 & 동시성

### 과제 3-1. 격리 수준 4가지 실험 (난이도 ★★★)

**목표:** 동시성 문제 종류와 격리 수준 매핑 체득

**준비물:** psql 터미널 2개 열고 동시 실행

**할 일**
각 격리 수준에서 다음 현상 재현/차단 확인

| 현상 | READ UNCOMMITTED | READ COMMITTED (PG 기본) | REPEATABLE READ | SERIALIZABLE |
|---|---|---|---|---|
| Dirty Read | ✓ 발생 (PG는 불가) | ✗ | ✗ | ✗ |
| Non-repeatable Read | ✓ | ✓ | ✗ | ✗ |
| Phantom Read | ✓ | ✓ | △ (PG는 ✗) | ✗ |
| Lost Update | ✓ | ✓ | ✗ | ✗ |

> PostgreSQL의 REPEATABLE READ는 **Snapshot Isolation** — MySQL과 다름

**할 일 (구체)**
1. 트랜잭션 A: 잔고 조회 → 1초 sleep → 출금 → commit
2. 트랜잭션 B: 같은 사용자 잔고를 동시에 출금
3. 두 트랜잭션이 모두 성공해서 **잔고가 마이너스가 되는** Lost Update 재현
4. 격리 수준을 SERIALIZABLE로 올려서 한쪽이 실패하는지 확인

**산출물**: 시나리오별 실행 순서 + 결과 표

---

### 과제 3-2. 비관적 락 vs 낙관적 락 (난이도 ★★★)

**목표:** 동시성 제어 전략 선택 기준

**할 일**
1. **비관적 락** — `SELECT FOR UPDATE` (`@Lock(LockModeType.PESSIMISTIC_WRITE)`)
   - 잔고 차감 로직에 적용
   - 동시 요청 100개 부하 → 처리 시간 측정
2. **낙관적 락** — `@Version`
   - 같은 로직 다시
   - `OptimisticLockException` 발생 시 재시도 로직 (`@Retryable`)
   - 동시 요청 100개 → 처리 시간 + 재시도 횟수 측정
3. **데드락 만들고 잡기**
   - 트랜잭션 A: 사용자1 → 사용자2 순서로 락
   - 트랜잭션 B: 사용자2 → 사용자1 순서로 락
   - PostgreSQL이 데드락 감지하고 한쪽 죽이는 것 확인
   - 해결책: 락 획득 순서 통일 (작은 ID부터)

**산출물**: 어떤 상황에 어느 락 쓸지 결정 트리

---

### 과제 3-3. 분산 락 — Redis (난이도 ★★★)

**목표:** DB 락이 답이 아닌 상황 대응

**시나리오:** "사용자가 동시에 두 디바이스에서 주문 → 한 번만 처리되어야 함"

**할 일**
1. **Redisson**으로 분산 락 적용
   ```java
   RLock lock = redissonClient.getLock("order:user:" + userId);
   lock.tryLock(3, 10, TimeUnit.SECONDS);
   ```
2. AOP로 `@DistributedLock("order:user:#{#userId}")` 어노테이션 만들기
3. **펜싱 토큰** 개념 학습 — Redis 락이 안전하지 않은 케이스
4. (선택) Lettuce + `SET NX EX`로 직접 구현해보기 — Redisson과의 차이 이해

---

## Week 4: 성능 & Redis 캐싱

### 과제 4-1. Spring Cache + Redis 적용 (난이도 ★★)

**목표:** 캐싱 전략과 함정 이해

**할 일**
1. `@EnableCaching` + Redis CacheManager 설정
2. 시세 조회 API에 `@Cacheable("quotes")` 적용 — TTL 5초
3. 거래 발생 시 `@CacheEvict`로 무효화
4. **캐시 패턴 4가지** 모두 코드로 구현
   - Cache-Aside (Look-Aside)
   - Read-Through
   - Write-Through
   - Write-Behind
5. **함정 재현**
   - 캐시 스탬피드(Cache Stampede) — TTL 만료 시점에 동시 요청 몰림
   - 해결: PER (Probabilistic Early Recomputation) 또는 분산 락

---

### 과제 4-2. Redis 자료구조 활용 (난이도 ★★)

trader-bot 도메인에 Redis 자료구조 적용

| 자료구조 | 용도 | trader-bot 적용 |
|---|---|---|
| String | 단순 캐시 | 환율, 시세 |
| Hash | 객체 | 사용자 세션 |
| **Sorted Set** | 랭킹 | 수익률 랭킹 보드 |
| **Stream** | 이벤트 큐 | 시세 변동 이벤트 |
| HyperLogLog | 중복 제거 카운트 | 일일 활성 사용자 |
| Bitmap | 출석 체크 | 사용자 일별 로그인 |

**산출물**: 각 자료구조의 명령어 + 적용 코드

---

### 과제 4-3. Connection Pool 튜닝 (난이도 ★★)

**목표:** HikariCP 동작 이해

**할 일**
1. 기본 풀 사이즈로 부하 테스트 → 응답 시간 측정
2. `maximumPoolSize`를 5, 10, 20, 50, 100으로 바꾸며 측정
3. **"풀 크기 = CPU 코어 수 × 2 + 디스크 수"** 공식 확인
4. `leakDetectionThreshold` 켜고 트랜잭션 누수 만들어서 잡기
5. PostgreSQL `max_connections`과의 관계 — 앱 서버 N대 × 풀크기 < max_connections

---

### 과제 4-4. 슬로우 쿼리 모니터링 (난이도 ★★)

**할 일**
1. PostgreSQL `log_min_duration_statement = 500` — 500ms 이상 쿼리 로그
2. `pg_stat_statements` 익스텐션 활성화 → 쿼리별 통계
3. 자주 호출되는 Top 10 쿼리 + 가장 느린 Top 10 쿼리 추출
4. Spring `p6spy` 또는 `datasource-proxy`로 애플리케이션 레벨 SQL 로그
5. 통합 테스트에서 **N+1 자동 감지** — `db-scheduler` 또는 직접 카운팅

---

## Week 4 보너스: NoSQL 맛보기 (선택)

### 과제 5-1. MongoDB로 거래 로그 저장 (난이도 ★★)

**목표:** "언제 NoSQL을 쓰는가" 감 잡기

**할 일**
1. 거래 로그(스키마 가변, 추가만 발생, 분석용)를 MongoDB에 저장
2. Spring Data MongoDB로 구현
3. 같은 쿼리를 PostgreSQL vs MongoDB로 비교
4. **언제 NoSQL이 적합한가** 정리

---

## 종합 과제

### "트레이딩 대시보드 성능 최적화" 프로젝트

기존 trader-bot에 다음 API 추가하고 **응답시간 100ms 이하** 달성

**API 요구사항**
- `GET /api/dashboard/{userId}` — 사용자 대시보드
  - 보유 종목 리스트 (최근 가격 포함)
  - 최근 30일 주문 100건
  - 누적 수익률
  - 동일 종목 보유 사용자 랭킹 TOP 10

**최적화 단계**
1. **Baseline 측정** — 아무 최적화 없이 응답시간 (아마 수 초)
2. **EXPLAIN으로 병목 쿼리 식별**
3. 단계별 적용 + 각 단계 응답시간 기록
   - 적절한 인덱스 추가
   - N+1 제거 (fetch join / batch size)
   - 커버링 인덱스
   - Redis 캐시 적용
   - 랭킹은 Redis Sorted Set으로
4. **부하 테스트** — k6로 동시 사용자 500명 시뮬레이션
5. **최종 결과 문서화** — Before/After 응답시간, 쿼리 수, DB CPU 사용률

**체크리스트**
- [ ] 응답 시간 100ms 이하
- [ ] DB 쿼리 5개 이하 (Redis hit 시 0~1개)
- [ ] 동시 500 사용자에서 p99 200ms 이하
- [ ] 캐시 무효화 정책 명확
- [ ] 분산 락으로 동시성 보호

---

## 추천 학습 자료

| 주제 | 자료 |
|---|---|
| SQL 일반 | "SQL Performance Explained" (Markus Winand) — 짧고 핵심만 |
| PostgreSQL | 공식 문서 "Performance Tips" 챕터, "PostgreSQL 9 Administration Cookbook" |
| MySQL (참고) | "Real MySQL 8.0" 1, 2권 |
| Redis | "Redis 운영과 활용" / Redis University 무료 강의 |
| 트랜잭션 | "데이터 중심 애플리케이션 설계" (마틴 클레프만) 7장 |

---

## 진도 체크

- [ ] Week 1: SQL 기본기 & 실행 계획
- [ ] Week 2: 인덱스 깊이 파기
- [ ] Week 3: 트랜잭션 & 동시성
- [ ] Week 4: Redis 캐싱 & 튜닝
- [ ] 종합 과제: 트레이딩 대시보드 최적화

---

## 1단계와의 연결 / 3단계 예고

- 1단계 JPA 트랜잭션 전파 → 2단계 격리 수준에서 한 단계 더 깊이
- 1단계 N+1 → 2단계에서 EXPLAIN으로 검증
- 다음 단계 (네트워크) 에서는 DB 커넥션도 결국 TCP라는 관점으로 이어짐
