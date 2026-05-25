# 3단계 실습 가이드: 네트워크 & HTTP

> **기간:** 3~4주
> **선수 과정:** [1단계](./LEARNING_STAGE1.md), [2단계](./LEARNING_STAGE2.md)
> **이 단계의 의의:** 백엔드의 인터페이스를 깊이 이해. 면접 단골 + 운영 디버깅의 기본기.

---

## 학습 원칙

1. **패킷 캡처를 직접 해본다** — 글로만 읽지 말고 Wireshark / `tcpdump`로 보기
2. **trader-bot으로 측정** — 외부 거래소 API 호출이 좋은 실험 대상
3. **보안은 항상 함께** — TLS, CORS, CSRF는 네트워크 학습과 분리 불가

---

## Week 1: 네트워크 기초

### 과제 1-1. TCP 3/4-way handshake 캡처 (난이도 ★★)

**할 일**
1. Wireshark 설치
2. `curl https://example.com` 실행하면서 캡처
3. SYN → SYN-ACK → ACK → ... → FIN 흐름 직접 확인
4. **TLS handshake**도 캡처해서 Client Hello / Server Hello / Certificate / Key Exchange 단계 식별
5. `tcpdump -i lo0 -nn port 8080` 으로 trader-bot 호출 캡처

**산출물**: 캡처 스크린샷 + 각 패킷 의미 주석

---

### 과제 1-2. TCP vs UDP 실험 (난이도 ★★)

**할 일**
1. Java NIO로 간단한 TCP 에코 서버 + UDP 에코 서버 작성
2. 클라이언트에서 1000개 메시지 전송
3. **네트워크 손실 시뮬레이션** (`tc` 또는 Network Link Conditioner)
4. TCP는 재전송으로 모두 도달, UDP는 손실 확인
5. 처리량 비교

**산출물**: 처리량/신뢰성 비교표

---

### 과제 1-3. DNS & 로드 밸런서 이해 (난이도 ★)

**할 일**
1. `dig`, `nslookup`으로 A, AAAA, CNAME, MX 레코드 조회
2. **DNS 라운드 로빈** 시뮬레이션 — 같은 호스트에 IP 2개 등록 (`/etc/hosts` 트릭)
3. Nginx로 L7 로드 밸런서 띄우고 trader-bot 백엔드 2개 분산
4. L4 vs L7 차이 정리 (TCP 레벨 vs HTTP 레벨)

---

## Week 2: HTTP 깊이 파기

### 과제 2-1. HTTP 메시지 직접 작성 (난이도 ★★)

**목표:** HTTP는 결국 텍스트

**할 일**
1. `telnet` 또는 `nc`로 trader-bot에 raw HTTP 요청 보내기
   ```
   GET /api/orders HTTP/1.1
   Host: localhost:8080
   Authorization: Bearer ...
   ```
2. 응답 헤더 직접 파싱
3. `Transfer-Encoding: chunked` 응답 보내는 엔드포인트 만들기
4. Keep-Alive vs Close 동작 차이 확인

---

### 과제 2-2. HTTP 캐시 헤더 (난이도 ★★)

**할 일**
1. 시세 조회 API에 다음 헤더 적용 후 동작 확인
   - `Cache-Control: max-age=5, public`
   - `ETag` + `If-None-Match` → 304 응답
   - `Last-Modified` + `If-Modified-Since`
2. 브라우저 캐시 / CDN 캐시 / 프록시 캐시 동작 차이 정리
3. `Vary` 헤더 — Accept-Language 별로 다른 캐시

---

### 과제 2-3. HTTP/2, HTTP/3 비교 (난이도 ★★★)

**할 일**
1. Spring Boot에서 HTTP/2 활성화 (`server.http2.enabled=true`)
2. `curl --http1.1` vs `--http2` 응답 시간 비교 (이미지 100개 동시 요청)
3. **HOL Blocking** 차이 — HTTP/1.1 파이프라이닝의 한계
4. HTTP/2 멀티플렉싱 / 서버 푸시 / 헤더 압축(HPACK) 개념
5. HTTP/3 = QUIC over UDP — 왜 UDP인지

---

### 과제 2-4. HTTPS / TLS 깊이 (난이도 ★★★)

**할 일**
1. 자체 서명 인증서로 trader-bot HTTPS 적용
   ```bash
   keytool -genkeypair -alias trader -keyalg RSA -keysize 2048 -keystore trader.p12
   ```
2. `openssl s_client -connect localhost:8443 -showcerts` 로 인증서 체인 확인
3. **TLS 1.2 vs 1.3 handshake 라운드 트립 차이** 캡처로 확인
4. Mutual TLS (mTLS) — 클라이언트 인증서 검증까지 구현
5. Let's Encrypt로 진짜 인증서 발급 (도메인 있을 때)

---

## Week 3: API 설계 & 보안

### 과제 3-1. REST API 성숙도 (난이도 ★★)

**목표:** Richardson Maturity Model 4단계

**할 일**
trader-bot API를 4단계 모두 적용해보기

| Level | 설명 | trader-bot 적용 |
|---|---|---|
| 0 | RPC 스타일 | `POST /api` body로 모든 액션 |
| 1 | 리소스 분리 | `/api/orders`, `/api/users` |
| 2 | HTTP 메서드 활용 | GET/POST/PUT/PATCH/DELETE 적절히 |
| 3 | HATEOAS | 응답에 다음 액션 링크 포함 |

**상태 코드 가이드**
- 200 / 201 / 204 차이
- 400 (클라이언트 잘못) / 401 (인증) / 403 (권한) / 404 / 409 (충돌) / 422 (검증)
- 500 / 502 / 503 / 504

---

### 과제 3-2. API 멱등성 (난이도 ★★★)

**목표:** 외부 결제/주문 API의 핵심

**시나리오:** 사용자가 주문 버튼 더블 클릭 → 두 번 주문되면 안 됨

**할 일**
1. `Idempotency-Key` 헤더 받는 인터셉터 구현
2. Redis에 키 저장 (TTL 24시간) — 처음이면 처리, 이후 같은 키는 캐시된 응답 반환
3. **PUT vs POST**의 멱등성 비교
4. 외부 거래소 API 호출 시 멱등성 키 전달

---

### 과제 3-3. 페이지네이션 3가지 (난이도 ★★)

**할 일**
1. **Offset 페이지네이션** — `?page=1&size=20` (구현 쉬움, 깊은 페이지 느림)
2. **Cursor 페이지네이션** — `?cursor=<encoded>` (성능 좋음, 점프 불가)
3. **Keyset 페이지네이션** — `WHERE id > last_id LIMIT 20`
4. 100만 건 데이터에서 1000페이지 조회 — 셋 다 응답시간 비교

---

### 과제 3-4. CORS, CSRF, XSS (난이도 ★★)

**할 일**
1. **CORS**
   - frontend(Next.js)에서 backend 호출 시 CORS 에러 재현
   - `@CrossOrigin` vs `WebMvcConfigurer.addCorsMappings` 차이
   - Preflight 요청(OPTIONS) 캡처
   - `Access-Control-Allow-Credentials: true`의 함정
2. **CSRF**
   - Spring Security CSRF 토큰 동작 확인
   - SPA + JWT 환경에서 CSRF 보호 전략
3. **XSS**
   - 거래 메모 필드에 `<script>` 입력 시도
   - `Content-Security-Policy` 헤더 적용

---

## Week 4: 다른 통신 방식 & 운영

### 과제 4-1. WebSocket / SSE 적용 (난이도 ★★★)

**목표:** 실시간 시세 push

**할 일**
1. **WebSocket** — Spring WebSocket으로 시세 push 구현 (STOMP)
2. **SSE (Server-Sent Events)** — 같은 기능을 SSE로 다시 구현
3. **Long Polling** — 같은 기능을 폴링으로
4. 셋 다 비교 — 언제 무엇을 쓸지

**참고:** trader-bot에 WebFlux가 이미 있으므로 `Flux<Quote>` 반환하는 SSE 엔드포인트가 자연스러움

---

### 과제 4-2. gRPC 맛보기 (난이도 ★★)

**할 일**
1. trader-bot 내부 모듈 간 통신 하나를 gRPC로 변경
2. `.proto` 파일 작성, 코드 생성
3. REST vs gRPC — 같은 데이터 100KB 전송 시 크기와 속도 비교
4. **Streaming RPC** — 시세 스트림에 적합

---

### 과제 4-3. RestClient / WebClient 마스터 (난이도 ★★)

**할 일**
1. Spring 6.1+ `RestClient`로 외부 거래소 API 호출 클라이언트 작성
2. **WebClient**도 동일 기능 구현 (비동기)
3. **Resilience 적용**
   - Timeout (Connection / Read)
   - Retry (with exponential backoff)
   - Circuit Breaker (Resilience4j)
4. RestTemplate은 왜 deprecated인가

---

### 과제 4-4. API 문서화 & 버저닝 (난이도 ★★)

**할 일**
1. **SpringDoc OpenAPI** 풀 활용
   - `@Operation`, `@Schema`, 그룹화
   - 예외 응답 모두 문서화
   - JWT 인증 Swagger UI에서 동작하게
2. **버저닝 전략 3가지** 비교
   - URL: `/api/v1/orders`, `/api/v2/orders`
   - Header: `Accept: application/vnd.app.v2+json`
   - Query: `?version=2`
3. **하위 호환성 깨지 않는 변경** 가이드 만들기

---

## 종합 과제

### "외부 거래소 통합 게이트웨이" 구축

trader-bot에 외부 거래소(KIS 등) API를 안정적으로 호출하는 게이트웨이 모듈

**요구사항**
- [x] HTTPS + mTLS로 외부 호출
- [x] WebClient 비동기 호출
- [x] Circuit Breaker (3회 실패 시 30초 차단)
- [x] Retry (지수 백오프, 최대 3회)
- [x] Timeout (Connection 3s, Read 10s)
- [x] Idempotency-Key 자동 생성/전달
- [x] 응답 캐싱 (Redis, TTL 5s, 시세 데이터만)
- [x] WebSocket으로 클라이언트에 실시간 push
- [x] 메트릭 노출 (호출 횟수, 실패율, p99 응답시간)
- [x] OpenAPI 문서 완비

**부하 테스트**
- k6로 초당 1000 요청 → p99 100ms 이하 유지
- 외부 API가 일부러 죽었을 때 Circuit Breaker 작동 확인

---

## 추천 학습 자료

| 주제 | 자료 |
|---|---|
| 네트워크 기초 | "그림으로 배우는 HTTP & Network Basic" — 입문용 |
| HTTP | "HTTP 완벽 가이드" (David Gourley) — 두꺼움, 사전식 |
| TCP/IP | "TCP/IP 완벽 가이드" 또는 "Computer Networking: A Top-Down Approach" |
| REST | "REST API Design Rulebook" |
| 보안 | OWASP Top 10 + "웹 해킹 & 보안 완벽 가이드" |

---

## 진도 체크

- [ ] Week 1: 네트워크 기초
- [ ] Week 2: HTTP 깊이
- [ ] Week 3: API 설계 & 보안
- [ ] Week 4: 다른 통신 방식 & 운영
- [ ] 종합 과제: 외부 거래소 게이트웨이
