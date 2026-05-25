# 3단계: 네트워크 & HTTP — 하루 단위 커리큘럼

> **기간:** 4주 (Week 13–16)
> **선수 과정:** [1단계](./LEARNING_STAGE1.md), [2단계](./LEARNING_STAGE2.md) 완료
> **목표:** "이 요청이 서버에 도달하기까지 무슨 일이 일어나는지" 설명할 수 있는 수준
> **하루:** 평일 2~3h / 토 5~6h / 일 2~3h (오후 휴식)
> **코딩 장소:** java-lab (실험) / trader-bot (적용)
> **매주 필수:** PR 1개 + 테스트 + 측정 + 블로그 1편

> **학습 원칙:**
> 1. **패킷 캡처를 직접 해본다** — Wireshark / `tcpdump`로 보기
> 2. **trader-bot으로 측정** — 외부 거래소 API 호출이 좋은 실험 대상
> 3. **보안은 항상 함께** — TLS, CORS, CSRF는 네트워크 학습과 분리 불가

---

# Week 13 — 네트워크 기초

---

## Day 85 (월) — OSI 7계층 & TCP/IP 4계층

**이해 (2h)**
- OSI 7계층 vs TCP/IP 4계층 매핑 그려보기
  - Application (HTTP, DNS, FTP)
  - Transport (TCP, UDP)
  - Internet/Network (IP, ICMP)
  - Link/Data Link (Ethernet, Wi-Fi)
- 각 계층이 하는 일과 PDU(Protocol Data Unit)
  - Application → Message/Data
  - Transport → Segment (TCP) / Datagram (UDP)
  - Network → Packet
  - Link → Frame
- **왜 질문:**
  - 왜 계층을 나누는가? (관심사 분리 — 각 계층은 아래 계층의 구현을 모름)
  - HTTP 요청 하나가 실제로 어떤 과정을 거쳐서 서버에 도달하나?
  - IP 주소와 MAC 주소는 왜 둘 다 필요한가? (논리 주소 vs 물리 주소)
  - 포트 번호는 왜 필요한가? (같은 IP에서 여러 프로세스 구분)
- TCP 헤더 구조 그려보기
  - Source Port, Dest Port, Sequence Number, Ack Number, Flags (SYN/ACK/FIN)

---

## Day 86 (화) — TCP 3-way / 4-way Handshake 캡처

**코드 (2.5h)**
- [ ] Wireshark 설치 (또는 `tcpdump` 사용)
- [ ] **3-way Handshake 캡처**
  - `curl https://example.com` 실행하면서 캡처
  - SYN → SYN-ACK → ACK 흐름 직접 확인
  - 각 패킷의 Sequence Number, Ack Number 추적
- [ ] **4-way Handshake (연결 종료) 캡처**
  - FIN → ACK → FIN → ACK
  - TIME_WAIT 상태 확인: `netstat -an | grep TIME_WAIT`
- [ ] `tcpdump -i lo0 -nn port 8080`으로 trader-bot 호출 캡처
- **왜 질문:**
  - 3-way Handshake가 왜 3번인가? 2번이면 안 되나? (양쪽 ISN 교환 + 확인)
  - SYN Flood 공격이란? 어떻게 방어하나? (SYN Cookie)
  - TIME_WAIT은 왜 존재하는가? (지연 패킷 수신 대기 — 보통 2MSL = 60초)
  - **왜 질문:** TIME_WAIT이 많으면 왜 문제인가? (포트 고갈 — 서버 쪽 이슈)
  - Keep-Alive는 이 문제를 어떻게 완화하는가?

---

## Day 87 (수) — TCP vs UDP 실험

**코드 (2.5h) — java-lab**
- [ ] Java NIO로 TCP 에코 서버 작성
  ```java
  ServerSocketChannel server = ServerSocketChannel.open();
  server.bind(new InetSocketAddress(9090));
  ```
- [ ] Java NIO로 UDP 에코 서버 작성
  ```java
  DatagramChannel channel = DatagramChannel.open();
  channel.bind(new InetSocketAddress(9091));
  ```
- [ ] 클라이언트에서 1000개 메시지 전송 + 처리량 비교
- [ ] **네트워크 손실 시뮬레이션** (Network Link Conditioner 또는 `tc`)
  - 패킷 10% 드롭 설정
  - TCP: 재전송으로 모든 메시지 도달 (느려짐)
  - UDP: 일부 메시지 유실
- **왜 질문:**
  - TCP가 신뢰성을 보장하는 메커니즘은? (Seq/Ack, 재전송, 순서 보장)
  - UDP가 TCP보다 빠를 수 있는 이유는? (핸드셰이크 없음, HOL blocking 없음)
  - 게임/영상 통화에서 UDP를 쓰는 이유는? (약간의 손실 < 지연)
  - HTTP/3가 UDP(QUIC)를 선택한 이유는?

---

## Day 88 (목) — TCP 흐름 제어 & 혼잡 제어

**이해 + 코드 (2.5h)**
- [ ] **흐름 제어 (Flow Control)**
  - Sliding Window 메커니즘 그려보기
  - 수신자가 Window Size로 "더 보내도 돼" 알려줌
  - **왜 질문:** 흐름 제어가 없으면 어떤 문제? (수신 버퍼 넘침 → 패킷 드롭)
- [ ] **혼잡 제어 (Congestion Control)**
  - Slow Start → Congestion Avoidance → Fast Recovery
  - cwnd (Congestion Window) 개념
  - **왜 질문:** 왜 처음에 천천히 보내다 점점 빠르게? (네트워크 상태 모르니까 탐색)
  - **왜 질문:** 패킷 로스가 발생하면 cwnd를 왜 줄이나? (혼잡 신호)
- [ ] Wireshark에서 Window Size 변화 관찰
- [ ] `ss -ti`로 현재 TCP 연결의 cwnd, rtt 확인
- **왜 질문:**
  - TCP Nagle 알고리즘이란? 왜 존재하나? 왜 끄기도 하나? (`TCP_NODELAY`)
  - **왜 질문:** HTTP API 서버에서 Nagle을 끄는 이유는? (작은 패킷 즉시 전송 → 낮은 지연)

---

## Day 89 (금) — DNS & 로드 밸런서

**이해 + 코드 (2.5h)**
- [ ] DNS 동작 과정 (재귀적 질의)
  - 브라우저 캐시 → OS 캐시 → Local DNS → Root → TLD → Authoritative
  - `dig google.com +trace`로 전체 과정 추적
- [ ] DNS 레코드 타입
  - A (IPv4), AAAA (IPv6), CNAME (별칭), MX (메일), TXT (검증), NS (네임서버)
  - `nslookup`, `dig` 명령어 실습
- [ ] **DNS 라운드 로빈** — 같은 도메인에 IP 여러 개 → 분산
- **왜 질문:**
  - DNS TTL이 뭔가? 짧으면/길면 각각 어떤 장단점?
  - DNS 라운드 로빈의 한계는? (헬스체크 없음, 세션 유지 안 됨)
  - DNS Prefetch(`dns-prefetch`)란? 프론트엔드에서 왜 쓰나?
- [ ] L4 vs L7 로드 밸런서 차이 정리
  - L4: TCP 레벨 (포트 기반, 빠름)
  - L7: HTTP 레벨 (URL/헤더 기반, 똑똑함)
  - **왜 질문:** AWS ALB는 L7, NLB는 L4. trader-bot에는 어느 게 맞나? 왜?
- [ ] (선택) Nginx로 L7 로드밸런서 설정
  ```nginx
  upstream backend {
    server localhost:8080;
    server localhost:8081;
  }
  ```

---

## Day 90 (토) — 소켓 프로그래밍 + TLS Handshake

**코드 + 이해 (5h)**

오전 (3h) — 소켓 프로그래밍
- [ ] Java로 HTTP 서버 직접 구현 (80줄)
  - `ServerSocket`으로 연결 수락
  - InputStream에서 HTTP 요청 파싱 (메서드, 경로, 헤더)
  - OutputStream으로 HTTP 응답 작성
  - `Content-Length` 헤더 직접 계산
- [ ] 멀티 스레드 처리 추가 — 동시 요청 처리
- **왜 질문:**
  - 소켓이란 정확히 뭔가? (OS 커널의 통신 엔드포인트 — fd)
  - HTTP 서버가 결국 하는 일은? (소켓 열고, 텍스트 읽고, 텍스트 쓰기)
  - Spring의 `DispatcherServlet`은 이 위에 뭘 더하는가?

오후 (2h) — TLS Handshake 캡처
- [ ] **TLS Handshake 과정** 이해
  - Client Hello (지원 cipher suite 목록)
  - Server Hello (선택된 cipher suite + 인증서)
  - Key Exchange (비대칭키로 대칭키 교환)
  - Finished (대칭키로 암호화 시작)
- [ ] `openssl s_client -connect google.com:443 -showcerts` 실행
- [ ] Wireshark에서 TLS 핸드셰이크 캡처
- **왜 질문:**
  - 왜 비대칭키(RSA/ECDH)로 직접 통신 안 하고 대칭키를 교환하나? (비대칭 = 느림)
  - TLS 1.2 vs 1.3 핸드셰이크 라운드 트립 차이는? (1.3은 1-RTT)
  - 인증서 체인이란? Root CA → Intermediate CA → 서버 인증서
  - **왜 질문:** 자체 서명 인증서가 왜 "안전하지 않다"고 뜨는가?

---

## Day 91 (일) — Week 13 정리 + 블로그

**오전 (2.5h)**
- [ ] TCP/IP 전체 흐름 다이어그램 최종 정리
- [ ] 3-way Handshake + TLS Handshake 패킷 캡처 스크린샷 정리
- [ ] **블로그 작성:** "HTTP 요청 하나가 서버에 닿기까지 — 패킷 캡처로 직접 확인"
- [ ] 다음 주 예습: HTTP/1.1 vs HTTP/2 RFC 훑기
- 오후: 휴식

**Week 13 PR:** TCP/UDP 에코 서버 + 간이 HTTP 서버 + 패킷 캡처 분석 노트

---

# Week 14 — HTTP 깊이 파기

---

## Day 92 (월) — HTTP 메시지 구조

**이해 + 코드 (2.5h)**
- [ ] HTTP 요청/응답 메시지 구조 직접 작성
  ```
  GET /api/orders HTTP/1.1\r\n
  Host: localhost:8080\r\n
  Authorization: Bearer xxx\r\n
  Accept: application/json\r\n
  \r\n
  ```
- [ ] `telnet` 또는 `nc`(netcat)로 raw HTTP 요청 보내기
  ```bash
  echo -e "GET /api/health HTTP/1.1\r\nHost: localhost:8080\r\n\r\n" | nc localhost 8080
  ```
- [ ] 응답 헤더 직접 파싱
- **왜 질문:**
  - HTTP는 왜 "텍스트 기반 프로토콜"이라 하는가? (바이너리가 아님)
  - `\r\n`(CRLF)이 왜 줄 구분자인가? (RFC 2616 규약)
  - `Content-Length`가 없으면 바디 끝을 어떻게 아는가? (`Transfer-Encoding: chunked`)
  - **왜 질문:** chunked 인코딩은 왜 필요한가? (전체 크기 모를 때 — 스트리밍)
- [ ] `Transfer-Encoding: chunked` 응답 보내는 엔드포인트 만들기
- [ ] Keep-Alive vs Connection: close 동작 차이 확인
  - **왜 질문:** HTTP/1.0은 기본 close, HTTP/1.1은 기본 Keep-Alive. 왜 바꿨나?

---

## Day 93 (화) — HTTP 메서드 & 상태 코드 심화

**이해 + 코드 (2.5h)**
- [ ] HTTP 메서드 의미론 정리
  - **안전(Safe):** GET, HEAD — 서버 상태 변경 없음
  - **멱등(Idempotent):** GET, PUT, DELETE — 여러 번 해도 결과 동일
  - **비멱등:** POST, PATCH
  - **왜 질문:** PUT은 멱등인데 PATCH는 왜 멱등이 아닌가? (부분 업데이트의 의미론)
  - **왜 질문:** DELETE가 멱등이라면, 이미 삭제된 걸 다시 삭제하면? (404 vs 204 논쟁)
- [ ] 상태 코드 깊이 이해
  - 2xx: 200(OK), 201(Created), 204(No Content)
  - 3xx: 301(Permanent Redirect), 302(Found), 304(Not Modified)
  - 4xx: 400, 401(인증), 403(인가), 404, 409(Conflict), 422(Validation)
  - 5xx: 500, 502(Bad Gateway), 503(Service Unavailable), 504(Gateway Timeout)
- **왜 질문:**
  - 401 vs 403 차이는? (인증 안 됨 vs 인증됐지만 권한 없음)
  - 502 vs 504 차이는? (프록시가 백엔드 응답 못 받음 vs 타임아웃)
  - 422 vs 400 차이는? (문법은 맞지만 의미 틀림 vs 문법 자체 에러)
- [ ] trader-bot API 상태 코드 점검 — 적절한 코드 사용하고 있는지

---

## Day 94 (수) — HTTP 캐시 헤더

**코드 (2.5h) — trader-bot**
- [ ] 시세 조회 API에 캐시 헤더 적용
  - `Cache-Control: max-age=5, public`
  - 5초 내 재요청 → 브라우저/프록시가 캐시 사용
- [ ] **ETag + Conditional Request**
  - 응답에 `ETag: "abc123"` 포함
  - 재요청 시 `If-None-Match: "abc123"` → 변경 없으면 304
  - **왜 질문:** ETag는 어떻게 생성하나? (해시, 버전, 타임스탬프)
- [ ] **Last-Modified + If-Modified-Since**
  - 변경 없으면 304 응답 (바디 없음 → 대역폭 절약)
- [ ] **Vary 헤더**
  - `Vary: Accept-Encoding` — 같은 URL이라도 gzip/br 별 다른 캐시
  - **왜 질문:** Vary를 안 쓰면 어떤 문제? (gzip 응답을 비압축 클라이언트에 전달)
- **왜 질문:**
  - `no-cache` vs `no-store` 차이는? (no-cache: 캐시하되 재검증 / no-store: 아예 저장 안 함)
  - `private` vs `public` 차이는? (CDN/프록시 캐시 허용 여부)
  - **왜 질문:** API 응답을 CDN에 캐시하면 좋은 점/위험한 점은?

---

## Day 95 (목) — HTTP/1.1 vs HTTP/2 vs HTTP/3

**이해 + 코드 (2.5h)**
- [ ] HTTP/1.1의 한계
  - **HOL(Head-of-Line) Blocking**: 한 요청 느리면 뒤에 다 대기
  - 파이프라이닝은 있지만 실제로 안 씀 (구현 복잡)
  - 해결 시도: 도메인 샤딩, 스프라이트, 번들링
- [ ] HTTP/2 핵심 기능
  - **멀티플렉싱**: 하나의 TCP 연결에 여러 스트림 → HOL 해결
  - **헤더 압축(HPACK)**: 중복 헤더 제거
  - **서버 푸시**: 클라이언트 요청 전에 리소스 보내기
  - **바이너리 프로토콜**: 텍스트 → 바이너리 프레임
- [ ] HTTP/3 (QUIC)
  - UDP 기반 — TCP의 HOL blocking 완전 제거
  - 0-RTT 연결 재개
  - **왜 질문:** TCP HOL blocking이 HTTP/2에서도 문제인 이유는? (패킷 로스 시 모든 스트림 멈춤)
  - **왜 질문:** QUIC이 UDP를 쓰는 진짜 이유는? (커널 변경 없이 배포 가능)
- [ ] Spring Boot에서 HTTP/2 활성화
  ```properties
  server.http2.enabled=true
  ```
- [ ] `curl --http1.1` vs `--http2` 비교
- **왜 질문:**
  - HTTP/2가 있는데 왜 HTTP/3를 만들었나?
  - 대부분의 API 서버에서 HTTP/2의 체감 차이는 크지 않다. 왜? (API는 요청 수가 적으니까)

---

## Day 96 (금) — HTTPS 적용 실습

**코드 (2.5h) — trader-bot**
- [ ] 자체 서명 인증서 생성
  ```bash
  keytool -genkeypair -alias trader -keyalg RSA -keysize 2048 \
    -storetype PKCS12 -keystore trader.p12 -validity 365
  ```
- [ ] Spring Boot HTTPS 설정
  ```properties
  server.ssl.key-store=classpath:trader.p12
  server.ssl.key-store-password=changeit
  server.port=8443
  ```
- [ ] HTTP → HTTPS 리다이렉트 설정
- [ ] `openssl s_client -connect localhost:8443` 로 인증서 확인
- **왜 질문:**
  - PKCS12 vs JKS 차이는? (PKCS12가 표준, JKS는 Java 전용)
  - 운영에서는 자체 서명 대신 뭘 쓰나? (Let's Encrypt, 회사 내부 CA)
  - Spring Boot에서 인증서 갱신 시 재시작 필요한가? 무중단 갱신은?
  - **왜 질문:** mTLS(Mutual TLS)란? 서버가 클라이언트 인증서도 검증
- [ ] (선택) mTLS 구현 — 클라이언트 인증서 검증까지

---

## Day 97 (토) — REST API 설계 성숙도

**이해 + 코드 (5h)**

오전 (3h) — Richardson Maturity Model
- [ ] 4단계 모두 이해 + trader-bot에 적용

| Level | 설명 | 예시 |
|---|---|---|
| 0 | RPC 스타일 | `POST /api` body로 모든 액션 |
| 1 | 리소스 분리 | `/api/orders`, `/api/users` |
| 2 | HTTP 메서드 활용 | GET/POST/PUT/DELETE 적절히 |
| 3 | HATEOAS | 응답에 다음 액션 링크 포함 |

- [ ] trader-bot API Level 2 검증
  - 리소스별 URL 분리
  - 적절한 HTTP 메서드
  - 적절한 상태 코드
- [ ] HATEOAS 맛보기
  ```json
  {
    "orderId": 123,
    "_links": {
      "self": { "href": "/api/orders/123" },
      "cancel": { "href": "/api/orders/123/cancel" }
    }
  }
  ```
  - **왜 질문:** HATEOAS를 실제로 쓰는 곳이 있나? (거의 없음 — 왜 안 쓰나?)

오후 (2h) — API 설계 Best Practices
- [ ] URL 설계 규칙 정리
  - 복수형: `/api/orders` (O) vs `/api/order` (X)
  - 중첩: `/api/users/{userId}/orders`
  - 필터: `/api/orders?status=OPEN&sort=created_at,desc`
- [ ] 에러 응답 표준화 (RFC 7807 Problem Details)
  ```json
  {
    "type": "/errors/insufficient-balance",
    "title": "Insufficient Balance",
    "status": 400,
    "detail": "잔고가 부족합니다. 현재: 10000, 필요: 50000",
    "traceId": "abc-123"
  }
  ```
- **왜 질문:**
  - REST는 아키텍처 스타일이지 프로토콜이 아니다. 무슨 뜻인가?
  - GraphQL이 REST 대신 쓰이는 경우는? (여러 리소스 한 번에, 클라이언트가 필드 선택)

---

## Day 98 (일) — Week 14 정리 + 블로그

**오전 (2.5h)**
- [ ] HTTP/1.1 vs 2 vs 3 비교표 최종 정리
- [ ] 캐시 헤더 결정 플로우차트 작성
- [ ] **블로그 작성:** "HTTP 캐시 전략 — ETag, Cache-Control, 304를 실제로 적용해보기"
- [ ] 다음 주 예습: CORS, CSRF 공식 문서 훑기
- 오후: 휴식

**Week 14 PR:** HTTP 캐시 헤더 + HTTPS 적용 + REST API 정비

---

# Week 15 — API 보안 & 멱등성

---

## Day 99 (월) — CORS 깊이 이해

**이해 + 코드 (2.5h)**
- [ ] CORS(Cross-Origin Resource Sharing) 동작 이해
  - Same-Origin Policy란? (프로토콜 + 호스트 + 포트 동일)
  - **왜 질문:** 브라우저가 왜 Same-Origin Policy를 강제하나? (보안 — 악성 사이트가 타 사이트 데이터 접근 방지)
- [ ] **Preflight 요청 (OPTIONS)**
  - 언제 발생하나? (simple request가 아닐 때 — custom header, PUT/DELETE 등)
  - `Access-Control-Allow-Origin`, `Access-Control-Allow-Methods`, `Access-Control-Allow-Headers`
  - Preflight 캐싱: `Access-Control-Max-Age`
- [ ] CORS 에러 재현
  - 다른 포트에서 fetch 호출 → 에러 확인
  - Spring 설정으로 해결
- [ ] Spring CORS 설정 2가지
  - `@CrossOrigin` (컨트롤러 단위)
  - `WebMvcConfigurer.addCorsMappings()` (글로벌)
- **왜 질문:**
  - `Access-Control-Allow-Credentials: true`의 함정은? (Allow-Origin을 `*`로 못 씀)
  - CORS는 서버가 아니라 **브라우저**가 강제한다. Postman에서는 왜 안 막히나?
  - **왜 질문:** CORS preflight가 성능에 미치는 영향은? 어떻게 줄이나?

---

## Day 100 (화) — CSRF & XSS 방어

**이해 + 코드 (2.5h)**
- [ ] **CSRF (Cross-Site Request Forgery)**
  - 공격 시나리오 그려보기: 악성 사이트가 로그인된 사용자의 브라우저로 요청
  - Spring Security CSRF 토큰 동작 확인
  - **왜 질문:** JWT + SPA 환경에서는 CSRF가 왜 덜 위험한가? (토큰이 쿠키가 아니라 헤더이므로)
  - **왜 질문:** 그래도 Refresh Token을 httpOnly 쿠키에 넣으면? → CSRF 다시 위험
- [ ] **XSS (Cross-Site Scripting)**
  - 거래 메모 필드에 `<script>alert('xss')</script>` 입력 시도
  - Stored XSS vs Reflected XSS vs DOM-based XSS
  - 방어: 입력 이스케이핑, `Content-Security-Policy` 헤더
  - **왜 질문:** CSP 헤더가 XSS를 어떻게 막는가? (허용된 소스에서만 스크립트 실행)
- [ ] **보안 헤더 적용**
  ```
  X-Content-Type-Options: nosniff
  X-Frame-Options: DENY
  Strict-Transport-Security: max-age=31536000
  Content-Security-Policy: default-src 'self'
  ```
- **왜 질문:**
  - HSTS는 뭔가? 왜 중요한가? (HTTP→HTTPS 강제, 중간자 공격 방지)
  - SQL Injection은 JPA가 어떻게 막아주는가? (PreparedStatement — 파라미터 바인딩)

---

## Day 101 (수) — API 멱등성 구현

**코드 (2.5h) — trader-bot**
- [ ] **멱등성이 왜 중요한가?**
  - 시나리오: 네트워크 불안정 → 클라이언트 재시도 → 주문 중복 실행
  - 시나리오: 사용자가 버튼 더블 클릭
- [ ] `Idempotency-Key` 헤더 기반 구현
  - 인터셉터에서 `Idempotency-Key` 헤더 추출
  - Redis에 키 저장 (TTL 24시간)
  - 처음이면 정상 처리 + 응답 캐시
  - 같은 키 재요청 → 캐시된 응답 반환
  ```java
  @Component
  public class IdempotencyInterceptor implements HandlerInterceptor {
      // Redis에서 key 확인 → 이미 있으면 캐시 응답 반환
  }
  ```
- [ ] 테스트: 같은 Idempotency-Key로 5번 요청 → 1번만 처리되는지
- **왜 질문:**
  - PUT vs POST의 멱등성 차이는? (PUT: 동일 요청 여러 번 = 같은 결과)
  - Stripe, 토스 등 결제 API가 멱등성 키를 필수로 하는 이유는?
  - **왜 질문:** 멱등성 키의 TTL을 어떻게 결정하나? (재시도 기간 + 여유)
  - 동시에 같은 키로 2개 요청이 오면? (분산 락으로 한쪽만 처리)

---

## Day 102 (목) — 페이지네이션 3가지

**코드 (2.5h) — trader-bot**
- [ ] **Offset 페이지네이션**
  ```
  GET /api/orders?page=0&size=20
  SELECT * FROM orders ORDER BY id LIMIT 20 OFFSET 0;
  ```
  - 100만 건에서 page=50000 요청 → 응답 시간 측정
  - **왜 질문:** 깊은 페이지에서 왜 느린가? (OFFSET만큼 스캔 후 버림)
- [ ] **Cursor 페이지네이션**
  ```
  GET /api/orders?cursor=eyJpZCI6MTAwfQ==&size=20
  SELECT * FROM orders WHERE id > 100 ORDER BY id LIMIT 20;
  ```
  - 같은 깊은 페이지 → 응답 시간 비교 (훨씬 빠름)
  - **왜 질문:** Cursor 방식이 빠른 이유는? (인덱스 탐색만 — OFFSET 스캔 없음)
  - **왜 질문:** Cursor의 단점은? (특정 페이지로 점프 불가)
- [ ] **Keyset 페이지네이션**
  ```
  SELECT * FROM orders WHERE (created_at, id) < (?, ?) ORDER BY created_at DESC, id DESC LIMIT 20;
  ```
  - 정렬 기준이 여러 컬럼일 때
- [ ] 100만 건에서 세 방식 응답 시간 비교표 작성
- **왜 질문:**
  - 무한 스크롤에는 어느 방식이 적합한가? (Cursor)
  - 페이지 번호가 필요한 UI에는? (Offset — 성능 타협)
  - `total count` 쿼리도 느릴 수 있다. 해결법은? (캐시, 근사치)

---

## Day 103 (금) — Rate Limiting

**코드 (2.5h) — trader-bot**
- [ ] Rate Limiting이 왜 필요한가?
  - DDoS 방어, 공정 사용, 외부 API 호출 제한
- [ ] 알고리즘 3가지 이해
  - **Fixed Window**: 분당 100회 (단순하지만 경계 시점 폭주)
  - **Sliding Window Log**: 정확하지만 메모리 사용
  - **Token Bucket**: 평균 속도 제한 + 버스트 허용
- [ ] Redis + Token Bucket 구현
  ```java
  // Redis Lua 스크립트로 원자적 토큰 차감
  ```
- [ ] 응답 헤더 추가
  ```
  X-RateLimit-Limit: 100
  X-RateLimit-Remaining: 95
  X-RateLimit-Reset: 1622505600
  ```
- [ ] 429 Too Many Requests 응답 + `Retry-After` 헤더
- **왜 질문:**
  - Fixed Window의 "경계 시점" 문제란? (59초에 100회 + 0초에 100회 = 1초 동안 200회)
  - Sliding Window가 이걸 어떻게 해결하나?
  - **왜 질문:** API Gateway에서 Rate Limiting하는 게 애플리케이션에서 하는 것보다 나은 이유는?
  - trader-bot에서 외부 거래소 API 호출 제한은 어떻게 지키나?

---

## Day 104 (토) — WebSocket & SSE 실시간 통신

**코드 + 이해 (5h)**

오전 (3h) — WebSocket 구현
- [ ] WebSocket 프로토콜 이해
  - HTTP Upgrade 요청 → 101 Switching Protocols
  - Full-duplex 양방향 통신
  - **왜 질문:** WebSocket이 HTTP 위에서 시작하는 이유는? (방화벽/프록시 통과 위해)
- [ ] Spring WebSocket + STOMP로 시세 push 구현
  ```java
  @MessageMapping("/subscribe")
  @SendTo("/topic/quotes")
  public Quote handleSubscription(SubscribeMessage msg) { ... }
  ```
- [ ] 클라이언트에서 연결 + 메시지 수신 확인
- **왜 질문:**
  - STOMP이 왜 필요한가? (raw WebSocket에는 라우팅, 구독 개념 없음)
  - WebSocket 연결 수 제한은? (파일 디스크립터, 메모리)
  - 서버 여러 대일 때 WebSocket 세션 공유는? (Redis Pub/Sub, 메시지 브로커)

오후 (2h) — SSE + Long Polling 비교
- [ ] **SSE (Server-Sent Events)** 구현
  ```java
  @GetMapping(value = "/api/quotes/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<Quote> streamQuotes() { ... }
  ```
  - 단방향 서버→클라이언트
  - 자동 재연결 내장
- [ ] **Long Polling** — 같은 기능 폴링으로
  - 클라이언트가 요청 → 서버가 새 데이터 있을 때까지 hold → 응답 후 즉시 재요청
- [ ] 세 방식 비교표

| | WebSocket | SSE | Long Polling |
|---|---|---|---|
| 방향 | 양방향 | 서버→클라 | 서버→클라 |
| 프로토콜 | ws:// | HTTP | HTTP |
| 재연결 | 직접 구현 | 자동 | 직접 구현 |
| 적합 | 채팅, 게임 | 알림, 시세 | 레거시 호환 |

- **왜 질문:** trader-bot 시세 push에는 어느 게 가장 적합한가? 왜?

---

## Day 105 (일) — Week 15 정리 + 블로그

**오전 (2.5h)**
- [ ] CORS/CSRF/XSS 방어 체크리스트
- [ ] 멱등성 + Rate Limiting 구현 다이어그램
- [ ] **블로그 작성:** "API 멱등성 — Idempotency-Key로 중복 주문 방지하기"
- [ ] 또는: "WebSocket vs SSE vs Long Polling — 실시간 시세 push 비교"
- 오후: 휴식

**Week 15 PR:** CORS 설정 + 멱등성 + Rate Limiting + WebSocket 시세 push

---

# Week 16 — 고급 HTTP 클라이언트 & 운영

---

## Day 106 (월) — RestClient & WebClient

**코드 (2.5h) — trader-bot**
- [ ] Spring 6.1+ **RestClient** (동기)
  ```java
  RestClient client = RestClient.builder()
      .baseUrl("https://openapi.koreainvestment.com")
      .defaultHeader("appkey", apiKey)
      .build();

  Quote quote = client.get()
      .uri("/uapi/domestic-stock/v1/quotations/inquire-price?fid_input_iscd={code}", "005930")
      .retrieve()
      .body(Quote.class);
  ```
- [ ] **WebClient** (비동기/리액티브)
  ```java
  WebClient client = WebClient.builder()
      .baseUrl("https://openapi.koreainvestment.com")
      .build();

  Mono<Quote> quote = client.get()
      .uri("/...")
      .retrieve()
      .bodyToMono(Quote.class);
  ```
- [ ] 동기 vs 비동기 성능 비교
  - 10개 종목 시세 동시 조회 — RestClient(순차) vs WebClient(병렬)
- **왜 질문:**
  - RestTemplate은 왜 deprecated인가? (블로킹 + 유연성 부족)
  - WebClient가 비동기인데 MVC에서도 쓸 수 있나? (`.block()` — 하지만 Virtual Thread에서는?)
  - **왜 질문:** Virtual Thread + RestClient vs WebClient, 어느 조합이 나을까?

---

## Day 107 (화) — Resilience: Timeout + Retry + Circuit Breaker

**코드 (2.5h) — trader-bot**
- [ ] **Timeout 설정**
  - Connection Timeout: 3초 (TCP 연결까지)
  - Read Timeout: 10초 (응답 대기)
  - **왜 질문:** 타임아웃 없으면 어떤 일이 생기나? (스레드/커넥션 고갈 → 서버 전체 마비)
- [ ] **Retry (재시도)**
  - `@Retryable` 또는 Resilience4j Retry
  - Exponential Backoff: 1초 → 2초 → 4초
  - 최대 3회
  - **왜 질문:** 일정 간격 재시도가 왜 위험한가? (Thundering Herd — 동시 재시도 폭주)
  - **왜 질문:** Jitter를 추가하는 이유는? (재시도 시점 분산)
- [ ] **Circuit Breaker** (Resilience4j)
  ```java
  @CircuitBreaker(name = "kisApi", fallbackMethod = "fallback")
  public Quote getQuote(String symbol) { ... }
  ```
  - CLOSED → OPEN (5회 실패 시) → HALF_OPEN (30초 후) → CLOSED
  - **왜 질문:** Circuit Breaker가 없으면? (장애 서비스에 계속 요청 → 자원 낭비 + 연쇄 장애)
  - **왜 질문:** HALF_OPEN 상태의 의미는? (복구됐는지 소량으로 확인)
- [ ] 세 가지 조합 적용 → 외부 API 호출에 완전 방어

---

## Day 108 (수) — gRPC 맛보기

**이해 + 코드 (2.5h)**
- [ ] gRPC vs REST 차이
  - Protocol Buffers (바이너리 직렬화) vs JSON (텍스트)
  - HTTP/2 기반 vs HTTP/1.1
  - 강타입 계약 (.proto 파일) vs OpenAPI 문서
- [ ] `.proto` 파일 작성
  ```protobuf
  service QuoteService {
    rpc GetQuote (QuoteRequest) returns (QuoteResponse);
    rpc StreamQuotes (QuoteRequest) returns (stream QuoteResponse);
  }
  ```
- [ ] Spring Boot + gRPC 설정
- [ ] 같은 데이터 100KB 전송 — REST(JSON) vs gRPC(Protobuf) 크기/속도 비교
- **왜 질문:**
  - gRPC가 REST보다 빠른 이유는? (바이너리 + HTTP/2 멀티플렉싱)
  - Streaming RPC가 WebSocket 대안이 될 수 있는 이유는?
  - 내부 마이크로서비스 간 통신에 gRPC가 적합한 이유는?
  - **왜 질문:** 브라우저에서 gRPC를 직접 못 쓰는 이유는? (gRPC-Web이 필요)
  - trader-bot 내부 모듈 간 통신에 적용 가능한 곳은?

---

## Day 109 (목) — API 문서화 (OpenAPI / Swagger)

**코드 (2.5h) — trader-bot**
- [ ] SpringDoc OpenAPI 풀 활용
  - `@Operation(summary = "주문 생성", description = "...")`
  - `@Schema(description = "종목코드", example = "005930")`
  - `@ApiResponse(responseCode = "201", description = "주문 생성 성공")`
- [ ] 예외 응답 모두 문서화
  - 400, 401, 403, 404, 409, 500 각각 응답 구조
- [ ] JWT 인증 Swagger UI에서 동작
  ```java
  @SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
  )
  ```
- [ ] **API 버저닝 전략 3가지** 비교
  - URL: `/api/v1/orders`
  - Header: `Accept: application/vnd.trader.v2+json`
  - Query: `?version=2`
  - **왜 질문:** 어떤 전략이 가장 실용적인가? (URL 방식이 가장 명확하고 디버깅 쉬움)
- **왜 질문:**
  - API 문서가 코드와 분리되면 왜 위험한가? (문서 ↔ 실제 불일치)
  - SpringDoc이 코드에서 자동 생성하는 장점은?
  - **하위 호환성**을 깨지 않는 변경이란? (필드 추가 OK, 필드 삭제/타입 변경 NG)

---

## Day 110 (금) — 종합 게이트웨이 구현

**코드 (2.5h) — trader-bot**
- [ ] 외부 거래소 통합 게이트웨이 모듈 구현
  - HTTPS 호출
  - WebClient 비동기
  - Circuit Breaker (3회 실패 → 30초 차단)
  - Retry (지수 백오프, 최대 3회)
  - Timeout (Connection 3s, Read 10s)
  - Idempotency-Key 자동 생성/전달
  - 응답 캐싱 (Redis, TTL 5s, 시세만)
- [ ] 메트릭 노출
  - 호출 횟수, 실패율, p99 응답시간
  - Micrometer + Prometheus
- **왜 질문:**
  - 이 모든 것을 조합할 때 순서가 중요한가? (Retry > Circuit Breaker > Timeout)
  - Bulkhead 패턴은 왜 추가로 필요한가? (한 외부 API 장애가 다른 API 호출까지 영향 방지)

---

## Day 111 (토) — 부하 테스트 + 3단계 종합 정리

**코드 + 측정 (5h)**

오전 (3h) — 부하 테스트
- [ ] k6로 게이트웨이 부하 테스트
  - 초당 1000 요청 → p99 100ms 이하 확인
  - 외부 API가 3초 지연될 때 Circuit Breaker 동작 확인
  - 외부 API가 완전히 죽었을 때 fallback 응답 확인
- [ ] WebSocket 동시 연결 수 테스트
  - 100 → 500 → 1000 동시 연결
  - 메모리 사용량 모니터링
- [ ] Rate Limiting 동작 확인
  - 100req/min 초과 시 429 응답 + Retry-After

오후 (2h) — 3단계 종합 정리
- [ ] 3단계 전체 학습 내용 요약
  - TCP/UDP + TLS 동작 원리
  - HTTP 버전별 차이 + 캐시 전략
  - API 보안 (CORS, CSRF, XSS)
  - 실시간 통신 (WebSocket, SSE)
  - Resilience 패턴 (Timeout, Retry, Circuit Breaker)
- [ ] "왜" 질문 10개 셀프 답변
- [ ] 성능 측정 결과 최종 정리

---

## Day 112 (일) — 3단계 졸업 + 블로그

**오전 (2.5h)**
- [ ] **블로그 작성:** "외부 API 호출, 안전하게 — Circuit Breaker + Retry + Timeout 조합"
- [ ] 3단계 회고
  - 패킷 캡처로 느낀 "HTTP는 결국 텍스트"
  - 외부 API 장애 대비의 중요성
  - 보안 헤더를 왜 몰랐을까
- [ ] 4단계(인프라 & 컨테이너) 예습: Docker 공식 튜토리얼 훑기
- 오후: 휴식

**Week 16 PR:** RestClient/WebClient + Resilience4j + 게이트웨이 모듈 + 부하 테스트

---

## 3단계 완료 체크리스트

### PR 목록 (4개)
- [ ] W13: TCP/UDP 서버 + 소켓 HTTP 서버 + 패킷 캡처
- [ ] W14: HTTP 캐시 + HTTPS + REST API 정비
- [ ] W15: CORS + 멱등성 + Rate Limiting + WebSocket
- [ ] W16: Resilience + 게이트웨이 + 부하 테스트

### 블로그 (4편)
- [ ] W13: HTTP 요청이 서버에 닿기까지 (패킷 캡처)
- [ ] W14: HTTP 캐시 전략 실전 적용
- [ ] W15: API 멱등성 또는 실시간 통신 비교
- [ ] W16: 외부 API 호출 안전하게 (Resilience 패턴)

### "왜"에 답할 수 있어야 하는 것들 (면접 대비)
- [ ] TCP 3-way Handshake가 3번인 이유
- [ ] TLS Handshake 과정과 대칭키/비대칭키 역할
- [ ] HTTP/1.1 vs HTTP/2 vs HTTP/3 핵심 차이
- [ ] CORS가 브라우저에서 동작하는 원리
- [ ] CSRF 공격 시나리오와 방어법
- [ ] 멱등성의 정의와 구현 방법
- [ ] Cursor vs Offset 페이지네이션 성능 차이 원인
- [ ] WebSocket vs SSE 선택 기준
- [ ] Circuit Breaker 상태 전이와 필요성
- [ ] Rate Limiting 알고리즘 3가지 차이
