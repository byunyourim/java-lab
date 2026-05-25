# TypeScript 학습 (백엔드 개발자용) — 하루 단위 커리큘럼

> **대상:** Java/Spring 익숙 + TypeScript 초보
> **목표:** Node.js 백엔드를 TypeScript로 짜고 "왜 이렇게 동작하는지" 설명할 수 있는 수준
> **기간:** 6주 (별도 트랙 — 메인 Java 학습과 병행 또는 이후 진행)
> **하루:** 평일 2~3h / 토 5~6h / 일 2~3h (오후 휴식)
> **매주 필수:** PR 1개 + 코드 실습 + 블로그 1편

> **왜 백엔드에서 TypeScript를?**
> - 빠른 프로토타이핑 — Spring보다 가볍게 API 서버
> - 풀스택 일관성 — frontend(Next.js)와 같은 언어
> - 이벤트 기반 / I/O 집약 워크로드에 강함
> - 서버리스(AWS Lambda) 친화적
> - AI/LLM 도구 생태계가 TS/Python 중심

> **핵심 마인드셋:**
> Java식으로 TS 짜면 안 됨. TypeScript는 JavaScript 위에 얹은 타입 시스템.
> JS 자체 동작도 함께 배워야 합니다.

---

## Java 개발자 관점에서 본 TypeScript (매핑 테이블)

| Java | TypeScript | 비고 |
|---|---|---|
| `class` | `class` | 거의 동일 |
| `interface` | `interface` 또는 `type` | TS는 더 유연 |
| `enum` | `enum` 또는 union type | union type 권장 |
| `Optional<T>` | `T \| undefined`, `T \| null` | 언어 차원 지원 |
| `List<T>` | `T[]` 또는 `Array<T>` | |
| `Map<K,V>` | `Map<K,V>` 또는 `Record<K,V>` | |
| `Generic<T>` | `Generic<T>` | 거의 동일 |
| `@Override` | (없음) | 구조적 타이핑 |
| `final` | `readonly`, `const` | |
| 체크 예외 | 없음 | 전부 unchecked |
| Maven/Gradle | npm/pnpm/yarn | |

---

## Java와 가장 다른 핵심 개념 3가지

### 1. 구조적 타이핑 (Structural Typing)
Java: 이름이 같아야 같은 타입 (명목적)
TS: 모양이 같으면 같은 타입 (구조적 — duck typing)

### 2. 타입은 런타임에 없다
TypeScript 타입은 컴파일 후 사라짐. `instanceof`는 클래스만 가능.
→ 런타임 검증: Zod, io-ts 같은 라이브러리 필요

### 3. `any` vs `unknown` vs `never`
- `any`: 타입 체크 끄기 — **쓰지 마세요**
- `unknown`: 모르겠으니 검사 후 사용
- `never`: 절대 도달 불가

---

# Week 1 — TypeScript 문법 핵심

---

## Day 1 (월) — 개발 환경 + 기본 타입

**이해 + 코드 (2.5h)**
- [ ] 개발 환경 설정
  - Node.js 20+ 설치
  - `pnpm` 설치 (npm보다 빠름)
  - VS Code + TypeScript 확장
  - `tsconfig.json` 기본 설정 (`strict: true` 필수!)
- [ ] 기본 타입 학습
  - `string`, `number`, `boolean`
  - `null`, `undefined`, `void`
  - `bigint`, `symbol` (잘 안 씀)
- [ ] Java와 비교하며 실습
  ```typescript
  // Java: int x = 10;
  const x: number = 10;  // TS에는 int/long/double 구분 없음!

  // Java: String s = "hello";
  const s: string = "hello";

  // Java: boolean b = true;
  const b: boolean = true;
  ```
- **왜 질문:**
  - TS에 `int`와 `double`이 없는 이유는? (JS의 `number`는 IEEE 754 double — 모든 숫자)
  - `strict: true`가 왜 필수인가? (없으면 TS 쓰는 의미 절반 사라짐)
  - **왜 질문:** `let` vs `const` vs `var` 차이는? (`var`는 쓰지 마 — 함수 스코프 문제)
  - `null` vs `undefined` 차이는? (null: 의도적 빈 값 / undefined: 할당 안 됨)

---

## Day 2 (화) — 배열, 튜플, 객체 타입

**코드 (2.5h)**
- [ ] **배열**
  ```typescript
  const numbers: number[] = [1, 2, 3];
  const names: Array<string> = ["a", "b"];  // 같은 의미
  ```
- [ ] **튜플** — 고정 길이 + 타입 배열
  ```typescript
  const point: [number, number] = [10, 20];
  const entry: [string, number] = ["age", 30];
  ```
  - **왜 질문:** Java에 튜플이 없는데 TS에 있는 이유는? (경량 데이터 묶음 — record 비슷)
- [ ] **객체 타입**
  ```typescript
  // 인라인
  const user: { name: string; age: number } = { name: "Kim", age: 25 };

  // interface
  interface User {
    name: string;
    age: number;
    email?: string;  // 옵셔널
  }
  ```
- [ ] **옵셔널 프로퍼티 (`?`)**
  ```typescript
  interface Config {
    host: string;
    port?: number;  // 있어도 되고 없어도 됨
  }
  ```
- [ ] **readonly**
  ```typescript
  interface Point {
    readonly x: number;
    readonly y: number;
  }
  ```
  - **왜 질문:** Java `final` 필드와 `readonly`의 차이는?
- **왜 질문:**
  - `{}` (빈 객체 타입)의 함정은? (거의 모든 값을 허용함!)
  - **왜 질문:** TS의 옵셔널(`?`)과 Java `Optional`의 차이는?

---

## Day 3 (수) — 함수 타입 + 유니온/인터섹션

**코드 (2.5h)**
- [ ] **함수 타입**
  ```typescript
  // 매개변수 + 반환 타입
  function add(a: number, b: number): number {
    return a + b;
  }

  // 화살표 함수
  const multiply = (a: number, b: number): number => a * b;

  // 옵셔널 파라미터
  function greet(name: string, greeting?: string): string {
    return `${greeting ?? "Hello"}, ${name}`;
  }

  // 기본값
  function connect(host: string, port: number = 8080): void { ... }
  ```
- [ ] **유니온 타입 (`|`)**
  ```typescript
  type Status = "OPEN" | "CLOSED" | "PENDING";
  type Id = string | number;

  function process(input: string | number) {
    if (typeof input === "string") {
      input.toUpperCase();  // 여기서 string으로 좁혀짐
    }
  }
  ```
  - **왜 질문:** Java enum과 유니온 타입의 차이는?
- [ ] **인터섹션 타입 (`&`)**
  ```typescript
  type Timestamped = { createdAt: Date; updatedAt: Date };
  type User = { name: string; email: string };
  type TimestampedUser = User & Timestamped;  // 둘 다 가짐
  ```
  - **왜 질문:** 인터섹션은 Java의 `implements A, B`와 유사한가?
- **왜 질문:**
  - 유니온 vs 인터섹션 — 이름이 반직관적이다. 왜? (타입이 허용하는 "값의 집합" 관점)
  - `string | number`에 .toUpperCase() 호출 가능한가? (불가 — 먼저 좁히기 필요)

---

## Day 4 (목) — 리터럴 타입 + 타입 좁히기 (Narrowing)

**코드 (2.5h)**
- [ ] **리터럴 타입**
  ```typescript
  type Direction = "up" | "down" | "left" | "right";
  type HttpMethod = "GET" | "POST" | "PUT" | "DELETE";

  // 숫자 리터럴도 가능
  type DiceRoll = 1 | 2 | 3 | 4 | 5 | 6;
  ```
- [ ] **타입 좁히기 (Narrowing)** — 가장 중요한 패턴!
  ```typescript
  function handle(input: string | number | null) {
    // typeof
    if (typeof input === "string") { /* input: string */ }
    // 진위 검사 (null 체크)
    if (input !== null) { /* input: string | number */ }
    // instanceof
    if (input instanceof Date) { /* input: Date */ }
    // in 연산자
    if ("name" in obj) { /* obj has name */ }
  }
  ```
- [ ] **Discriminated Union** (태그드 유니온) — Java sealed class 대안
  ```typescript
  type Shape =
    | { kind: "circle"; radius: number }
    | { kind: "rectangle"; width: number; height: number };

  function area(shape: Shape): number {
    switch (shape.kind) {
      case "circle": return Math.PI * shape.radius ** 2;
      case "rectangle": return shape.width * shape.height;
    }
  }
  ```
- **왜 질문:**
  - Discriminated Union이 왜 강력한가? (컴파일 타임에 모든 케이스 처리 보장 — exhaustive check)
  - Java의 sealed interface + pattern matching과 뭐가 비슷/다른가?
  - **왜 질문:** `never` 타입으로 exhaustive check하는 방법은?
  ```typescript
  default: {
    const _exhaustive: never = shape;  // 미처리 케이스 있으면 컴파일 에러
  }
  ```

---

## Day 5 (금) — 제네릭 (Generics)

**코드 (2.5h)**
- [ ] **기본 제네릭** — Java와 거의 동일
  ```typescript
  function identity<T>(value: T): T {
    return value;
  }

  interface Repository<T> {
    findById(id: string): T | undefined;
    save(entity: T): T;
  }
  ```
- [ ] **제네릭 제약 (extends)**
  ```typescript
  // Java: <T extends Comparable<T>>
  interface HasId { id: string }
  function findById<T extends HasId>(items: T[], id: string): T | undefined {
    return items.find(item => item.id === id);
  }
  ```
- [ ] **제네릭 유틸리티**
  ```typescript
  // 여러 타입 파라미터
  function map<T, U>(items: T[], fn: (item: T) => U): U[] {
    return items.map(fn);
  }

  // 기본값
  interface ApiResponse<T = unknown> {
    data: T;
    status: number;
  }
  ```
- **왜 질문:**
  - Java 제네릭은 런타임에 소거(erasure)된다. TS도 마찬가지인가? (TS는 컴파일 후 전부 사라짐 — 더 심함)
  - `<T extends string>`과 Java `<T extends String>`의 차이는?
  - **왜 질문:** TS에서 `new T()`가 안 되는 이유는? (타입이 런타임에 없으니까)

---

## Day 6 (토) — 구조적 타이핑 깊이 + 실습

**코드 (5h)**

오전 (3h) — 구조적 타이핑 실험
- [ ] **구조적 타이핑** 직접 체험
  ```typescript
  interface Dog { name: string; bark(): void }
  interface Robot { name: string; bark(): void }

  const dog: Dog = { name: "Rex", bark: () => console.log("Woof") };
  const robot: Robot = dog;  // ✓ 모양이 같으니까!
  ```
  - Java에서는 불가능한 이유: 명목적 타이핑 (이름이 달라서)
  - **왜 질문:** 구조적 타이핑의 장점은? (인터페이스 구현 선언 없이도 호환)
  - **왜 질문:** 단점은? (의도치 않은 타입 호환 — Dog ≠ Robot인데 호환됨)
- [ ] **Excess Property Check** — 객체 리터럴만 엄격
  ```typescript
  interface User { name: string }
  const u: User = { name: "Kim", age: 25 };  // ✗ 에러! (객체 리터럴)
  const obj = { name: "Kim", age: 25 };
  const u2: User = obj;  // ✓ 통과! (변수 대입)
  ```
  - **왜 질문:** 왜 객체 리터럴에서만 엄격한가? (오타 방지 목적)
- [ ] Java 코드 → TypeScript 변환 5개 실습
  - DTO, Service interface, Repository interface
  - Optional → union type
  - enum → union literal type

오후 (2h) — 외부 API 타입 정의
- [ ] 외부 API JSON 응답을 타입으로 정의
  ```typescript
  interface KisQuoteResponse {
    output: {
      stck_prpr: string;   // 현재가
      prdy_vrss: string;   // 전일 대비
      prdy_ctrt: string;   // 전일 대비율
    };
    rt_cd: "0" | "1";     // 응답 코드
    msg_cd: string;
  }
  ```
- [ ] fetch로 호출 + 타입 적용
- **왜 질문:** API 응답을 `any`로 받으면 왜 위험한가?

---

## Day 7 (일) — Week 1 정리 + 블로그

**오전 (2.5h)**
- [ ] Java ↔ TypeScript 매핑표 최종 정리
- [ ] 구조적 타이핑 vs 명목적 타이핑 비교 다이어그램
- [ ] **블로그 작성:** "Java 개발자가 TypeScript 배울 때 가장 헷갈리는 3가지"
- [ ] 다음 주 예습: Utility Types 문서 훑기
- 오후: 휴식

**Week 1 PR:** TS 기본 타입 실습 + 외부 API 타입 정의 + Java→TS 변환 코드

---

# Week 2 — TypeScript 중급

---

## Day 8 (월) — interface vs type

**이해 + 코드 (2.5h)**
- [ ] **interface** — 객체 형태 정의
  ```typescript
  interface User {
    name: string;
    age: number;
  }
  // 확장
  interface Admin extends User {
    role: "admin";
  }
  // 선언 병합 (같은 이름으로 다시 선언 가능)
  interface User {
    email: string;  // 기존에 추가됨!
  }
  ```
- [ ] **type** — 더 유연한 타입 별칭
  ```typescript
  type Status = "OPEN" | "CLOSED";         // union
  type Point = { x: number; y: number };   // 객체
  type Callback = (data: string) => void;  // 함수
  type Pair<T> = [T, T];                   // 튜플
  ```
- [ ] **언제 뭘 쓰나?**
  - 객체 형태 → `interface` (확장 자연스러움)
  - union, intersection, 함수, 튜플 → `type`
  - **결론:** 객체는 interface, 그 외는 type
- **왜 질문:**
  - 선언 병합이 유용한 경우는? (라이브러리 타입 확장 — `express`의 `Request` 타입 등)
  - **왜 질문:** 선언 병합이 위험한 경우는? (의도치 않은 충돌)
  - interface가 type보다 에러 메시지가 좋다는 건 사실인가?

---

## Day 9 (화) — Utility Types (필수 암기)

**코드 (2.5h)**
- [ ] **핵심 Utility Types** 전부 실습
  ```typescript
  interface Order {
    id: string;
    symbol: string;
    price: number;
    quantity: number;
    status: "OPEN" | "FILLED" | "CANCELLED";
    createdAt: Date;
  }

  // 모든 필드 옵셔널
  type UpdateOrder = Partial<Order>;

  // 모든 필드 필수
  type RequiredOrder = Required<Order>;

  // 모든 필드 읽기 전용
  type FrozenOrder = Readonly<Order>;

  // 특정 필드만
  type OrderSummary = Pick<Order, "id" | "symbol" | "status">;

  // 특정 필드 제외
  type CreateOrder = Omit<Order, "id" | "createdAt">;

  // Map 같은
  type ErrorMap = Record<string, string[]>;

  // 함수 반환 타입 추출
  type OrderResult = ReturnType<typeof createOrder>;

  // Promise 풀기
  type Resolved = Awaited<ReturnType<typeof fetchOrder>>;
  ```
- [ ] **실전 적용**: Spring DTO 패턴을 Utility Type으로
  - CreateRequest = `Omit<Order, 'id' | 'createdAt'>`
  - UpdateRequest = `Partial<Omit<Order, 'id'>>`
  - Response = `Order` 그대로
- **왜 질문:**
  - Java에서 Create/Update/Response DTO를 3개 만드는 것 vs TS의 Utility Type 접근 — 장단점?
  - **왜 질문:** `Partial<T>`를 남발하면 왜 위험한가? (모든 필드가 옵셔널 → 타입 안전성 저하)
  - `Readonly<T>`를 쓰면 런타임에도 불변인가? (아님! 컴파일 타임만)

---

## Day 10 (수) — 타입 가드 & enum vs union

**코드 (2.5h)**
- [ ] **커스텀 타입 가드**
  ```typescript
  interface Order { type: "order"; symbol: string }
  interface Trade { type: "trade"; executedPrice: number }
  type Event = Order | Trade;

  // 타입 가드 함수
  function isOrder(event: Event): event is Order {
    return event.type === "order";
  }

  function process(event: Event) {
    if (isOrder(event)) {
      console.log(event.symbol);  // Order로 좁혀짐
    }
  }
  ```
- [ ] **enum vs union** — union 권장
  ```typescript
  // 비추: enum은 런타임에 객체가 됨
  enum Status { OPEN = "OPEN", CLOSED = "CLOSED" }

  // 권장: union은 런타임에 사라짐 (가벼움)
  type Status = "OPEN" | "CLOSED";
  ```
  - **왜 질문:** const enum은? (인라인 치환 — 빠르지만 한계 있음)
  - **왜 질문:** Java에서 enum을 많이 쓰는데 TS에서 왜 union을 권장하나?
- [ ] **as const** — 리터럴 타입 추론
  ```typescript
  const ROLES = ["admin", "user", "guest"] as const;
  type Role = (typeof ROLES)[number];  // "admin" | "user" | "guest"
  ```
  - **왜 질문:** `as const`가 없으면 타입이 뭐가 되나? (`string[]` — 구체성 사라짐)
- [ ] **satisfies** (TS 4.9+) — 타입 검증하되 추론 유지
  ```typescript
  const config = {
    port: 8080,
    host: "localhost",
  } satisfies Record<string, string | number>;
  // config.port는 number로 추론됨 (Record<string, string|number>가 아님!)
  ```

---

## Day 11 (목) — Mapped Types & Conditional Types

**코드 (2.5h)**
- [ ] **Mapped Types** — 기존 타입 변환
  ```typescript
  // Partial 직접 구현해보기
  type MyPartial<T> = {
    [K in keyof T]?: T[K];
  };

  // 모든 필드를 Promise로
  type Async<T> = {
    [K in keyof T]: Promise<T[K]>;
  };
  ```
- [ ] **Conditional Types** — 조건부 타입
  ```typescript
  type IsString<T> = T extends string ? "yes" : "no";
  type A = IsString<string>;  // "yes"
  type B = IsString<number>;  // "no"

  // 실전: API 응답 추출
  type ApiResponse<T> = T extends { data: infer D } ? D : never;
  ```
  - **왜 질문:** `infer`는 뭔가? (조건부 타입 내에서 타입 추출)
- [ ] **Template Literal Types**
  ```typescript
  type HttpMethod = "GET" | "POST" | "PUT" | "DELETE";
  type ApiPath = `/api/${string}`;
  type Endpoint = `${HttpMethod} ${ApiPath}`;
  ```
- **왜 질문:**
  - 이런 고급 타입이 왜 필요한가? (라이브러리 작성자에게 필수 — 사용자에겐 드물게)
  - **왜 질문:** Utility Type들이 내부적으로 Mapped/Conditional Type으로 구현된다. 이해하면 뭐가 좋은가?

---

## Day 12 (금) — 에러 처리 & null 안전

**코드 (2.5h)**
- [ ] **TS 에러 처리** — Java와 가장 다른 점
  ```typescript
  // catch의 error는 unknown (TS 4.4+)
  try {
    await fetchData();
  } catch (error: unknown) {
    if (error instanceof Error) {
      console.error(error.message);
    }
  }
  ```
  - **왜 질문:** Java는 catch 타입을 선언하는데 TS는 왜 unknown인가? (throw에 아무거나 던질 수 있으니까)
- [ ] **Result 패턴** (Java의 Either 비슷)
  ```typescript
  type Result<T, E = Error> =
    | { success: true; data: T }
    | { success: false; error: E };

  function divide(a: number, b: number): Result<number, string> {
    if (b === 0) return { success: false, error: "Division by zero" };
    return { success: true, data: a / b };
  }
  ```
- [ ] **Null 안전성**
  - Optional Chaining: `user?.address?.city`
  - Nullish Coalescing: `value ?? defaultValue`
  - Non-null Assertion: `value!` (⚠️ 위험 — 확실할 때만)
  - **왜 질문:** `??` vs `||` 차이는? (`||`는 `0`, `""`, `false`도 falsy)
- **왜 질문:**
  - Java의 checked exception이 없는 게 장점인가 단점인가?
  - **왜 질문:** `!` (non-null assertion)을 쓰면 왜 위험한가? (런타임 null 가능)

---

## Day 13 (토) — 비동기: Promise & async/await

**코드 (5h)**

오전 (3h) — Promise 깊이 이해
- [ ] **Promise** — Java `CompletableFuture` 대응
  ```typescript
  // 생성
  const promise = new Promise<string>((resolve, reject) => {
    setTimeout(() => resolve("done"), 1000);
  });

  // 체이닝
  fetch("/api/orders")
    .then(res => res.json())
    .then(data => console.log(data))
    .catch(err => console.error(err));
  ```
- [ ] **async/await** — 동기처럼 비동기 작성
  ```typescript
  async function getOrders(): Promise<Order[]> {
    const res = await fetch("/api/orders");
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return res.json();
  }
  ```
- [ ] **동시 실행** 패턴
  ```typescript
  // 모두 성공해야
  const [users, orders] = await Promise.all([fetchUsers(), fetchOrders()]);

  // 실패해도 결과 모두 받기
  const results = await Promise.allSettled([api1(), api2(), api3()]);

  // 가장 빠른 하나
  const fastest = await Promise.race([api1(), api2()]);
  ```
- **왜 질문:**
  - `Promise.all` vs `Promise.allSettled` 언제 쓰나?
  - `await`를 루프 안에 쓰면 왜 느린가? (순차 실행됨 — `Promise.all`로 병렬화)
  - **왜 질문:** Node.js가 싱글 스레드인데 어떻게 비동기가 되나? (이벤트 루프!)

오후 (2h) — 이벤트 루프 이해
- [ ] **Node.js 이벤트 루프**
  - Call Stack → Microtask Queue (Promise) → Macrotask Queue (setTimeout)
  - **왜 질문:** Java의 멀티스레드 동시성과 Node.js 이벤트 루프의 근본 차이는?
  - **왜 질문:** CPU 바운드 작업을 하면 이벤트 루프가 블록된다. 왜?
  - 해결: Worker Threads 또는 별도 프로세스
- [ ] **타임아웃 + 재시도 유틸** 작성
  ```typescript
  async function withRetry<T>(
    fn: () => Promise<T>,
    maxRetries: number = 3,
    delay: number = 1000
  ): Promise<T> { ... }
  ```

---

## Day 14 (일) — Week 2 정리 + 블로그

**오전 (2.5h)**
- [ ] Utility Types 치트시트 정리
- [ ] async/await 패턴 정리
- [ ] **블로그 작성:** "TypeScript Utility Types — Java DTO 3개를 1개로 줄이기"
- [ ] 다음 주 예습: ESM, tsconfig 심화
- 오후: 휴식

**Week 2 PR:** Utility Types 실습 + Result 패턴 + 비동기 유틸 함수

---

# Week 3 — 모듈 & 설정 심화

---

## Day 15 (월) — ESM vs CommonJS

**이해 + 코드 (2.5h)**
- [ ] **CommonJS** (구식, Node.js 기본)
  ```javascript
  const express = require("express");
  module.exports = { ... };
  ```
- [ ] **ESM** (표준, 권장)
  ```typescript
  import express from "express";
  export const handler = () => { ... };
  ```
- [ ] `package.json`에 `"type": "module"` 설정
- [ ] 혼용 시 문제점 + 해결법
- **왜 질문:**
  - ESM과 CJS의 근본 차이는? (ESM: 정적 분석 가능, tree-shaking / CJS: 동적 require)
  - **왜 질문:** `import`는 왜 파일 최상단에만 써야 하나? (정적 분석 — 빌드 타임에 의존성 파악)
  - `require`는 동적이라 조건부 로딩이 된다. ESM에서는? (dynamic import: `await import()`)
  - 패키지가 CJS만 지원할 때? (interop 문제 — `esModuleInterop: true`)

---

## Day 16 (화) — tsconfig.json 마스터

**이해 + 코드 (2.5h)**
- [ ] 핵심 옵션 이해
  ```json
  {
    "compilerOptions": {
      "target": "ES2022",           // 출력 JS 버전
      "module": "NodeNext",         // 모듈 시스템
      "moduleResolution": "NodeNext",
      "strict": true,               // 모든 strict 옵션 켜기
      "esModuleInterop": true,      // CJS 호환
      "skipLibCheck": true,         // .d.ts 검사 스킵 (빌드 속도)
      "outDir": "./dist",
      "rootDir": "./src",
      "declaration": true,          // .d.ts 생성
      "sourceMap": true             // 디버깅용
    },
    "include": ["src/**/*"],
    "exclude": ["node_modules", "dist"]
  }
  ```
- [ ] `strict: true`가 켜는 것들
  - `strictNullChecks`: null/undefined 엄격 체크
  - `noImplicitAny`: 암시적 any 금지
  - `strictFunctionTypes`: 함수 타입 엄격
- [ ] `paths` 별칭 설정 (절대 경로 import)
  ```json
  "paths": {
    "@/*": ["./src/*"]
  }
  ```
- **왜 질문:**
  - `target`을 너무 낮추면? (폴리필 필요, 번들 커짐)
  - `skipLibCheck`를 켜면 뭘 놓칠 수 있나?
  - **왜 질문:** declaration 파일(.d.ts)은 뭔가? 왜 필요한가? (라이브러리 배포 시 타입 정보 제공)

---

## Day 17 (수) — 패키지 매니저 & 프로젝트 구조

**코드 (2.5h)**
- [ ] **pnpm** 사용법
  - `pnpm init`, `pnpm add`, `pnpm add -D`
  - `pnpm-lock.yaml` — 정확한 버전 고정
  - **왜 질문:** pnpm이 npm보다 빠른 이유는? (심볼릭 링크 + 공유 스토어)
- [ ] **프로젝트 구조** (NestJS 스타일)
  ```
  src/
  ├── app.module.ts
  ├── main.ts
  ├── order/
  │   ├── order.controller.ts
  │   ├── order.service.ts
  │   ├── order.repository.ts
  │   ├── dto/
  │   │   ├── create-order.dto.ts
  │   │   └── order-response.dto.ts
  │   └── order.module.ts
  └── common/
      ├── filters/
      ├── guards/
      └── interceptors/
  ```
- [ ] **ESLint + Prettier** 설정
  - `@typescript-eslint/eslint-plugin`
  - `eslint-config-prettier` (충돌 방지)
- **왜 질문:**
  - monorepo에서 pnpm workspace란?
  - **왜 질문:** `devDependencies` vs `dependencies` — 빌드 후에 차이가 있나? (Docker에서 --production 시 dev 안 설치)

---

## Day 18 (목) — Zod 런타임 검증

**코드 (2.5h)**
- [ ] **왜 런타임 검증이 필요한가?**
  - TS 타입은 컴파일 후 사라짐 → 외부 입력(API, DB, 파일)은 못 믿음
  - Java의 Bean Validation 대응
- [ ] **Zod** 기본 사용
  ```typescript
  import { z } from "zod";

  const CreateOrderSchema = z.object({
    symbol: z.string().length(6),
    price: z.number().positive(),
    quantity: z.number().int().min(1),
    type: z.enum(["BUY", "SELL"]),
  });

  // 스키마에서 타입 추출!
  type CreateOrderDto = z.infer<typeof CreateOrderSchema>;

  // 검증
  const result = CreateOrderSchema.safeParse(req.body);
  if (!result.success) {
    return res.status(400).json(result.error.flatten());
  }
  const order = result.data;  // 타입 안전!
  ```
- [ ] **Zod + API 응답 검증**
  ```typescript
  const ApiResponseSchema = z.object({
    output: z.object({
      stck_prpr: z.string(),
    }),
    rt_cd: z.enum(["0", "1"]),
  });

  const validated = ApiResponseSchema.parse(await res.json());
  ```
- **왜 질문:**
  - `z.infer`가 왜 강력한가? (스키마 하나로 검증 + 타입 동시에!)
  - Java Bean Validation(`@NotNull`)과 Zod의 철학 차이는?
  - **왜 질문:** `.parse()` vs `.safeParse()` — 언제 뭘 쓰나? (parse: throw / safeParse: Result 반환)

---

## Day 19 (금) — 에러 처리 패턴 & 타입 안전 에러

**코드 (2.5h)**
- [ ] **커스텀 에러 클래스**
  ```typescript
  class AppError extends Error {
    constructor(
      message: string,
      public statusCode: number,
      public code: string,
    ) {
      super(message);
      this.name = this.constructor.name;
    }
  }

  class NotFoundError extends AppError {
    constructor(resource: string) {
      super(`${resource} not found`, 404, "NOT_FOUND");
    }
  }
  ```
- [ ] **Global Error Handler** 패턴
  ```typescript
  app.use((err: Error, req: Request, res: Response, next: NextFunction) => {
    if (err instanceof AppError) {
      return res.status(err.statusCode).json({ code: err.code, message: err.message });
    }
    console.error(err);
    res.status(500).json({ code: "INTERNAL_ERROR", message: "Internal Server Error" });
  });
  ```
- [ ] **neverthrow** 라이브러리 (Result 모나드)
  ```typescript
  import { ok, err, Result } from "neverthrow";

  function divide(a: number, b: number): Result<number, string> {
    if (b === 0) return err("Division by zero");
    return ok(a / b);
  }
  ```
- **왜 질문:**
  - try-catch 남발의 문제점은? (에러 흐름 추적 어려움)
  - **왜 질문:** Result 패턴이 try-catch보다 나은 점은? (타입 레벨에서 에러 가능성 표현)
  - Spring의 `@ControllerAdvice`와 Express의 error middleware 비교?

---

## Day 20 (토) — NestJS 입문: DI & 모듈

**코드 (5h)**

오전 (3h) — NestJS 기본
- [ ] NestJS 프로젝트 생성
  ```bash
  pnpm add -g @nestjs/cli
  nest new trader-alert-service
  ```
- [ ] **NestJS와 Spring 비교**
  - `@Module` = `@Configuration`
  - `@Injectable` = `@Component` / `@Service`
  - `@Controller` = `@RestController`
  - `@Get`, `@Post` = `@GetMapping`, `@PostMapping`
- [ ] **DI(Dependency Injection)** — Spring과 동일!
  ```typescript
  @Injectable()
  export class OrderService {
    constructor(private readonly orderRepository: OrderRepository) {}
  }
  ```
- [ ] **Module** 구조
  ```typescript
  @Module({
    imports: [DatabaseModule],
    controllers: [OrderController],
    providers: [OrderService, OrderRepository],
    exports: [OrderService],
  })
  export class OrderModule {}
  ```
- **왜 질문:**
  - NestJS DI와 Spring DI의 차이는? (둘 다 IoC 컨테이너 — NestJS는 reflect-metadata 사용)
  - **왜 질문:** `providers`에 넣는 것과 `exports`에 넣는 것의 차이는?

오후 (2h) — Controller & Validation
- [ ] **Controller** 작성
  ```typescript
  @Controller("orders")
  export class OrderController {
    constructor(private readonly orderService: OrderService) {}

    @Post()
    @HttpCode(HttpStatus.CREATED)
    async create(@Body() dto: CreateOrderDto): Promise<OrderResponse> {
      return this.orderService.create(dto);
    }

    @Get(":id")
    async findOne(@Param("id") id: string): Promise<OrderResponse> {
      return this.orderService.findOne(id);
    }
  }
  ```
- [ ] **ValidationPipe** + class-validator (Bean Validation 대응)
  ```typescript
  class CreateOrderDto {
    @IsString() @Length(6, 6) symbol: string;
    @IsNumber() @Min(1) price: number;
    @IsEnum(OrderType) type: OrderType;
  }
  ```
- **왜 질문:** class-validator vs Zod — NestJS에서 뭘 쓰나? (공식은 class-validator, 하지만 Zod도 가능)

---

## Day 21 (일) — Week 3 정리 + 블로그

**오전 (2.5h)**
- [ ] tsconfig 핵심 옵션 정리
- [ ] NestJS vs Spring 구조 비교표
- [ ] **블로그 작성:** "NestJS — Spring 개발자가 느끼는 기시감과 차이점"
- [ ] 다음 주 예습: Prisma/TypeORM 문서
- 오후: 휴식

**Week 3 PR:** NestJS 프로젝트 구조 + Zod 검증 + 기본 CRUD API

---

# Week 4 — NestJS 심화: ORM & 미들웨어

---

## Day 22 (월) — Prisma ORM

**코드 (2.5h)**
- [ ] **Prisma** 설정 (JPA 대안)
  ```prisma
  // schema.prisma
  model Order {
    id        String   @id @default(uuid())
    symbol    String   @db.VarChar(6)
    price     Decimal
    quantity  Int
    status    OrderStatus @default(OPEN)
    createdAt DateTime @default(now())
    user      User     @relation(fields: [userId], references: [id])
    userId    String
  }

  enum OrderStatus {
    OPEN
    FILLED
    CANCELLED
  }
  ```
- [ ] `npx prisma migrate dev` — 마이그레이션
- [ ] `npx prisma generate` — 타입 안전 클라이언트 생성
- [ ] CRUD 구현
  ```typescript
  @Injectable()
  export class OrderRepository {
    constructor(private readonly prisma: PrismaService) {}

    async findById(id: string): Promise<Order | null> {
      return this.prisma.order.findUnique({ where: { id } });
    }

    async create(data: CreateOrderDto): Promise<Order> {
      return this.prisma.order.create({ data });
    }
  }
  ```
- **왜 질문:**
  - Prisma vs TypeORM 차이는? (Prisma: 스키마 우선, 타입 안전 / TypeORM: 엔티티 클래스, JPA 유사)
  - **왜 질문:** Prisma가 N+1을 어떻게 다루나? (`include`로 eager loading)
  - JPA의 영속성 컨텍스트 같은 개념이 있나? (없음 — 매 쿼리가 독립적)

---

## Day 23 (화) — Guard & Interceptor (AOP 대응)

**코드 (2.5h)**
- [ ] **Guard** — Spring Security Filter 대응
  ```typescript
  @Injectable()
  export class JwtAuthGuard implements CanActivate {
    canActivate(context: ExecutionContext): boolean {
      const request = context.switchToHttp().getRequest();
      const token = request.headers.authorization?.split(" ")[1];
      // 토큰 검증 로직
      return !!token;
    }
  }

  @UseGuards(JwtAuthGuard)
  @Controller("orders")
  export class OrderController { ... }
  ```
- [ ] **Interceptor** — AOP @Around 대응
  ```typescript
  @Injectable()
  export class LoggingInterceptor implements NestInterceptor {
    intercept(context: ExecutionContext, next: CallHandler): Observable<any> {
      const start = Date.now();
      return next.handle().pipe(
        tap(() => console.log(`${Date.now() - start}ms`)),
      );
    }
  }
  ```
- [ ] **Pipe** — 입력 변환/검증
- [ ] **Filter** — 예외 처리 (Spring @ControllerAdvice 대응)
  ```typescript
  @Catch(AppError)
  export class AppErrorFilter implements ExceptionFilter {
    catch(exception: AppError, host: ArgumentsHost) {
      const response = host.switchToHttp().getResponse();
      response.status(exception.statusCode).json({
        code: exception.code,
        message: exception.message,
      });
    }
  }
  ```
- **왜 질문:**
  - NestJS의 요청 흐름: Middleware → Guard → Interceptor(전) → Pipe → Handler → Interceptor(후) → Filter
  - Spring과 비교: Filter → Interceptor → AOP → Controller → @ControllerAdvice
  - **왜 질문:** 구조가 왜 이렇게 비슷한가? (NestJS가 Spring에서 영감받음)

---

## Day 24 (수) — JWT 인증 (Passport)

**코드 (2.5h)**
- [ ] `@nestjs/passport` + `passport-jwt` 설정
  ```typescript
  @Injectable()
  export class JwtStrategy extends PassportStrategy(Strategy) {
    constructor() {
      super({
        jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
        secretOrKey: process.env.JWT_SECRET,
      });
    }

    async validate(payload: JwtPayload): Promise<User> {
      return { id: payload.sub, email: payload.email };
    }
  }
  ```
- [ ] 로그인 → JWT 발급 → 보호된 API 접근
- [ ] `@CurrentUser()` 커스텀 데코레이터
  ```typescript
  export const CurrentUser = createParamDecorator(
    (data: unknown, ctx: ExecutionContext) => {
      return ctx.switchToHttp().getRequest().user;
    },
  );
  ```
- **왜 질문:**
  - Spring Security와 Passport.js의 차이는? (Passport: 전략 패턴, 가벼움)
  - **왜 질문:** NestJS에서 `@UseGuards(AuthGuard('jwt'))`가 내부적으로 하는 일은?

---

## Day 25 (목) — 테스트 (Jest)

**코드 (2.5h)**
- [ ] **단위 테스트**
  ```typescript
  describe("OrderService", () => {
    let service: OrderService;
    let repository: jest.Mocked<OrderRepository>;

    beforeEach(async () => {
      const module = await Test.createTestingModule({
        providers: [
          OrderService,
          { provide: OrderRepository, useValue: { findById: jest.fn() } },
        ],
      }).compile();
      service = module.get(OrderService);
      repository = module.get(OrderRepository);
    });

    it("should throw NotFoundError", async () => {
      repository.findById.mockResolvedValue(null);
      await expect(service.findOne("1")).rejects.toThrow(NotFoundError);
    });
  });
  ```
- [ ] **E2E 테스트** (통합)
  ```typescript
  describe("OrderController (e2e)", () => {
    let app: INestApplication;

    beforeAll(async () => {
      const module = await Test.createTestingModule({
        imports: [AppModule],
      }).compile();
      app = module.createNestApplication();
      await app.init();
    });

    it("POST /orders → 201", () => {
      return request(app.getHttpServer())
        .post("/orders")
        .send({ symbol: "005930", price: 70000, quantity: 10 })
        .expect(201);
    });
  });
  ```
- **왜 질문:**
  - Jest vs Vitest 차이는? (Vitest: ESM 네이티브, 빠름)
  - NestJS `Test.createTestingModule`이 Spring의 `@SpringBootTest`와 비슷한 점은?
  - **왜 질문:** 테스트에서 DB를 어떻게 격리하나? (트랜잭션 롤백, 또는 Testcontainers)

---

## Day 26 (금) — Swagger + 환경 설정

**코드 (2.5h)**
- [ ] **Swagger** (OpenAPI) 자동 생성
  ```typescript
  const config = new DocumentBuilder()
    .setTitle("Trader Alert Service")
    .addBearerAuth()
    .build();
  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup("docs", app, document);
  ```
- [ ] **환경 설정** (`@nestjs/config`)
  ```typescript
  @Module({
    imports: [ConfigModule.forRoot({ isGlobal: true })],
  })
  ```
  - `.env` + `ConfigService`로 환경변수 관리
  - Zod로 환경변수 검증
- [ ] **Docker** 설정
  ```dockerfile
  FROM node:20-alpine AS builder
  WORKDIR /app
  COPY package.json pnpm-lock.yaml ./
  RUN corepack enable && pnpm install --frozen-lockfile
  COPY . .
  RUN pnpm build

  FROM node:20-alpine
  WORKDIR /app
  COPY --from=builder /app/dist ./dist
  COPY --from=builder /app/node_modules ./node_modules
  CMD ["node", "dist/main.js"]
  ```
- **왜 질문:**
  - Spring Boot의 application.yml과 .env의 차이는?
  - **왜 질문:** Node.js 이미지를 alpine으로 쓰는 이유와 주의점은?

---

## Day 27 (토) — 비동기 작업 (BullMQ)

**코드 (5h)**

오전 (3h) — BullMQ (메시지 큐)
- [ ] **BullMQ** — Redis 기반 작업 큐 (Spring의 `@Async` + 큐 대응)
  ```typescript
  // Producer
  @Injectable()
  export class AlertProducer {
    constructor(@InjectQueue("alerts") private queue: Queue) {}

    async addAlert(data: AlertData) {
      await this.queue.add("check-price", data, {
        delay: 5000,
        attempts: 3,
        backoff: { type: "exponential", delay: 1000 },
      });
    }
  }

  // Consumer
  @Processor("alerts")
  export class AlertConsumer {
    @Process("check-price")
    async handleAlert(job: Job<AlertData>) {
      // 시세 확인 + 조건 만족 시 알림
    }
  }
  ```
- [ ] 재시도, 지연 실행, 스케줄링
- **왜 질문:**
  - BullMQ vs Kafka 차이는? (BullMQ: 작업 큐 / Kafka: 이벤트 스트림)
  - **왜 질문:** 왜 API 핸들러에서 직접 처리 안 하고 큐에 넣나? (응답 속도 + 재시도 + 격리)

오후 (2h) — 스케줄링 + 외부 API 호출
- [ ] **Cron** 작업 (`@nestjs/schedule`)
  ```typescript
  @Cron("*/10 * * * * *")  // 10초마다
  async checkPrices() {
    // 조건 확인 → 알림 발행
  }
  ```
- [ ] **외부 API 호출** (axios + 재시도)
  ```typescript
  const quote = await firstValueFrom(
    this.httpService.get<KisResponse>("/quote", { params: { symbol } }).pipe(
      retry({ count: 3, delay: 1000 }),
      catchError(err => { throw new ExternalApiError(err.message); }),
    ),
  );
  ```
- **왜 질문:** Node.js 싱글 스레드에서 Cron이 겹치면? (concurrent 실행 제어 필요)

---

## Day 28 (일) — Week 4 정리 + 블로그

**오전 (2.5h)**
- [ ] NestJS 요청 흐름 다이어그램 (vs Spring)
- [ ] Prisma vs JPA 비교표
- [ ] **블로그 작성:** "NestJS로 CRUD API 만들기 — Spring 개발자의 시선"
- [ ] 다음 주 예습: 실전 프로젝트 기획
- 오후: 휴식

**Week 4 PR:** JWT 인증 + Prisma CRUD + BullMQ 비동기 + 테스트

---

# Week 5–6 — 실전 프로젝트: 시세 알림 서비스

---

## Day 29 (월) — 프로젝트 설계

**이해 + 코드 (2.5h)**
- [ ] 요구사항 정의
  - 사용자가 종목 + 가격 조건 등록 (예: "삼성전자 70000원 이하이면 알림")
  - 백그라운드 워커가 주기적으로 시세 확인
  - 조건 만족 시 알림 (Slack webhook)
- [ ] 아키텍처 설계
  ```
  [Client] → [API Server (NestJS)]
                    ↓
              [PostgreSQL] (조건 저장)
                    ↓
              [BullMQ + Redis] (주기적 체크 작업)
                    ↓
              [외부 거래소 API] (시세 조회)
                    ↓
              [Slack Webhook] (알림 발송)
  ```
- [ ] 엔티티 설계
  ```prisma
  model AlertCondition {
    id        String   @id @default(uuid())
    userId    String
    symbol    String
    targetPrice Decimal
    direction   Direction  // ABOVE | BELOW
    active    Boolean  @default(true)
    triggeredAt DateTime?
    createdAt DateTime @default(now())
  }
  ```
- [ ] API 설계
  - `POST /alerts` — 조건 등록
  - `GET /alerts` — 내 조건 목록
  - `DELETE /alerts/:id` — 조건 삭제
  - `GET /alerts/history` — 발동 이력

---

## Day 30 (화) — API 구현

**코드 (2.5h)**
- [ ] AlertModule 생성
- [ ] AlertController + AlertService + AlertRepository 구현
- [ ] Zod 스키마로 입력 검증
  ```typescript
  const CreateAlertSchema = z.object({
    symbol: z.string().length(6),
    targetPrice: z.number().positive(),
    direction: z.enum(["ABOVE", "BELOW"]),
  });
  ```
- [ ] JWT 인증 적용 — 본인 조건만 관리
- [ ] Swagger 문서 완성

---

## Day 31 (수) — 시세 조회 모듈

**코드 (2.5h)**
- [ ] QuoteService — 외부 거래소 API 호출
  ```typescript
  @Injectable()
  export class QuoteService {
    async getPrice(symbol: string): Promise<number> {
      const response = await this.httpService.axiosRef.get(...);
      const validated = QuoteResponseSchema.parse(response.data);
      return Number(validated.output.stck_prpr);
    }
  }
  ```
- [ ] 캐싱 (Redis, TTL 10초)
- [ ] 에러 처리 (외부 API 실패 시 fallback)
- [ ] Rate Limiting 준수 (외부 API 호출 제한)
- **왜 질문:** 여러 사용자가 같은 종목을 구독하면 API 호출을 어떻게 최적화하나? (종목별 1회만 조회)

---

## Day 32 (목) — 백그라운드 워커 (가격 체크)

**코드 (2.5h)**
- [ ] BullMQ 반복 작업 설정
  ```typescript
  // 10초마다 활성 조건 체크
  await this.queue.add("check-all", {}, {
    repeat: { every: 10000 },
  });
  ```
- [ ] 가격 체크 로직
  ```typescript
  @Process("check-all")
  async handleCheck(job: Job) {
    const activeAlerts = await this.alertRepo.findAllActive();
    const symbols = [...new Set(activeAlerts.map(a => a.symbol))];

    // 종목별 시세 조회 (중복 제거)
    const prices = await Promise.all(
      symbols.map(s => this.quoteService.getPrice(s))
    );

    // 조건 매칭
    for (const alert of activeAlerts) {
      const currentPrice = priceMap.get(alert.symbol);
      if (this.isTriggered(alert, currentPrice)) {
        await this.triggerAlert(alert, currentPrice);
      }
    }
  }
  ```
- [ ] 트리거 시 → 알림 발송 + 조건 비활성화
- **왜 질문:** 조건이 10만 개면? (배치 처리, 파티셔닝, 또는 여러 워커 분산)

---

## Day 33 (금) — 알림 발송 + 에러 처리

**코드 (2.5h)**
- [ ] Slack Webhook 알림
  ```typescript
  @Injectable()
  export class NotificationService {
    async sendSlack(alert: AlertCondition, currentPrice: number) {
      await this.httpService.axiosRef.post(this.webhookUrl, {
        text: `🔔 ${alert.symbol} 현재가 ${currentPrice}원 — 목표 ${alert.targetPrice}원 ${alert.direction === "BELOW" ? "이하" : "이상"} 도달!`,
      });
    }
  }
  ```
- [ ] 에러 처리
  - 외부 API 장애 → 재시도 (BullMQ attempts)
  - 알림 발송 실패 → DLQ (Dead Letter Queue)
  - 로그 기록
- [ ] 모니터링 메트릭
  - 체크 횟수, 트리거 횟수, 실패 횟수
- **왜 질문:** 알림이 중복 발송되면? (멱등성 — triggeredAt으로 중복 방지)

---

## Day 34 (토) — 테스트 + Docker 배포

**코드 (5h)**

오전 (3h) — 테스트
- [ ] 단위 테스트
  - AlertService: 조건 생성/삭제/조회
  - 가격 체크 로직: 조건 매칭 (ABOVE/BELOW)
  - 트리거 시 알림 발송 + 비활성화
- [ ] 통합 테스트
  - API E2E (인증 포함)
  - Testcontainers (PostgreSQL + Redis)
- [ ] 엣지 케이스
  - 외부 API 장애 시 graceful 처리
  - 동시에 같은 조건 트리거 (중복 방지)

오후 (2h) — Docker + 배포
- [ ] Docker Compose (NestJS + PostgreSQL + Redis)
- [ ] 헬스체크 엔드포인트
- [ ] 환경변수 분리 (.env.dev / .env.prod)
- [ ] CI/CD (GitHub Actions)
  - PR: lint + test
  - main: Docker build + push

---

## Day 35 (일) — Week 5 정리 + 블로그

**오전 (2.5h)**
- [ ] 프로젝트 아키텍처 다이어그램 최종 정리
- [ ] Spring으로 같은 것 만들었을 때와 비교
- [ ] **블로그 작성:** "NestJS로 시세 알림 서비스 만들기 — Spring 개발자의 TS 실전기"
- 오후: 휴식

**Week 5 PR:** 시세 알림 서비스 MVP (API + 워커 + 알림 + 테스트)

---

## Day 36~40 (Week 6 월~금) — 고도화 + 마무리

### Day 36 (월) — 성능 최적화
- [ ] 대량 조건 처리 최적화 (배치 조회, 인메모리 캐시)
- [ ] DB 쿼리 최적화 (인덱스, 페이지네이션)
- [ ] 부하 테스트 (k6)

### Day 37 (화) — WebSocket 실시간 알림
- [ ] NestJS Gateway (WebSocket)으로 실시간 알림 push
- [ ] 클라이언트 연결 관리
- [ ] SSE 대안 구현

### Day 38 (수) — 모니터링 & 로깅
- [ ] 구조화된 로깅 (pino / winston)
- [ ] Prometheus 메트릭 노출
- [ ] Grafana 대시보드

### Day 39 (목) — 코드 리뷰 & 리팩토링
- [ ] 전체 코드 셀프 리뷰
- [ ] Java/Spring으로 짰을 때와 비교 노트
- [ ] 리팩토링 (타입 안전성 강화, 에러 처리 정비)

### Day 40 (금) — 문서화 + API 완성
- [ ] README 작성 (설치, 실행, API 문서)
- [ ] Swagger 최종 정비
- [ ] Docker Compose 원클릭 실행 확인

---

## Day 41 (토) — 종합 정리 + 비교 분석

**이해 + 정리 (5h)**

오전 (3h) — TS vs Java 최종 비교
- [ ] 같은 기능을 Java/Spring으로 만들었을 때 코드량 비교
- [ ] 개발 속도 비교 (체감)
- [ ] 타입 안전성 비교
- [ ] 운영 안정성 비교 (싱글 스레드 한계)
- [ ] 언제 Java, 언제 TS를 쓸지 결정 기준 정리

| 상황 | Java/Spring | TypeScript/NestJS |
|---|---|---|
| 대규모 엔터프라이즈 | ✓ | |
| 빠른 프로토타이핑 | | ✓ |
| CPU 집약 작업 | ✓ | |
| I/O 집약 작업 | | ✓ |
| 풀스택 (Next.js) | | ✓ |
| 서버리스 | | ✓ |
| 복잡한 도메인 로직 | ✓ | |

오후 (2h) — 앞으로의 학습 로드맵
- [ ] TS 심화: 타입 챌린지 (github.com/type-challenges)
- [ ] Deno / Bun 런타임
- [ ] tRPC (타입 안전 API)
- [ ] Turborepo (모노레포)

---

## Day 42 (일) — TypeScript 학습 졸업 + 블로그

**오전 (2.5h)**
- [ ] **블로그 작성:** "6주간 TypeScript 학습 회고 — Java 백엔드가 TS를 배우면"
- [ ] 면접 대비: TS 핵심 질문 10개 셀프 답변
- [ ] 시세 알림 서비스 GitHub에 공개
- 오후: 완전 휴식

**Week 6 PR:** 고도화 (WebSocket + 모니터링 + 성능 최적화) + 최종 문서

---

## TypeScript 완료 체크리스트

### PR 목록 (6개)
- [ ] W1: 기본 타입 + 외부 API 타입 정의
- [ ] W2: Utility Types + async/await + 에러 처리
- [ ] W3: NestJS 기본 + Zod + CRUD
- [ ] W4: JWT + Prisma + BullMQ + 테스트
- [ ] W5: 시세 알림 서비스 MVP
- [ ] W6: 고도화 + WebSocket + 모니터링

### 블로그 (6편)
- [ ] W1: Java 개발자가 TS 배울 때 헷갈리는 것
- [ ] W2: Utility Types로 DTO 줄이기
- [ ] W3: NestJS — Spring 개발자의 시선
- [ ] W4: NestJS CRUD 만들기
- [ ] W5: 시세 알림 서비스 실전기
- [ ] W6: 6주 TypeScript 학습 회고

### "왜"에 답할 수 있어야 하는 것들
- [ ] 구조적 타이핑 vs 명목적 타이핑
- [ ] TS 타입이 런타임에 없는 이유와 대안 (Zod)
- [ ] `any` vs `unknown` vs `never` 차이
- [ ] Utility Types 동작 원리 (Mapped Types)
- [ ] async/await과 이벤트 루프 관계
- [ ] Node.js 싱글 스레드 모델과 Java 멀티스레드의 차이
- [ ] NestJS DI와 Spring DI의 유사점/차이점
- [ ] Prisma vs JPA 접근 방식 차이
- [ ] Result 패턴이 try-catch보다 나은 점
- [ ] TS/NestJS vs Java/Spring 선택 기준
