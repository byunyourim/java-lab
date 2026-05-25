# 2단계: 데이터베이스 & SQL — 하루 단위 커리큘럼

> **기간:** 4주 (Week 9–12)
> **선수 과정:** [1단계](./LEARNING_STAGE1.md) 완료 (특히 JPA N+1, 트랜잭션 전파/격리)
> **목표:** "이 쿼리가 왜 느린지", "이 인덱스가 왜 안 타는지" 설명할 수 있는 수준
> **하루:** 평일 2~3h / 토 5~6h / 일 2~3h (오후 휴식)
> **코딩 장소:** java-lab (실험) / trader-bot (적용)
> **매주 필수:** PR 1개 + 테스트 + 측정 + 블로그 1편
> **현재 DB 스택:** PostgreSQL · Redis · JPA

> **학습 원칙:**
> 1. "JPA가 만든 SQL"을 항상 본다 — `show-sql=true`로 시작, 실행 계획까지 읽기
> 2. EXPLAIN 없이는 인덱스 얘기하지 않는다 — 추측 금지, 측정만
> 3. 재현 데이터 — 부하 테스트용 더미 데이터 100만 건 이상
> 4. 실험은 격리된 DB에서 — Testcontainers 또는 Docker로 일회용 PostgreSQL

---

# Week 9 — SQL 기본기 & 실행 계획

---

## Day 57 (월) — PostgreSQL 아키텍처 이해

**이해 (2h)**
- PostgreSQL 전체 구조 그려보기 (종이에 직접)
  - Client → Postmaster → Backend Process
  - Shared Buffer → WAL Buffer → Disk
- 주요 프로세스 이해
  - **Postmaster**: 연결 관리자, 자식 프로세스 생성
  - **Backend Process**: 클라이언트 당 하나, 쿼리 처리
  - **Background Writer**: shared buffer → disk 쓰기
  - **WAL Writer**: WAL 버퍼 → WAL 파일
  - **Autovacuum**: MVCC 정리
  - **Checkpointer**: 주기적 체크포인트
- **왜 질문:**
  - PostgreSQL은 왜 프로세스 기반인가? (MySQL은 스레드 기반인데)
  - WAL(Write-Ahead Log)이 왜 필요한가? 바로 데이터 파일에 쓰면 안 되나?
  - Shared Buffer는 JVM의 어떤 개념과 유사한가?
  - MVCC(Multi-Version Concurrency Control)란? 왜 필요한가?

**참고:** PostgreSQL 공식 문서 "Database System Concepts" 챕터

---

## Day 58 (화) — 더미 데이터 100만 건 생성

**코드 (2.5h) — java-lab 또는 trader-bot**
- [ ] 테이블 구조 설계 (실험용)
  - `users` 1만 명, `accounts` 3만 개, `orders` 100만 건, `trades` 500만 건
- [ ] PostgreSQL `generate_series`로 데이터 한 방 생성
  ```sql
  INSERT INTO orders (user_id, symbol, price, quantity, created_at)
  SELECT
    (random() * 10000)::int + 1,
    (ARRAY['005930','000660','035720','051910','006400'])[floor(random()*5+1)],
    (random() * 100000)::numeric(10,2),
    (random() * 100)::int + 1,
    now() - (random() * interval '365 days')
  FROM generate_series(1, 1000000);
  ```
- [ ] `ANALYZE` 실행 → 통계 갱신
- [ ] `pg_stat_user_tables`로 데이터 분포 확인
- **왜 질문:**
  - `ANALYZE`가 왜 필요한가? 안 하면 실행 계획이 어떻게 달라지나?
  - 통계 정보(pg_stats)에는 뭐가 들어있나? (n_distinct, histogram_bounds 등)
  - 데이터 분포가 치우치면 쿼리 플래너에 어떤 영향을 주는가?

---

## Day 59 (수) — EXPLAIN 4종 세트 마스터

**이해 + 코드 (2.5h)**
- [ ] EXPLAIN 4가지 모드 모두 실행 + 차이 이해
  - `EXPLAIN` — 예측만 (실제 실행 안 함)
  - `EXPLAIN ANALYZE` — 실제 실행 + 시간 측정
  - `EXPLAIN (ANALYZE, BUFFERS)` — 버퍼 사용량까지
  - `EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)` — JSON으로 시각화
- [ ] explain.dalibo.com 에 JSON 붙여넣기 → 시각화 읽기
- [ ] **왜 질문:**
  - `cost=0.00..35.50`의 단위는 뭔가? (임의 단위, seq_page_cost 기준)
  - `rows`는 예측인데, `ANALYZE`의 `actual rows`와 왜 다를 수 있나?
  - `Buffers: shared hit=100 read=50`에서 hit과 read의 차이는?
  - Planning Time vs Execution Time — 언제 Planning이 더 오래 걸리나?
- [ ] 스캔 방식 4가지 재현
  - **Seq Scan**: 인덱스 없이 전체 테이블 스캔
  - **Index Scan**: 인덱스 타고 테이블 lookup
  - **Index Only Scan**: 인덱스만으로 결과 반환 (커버링)
  - **Bitmap Heap Scan**: 여러 행 범위 조회 시
- **왜 질문:**
  - Seq Scan이 Index Scan보다 빠를 때는 언제인가? (소량 데이터, 높은 selectivity)
  - Bitmap Scan은 왜 두 단계(Bitmap Index Scan + Bitmap Heap Scan)인가?

---

## Day 60 (목) — JOIN 실행 계획 3가지

**이해 + 코드 (2.5h)**
- [ ] JOIN 알고리즘 3가지 발생시키기
  - **Nested Loop**: 소량 데이터 조인 (인덱스 있을 때)
  - **Hash Join**: 등호 조인, 한쪽 테이블이 메모리에 들어갈 때
  - **Merge Join**: 양쪽 정렬된 데이터 조인
- [ ] 각각 강제 발생시키기
  ```sql
  SET enable_hashjoin = off;
  SET enable_mergejoin = off;
  -- 또는 enable_nestloop = off
  ```
- [ ] 같은 쿼리인데 JOIN 방식이 다를 때 성능 차이 측정
- **왜 질문:**
  - Nested Loop는 언제 선택되나? (외부 테이블 행 수 적고, 내부에 인덱스 있을 때)
  - Hash Join이 메모리 부족하면? (`work_mem` 초과 시 디스크로 spill)
  - Merge Join은 왜 정렬이 전제인가? 정렬 비용이 있는데 왜 빠를 수 있나?
  - `work_mem`을 키우면 무조건 좋은가? 왜 기본값이 작은가?
- [ ] `SET work_mem = '256MB'` 전후 실행 계획 비교

---

## Day 61 (금) — 느린 쿼리 만들고 튜닝

**코드 (2.5h)**
- [ ] 인덱스 없이 다음 쿼리 실행 시간 측정
  1. 특정 사용자의 최근 30일 주문 조회
  2. 종목별 매도 거래 합계 (GROUP BY)
  3. 부분 일치 검색 `LIKE 'BTC%'` vs `LIKE '%BTC%'`
  4. `IN (1000개)` 절 쿼리
- [ ] 각 쿼리에 적절한 인덱스 추가
  - `CREATE INDEX idx_orders_user_date ON orders (user_id, created_at DESC);`
- [ ] 인덱스 추가 후 다시 측정 → Before/After 표 작성
- **왜 질문:**
  - `LIKE 'BTC%'`는 인덱스 타는데 `LIKE '%BTC%'`는 왜 안 타나? (B-Tree 정렬 원리)
  - `IN (1000개)`는 인덱스를 탈까? 성능은? (대부분 탐, 하지만 너무 많으면 Seq Scan)
  - 집계(GROUP BY) 쿼리에 인덱스가 도움이 되려면?
  - `ORDER BY created_at DESC`에 인덱스를 걸면 정렬이 필요 없어지는 이유는?
- [ ] `pg_stat_statements` 활성화 → 쿼리별 통계 확인

---

## Day 62 (토) — B-Tree 인덱스 깊이 파기

**이해 + 코드 (5h)**

오전 (3h) — B-Tree 구조 이해 + 복합 인덱스
- [ ] B-Tree 구조 종이에 그려보기
  - Root → Internal → Leaf 노드
  - Leaf 노드에 실제 데이터 위치(ctid) 저장
  - **왜 질문:** B-Tree가 왜 DB 인덱스의 기본인가? (범위 검색 + 정렬 + 등호 모두 가능)
  - **왜 질문:** B-Tree와 B+Tree의 차이는? PostgreSQL은 어느 쪽?
- [ ] 복합 인덱스 `(user_id, symbol, created_at)` 생성 후 실험
  - `WHERE user_id = ?` → ✓ 사용
  - `WHERE user_id = ? AND symbol = ?` → ✓ 사용
  - `WHERE symbol = ?` → ✗ 사용 안 됨
  - `WHERE user_id = ? AND created_at > ?` → 부분 사용
  - `WHERE user_id = ? ORDER BY created_at` → 정렬에 인덱스 활용
- **왜 질문:**
  - 복합 인덱스에서 첫 번째 컬럼만 효과 있는 이유는? (전화번호부 비유: 성→이름 순)
  - `(A, B, C)` 인덱스에서 `WHERE A = ? AND C = ?`는? (A만 인덱스 사용)
  - 컬럼 순서를 어떻게 결정하는가? (카디널리티? 사용 빈도?)

오후 (2h) — 카디널리티 + 부분 인덱스
- [ ] 카디널리티 실험
  - 성별(M/F) 같은 낮은 카디널리티 컬럼에 인덱스 → 효과 측정
  - **왜 질문:** 카디널리티가 낮으면 인덱스가 왜 효과 없나? (선택도가 낮아서 결국 대부분 읽음)
- [ ] PostgreSQL 부분 인덱스(Partial Index)
  ```sql
  CREATE INDEX idx_active_orders ON orders (user_id) WHERE status = 'OPEN';
  ```
  - 활성 주문만 인덱싱 → 인덱스 크기 비교
  - **왜 질문:** 부분 인덱스는 왜 MySQL에는 없는가?
- [ ] `pg_stats` 테이블에서 n_distinct, most_common_vals 확인

---

## Day 63 (일) — Week 9 정리 + 블로그

**오전 (2.5h)**
- [ ] EXPLAIN 읽는 법 치트시트 정리
- [ ] JOIN 알고리즘 3가지 + 선택 기준 정리
- [ ] **블로그 작성:** "EXPLAIN ANALYZE 실전 가이드 — 100만 건에서 느린 쿼리 튜닝하기"
- [ ] 다음 주 예습: 커버링 인덱스, GIN/BRIN 인덱스 문서 훑기
- 오후: 휴식

**Week 9 PR:** 더미 데이터 생성 스크립트 + 쿼리 튜닝 Before/After + EXPLAIN 분석 노트

---

# Week 10 — 인덱스 심화

---

## Day 64 (월) — 커버링 인덱스 (Index Only Scan)

**이해 + 코드 (2.5h)**
- [ ] 커버링 인덱스란? — 테이블 접근 없이 인덱스만으로 쿼리 완료
- [ ] 실험
  - `SELECT symbol, price FROM orders WHERE user_id = ?`
  - 일반 인덱스 `(user_id)` → Index Scan + Heap Fetch
  - 커버링 인덱스:
    ```sql
    CREATE INDEX idx_orders_covering ON orders (user_id) INCLUDE (symbol, price);
    ```
  - Index Only Scan 발생 확인 + 성능 비교
- **왜 질문:**
  - `INCLUDE`와 복합 인덱스 `(user_id, symbol, price)`의 차이는?
  - INCLUDE 컬럼은 B-Tree 내부 노드에 안 들어가는데, 왜?
  - Index Only Scan인데 Heap Fetches가 0이 아닐 때는? (Visibility Map과 VACUUM 관계)
  - **왜 질문:** VACUUM이 안 돌면 Index Only Scan이 비효율적인 이유는?
- [ ] `pg_stat_user_indexes`에서 idx_scan, idx_tup_read 확인

---

## Day 65 (화) — 다양한 인덱스 타입

**이해 + 코드 (2.5h)**
- [ ] PostgreSQL 인덱스 타입 실험

| 타입 | 용도 | 실험 |
|---|---|---|
| B-Tree | 기본 (범위, 정렬, 등호) | 이미 완료 |
| Hash | 등호 비교만 | 생성 후 B-Tree와 비교 |
| **GIN** | 배열, JSONB, 전문검색 | JSONB 컬럼에 적용 |
| **BRIN** | 시계열 거대 테이블 | created_at에 적용 |
| GiST | 공간/범위 | (선택) |

- [ ] Hash 인덱스 실험
  - `CREATE INDEX idx_hash ON orders USING hash (symbol);`
  - **왜 질문:** Hash가 B-Tree보다 나은 상황은? (거의 없음 — B-Tree도 등호 빠름)
  - **왜 질문:** PostgreSQL 10 이전에 Hash 인덱스가 왜 위험했나? (WAL 미지원)
- [ ] GIN 인덱스 — JSONB 검색
  - orders에 `metadata jsonb` 컬럼 추가
  - `CREATE INDEX idx_gin ON orders USING gin (metadata);`
  - `WHERE metadata @> '{"type": "market"}'` 쿼리 성능 확인
  - **왜 질문:** GIN은 왜 Generalized Inverted Index인가? (키→문서 목록 역방향 매핑)
- [ ] BRIN 인덱스 — 시계열 데이터
  - `CREATE INDEX idx_brin ON orders USING brin (created_at);`
  - **왜 질문:** BRIN이 B-Tree보다 크기가 수백 배 작은 이유는? (블록 범위 요약만 저장)
  - **왜 질문:** BRIN은 어떤 데이터에 효과적인가? (물리적으로 정렬된 데이터 — 시계열)
  - 크기 비교: `\di+` 명령

---

## Day 66 (수) — 인덱스의 비용 측정

**코드 (2.5h)**
- [ ] INSERT 성능 영향 측정
  - 인덱스 0개 → 100만 건 INSERT 시간
  - 인덱스 5개 추가 후 → 같은 INSERT 시간
  - 인덱스 10개 → 같은 INSERT 시간
  - **결과 표 작성**
- [ ] UPDATE 성능 영향
  - 인덱스 걸린 컬럼 UPDATE vs 안 걸린 컬럼 UPDATE 비교
  - **왜 질문:** 인덱스 컬럼 UPDATE가 왜 느린가? (인덱스도 같이 갱신해야 하니까)
  - **왜 질문:** PostgreSQL의 HOT(Heap-Only Tuple) 업데이트란? 인덱스 안 타는 UPDATE?
- [ ] 디스크 사용량 비교
  - `\di+` 로 인덱스 크기 확인
  - 테이블 크기 대비 인덱스 크기 비율
- [ ] 안 쓰이는 인덱스 찾기
  - `pg_stat_user_indexes`에서 `idx_scan = 0`인 인덱스
  - **왜 질문:** 안 쓰이는 인덱스를 왜 지워야 하나? (쓰기 성능 + 디스크 + VACUUM 비용)
- [ ] **인덱스 추가 시 체크리스트 작성**
  - WHERE 절 컬럼 확인, 카디널리티, 쓰기 빈도, 디스크 여유

---

## Day 67 (목) — PostgreSQL 고급 인덱스 기법

**이해 + 코드 (2.5h)**
- [ ] Expression Index (함수 인덱스)
  ```sql
  CREATE INDEX idx_lower_email ON users (lower(email));
  ```
  - `WHERE lower(email) = ?` 쿼리에서 인덱스 사용 확인
  - **왜 질문:** 왜 `WHERE email = ?` 인덱스가 `WHERE lower(email) = ?`에 안 먹히나?
- [ ] 인덱스와 정렬
  - `CREATE INDEX idx_desc ON orders (created_at DESC);`
  - `ORDER BY created_at DESC LIMIT 10` → 인덱스로 정렬 생략
  - **왜 질문:** ASC 인덱스로 DESC 정렬이 가능한가? (Backward Index Scan)
- [ ] Multicolumn Index 정렬 방향
  ```sql
  CREATE INDEX idx_mixed ON orders (user_id ASC, created_at DESC);
  ```
  - `ORDER BY user_id ASC, created_at DESC` → 인덱스 사용
  - `ORDER BY user_id ASC, created_at ASC` → 인덱스 사용 불가
  - **왜 질문:** 왜 정렬 방향이 다르면 인덱스를 못 쓰는가?
- [ ] Concurrent Index Creation
  - `CREATE INDEX CONCURRENTLY` — 락 없이 인덱스 생성
  - **왜 질문:** 일반 CREATE INDEX는 왜 테이블 락을 잡는가?
  - **왜 질문:** CONCURRENTLY의 단점은? (느림, 실패 시 INVALID 인덱스)

---

## Day 68 (금) — JPA + 인덱스 실전 연결

**코드 (2.5h) — trader-bot**
- [ ] JPA가 생성하는 SQL과 인덱스 매칭 확인
  - `spring.jpa.show-sql=true` + `org.hibernate.orm.jdbc.bind=trace`
  - 주요 Repository 메서드의 SQL 확인 → EXPLAIN
- [ ] N+1 쿼리에 인덱스가 있어도 느린 이유 확인
  - 인덱스는 개별 쿼리를 빠르게 하지만, 쿼리 수 자체가 문제
  - **왜 질문:** fetch join vs 인덱스, 어느 게 먼저인가? (쿼리 수 줄이기 → 그 다음 인덱스)
- [ ] `@Index` 어노테이션으로 엔티티에 인덱스 선언
  ```java
  @Table(indexes = {
    @Index(name = "idx_order_user_date", columnList = "userId, createdAt DESC")
  })
  ```
- [ ] Flyway 마이그레이션으로 인덱스 관리
  - **왜 질문:** DDL을 JPA auto-ddl로 관리하면 왜 위험한가? (운영 DB 변경 통제 불가)
- [ ] 현재 trader-bot 쿼리 중 가장 느린 것 3개 식별 + 인덱스 추가

---

## Day 69 (토) — 격리 수준 4가지 실험

**코드 + 이해 (5h)**

오전 (3h) — 격리 수준 실험
- [ ] psql 터미널 2개 열고 동시 실행
- [ ] 각 격리 수준에서 동시성 문제 재현

| 현상 | READ COMMITTED (PG 기본) | REPEATABLE READ | SERIALIZABLE |
|---|---|---|---|
| Non-repeatable Read | ✓ 발생 | ✗ 차단 | ✗ 차단 |
| Phantom Read | ✓ 발생 | ✗ (PG는 차단) | ✗ 차단 |
| Lost Update | ✓ 발생 | ✗ 차단 | ✗ 차단 |

- [ ] **Lost Update 재현** (가장 중요!)
  - 트랜잭션 A: 잔고 조회 → 1초 sleep → 출금 → commit
  - 트랜잭션 B: 같은 사용자 잔고를 동시에 출금
  - 결과: 잔고 마이너스 가능!
- [ ] REPEATABLE READ로 올려서 한쪽 실패 확인
  - **왜 질문:** PostgreSQL REPEATABLE READ는 Snapshot Isolation이다. MySQL과 뭐가 다른가?
  - **왜 질문:** Snapshot Isolation에서는 Write Skew가 발생할 수 있다. 이게 뭔가?
- [ ] SERIALIZABLE로 올려서 직렬화 보장 확인
  - **왜 질문:** SERIALIZABLE이 제일 안전한데 왜 기본이 아닌가? (성능 저하, 재시도 필요)

오후 (2h) — Spring에서 격리 수준 적용
- [ ] `@Transactional(isolation = Isolation.REPEATABLE_READ)` 실험
- [ ] 격리 수준별 동시 요청 100개 성능 차이 측정
- [ ] **왜 질문:**
  - Spring에서 격리 수준을 메서드마다 다르게 쓸 수 있나? 주의할 점은?
  - 격리 수준을 높이면 데드락 확률이 올라가는 이유는?
  - PostgreSQL의 기본(READ COMMITTED)에서 안전하게 동시성을 처리하려면? (SELECT FOR UPDATE)

---

## Day 70 (일) — Week 10 정리 + 블로그

**오전 (2.5h)**
- [ ] 인덱스 타입별 특징 + 사용 시점 표 정리
- [ ] 격리 수준 실험 결과 다이어그램
- [ ] **블로그 작성:** "PostgreSQL 인덱스 6가지 — 언제 뭘 써야 하나"
- [ ] 또는: "Lost Update를 터미널 2개로 직접 재현해봤다"
- 오후: 휴식

**Week 10 PR:** 인덱스 실험 + 격리 수준 재현 코드 + Before/After 성능 표

---

# Week 11 — 트랜잭션 동시성 & 락

---

## Day 71 (월) — MVCC 깊이 이해

**이해 (2h)**
- PostgreSQL MVCC 동작 원리 그려보기
  - 모든 행에 `xmin`(생성 트랜잭션 ID), `xmax`(삭제 트랜잭션 ID)
  - 각 트랜잭션은 자기 snapshot 기준으로 보이는 행만 읽음
  - UPDATE = 기존 행 DELETE(xmax 표시) + 새 행 INSERT
- **왜 질문:**
  - MVCC에서 UPDATE가 왜 DELETE + INSERT인가? (버전 관리 위해)
  - 이렇게 하면 디스크 공간이 계속 늘어나는데? → VACUUM이 정리
  - VACUUM이 안 돌면 어떤 문제가 생기나? (Table Bloat, 트랜잭션 ID 순환 문제)
  - Autovacuum은 언제 동작하나? 설정값은?
- [ ] `pageinspect` 확장으로 실제 행의 xmin, xmax 확인
  ```sql
  CREATE EXTENSION pageinspect;
  SELECT lp, t_xmin, t_xmax, t_ctid FROM heap_page_items(get_raw_page('orders', 0));
  ```
- **왜 질문:**
  - MySQL InnoDB의 MVCC와 PostgreSQL MVCC의 구현 차이는?
  - MVCC 덕분에 읽기가 쓰기를 블로킹하지 않는다. 왜?

---

## Day 72 (화) — 비관적 락 (SELECT FOR UPDATE)

**코드 (2.5h)**
- [ ] `SELECT FOR UPDATE` 동작 실험
  - 터미널 A: `BEGIN; SELECT * FROM accounts WHERE id = 1 FOR UPDATE;`
  - 터미널 B: 같은 행 `SELECT FOR UPDATE` → **대기** 확인
  - A가 COMMIT하면 B가 진행
- [ ] Spring JPA에서 비관적 락
  ```java
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM Account a WHERE a.id = :id")
  Optional<Account> findByIdForUpdate(@Param("id") Long id);
  ```
- [ ] 동시 요청 100개 부하 → 처리 시간 측정
- **왜 질문:**
  - `FOR UPDATE` vs `FOR SHARE` 차이는? (배타 락 vs 공유 락)
  - `FOR UPDATE NOWAIT`은? `SKIP LOCKED`은? (대기 안 함 / 이미 잠긴 행 건너뜀)
  - **왜 질문:** SKIP LOCKED는 어디에 유용한가? (작업 큐 패턴 — 여러 워커가 행을 가져갈 때)
  - 비관적 락의 단점은? (동시성 저하, 대기 시간, 데드락 위험)

---

## Day 73 (수) — 낙관적 락 + 데드락

**코드 (2.5h)**
- [ ] 낙관적 락 실험 (`@Version` 기반)
  - 동시에 같은 엔티티 수정 → `OptimisticLockException` 재현
  - `@Retryable(value = OptimisticLockException.class, maxAttempts = 3)`
  - 동시 요청 100개 → 처리 시간 + 재시도 횟수 측정
- [ ] 비관적 vs 낙관적 비교표 작성
  - **왜 질문:** 충돌이 적을 때는 낙관적이 왜 좋은가? (락 대기 없이 대부분 성공)
  - **왜 질문:** 충돌이 잦을 때는 비관적이 왜 좋은가? (재시도 비용 > 대기 비용)
- [ ] **데드락 만들고 잡기**
  - 트랜잭션 A: user1 → user2 순서로 락
  - 트랜잭션 B: user2 → user1 순서로 락
  - PostgreSQL이 데드락 감지 → 한쪽 abort
  - `pg_locks` 뷰로 락 상태 확인
- **왜 질문:**
  - PostgreSQL은 데드락을 어떻게 감지하나? (Wait-for graph 주기적 탐색)
  - `deadlock_timeout` 설정은? (기본 1초 — 이 시간 후에 감지 시작)
  - 데드락 방지 전략은? (항상 같은 순서로 락 — 작은 ID부터)

---

## Day 74 (목) — 분산 락 (Redis)

**코드 (2.5h) — trader-bot**
- [ ] 왜 분산 락이 필요한가?
  - DB 락은 단일 DB 범위 — 여러 서버에서 같은 리소스 접근 시?
  - 시나리오: 사용자가 동시에 두 디바이스에서 주문
- [ ] Redisson으로 분산 락 구현
  ```java
  RLock lock = redissonClient.getLock("order:user:" + userId);
  boolean acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
  try {
      if (acquired) { /* 비즈니스 로직 */ }
  } finally {
      if (acquired) lock.unlock();
  }
  ```
- [ ] AOP로 `@DistributedLock` 어노테이션 만들기
  - `@DistributedLock(key = "'order:user:' + #userId")`
- **왜 질문:**
  - Redis `SET NX EX`로 직접 구현하면 어떤 문제가 있나? (만료 전에 작업 안 끝나면?)
  - Redisson은 이걸 어떻게 해결하는가? (Watch Dog — TTL 자동 연장)
  - **왜 질문:** Redis 마스터가 죽으면 분산 락이 깨질 수 있다. 왜? (Redlock 알고리즘)
  - 펜싱 토큰(Fencing Token)이란? 왜 필요한가?
- [ ] 동시 요청 테스트: 분산 락 없이 → 중복 주문 발생 / 있으면 → 1건만 처리

---

## Day 75 (금) — Advisory Lock + 실전 패턴

**코드 (2.5h)**
- [ ] PostgreSQL Advisory Lock
  ```sql
  SELECT pg_advisory_lock(hashtext('order:' || user_id));
  -- 비즈니스 로직
  SELECT pg_advisory_unlock(hashtext('order:' || user_id));
  ```
  - **왜 질문:** Advisory Lock이 행 레벨 락과 다른 점은? (데이터와 무관한 논리적 락)
  - **왜 질문:** 언제 Advisory Lock을 쓰는 게 좋은가? (배치 작업 중복 실행 방지 등)
- [ ] 락 선택 기준 정리 (의사결정 트리)
  - 단일 행 동시 수정 → 낙관적 락 (`@Version`)
  - 잔고 차감 같은 크리티컬 → 비관적 락 (`FOR UPDATE`)
  - 여러 서버 간 조율 → 분산 락 (Redis)
  - 배치/스케줄러 중복 방지 → Advisory Lock
- [ ] trader-bot 동시성 이슈 정리 + 적절한 락 전략 적용
- **왜 질문:**
  - 락 순서를 왜 통일해야 하는가?
  - 락 획득 타임아웃을 왜 설정해야 하는가?
  - 락을 최소 범위로 잡아야 하는 이유는?

---

## Day 76 (토) — Spring Cache + Redis 캐싱

**코드 + 이해 (5h)**

오전 (3h) — Spring Cache 기초
- [ ] `@EnableCaching` + Redis CacheManager 설정
- [ ] 캐싱 어노테이션 3종 적용
  - `@Cacheable("quotes")` — 시세 조회 (TTL 5초)
  - `@CacheEvict("quotes")` — 거래 발생 시 무효화
  - `@CachePut` — 갱신 시 캐시도 갱신
- [ ] 캐시 동작 확인 — 로그로 DB 조회 여부 확인
- **왜 질문:**
  - `@Cacheable`의 key는 어떻게 결정되나? (파라미터 기반 SpEL)
  - 같은 메서드 내부 호출(self-invocation)에서 캐시 안 되는 이유는? (AOP 프록시!)
  - **왜 질문:** 이 문제는 1단계 `@Transactional` 자기호출과 같은 원인이다!
  - TTL을 너무 길게/짧게 잡으면 각각 어떤 문제?

오후 (2h) — 캐시 패턴 4가지
- [ ] **Cache-Aside (Look-Aside)**: 앱이 직접 캐시 조회 → miss면 DB → 캐시 저장
- [ ] **Read-Through**: 캐시가 알아서 DB 조회
- [ ] **Write-Through**: 쓸 때 캐시+DB 동시 갱신
- [ ] **Write-Behind**: 쓸 때 캐시만, 나중에 DB 반영
- **왜 질문:**
  - trader-bot 시세 데이터에는 어떤 패턴이 적합한가? (Cache-Aside + 짧은 TTL)
  - Write-Behind의 위험은? (캐시 장애 시 데이터 유실)
- [ ] **캐시 스탬피드(Stampede) 재현**
  - TTL 동시 만료 → 동시 DB 조회 몰림
  - 해결: 분산 락 또는 PER(Probabilistic Early Recomputation)

---

## Day 77 (일) — Week 11 정리 + 블로그

**오전 (2.5h)**
- [ ] 락 종류별 비교표 최종 정리
- [ ] 캐시 패턴 4가지 다이어그램
- [ ] **블로그 작성:** "동시성 제어 전략 — 낙관적 락부터 분산 락까지"
- [ ] 또는: "데드락을 직접 만들고 해결해봤다"
- 오후: 휴식

**Week 11 PR:** 격리 수준 실험 + 분산 락 + @DistributedLock AOP + Spring Cache

---

# Week 12 — Redis 활용 & 성능 종합

---

## Day 78 (월) — Redis 자료구조 활용

**이해 + 코드 (2.5h)**
- [ ] Redis 자료구조별 trader-bot 적용

| 자료구조 | 명령어 | trader-bot 적용 |
|---|---|---|
| String | GET/SET/INCR | 환율, 시세 캐시 |
| Hash | HGET/HSET/HGETALL | 사용자 세션 |
| **Sorted Set** | ZADD/ZRANGE/ZRANK | 수익률 랭킹 |
| List | LPUSH/RPOP/LRANGE | 최근 거래 내역 |
| **Stream** | XADD/XREAD/XACK | 시세 이벤트 큐 |

- [ ] Sorted Set으로 수익률 랭킹 보드 구현
  ```
  ZADD ranking 15.5 "user:1"
  ZADD ranking 22.3 "user:2"
  ZREVRANGE ranking 0 9 WITHSCORES  -- TOP 10
  ```
- **왜 질문:**
  - Sorted Set 내부 구조는? (Skip List + Hash Table)
  - Skip List는 왜 B-Tree 대신 쓰이나? (구현 간단, 범위 쿼리 빠름)
  - Redis Stream과 Kafka의 차이는? 언제 뭘 쓰나?
- [ ] HyperLogLog — 일일 활성 사용자 중복 제거 카운트
  - `PFADD dau:2024-01-15 "user:1" "user:2"`
  - `PFCOUNT dau:2024-01-15`
  - **왜 질문:** HyperLogLog는 왜 12KB만으로 수백만 개를 카운트하나? (확률적 자료구조)

---

## Day 79 (화) — Redis 운영 이슈

**이해 + 코드 (2.5h)**
- [ ] Redis 메모리 정책 (`maxmemory-policy`)
  - `noeviction` — 메모리 꽉 차면 에러
  - `allkeys-lru` — LRU로 삭제
  - `volatile-ttl` — TTL 짧은 것부터 삭제
  - **왜 질문:** trader-bot에는 어떤 정책이 맞나? 왜?
- [ ] Redis Persistence
  - **RDB**: 주기적 스냅샷 (fork + COW)
  - **AOF**: 모든 명령 기록
  - **왜 질문:** RDB의 fork가 왜 위험한가? (메모리 2배 순간 사용 가능)
  - **왜 질문:** AOF rewrite란? 왜 필요한가?
- [ ] Redis 클러스터 vs Sentinel
  - **왜 질문:** Redis는 싱글 스레드인데 왜 빠른가? (I/O 멀티플렉싱, 메모리 기반)
  - **왜 질문:** 싱글 스레드면 CPU 바운드 작업에 취약한가?
- [ ] `redis-cli INFO memory` — 메모리 사용량 확인
- [ ] `redis-cli --bigkeys` — 큰 키 찾기

---

## Day 80 (수) — HikariCP 커넥션 풀 튜닝

**이해 + 코드 (2.5h)**
- [ ] HikariCP 핵심 설정
  - `maximumPoolSize`: 최대 커넥션 수
  - `minimumIdle`: 유휴 커넥션 최소 수
  - `connectionTimeout`: 커넥션 대기 시간 (기본 30초)
  - `maxLifetime`: 커넥션 최대 수명
  - `leakDetectionThreshold`: 누수 감지
- [ ] 풀 크기별 부하 테스트
  - poolSize=5, 10, 20, 50으로 각각 동시 요청 100개
  - 응답 시간 + connection wait 시간 측정
- **왜 질문:**
  - 커넥션 풀이 왜 필요한가? (TCP 연결 + 인증 비용이 매 요청마다 비쌈)
  - 풀 크기 공식: `connections = (core_count * 2) + effective_spindle_count` — 왜?
  - 풀 크기를 100으로 키우면 왜 오히려 느려질 수 있나? (컨텍스트 스위칭, DB max_connections)
  - `leakDetectionThreshold`를 켜면 뭘 잡을 수 있나? (커넥션 반환 안 된 코드)
- [ ] 커넥션 누수 재현
  - `@Transactional` 없이 EntityManager 직접 사용 후 close 안 함
  - 누수 감지 로그 확인
- [ ] `spring.datasource.hikari` 설정 최적화 적용

---

## Day 81 (목) — 슬로우 쿼리 모니터링

**코드 (2.5h)**
- [ ] PostgreSQL 슬로우 쿼리 설정
  ```sql
  ALTER SYSTEM SET log_min_duration_statement = 500;  -- 500ms 이상 로그
  SELECT pg_reload_conf();
  ```
- [ ] `pg_stat_statements` 활성화
  ```sql
  CREATE EXTENSION pg_stat_statements;
  ```
  - Top 10 가장 느린 쿼리 추출
  - Top 10 가장 자주 호출되는 쿼리 추출
  - **왜 질문:** `total_exec_time`이 높은 쿼리 vs `mean_exec_time`이 높은 쿼리, 어느 게 먼저 최적화 대상?
- [ ] Spring 레벨 SQL 모니터링
  - `datasource-proxy` 또는 `p6spy` 적용
  - 쿼리 실행 시간 + 쿼리 수 로깅
- [ ] 통합 테스트에서 N+1 자동 감지
  - `@BeforeEach`에서 SQL 카운터 리셋
  - `@AfterEach`에서 SQL 수 검증 (예: 조회 API는 5개 이하)
  - **왜 질문:** 왜 테스트에서 SQL 수를 검증해야 하나? (코드 변경 후 무의식적 N+1 유입 방지)
- [ ] trader-bot에 쿼리 카운팅 인터셉터 적용

---

## Day 82 (금) — 종합 대시보드 API 최적화

**코드 (2.5h) — trader-bot**
- [ ] "트레이딩 대시보드" API 구현 + 최적화
  - `GET /api/dashboard/{userId}`
    - 보유 종목 리스트 (최근 가격 포함)
    - 최근 30일 주문 100건
    - 누적 수익률
    - 동일 종목 보유 사용자 랭킹 TOP 10
- [ ] **Baseline 측정** — 아무 최적화 없이 응답시간
- [ ] 단계별 최적화 + 각 단계 응답시간 기록
  1. EXPLAIN으로 병목 쿼리 식별
  2. 적절한 인덱스 추가
  3. N+1 제거 (fetch join / batch size)
  4. 커버링 인덱스 적용
  5. Redis 캐시 (시세 데이터)
  6. 랭킹은 Redis Sorted Set으로
- [ ] **목표: 응답시간 100ms 이하**
- **왜 질문:**
  - 쿼리 수를 줄이는 게 먼저인가, 개별 쿼리를 빠르게 하는 게 먼저인가?
  - 캐시를 먼저 적용하면 안 되는 이유는? (근본 문제를 숨김)

---

## Day 83 (토) — 부하 테스트 + 종합 정리

**코드 + 측정 (5h)**

오전 (3h) — 부하 테스트
- [ ] k6로 대시보드 API 부하 테스트
  - 동시 사용자 100 → 300 → 500명
  - 응답 시간 p50/p95/p99 기록
  - DB CPU 사용률 모니터링
- [ ] DB 쿼리 5개 이하 확인 (Redis hit 시 0~1개)
- [ ] HikariCP 메트릭 확인 (active, idle, waiting)
- [ ] 캐시 무효화 정책 검증 — 데이터 변경 후 즉시 반영되는지
- [ ] EXPLAIN ANALYZE로 최종 실행 계획 캡처

오후 (2h) — 2단계 종합 정리
- [ ] 2단계 전체 학습 내용 요약
  - SQL 실행 계획 읽기
  - 인덱스 6가지 타입 + 선택 기준
  - 격리 수준 + 동시성 제어 (4가지 락)
  - Redis 자료구조 + 캐시 패턴
  - 커넥션 풀 + 모니터링
- [ ] 면접 대비 "왜" 질문 10개 셀프 답변
- [ ] Before/After 최종 성능 표 작성

---

## Day 84 (일) — 2단계 졸업 + 블로그

**오전 (2.5h)**
- [ ] **블로그 작성:** "응답시간 3초 → 100ms로 — 대시보드 API 최적화 전 과정"
- [ ] 2단계 회고
  - 가장 충격적이었던 실행 계획
  - 인덱스에 대한 오해가 깨진 순간
  - 락 전략 선택의 어려움
- [ ] 3단계(네트워크 & HTTP) 예습: TCP/IP 기초 문서 훑기
- 오후: 휴식

**Week 12 PR:** Redis 자료구조 활용 + 커넥션 풀 튜닝 + 대시보드 API 최적화 + 부하 테스트

---

## 2단계 완료 체크리스트

### PR 목록 (4개)
- [ ] W9: 더미 데이터 + EXPLAIN 분석 + 쿼리 튜닝
- [ ] W10: 인덱스 심화 + 격리 수준 실험
- [ ] W11: 락 전략 + 분산 락 + Spring Cache
- [ ] W12: Redis 활용 + 커넥션 풀 + 대시보드 최적화

### 블로그 (4편)
- [ ] W9: EXPLAIN ANALYZE 실전 가이드
- [ ] W10: PostgreSQL 인덱스 종류별 비교
- [ ] W11: 동시성 제어 전략 비교
- [ ] W12: 대시보드 API 최적화 과정

### "왜"에 답할 수 있어야 하는 것들 (면접 대비)
- [ ] EXPLAIN 실행 계획에서 cost, rows, buffers 의미
- [ ] B-Tree 복합 인덱스에서 컬럼 순서가 중요한 이유
- [ ] 커버링 인덱스가 빠른 이유 (Heap Fetch 제거)
- [ ] PostgreSQL MVCC 동작 원리 (xmin, xmax, snapshot)
- [ ] 격리 수준 4가지와 각각 방지하는 동시성 문제
- [ ] 비관적 vs 낙관적 vs 분산 락 선택 기준
- [ ] 데드락 원인과 방지 전략
- [ ] Redis가 싱글 스레드인데 빠른 이유
- [ ] 캐시 패턴 4가지와 선택 기준
- [ ] HikariCP 풀 크기 결정 공식과 이유
