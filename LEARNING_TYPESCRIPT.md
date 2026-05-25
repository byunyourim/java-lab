# TypeScript 학습 가이드 (백엔드 개발자용)

> **대상:** Java/Spring 익숙 + TypeScript 초보
> **목표:** Node.js 백엔드를 TypeScript로 짤 수 있는 수준
> **기간:** 4~6주

---

## 왜 백엔드에서 TypeScript를?

- **빠른 프로토타이핑** — Spring보다 가볍게 API 서버
- **풀스택 일관성** — frontend(Next.js)와 같은 언어
- **이벤트 기반 / I/O 집약** 워크로드에 강함 (Node.js 특성)
- **서버리스** (AWS Lambda) 친화적
- **AI/LLM 도구 생태계** 가 TS/Python 중심

Java 개발자가 TS를 배우면 **양쪽 도구를 상황에 맞게 골라 쓰는 백엔드**가 될 수 있습니다.

---

## Java 개발자 관점에서 본 TypeScript

먼저 머릿속에 매핑 테이블을 만들면 학습이 빨라집니다.

| Java | TypeScript | 비고 |
|---|---|---|
| `class` | `class` | 거의 동일 |
| `interface` | `interface` 또는 `type` | TS는 더 유연 |
| `enum` | `enum` 또는 union type | union type이 더 권장 |
| `Optional<T>` | `T \| undefined`, `T \| null` | 언어 차원에서 지원 |
| `List<T>` | `T[]` 또는 `Array<T>` | |
| `Map<K,V>` | `Map<K,V>` 또는 `Record<K,V>` | |
| `Generic<T>` | `Generic<T>` | 거의 동일 |
| `@Override` | (없음) | TS는 구조적 타이핑 |
| `final` | `readonly`, `const` | |
| 체크 예외 | 없음 | 전부 unchecked |
| Maven/Gradle | npm/pnpm/yarn | |
| `pom.xml` / `build.gradle` | `package.json` | |

---

## ❗ Java와 가장 다른 핵심 개념 3가지

이걸 모르면 자바스럽게 짜다가 막힙니다.

### 1. 구조적 타이핑 (Structural Typing)

Java는 **명목적(Nominal)** — 이름이 같아야 같은 타입
TS는 **구조적(Structural)** — 모양이 같으면 같은 타입

```typescript
interface Dog { bark(): void }
interface Robot { bark(): void }

const d: Dog = { bark: () => {} };
const r: Robot = d;  // ✓ 컴파일 통과! 모양이 같으니까
```

Java라면 `Dog`를 `Robot`에 못 넣지만, TS는 됩니다. **"오리처럼 걷고 울면 오리"**(duck typing).

---

### 2. 타입은 런타임에 없다

TypeScript 타입은 **컴파일 후 사라집니다.** 자바스크립트로 변환되면 타입 정보가 다 없어짐.

```typescript
function check(x: unknown) {
  if (x instanceof MyClass) {  // ✓ 클래스는 런타임에 존재
    // ...
  }
  if (typeof x === 'object') {  // ✓ JS 자체 기능
    // ...
  }
  // x instanceof MyInterface  // ✗ 인터페이스는 컴파일 후 없음
}
```

→ Java의 리플렉션처럼 "타입을 런타임에 검사"는 안 됨. 대안: **Zod**, **io-ts** 같은 런타임 검증 라이브러리.

---

### 3. `any`, `unknown`, `never`

| 타입 | 의미 | 사용 |
|---|---|---|
| `any` | 타입 체크 끄기 | **쓰지 마세요.** TS 쓰는 의미가 없어짐 |
| `unknown` | 무슨 타입인지 모름 | 외부 입력값 — 검사 후 사용 |
| `never` | 절대 도달 불가 | exhaustive check |

```typescript
function bad(x: any) { x.whatever() }      // 컴파일 통과, 런타임 폭발
function good(x: unknown) {
  if (typeof x === 'string') x.toUpperCase()  // 좁히기(narrowing) 후 사용
}
```

---

## Week 1: TypeScript 문법 핵심

### 학습 자료
- **공식 핸드북** — https://www.typescriptlang.org/docs/handbook/intro.html (강추, 한국어 번역 있음)
- **점프 투 TS** 또는 inflearn "한 입 크기로 잘라먹는 TypeScript" (이정환)

### 학습 순서

1. **기본 타입** — `string`, `number`, `boolean`, `null`, `undefined`, `void`
2. **배열 & 튜플** — `T[]`, `[string, number]`
3. **객체 타입** — `interface` vs `type`
4. **함수 타입** — 매개변수/반환 타입, 옵셔널 파라미터
5. **유니온 & 인터섹션** — `A | B`, `A & B`
6. **리터럴 타입** — `'red' | 'green' | 'blue'`
7. **제네릭** — Java와 거의 동일
8. **타입 좁히기 (Narrowing)** — `typeof`, `instanceof`, `in`

### 손코딩 과제 (작게 자주)
- Java 코드 → TypeScript 변환 5개
- 외부 API JSON 응답을 받아서 타입 정의 + 처리
- `Optional`을 union type으로 표현

---

## Week 2: TypeScript 중급

### 핵심 주제

1. **`interface` vs `type` 차이**
   - interface: 확장(extends) 자연스러움, 선언 병합 가능
   - type: union, intersection, mapped type 가능
   - **결론:** 객체는 interface, 그 외는 type
2. **Utility Types** — 무조건 외워야 하는 것들
   ```typescript
   Partial<T>       // 모든 필드 옵셔널
   Required<T>      // 모든 필드 필수
   Readonly<T>      // 모든 필드 readonly
   Pick<T, K>       // 특정 필드만
   Omit<T, K>       // 특정 필드 제외
   Record<K, V>     // Map 같은
   ReturnType<F>    // 함수 반환 타입 추출
   Awaited<T>       // Promise 풀기
   ```
3. **enum vs union**
   ```typescript
   // 비추
   enum Status { OPEN, CLOSED }
   // 권장
   type Status = 'OPEN' | 'CLOSED';
   ```
4. **타입 가드 (Type Guard)**
   ```typescript
   function isOrder(x: unknown): x is Order { ... }
   ```
5. **as const & satisfies** (Java에 없는 강력한 기능)

### 손코딩 과제
- Spring DTO를 TypeScript interface로 옮기기
- API 응답에 Utility Type 적용 (CreateRequest = Omit<Order, 'id'>)
- enum을 union으로 리팩토링

---

## Week 3: 비동기 & 모듈

### 핵심 주제

1. **Promise** — Java `CompletableFuture`와 유사
2. **async / await** — Spring의 `@Async`보다 자연스러움
3. **에러 처리** — try/catch + Promise.catch
4. **ESM vs CommonJS** — `import` vs `require` (백엔드는 ESM 권장)
5. **tsconfig.json** — 빌드 설정
   - `strict: true` 필수
   - `target`, `module`, `moduleResolution`

### 손코딩 과제
- 외부 API 동시 호출 — `Promise.all`, `Promise.allSettled`
- 타임아웃 + 재시도 유틸 함수 작성
- `tsconfig.json` strict 모드로 마이그레이션

---

## Week 4: Node.js 백엔드 프레임워크

Java/Spring에 익숙하면 **NestJS** 가 가장 자연스럽습니다.

### NestJS (Spring과 거의 동일한 철학)
- **DI 컨테이너** — Spring과 똑같음
- **데코레이터** — `@Controller`, `@Injectable`, `@Module`
- **AOP**, Interceptor, Guard, Pipe
- **TypeORM/Prisma** — JPA처럼 ORM 사용

```typescript
@Controller('orders')
export class OrderController {
  constructor(private orderService: OrderService) {}

  @Post()
  async create(@Body() dto: CreateOrderDto) {
    return this.orderService.create(dto);
  }
}
```

→ Spring 알면 1주일이면 익숙해집니다.

### 대안 프레임워크

| 프레임워크 | 특징 | 추천 대상 |
|---|---|---|
| **NestJS** | Spring 스타일 | Java 백엔드 출신 ⭐ |
| **Express** | 가장 가벼움, 자유 | 단순 API |
| **Fastify** | 빠름 | 성능 중시 |
| **Hono** | Edge/Serverless 친화 | Cloudflare Workers 등 |

---

## Week 5-6: 실전 프로젝트

### 미니 프로젝트 아이디어
trader-bot에 **사이드카 서비스**를 NestJS로 만들기

**시나리오:** 시세 알림 서비스
- 사용자가 종목 + 가격 조건 등록
- 백그라운드 워커가 시세 폴링
- 조건 만족 시 알림 (이메일/Slack)

**기능 요구사항**
- [x] NestJS + TypeScript + strict 모드
- [x] Prisma로 PostgreSQL 연결
- [x] **Zod**로 입력 검증
- [x] JWT 인증
- [x] 외부 거래소 API 호출 (axios + 재시도)
- [x] Kafka 또는 BullMQ로 비동기 작업
- [x] Swagger 자동 생성
- [x] Jest 테스트

**학습 포인트**
- Java Spring으로 짰을 때와 비교
- 같은 패턴이 TS에서 어떻게 표현되는지

---

## 실수하기 쉬운 부분 (Java 출신 관점)

### 1. `null` vs `undefined`
- `null`: "의도적으로 값 없음"
- `undefined`: "할당 안 됨"
- **권장:** 둘 다 쓰지 말고 명시적으로 — `Optional` 흉내 내고 싶으면 `T | null`로 통일

### 2. `==` 쓰지 마세요
```typescript
'1' == 1    // true  😱
'1' === 1   // false ✓
```
ESLint가 잡아주지만 항상 `===` 쓰는 습관

### 3. `this` 바인딩 함정
화살표 함수와 일반 함수의 `this`가 다름. Java처럼 동작하지 않음.

### 4. 모든 게 `Object` 아님
TS의 `object` 타입은 Java `Object`가 아님. Primitive 제외한 모든 것.

### 5. 동시성
Node.js는 **싱글 스레드 이벤트 루프**. Java의 멀티스레드 동시성 고민과 완전히 다른 모델.
- `synchronized` 없음
- 대신 이벤트 루프 블로킹 주의 (`fs.readFileSync` 같은 동기 I/O 금지)
- CPU 무거운 작업은 `Worker Threads` 또는 별도 프로세스로

---

## 추천 도구

| 용도 | 도구 |
|---|---|
| 런타임 | **Node.js 20+** 또는 **Bun** |
| 패키지 매니저 | **pnpm** (npm보다 빠르고 가벼움) |
| 린터 | **ESLint** + `@typescript-eslint` |
| 포맷터 | **Prettier** |
| 검증 | **Zod** ← 백엔드 필수 |
| ORM | **Prisma** (입문) 또는 **TypeORM** (Spring JPA 닮음) |
| 테스트 | **Vitest** (빠름) 또는 **Jest** |
| API 클라이언트 | **axios** 또는 **ky** |

---

## 추천 학습 자료

| 단계 | 자료 |
|---|---|
| 입문 | "한 입 크기로 잘라먹는 TypeScript" (인프런, 이정환) — 입문 최고 |
| 공식 | TypeScript Handbook (한국어) |
| 중급 | "이펙티브 타입스크립트" (Dan Vanderkam) — 강추 |
| 심화 | "Programming TypeScript" (Boris Cherny) |
| NestJS | 공식 문서 (NestJS Fundamentals) |
| 타입 챌린지 | github.com/type-challenges/type-challenges |

---

## Java 개발자를 위한 학습 순서 요약

```
Week 1: 문법 핵심 (타입, 인터페이스, 제네릭)
   ↓
Week 2: 중급 (Utility Types, 타입 가드, satisfies)
   ↓
Week 3: 비동기 & 모듈 (Promise, async/await, ESM)
   ↓
Week 4: NestJS (Spring 같은 프레임워크)
   ↓
Week 5-6: 사이드 프로젝트 (시세 알림 서비스)
```

**한 가지 원칙:** Java식으로 TS 짜면 안 됨. **TypeScript는 JavaScript 위에 얹은 타입 시스템**이라는 정체성을 잊지 말 것. JS 자체 동작도 함께 배워야 합니다.

---

## 진도 체크
- [ ] Week 1: 기본 문법
- [ ] Week 2: 중급 (Utility Types, Narrowing)
- [ ] Week 3: 비동기 & 모듈
- [ ] Week 4: NestJS 입문
- [ ] Week 5-6: 사이드 프로젝트
