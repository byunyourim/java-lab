# 4단계: 인프라 & 컨테이너 — 하루 단위 커리큘럼

> **기간:** 5주 (Week 17–21)
> **선수 과정:** 1~3단계 완료
> **목표:** "이 앱을 컨테이너로 띄우고 클라우드에 배포하는 전 과정" 설명할 수 있는 수준
> **하루:** 평일 2~3h / 토 5~6h / 일 2~3h (오후 휴식)
> **코딩 장소:** java-lab (실험) / trader-bot (적용)
> **매주 필수:** PR 1개 + 테스트 + 측정 + 블로그 1편

> **학습 원칙:**
> 1. **로컬에서 컨테이너로** → **클라우드로** 점진적 이동
> 2. **비용 관리** — AWS Free Tier로 시작, 안 쓰면 끄기
> 3. **"왜 이게 필요한가"부터** — 컨테이너가 해결하는 문제부터 이해

---

# Week 17 — Linux 기본기

---

## Day 113 (월) — Linux 프로세스 & 시그널

**이해 + 코드 (2.5h)**
- [ ] 프로세스 개념 이해
  - PID, PPID, 프로세스 트리 (`pstree`)
  - 포그라운드 / 백그라운드 프로세스
  - `ps aux`, `top`, `htop` 사용
- [ ] 시그널(Signal) 이해
  - `SIGTERM` (15): 정상 종료 요청
  - `SIGKILL` (9): 강제 종료 (무시 불가)
  - `SIGHUP` (1): 터미널 끊김 / 설정 재로드
  - `SIGINT` (2): Ctrl+C
- [ ] Spring Boot Graceful Shutdown 실험
  - `server.shutdown=graceful` 설정
  - `kill -15 <pid>` → 진행 중 요청 대기 후 종료 확인
  - `kill -9 <pid>` → 즉시 죽음 (트랜잭션 중간에 끊김)
- **왜 질문:**
  - `kill -9`를 왜 최후의 수단으로만 써야 하나? (리소스 정리 기회 없음)
  - Docker `stop`은 어떤 시그널을 보내나? (SIGTERM → 10초 후 SIGKILL)
  - **왜 질문:** Java에서 ShutdownHook이란? Spring은 이걸 어떻게 활용하나?
  - zombie 프로세스란? 왜 생기나?

---

## Day 114 (화) — 파일 시스템 & 디스크

**이해 + 코드 (2.5h)**
- [ ] 자주 쓰는 명령어 (trader-bot 운영 시나리오)

| 시나리오 | 명령어 |
|---|---|
| 8080 포트 누가 점유? | `lsof -i :8080`, `netstat -tnlp` |
| 메모리 많이 쓰는 프로세스? | `ps aux --sort=-%mem \| head` |
| 로그에서 ERROR만? | `grep ERROR app.log` |
| 시간별 에러 추이? | `grep ERROR app.log \| awk '{print $1}' \| sort \| uniq -c` |
| 디스크 어디가 가득? | `du -sh * \| sort -rh \| head` |
| 파일 실시간 감시? | `tail -f app.log` |

- [ ] 파일 시스템 구조 이해
  - inode, 하드링크/심볼릭링크
  - `/proc` 가상 파일시스템 — PID별 정보
  - `/proc/<pid>/fd` — 열린 파일 디스크립터
- **왜 질문:**
  - 파일 디스크립터 제한(`ulimit -n`)이 왜 서버에서 중요한가? (소켓도 fd — 동시 연결 제한)
  - "Too many open files" 에러는 왜 발생하나?
  - 로그 파일이 커지면 왜 문제인가? (디스크 풀 → 앱 죽음)

---

## Day 115 (수) — 쉘 스크립트 작성

**코드 (2.5h)**
- [ ] trader-bot 운영 스크립트 5개 작성
  1. **start.sh** — Spring Boot 백그라운드 실행 + PID 파일 저장
     ```bash
     #!/bin/bash
     java -jar trader-bot.jar > app.log 2>&1 &
     echo $! > app.pid
     ```
  2. **stop.sh** — Graceful shutdown (SIGTERM → 30초 대기 → SIGKILL)
  3. **health-check.sh** — `/actuator/health` 호출, 실패 시 알림
  4. **backup.sh** — PostgreSQL pg_dump → 압축 → 날짜별 저장
  5. **log-rotate.sh** — 7일 이상 된 로그 압축 + 삭제
- [ ] `crontab`에 등록
  - 매 5분: health-check
  - 매일 새벽: backup + log-rotate
- **왜 질문:**
  - `2>&1`의 의미는? (stderr를 stdout으로 리다이렉트)
  - `nohup`이 왜 필요한가? (터미널 종료해도 프로세스 유지)
  - **왜 질문:** 이런 스크립트들을 나중에 뭘로 대체하나? (Docker, systemd, K8s)

---

## Day 116 (목) — 네트워크 & 방화벽

**이해 + 코드 (2.5h)**
- [ ] 네트워크 명령어 실습
  - `ip addr` / `ifconfig` — 네트워크 인터페이스
  - `ss -tnlp` — 열린 포트 확인
  - `netstat -an` — 연결 상태
  - `traceroute` — 경로 추적
  - `curl -v` — HTTP 상세 요청/응답
- [ ] 방화벽 기초 (`iptables` / `ufw`)
  - 특정 포트만 열기
  - 특정 IP 차단
  - **왜 질문:** 왜 기본적으로 모든 포트를 닫고 필요한 것만 여는가? (최소 권한 원칙)
- [ ] SSH 키 인증
  - `ssh-keygen` → 공개키/개인키 생성
  - `~/.ssh/authorized_keys`에 공개키 등록
  - **왜 질문:** 패스워드 인증 대신 키 인증을 쓰는 이유는? (brute force 방지)
- **왜 질문:**
  - 0.0.0.0 vs 127.0.0.1 차이는? (모든 인터페이스 vs 로컬만)
  - Docker에서 `-p 8080:8080`은 네트워크 레벨에서 뭘 하는 건가? (포트 포워딩/NAT)

---

## Day 117 (금) — systemd & 서비스 관리

**코드 (2.5h)**
- [ ] systemd 이해
  - Unit 파일 구조 (`/etc/systemd/system/`)
  - `systemctl start/stop/restart/status`
  - `journalctl -u <service>` — 로그 확인
- [ ] trader-bot을 systemd 서비스로 등록
  ```ini
  [Unit]
  Description=Trader Bot
  After=network.target postgresql.service

  [Service]
  User=trader
  ExecStart=/usr/bin/java -jar /opt/trader-bot/app.jar
  Restart=on-failure
  RestartSec=10

  [Install]
  WantedBy=multi-user.target
  ```
- [ ] 자동 재시작 테스트 — 프로세스 kill → 자동 복구 확인
- **왜 질문:**
  - `After=postgresql.service`는 왜 필요한가? (의존성 순서)
  - `Restart=on-failure` vs `always` 차이는?
  - **왜 질문:** systemd가 init.d를 대체한 이유는? (의존성 관리, 병렬 시작, 로그 통합)
  - Docker 환경에서는 systemd 대신 뭘 쓰나? (컨테이너 오케스트레이터가 관리)

---

## Day 118 (토) — Docker 기초: 이미지 & 컨테이너

**코드 + 이해 (5h)**

오전 (3h) — Docker 핵심 개념
- [ ] 컨테이너가 해결하는 문제
  - "내 컴퓨터에서는 되는데" → 환경 차이 제거
  - 의존성 격리
  - 일관된 배포 단위
- [ ] 이미지 vs 컨테이너
  - 이미지: 불변 템플릿 (클래스)
  - 컨테이너: 이미지의 실행 인스턴스 (객체)
- [ ] Docker 기본 명령어
  ```bash
  docker pull postgres:16-alpine
  docker run -d --name mydb -e POSTGRES_PASSWORD=secret -p 5432:5432 postgres:16-alpine
  docker ps / docker logs / docker exec -it mydb bash
  docker stop / docker rm
  ```
- **왜 질문:**
  - 컨테이너는 VM과 뭐가 다른가? (커널 공유 — 가벼움, 빠른 시작)
  - Docker 이미지의 레이어 구조란? (각 명령어 = 하나의 레이어, 공유 가능)
  - **왜 질문:** 컨테이너가 격리를 제공하는 Linux 기술은? (namespace + cgroups)
  - namespace: PID, NET, MNT, UTS, IPC 분리
  - cgroups: CPU, 메모리 제한

오후 (2h) — Dockerfile 작성
- [ ] trader-bot Dockerfile 작성 (단순 버전)
  ```dockerfile
  FROM eclipse-temurin:21-jre-alpine
  COPY build/libs/trader-bot.jar app.jar
  ENTRYPOINT ["java", "-jar", "/app.jar"]
  ```
- [ ] 빌드 + 실행 확인
  ```bash
  docker build -t trader-bot:v1 .
  docker run -d -p 8080:8080 trader-bot:v1
  ```
- [ ] 이미지 크기 확인 (`docker images`)
- **왜 질문:**
  - `alpine` 베이스를 쓰는 이유는? (이미지 크기 최소화)
  - `ENTRYPOINT` vs `CMD` 차이는?
  - `.dockerignore`가 왜 필요한가?

---

## Day 119 (일) — Week 17 정리 + 블로그

**오전 (2.5h)**
- [ ] Linux 운영 명령어 치트시트 최종 정리
- [ ] 쉘 스크립트 → Docker로 대체되는 과정 정리
- [ ] **블로그 작성:** "백엔드 개발자의 Linux 필수 명령어 — 운영 시나리오별 정리"
- [ ] 다음 주 예습: Docker 멀티 스테이지 빌드, Docker Compose 문서
- 오후: 휴식

**Week 17 PR:** 운영 스크립트 5개 + systemd 서비스 파일 + 기본 Dockerfile

---

# Week 18 — Docker 심화

---

## Day 120 (월) — Docker 이미지 최적화

**코드 (2.5h)**
- [ ] 이미지 크기 최적화 단계별 측정
  1. `openjdk:21` (700MB+)
  2. `eclipse-temurin:21-jre-alpine` (~200MB)
  3. `gcr.io/distroless/java21-debian12` (~130MB)
- [ ] **멀티 스테이지 빌드**
  ```dockerfile
  # Build stage
  FROM eclipse-temurin:21-jdk AS builder
  WORKDIR /app
  COPY . .
  RUN ./gradlew bootJar

  # Run stage
  FROM eclipse-temurin:21-jre-alpine
  COPY --from=builder /app/build/libs/*.jar app.jar
  ENTRYPOINT ["java", "-jar", "/app.jar"]
  ```
  - **왜 질문:** 멀티 스테이지가 왜 이미지를 줄이나? (빌드 도구가 최종 이미지에 안 들어감)
- [ ] **Layered Jar** 활용
  ```dockerfile
  FROM eclipse-temurin:21-jre AS builder
  COPY app.jar app.jar
  RUN java -Djarmode=layertools -jar app.jar extract

  FROM eclipse-temurin:21-jre-alpine
  COPY --from=builder dependencies/ ./
  COPY --from=builder spring-boot-loader/ ./
  COPY --from=builder snapshot-dependencies/ ./
  COPY --from=builder application/ ./
  ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
  ```
  - **왜 질문:** Layered Jar의 장점은? (코드만 바뀌면 마지막 레이어만 재빌드 → 빠른 빌드)
- [ ] 빌드 시간 비교: 의존성만 바뀌었을 때 vs 코드만 바뀌었을 때

---

## Day 121 (화) — Docker Compose 풀스택

**코드 (2.5h)**
- [ ] trader-bot 전체를 docker-compose로 구성
  ```yaml
  services:
    backend:
      build: .
      depends_on:
        postgres:
          condition: service_healthy
      environment:
        SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/trader
      ports: ["8080:8080"]

    postgres:
      image: postgres:16-alpine
      volumes: ["postgres_data:/var/lib/postgresql/data"]
      environment:
        POSTGRES_DB: trader
        POSTGRES_PASSWORD: ${DB_PASSWORD}
      healthcheck:
        test: ["CMD-SHELL", "pg_isready -U postgres"]
        interval: 5s

    redis:
      image: redis:7-alpine
      volumes: ["redis_data:/data"]

  volumes:
    postgres_data:
    redis_data:
  ```
- [ ] 한 줄로 전체 실행: `docker compose up -d`
- [ ] 비밀번호는 `.env` 파일로 분리
- **왜 질문:**
  - `depends_on`만으로 충분한가? (아님 — 앱이 시작해도 DB가 ready 아닐 수 있음 → healthcheck)
  - named volume vs bind mount 차이는? (named: Docker 관리 / bind: 호스트 경로 직접)
  - **왜 질문:** 볼륨 없이 컨테이너 재시작하면? (데이터 날아감!)
  - `docker compose down -v` 하면 볼륨도 삭제된다. 주의!

---

## Day 122 (수) — Docker 네트워크 & 보안

**이해 + 코드 (2.5h)**
- [ ] Docker 네트워크 3가지
  - **bridge** (기본): 컨테이너 간 통신, 호스트와 격리
  - **host**: 호스트 네트워크 직접 사용 (격리 없음)
  - **none**: 네트워크 없음
- [ ] 컨테이너 간 통신 실험
  - 같은 네트워크: 서비스명으로 DNS 해석 (`postgres:5432`)
  - 다른 네트워크: 통신 불가 확인
  - **왜 질문:** Docker Compose가 자동으로 만드는 네트워크는? (프로젝트명_default)
- [ ] Docker 보안 기초
  - root로 실행하지 않기: `USER nonroot`
  - 읽기 전용 파일시스템: `--read-only`
  - 리소스 제한: `--memory=512m --cpus=1`
  - **왜 질문:** 컨테이너를 root로 실행하면 왜 위험한가? (컨테이너 탈출 시 호스트 root 권한)
- [ ] `docker scout` 또는 `trivy`로 이미지 취약점 스캔

---

## Day 123 (목) — 모니터링 스택 (Prometheus + Grafana)

**코드 (2.5h)**
- [ ] compose에 모니터링 추가
  ```yaml
  prometheus:
    image: prom/prometheus
    volumes: ["./prometheus.yml:/etc/prometheus/prometheus.yml"]
    ports: ["9090:9090"]

  grafana:
    image: grafana/grafana
    ports: ["3000:3000"]
    depends_on: [prometheus]
  ```
- [ ] Spring Actuator + Prometheus 연동
  - `/actuator/prometheus` 엔드포인트 활성화
  - `prometheus.yml`에 scrape target 추가
- [ ] Grafana 대시보드 만들기
  - JVM 메모리 / GC 횟수
  - HTTP 요청 수 / 응답 시간 p50, p95, p99
  - DB 커넥션 풀 (active, idle)
- **왜 질문:**
  - Pull 방식(Prometheus) vs Push 방식(CloudWatch) 차이는? 장단점?
  - **왜 질문:** Prometheus가 Pull 방식인 이유는? (모니터링 대상이 죽으면 바로 감지 가능)
  - Grafana가 데이터를 저장하나? (아님 — Prometheus가 저장, Grafana는 시각화만)

---

## Day 124 (금) — 로그 수집 (Loki)

**코드 (2.5h)**
- [ ] Loki + Promtail compose에 추가
  ```yaml
  loki:
    image: grafana/loki
    ports: ["3100:3100"]

  promtail:
    image: grafana/promtail
    volumes:
      - /var/log:/var/log
      - ./promtail-config.yml:/etc/promtail/config.yml
  ```
- [ ] Spring Boot 로그 → Loki 수집
  - JSON 포맷 로그 설정
  - label: service, level, traceId
- [ ] Grafana에서 로그 조회
  - 메트릭 이상 → 해당 시점 로그 확인 연계
- **왜 질문:**
  - ELK(Elasticsearch+Logstash+Kibana) vs Loki 차이는?
  - Loki가 "like Prometheus, but for logs"인 이유는? (라벨 기반 인덱싱, 본문은 압축만)
  - **왜 질문:** 로그를 왜 중앙 수집하나? (컨테이너는 죽으면 로그도 사라짐)
  - 로그 보관 기간은 어떻게 결정하나? (비용 vs 디버깅 필요)

---

## Day 125 (토) — Kubernetes 기초: 왜 K8s인가

**이해 + 코드 (5h)**

오전 (3h) — K8s 필요성 + 로컬 환경 구축
- [ ] Docker만으로 부족한 점
  - 컨테이너 죽으면 누가 다시 띄우나? → 자동 복구
  - 트래픽 늘면 인스턴스 추가는? → 자동 스케일링
  - 여러 서버에 어떻게 분배? → 스케줄링
  - 무중단 배포는? → 롤링 업데이트
  - **왜 질문:** Docker Compose로 운영하면 안 되나? (단일 호스트 제한, 자동 복구 없음)
- [ ] K8s 핵심 아키텍처
  - **Control Plane**: API Server, etcd, Scheduler, Controller Manager
  - **Worker Node**: kubelet, kube-proxy, Container Runtime
  - **왜 질문:** etcd는 왜 필요한가? (클러스터 상태의 진실의 원천)
  - **왜 질문:** kubelet의 역할은? (Pod 상태 관리, Control Plane과 통신)
- [ ] 로컬 K8s 설치: **Kind** 또는 Docker Desktop Kubernetes
  ```bash
  kind create cluster --name trader
  kubectl cluster-info
  ```
- [ ] `kubectl` 기본 명령어
  - `get`, `describe`, `logs`, `exec`, `port-forward`
- [ ] `k9s` (TUI) 설치

오후 (2h) — Pod부터 Deployment까지
- [ ] **Pod** 단독 실행
  ```yaml
  apiVersion: v1
  kind: Pod
  metadata:
    name: trader-bot
  spec:
    containers:
    - name: backend
      image: trader-bot:v1
      ports:
      - containerPort: 8080
  ```
- [ ] Pod 죽이기 → 다시 안 살아남 확인
- [ ] **Deployment**로 감싸기 (replicas: 3)
  - Pod 하나 죽이기 → 자동으로 새로 생성 확인
  - **왜 질문:** Pod를 직접 만들지 않고 왜 Deployment로 감싸나? (선언적 상태 관리 + 자동 복구)
- [ ] 이미지 버전 업데이트 → 롤링 업데이트 확인
  ```bash
  kubectl set image deployment/trader-bot backend=trader-bot:v2
  ```

---

## Day 126 (일) — Week 18 정리 + 블로그

**오전 (2.5h)**
- [ ] Docker 이미지 최적화 단계별 크기 비교표
- [ ] Docker Compose 구성도 (서비스 간 관계)
- [ ] **블로그 작성:** "Spring Boot Docker 이미지, 700MB → 130MB로 줄이기"
- [ ] 다음 주 예습: K8s Service, Ingress 문서 훑기
- 오후: 휴식

**Week 18 PR:** 최적화된 Dockerfile + Docker Compose 풀스택 + 모니터링 스택

---

# Week 19 — Kubernetes 핵심

---

## Day 127 (월) — Service & Ingress

**코드 (2.5h)**
- [ ] **Service 3가지** 실험
  - **ClusterIP** (기본): 클러스터 내부 통신만
  - **NodePort**: 외부에서 노드IP:포트로 접근
  - **LoadBalancer**: 클라우드 LB 연동
- [ ] Service 동작 확인
  ```yaml
  apiVersion: v1
  kind: Service
  metadata:
    name: trader-bot-svc
  spec:
    selector:
      app: trader-bot
    ports:
    - port: 80
      targetPort: 8080
    type: ClusterIP
  ```
- [ ] **Ingress** — 도메인/경로 기반 라우팅
  - nginx-ingress-controller 설치
  - `/api/*` → backend, `/*` → frontend
- **왜 질문:**
  - Service의 selector는 어떻게 Pod를 찾는가? (label 매칭)
  - **왜 질문:** ClusterIP Service는 IP가 변하지 않는다. Pod IP는 변하는데 어떻게? (kube-proxy가 iptables/IPVS로 라우팅)
  - Ingress vs LoadBalancer Service 차이는? (Ingress: L7 라우팅, 하나의 LB로 여러 서비스)
  - NodePort의 범위는 왜 30000-32767인가?

---

## Day 128 (화) — ConfigMap & Secret

**코드 (2.5h)**
- [ ] **ConfigMap** — 설정 외부화
  ```bash
  kubectl create configmap trader-config \
    --from-literal=SPRING_PROFILES_ACTIVE=prod \
    --from-file=application-prod.yml
  ```
  - 환경변수 주입 방식
  - 볼륨 마운트 방식 (파일로)
- [ ] **Secret** — 민감 정보
  ```bash
  kubectl create secret generic trader-secrets \
    --from-literal=DB_PASSWORD=secret123 \
    --from-literal=JWT_SECRET=mykey
  ```
  - Base64 인코딩 (암호화 아님!)
  - **왜 질문:** Secret이 Base64일 뿐인데 왜 ConfigMap과 분리하나? (RBAC 권한 분리, 감사)
- [ ] Pod에 ConfigMap/Secret 마운트
  ```yaml
  envFrom:
  - configMapRef:
      name: trader-config
  - secretRef:
      name: trader-secrets
  ```
- **왜 질문:**
  - ConfigMap 변경하면 Pod가 자동 재시작하나? (아님 — 볼륨 마운트면 파일은 업데이트되지만 앱은 재시작 안 함)
  - 운영에서 Secret을 어떻게 안전하게 관리하나? (Sealed Secrets, External Secrets Operator, Vault)
  - Git에 Secret YAML을 커밋하면 왜 위험한가?

---

## Day 129 (수) — 헬스체크 & 자동 복구

**코드 (2.5h)**
- [ ] Probe 3가지 이해 + 설정
  - **Liveness Probe**: 죽었으면 재시작
  - **Readiness Probe**: 준비 안 됐으면 트래픽 안 보냄
  - **Startup Probe**: 느린 시작 앱용 (Spring Boot에 적합)
- [ ] Spring Actuator 연동
  ```yaml
  livenessProbe:
    httpGet:
      path: /actuator/health/liveness
      port: 8080
    initialDelaySeconds: 30
    periodSeconds: 10
  readinessProbe:
    httpGet:
      path: /actuator/health/readiness
      port: 8080
    initialDelaySeconds: 10
    periodSeconds: 5
  startupProbe:
    httpGet:
      path: /actuator/health
      port: 8080
    failureThreshold: 30
    periodSeconds: 10
  ```
- [ ] 자동 복구 테스트
  - 일부러 /actuator/health를 실패하게 만드는 코드
  - Pod 재시작 확인 (`kubectl get pods` — RESTARTS 카운트)
- **왜 질문:**
  - Liveness와 Readiness를 왜 분리하나? (Liveness 실패 = 재시작, Readiness 실패 = 트래픽만 차단)
  - `initialDelaySeconds`가 너무 짧으면? (앱 시작 전에 죽이기 시작 → 무한 재시작)
  - **왜 질문:** Startup Probe가 없으면 무거운 앱(Spring Boot)에 어떤 문제? (시작 느림 → Liveness 실패 → 무한 재시작)

---

## Day 130 (목) — 리소스 관리 & HPA

**코드 (2.5h)**
- [ ] 리소스 요청(requests) & 제한(limits)
  ```yaml
  resources:
    requests:
      memory: "256Mi"
      cpu: "250m"
    limits:
      memory: "512Mi"
      cpu: "500m"
  ```
  - **왜 질문:** requests vs limits 차이는? (requests: 스케줄링 기준 / limits: 최대 사용량)
  - **왜 질문:** limits를 너무 타이트하게 잡으면? (OOMKilled 빈번 발생)
- [ ] **OOMKilled 재현**
  - JVM `-Xmx256m` + container limits memory=256Mi → OOMKilled
  - **왜 질문:** JVM 힙과 컨테이너 메모리 제한의 관계는? (JVM은 힙 외에도 메타스페이스, 스택 등 사용)
  - 규칙: 컨테이너 limits > JVM -Xmx + 여유 (보통 1.5~2배)
- [ ] **HPA (Horizontal Pod Autoscaler)**
  ```bash
  kubectl autoscale deployment trader-bot --cpu-percent=70 --min=2 --max=10
  ```
  - 부하 주기 → Pod 자동 증가 확인
  - 부하 제거 → Pod 자동 감소 확인 (cooldown 기간)
- **왜 질문:**
  - HPA가 메트릭을 어디서 가져오나? (Metrics Server → kubelet → cAdvisor)
  - 스케일 아웃 판단 기준을 CPU 외에 뭘로 할 수 있나? (커스텀 메트릭 — 큐 길이, 응답 시간)

---

## Day 131 (금) — StatefulSet & PersistentVolume

**코드 (2.5h)**
- [ ] **StatefulSet** — PostgreSQL 배포
  ```yaml
  apiVersion: apps/v1
  kind: StatefulSet
  metadata:
    name: postgres
  spec:
    serviceName: postgres
    replicas: 1
    template:
      spec:
        containers:
        - name: postgres
          image: postgres:16-alpine
          volumeMounts:
          - name: pg-data
            mountPath: /var/lib/postgresql/data
    volumeClaimTemplates:
    - metadata:
        name: pg-data
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 10Gi
  ```
- [ ] **PVC + PV** 관계 이해
  - PV (PersistentVolume): 실제 스토리지
  - PVC (PersistentVolumeClaim): 스토리지 요청
  - StorageClass: 동적 프로비저닝
- [ ] Pod 재시작해도 데이터 유지 확인
- **왜 질문:**
  - Deployment vs StatefulSet 차이는?
    - Deployment: 동일한 Pod, 순서 무관, 공유 PV
    - StatefulSet: 고유 이름(pod-0, pod-1), 순서 보장, 개별 PV
  - **왜 질문:** DB를 K8s에 올리는 게 좋은가? (논쟁적 — 운영 복잡성 vs 일관된 관리)
  - 운영에서는 보통 관리형 DB (RDS) 사용. 왜?

---

## Day 132 (토) — Helm 차트 작성

**코드 (5h)**

오전 (3h) — Helm 기초 + 차트 작성
- [ ] Helm이란? — K8s의 패키지 매니저
  - Chart: 매니페스트 템플릿 묶음
  - Release: Chart의 배포 인스턴스
  - Values: 환경별 설정값
- [ ] trader-bot Helm 차트 직접 작성
  ```
  charts/trader-bot/
  ├── Chart.yaml
  ├── values.yaml
  ├── values-dev.yaml
  ├── values-prod.yaml
  └── templates/
      ├── deployment.yaml
      ├── service.yaml
      ├── ingress.yaml
      ├── configmap.yaml
      └── secret.yaml
  ```
- [ ] 템플릿 문법 (`{{ .Values.image.tag }}`)
- [ ] `helm install`, `helm upgrade`, `helm rollback`
- **왜 질문:**
  - Helm이 왜 필요한가? (환경별 다른 설정, 재사용, 버전 관리)
  - `helm template`으로 렌더링만 해보기 — 실제 어떤 YAML이 생성되는지

오후 (2h) — 공개 차트 사용 + 전체 통합
- [ ] Bitnami 차트 사용
  ```bash
  helm install postgres oci://registry-1.docker.io/bitnamicharts/postgresql
  helm install redis oci://registry-1.docker.io/bitnamicharts/redis
  ```
- [ ] trader-bot 전체를 Helm으로 로컬 K8s에 배포
- [ ] `helm list`, `helm history`, `helm rollback` 실습
- **왜 질문:**
  - Kustomize vs Helm 차이는? (Kustomize: 오버레이 패치, Helm: 템플릿 + 패키지)
  - 운영에서 둘 다 쓰는 경우도 있다. 언제?

---

## Day 133 (일) — Week 19 정리 + 블로그

**오전 (2.5h)**
- [ ] K8s 리소스 관계도 (Deployment → ReplicaSet → Pod → Container)
- [ ] Helm 차트 구조 정리
- [ ] **블로그 작성:** "Kubernetes 입문 — Pod부터 HPA까지 한 번에 이해하기"
- [ ] 다음 주 예습: AWS VPC, EC2 문서 훑기
- 오후: 휴식

**Week 19 PR:** K8s 매니페스트 + Helm 차트 + 헬스체크 + HPA

---

# Week 20 — Kubernetes 운영 + CI 연동

---

## Day 134 (월) — K8s 로깅 & 디버깅

**코드 (2.5h)**
- [ ] Pod 디버깅 기본
  - `kubectl logs <pod>` — 로그 확인
  - `kubectl logs <pod> --previous` — 이전 컨테이너 로그 (크래시 시)
  - `kubectl describe pod <pod>` — 이벤트 확인
  - `kubectl exec -it <pod> -- /bin/sh` — 컨테이너 접속
- [ ] 흔한 에러 패턴 재현 + 해결
  - **ImagePullBackOff**: 이미지 이름 오타 → 수정
  - **CrashLoopBackOff**: 앱 시작 실패 → 로그 확인
  - **Pending**: 리소스 부족 → describe로 원인 확인
  - **OOMKilled**: 메모리 초과 → limits 조정
- [ ] `kubectl top pods` — CPU/메모리 사용량 확인
- **왜 질문:**
  - CrashLoopBackOff에서 재시작 간격이 점점 늘어나는 이유는? (Exponential Backoff)
  - Pod가 Pending인데 이유를 모르겠을 때? (`kubectl describe` → Events 섹션)
  - **왜 질문:** 프로덕션에서 exec로 직접 접속하는 게 왜 위험한가?

---

## Day 135 (화) — Namespace & RBAC

**코드 (2.5h)**
- [ ] Namespace로 환경 분리
  ```bash
  kubectl create namespace dev
  kubectl create namespace staging
  kubectl create namespace prod
  ```
  - 같은 클러스터에서 환경 격리
  - **왜 질문:** Namespace가 네트워크를 격리하나? (기본적으로 아님 — NetworkPolicy 필요)
- [ ] **RBAC** (Role-Based Access Control)
  - Role: 네임스페이스 내 권한
  - ClusterRole: 클러스터 전체 권한
  - RoleBinding: 사용자/SA에 Role 부여
  - **왜 질문:** 왜 모든 사용자에게 admin 권한을 주면 안 되나? (최소 권한 원칙)
- [ ] ServiceAccount — Pod가 K8s API에 접근하는 신원
  - trader-bot 전용 ServiceAccount 생성
  - 필요한 권한만 부여
- **왜 질문:**
  - CI/CD 파이프라인이 K8s에 배포할 때 어떤 권한이 필요한가?
  - ServiceAccount Token은 어디에 마운트되나? (`/var/run/secrets/kubernetes.io/serviceaccount`)

---

## Day 136 (수) — NetworkPolicy & 보안

**코드 (2.5h)**
- [ ] **NetworkPolicy** — Pod 간 통신 제어
  ```yaml
  apiVersion: networking.k8s.io/v1
  kind: NetworkPolicy
  metadata:
    name: backend-policy
  spec:
    podSelector:
      matchLabels:
        app: trader-bot
    ingress:
    - from:
      - podSelector:
          matchLabels:
            app: ingress-nginx
      ports:
      - port: 8080
  ```
  - backend는 ingress에서만 접근 가능
  - DB는 backend에서만 접근 가능
- [ ] Pod Security Standards
  - **왜 질문:** 컨테이너를 privileged로 실행하면 왜 위험한가?
  - `securityContext`: runAsNonRoot, readOnlyRootFilesystem
- [ ] 이미지 풀 정책
  - `imagePullPolicy: Always` vs `IfNotPresent`
  - **왜 질문:** `latest` 태그를 운영에서 쓰면 왜 위험한가? (어떤 버전인지 모름, 롤백 불가)
- **왜 질문:**
  - Zero Trust 네트워크란? K8s에서 어떻게 구현하나?
  - 컨테이너 이미지 서명 검증(Cosign)은 왜 필요한가?

---

## Day 137 (목) — K8s 배포 전략

**코드 (2.5h)**
- [ ] **Rolling Update** (기본)
  - `maxSurge: 1`, `maxUnavailable: 0` → 무중단 보장
  - 이미지 변경 → 순차적 교체 확인
- [ ] **Blue/Green 배포** (직접 구현)
  - v1 Deployment (blue) + v2 Deployment (green)
  - Service selector를 green으로 변경 → 즉시 전환
  - 문제 있으면 blue로 복귀
- [ ] **Canary 배포** (직접 구현)
  - v1: replicas 9, v2: replicas 1 (10% 트래픽)
  - 점진적으로 v2 비율 증가
  - **왜 질문:** Canary가 단순 replica 비율로 되나? (대략적으로만 — 정밀하려면 Istio/Argo Rollouts)
- **왜 질문:**
  - Rolling Update 중에 요청이 실패할 수 있나? (Readiness Probe가 핵심)
  - Blue/Green의 단점은? (리소스 2배 필요)
  - Canary의 장점은? (소수 사용자에게만 먼저 → 위험 최소화)
  - 롤백은 어떻게? `kubectl rollout undo deployment/trader-bot`

---

## Day 138 (금) — 로컬 개발 워크플로우 (Skaffold/Tilt)

**코드 (2.5h)**
- [ ] 로컬 K8s 개발의 불편함
  - 코드 변경 → 이미지 빌드 → push → kubectl apply — 너무 느림
- [ ] **Skaffold** (또는 Tilt) 설정
  ```yaml
  # skaffold.yaml
  apiVersion: skaffold/v4beta1
  kind: Config
  build:
    artifacts:
    - image: trader-bot
      docker:
        dockerfile: Dockerfile
  deploy:
    helm:
      releases:
      - name: trader-bot
        chartPath: charts/trader-bot
  ```
- [ ] `skaffold dev` — 파일 변경 감지 → 자동 빌드 → 자동 배포
- [ ] Hot reload 체감 — 코드 수정 후 몇 초 만에 반영
- **왜 질문:**
  - 로컬 K8s 개발이 Docker Compose보다 나은 점은? (운영 환경과 동일한 구성)
  - 단점은? (복잡성, 리소스 사용량)
  - **왜 질문:** 언제 Docker Compose, 언제 로컬 K8s를 쓰나?

---

## Day 139 (토) — AWS 기초: EC2 + VPC

**이해 + 코드 (5h)**

오전 (3h) — VPC & EC2
- [ ] **VPC** (Virtual Private Cloud) 만들기
  - CIDR 블록: `10.0.0.0/16`
  - Public Subnet: `10.0.1.0/24` (인터넷 접근 가능)
  - Private Subnet: `10.0.2.0/24` (내부만)
  - Internet Gateway + NAT Gateway
  - **왜 질문:** Public/Private 서브넷을 왜 나누나? (DB는 인터넷에 노출 안 됨)
  - **왜 질문:** NAT Gateway가 왜 필요한가? (Private 서브넷에서 인터넷 나가기 위해)
- [ ] **Security Group** — 인스턴스 레벨 방화벽
  - 인바운드: 22(SSH), 80, 443만 허용
  - 소스: 내 IP만 (0.0.0.0/0 금지!)
- [ ] **EC2** t2.micro 띄우기
  - AMI 선택 (Amazon Linux 2023)
  - SSH 접속
  - Docker 설치 + trader-bot 실행

오후 (2h) — RDS + ElastiCache
- [ ] **RDS** PostgreSQL 생성 (db.t3.micro, Free Tier)
  - Private 서브넷에 배치
  - Security Group: backend에서만 접근
  - **왜 질문:** RDS를 쓰는 이유는? (백업, 복제, 패치 자동화)
- [ ] **ElastiCache** Redis (cache.t3.micro)
- [ ] EC2에서 RDS, Redis 접속 확인
- **비용 주의:** RDS Free Tier = 750시간/월. 안 쓸 때 stop!
- **왜 질문:**
  - Multi-AZ 배포란? 왜 필요한가? (가용 영역 장애 대비)
  - Read Replica는 어떤 문제를 해결하나? (읽기 부하 분산)

---

## Day 140 (일) — Week 20 정리 + 블로그

**오전 (2.5h)**
- [ ] K8s 배포 전략 3가지 비교표
- [ ] AWS VPC 네트워크 다이어그램
- [ ] **블로그 작성:** "K8s 배포 전략 — Rolling, Blue/Green, Canary 직접 비교"
- [ ] 다음 주 예습: AWS IAM, ECS/EKS 문서
- 오후: 휴식

**Week 20 PR:** K8s 보안 설정 + 배포 전략 + AWS VPC/EC2/RDS 구성

---

# Week 21 — AWS 클라우드 배포

---

## Day 141 (월) — IAM & 보안

**이해 + 코드 (2.5h)**
- [ ] IAM 핵심 개념
  - **User**: 사람
  - **Role**: 서비스/앱이 사용
  - **Policy**: 권한 정의 (JSON)
  - **Group**: User 묶음
- [ ] trader-bot용 IAM Role 생성
  - S3 특정 버킷만 접근
  - RDS 특정 DB만 접근
  - **최소 권한 원칙** 적용
- [ ] EC2에 Role 부여 → 액세스 키 없이 S3 접근
  ```java
  // SDK가 자동으로 EC2 메타데이터에서 임시 자격증명 획득
  ```
- **왜 질문:**
  - Access Key를 코드에 하드코딩하면 왜 위험한가? (유출 시 즉시 피해)
  - IAM Role이 Access Key보다 안전한 이유는? (임시 자격증명, 자동 갱신)
  - **왜 질문:** AssumeRole이란? 왜 필요한가? (교차 계정 접근, 임시 권한 상승)
  - MFA 강제는 왜 필요한가?

---

## Day 142 (화) — ECR + ECS Fargate

**코드 (2.5h)**
- [ ] **ECR** (Elastic Container Registry) — 이미지 저장소
  ```bash
  aws ecr create-repository --repository-name trader-bot
  docker tag trader-bot:v1 <account>.dkr.ecr.<region>.amazonaws.com/trader-bot:v1
  docker push <account>.dkr.ecr.<region>.amazonaws.com/trader-bot:v1
  ```
- [ ] **ECS Fargate** — 서버리스 컨테이너
  - Task Definition 작성 (컨테이너 정의)
  - Service 생성 (desired count: 2)
  - ALB 연동
  - **왜 질문:** Fargate vs EC2 런타임 차이는? (Fargate: 서버 관리 안 함 / EC2: 직접 관리)
  - **왜 질문:** Fargate가 편한데 왜 EC2 타입도 쓰나? (비용, GPU, 세밀한 제어)
- [ ] 헬스체크 + 자동 복구 확인
  - Task 하나 강제 종료 → 자동 재생성
- **왜 질문:**
  - ECS vs EKS 선택 기준은? (ECS: AWS 네이티브, 간단 / EKS: K8s 표준, 이식성)
  - Task Definition의 memory/cpu는 어떻게 결정하나?

---

## Day 143 (수) — ALB + Route53 + ACM

**코드 (2.5h)**
- [ ] **ALB** (Application Load Balancer)
  - Target Group: ECS Service 연결
  - 리스너: 80 → 443 리다이렉트, 443 → Target Group
  - **왜 질문:** ALB vs NLB 선택 기준은? (ALB: HTTP 라우팅 / NLB: TCP, 고성능)
- [ ] **Route53** — DNS
  - 도메인 → ALB로 A 레코드 (Alias)
  - **왜 질문:** Alias 레코드가 CNAME보다 나은 점은? (zone apex 사용 가능, 비용 무료)
- [ ] **ACM** (AWS Certificate Manager) — HTTPS
  - 무료 SSL 인증서 발급
  - ALB에 인증서 연결 → HTTPS 동작 확인
  - **왜 질문:** ACM 인증서는 왜 무료인가? (AWS 서비스에서만 사용 가능)
- [ ] 전체 흐름 확인: 도메인 → Route53 → ALB(HTTPS) → ECS → trader-bot

---

## Day 144 (목) — CloudWatch 모니터링 & 알람

**코드 (2.5h)**
- [ ] **CloudWatch Logs** — 컨테이너 로그 수집
  - ECS Task에 awslogs 드라이버 설정
  - Log Group에서 로그 확인
  - **Logs Insights**로 쿼리
    ```
    fields @timestamp, @message
    | filter @message like /ERROR/
    | sort @timestamp desc
    | limit 20
    ```
- [ ] **CloudWatch Metrics** — Spring Boot 메트릭
  - Micrometer CloudWatch 연동
  - 또는 Container Insights 활성화
- [ ] **알람 설정**
  - 에러율 5% 초과 → SNS → 이메일/Slack
  - CPU 80% 초과 → Auto Scaling 트리거
  - 5xx 응답 증가 → 알람
- **왜 질문:**
  - CloudWatch vs Prometheus+Grafana 차이는?
  - CloudWatch 비용 구조는? (로그 수집량, 메트릭 수, 알람 수)
  - **왜 질문:** 운영에서 "알람 피로"를 어떻게 방지하나? (적절한 임계값, 알람 그룹핑)

---

## Day 145 (금) — Auto Scaling + 비용 최적화

**코드 (2.5h)**
- [ ] **ECS Auto Scaling** 설정
  - Target Tracking: CPU 70% 유지
  - Min: 2, Max: 10
  - Scale-in cooldown: 300초
- [ ] 부하 테스트 → 스케일 아웃 확인
  - k6로 부하 → Task 수 증가 → 부하 제거 → Task 감소
- [ ] **비용 최적화 체크리스트**
  - RDS: 안 쓸 때 stop (최대 7일)
  - EC2: 개발 환경은 야간/주말 stop (Lambda 스케줄)
  - NAT Gateway: 시간당 과금 — 꼭 필요한가?
  - Data Transfer: 같은 AZ면 무료
  - **왜 질문:** Spot 인스턴스란? 왜 70% 저렴한가? (남는 용량 경매 — 중단 가능)
  - Reserved Instance vs Savings Plan?
- **왜 질문:**
  - 비용 최적화와 성능/가용성의 트레이드오프는?
  - 개발 환경에서 비용 줄이는 핵심 전략은? (사용 시간 최소화)

---

## Day 146 (토) — 종합: 완전 클라우드 배포

**코드 + 측정 (5h)**

오전 (3h) — 전체 아키텍처 구축
- [ ] 최종 아키텍처 구현 확인
  ```
  인터넷 → Route53 → ALB(HTTPS)
                          ↓
                    ECS Fargate
                    ├── backend × 2~10 (HPA)
                    └── frontend × 2
                          ↓
                    Private Subnet
                    ├── RDS PostgreSQL
                    └── ElastiCache Redis
  ```
- [ ] 체크리스트 확인
  - [x] HTTPS (ACM 인증서)
  - [x] Private 서브넷에 DB
  - [x] Auto Scaling
  - [x] CloudWatch 알람
  - [x] 헬스체크 + 자동 복구

오후 (2h) — 부하 테스트 + 장애 테스트
- [ ] k6로 부하 테스트
  - 동시 사용자 100명 → 500명
  - 응답 시간 p50/p95/p99
  - Auto Scaling 동작 확인
- [ ] 장애 시나리오 테스트
  - Task 하나 강제 종료 → 자동 복구 확인
  - 잘못된 이미지 배포 → 롤백
- [ ] 비용 예측: 현재 구성으로 월 예상 비용 계산

---

## Day 147 (일) — 4단계 졸업 + 블로그

**오전 (2.5h)**
- [ ] **블로그 작성:** "Spring Boot 앱을 AWS에 완전 배포하기 — ECS + RDS + 모니터링"
- [ ] 4단계 회고
  - Docker 없이 배포하던 시절과의 차이
  - K8s가 해결하는 문제 vs 만드는 복잡성
  - 클라우드 비용의 무서움
- [ ] 5단계(CI/CD) 예습: GitHub Actions 문서 훑기
- 오후: 휴식

**Week 21 PR:** AWS 전체 배포 + ALB + ACM + CloudWatch + Auto Scaling

---

## 4단계 완료 체크리스트

### PR 목록 (5개)
- [ ] W17: 운영 스크립트 + systemd + 기본 Dockerfile
- [ ] W18: Docker 최적화 + Compose + 모니터링 스택
- [ ] W19: K8s 매니페스트 + Helm + 헬스체크 + HPA
- [ ] W20: K8s 보안/배포 전략 + AWS VPC/EC2/RDS
- [ ] W21: AWS 완전 배포 + 모니터링 + Auto Scaling

### 블로그 (5편)
- [ ] W17: Linux 필수 명령어 정리
- [ ] W18: Docker 이미지 최적화
- [ ] W19: K8s 입문 (Pod~HPA)
- [ ] W20: K8s 배포 전략 비교
- [ ] W21: AWS 완전 배포 가이드

### "왜"에 답할 수 있어야 하는 것들 (면접 대비)
- [ ] 컨테이너가 VM과 다른 점 (namespace, cgroups)
- [ ] Docker 이미지 레이어 구조와 캐싱 원리
- [ ] K8s가 Docker만으로 부족한 점을 어떻게 해결하는가
- [ ] Pod, ReplicaSet, Deployment의 관계
- [ ] Liveness vs Readiness Probe의 차이와 용도
- [ ] HPA 동작 원리
- [ ] Service가 Pod를 찾는 방식 (label selector + kube-proxy)
- [ ] Blue/Green vs Canary 배포의 장단점
- [ ] VPC Public/Private 서브넷 분리 이유
- [ ] IAM Role이 Access Key보다 안전한 이유
