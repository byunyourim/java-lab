# 4단계 실습 가이드: 인프라 & 컨테이너

> **기간:** 4~6주
> **선수 과정:** 1~3단계 완료
> **이 단계의 의의:** 배포·운영 능력. 시니어로 가는 가장 큰 갈림길.

---

## 학습 원칙

1. **로컬에서 컨테이너로** → **클라우드로** 점진적 이동
2. **비용 관리** — AWS Free Tier로 시작, 안 쓰면 끄기 (RDS 켜두면 한 달 5만원 순삭)
3. **"왜 이게 필요한가"부터** — 컨테이너가 해결하는 문제부터 이해

---

## Week 1: Linux 기본기

### 과제 1-1. 자주 쓰는 명령어 익히기 (난이도 ★)

**할 일**
실제 trader-bot 운영 시나리오로 연습

| 시나리오 | 명령어 |
|---|---|
| 8080 포트 누가 점유? | `lsof -i :8080`, `netstat -tnlp` |
| 메모리 많이 쓰는 프로세스? | `top`, `htop`, `ps aux --sort=-%mem` |
| 로그에서 ERROR만? | `grep ERROR app.log` |
| 시간별 에러 추이? | `grep ERROR app.log \| awk '{print $1}' \| sort \| uniq -c` |
| 디스크 어디가 가득? | `du -sh *`, `df -h` |
| 파일 변경 감지? | `tail -f app.log` |

---

### 과제 1-2. 쉘 스크립트 작성 (난이도 ★★)

**할 일**
trader-bot 운영 스크립트 5개 작성

1. `start.sh` — Spring Boot 백그라운드 실행 + PID 저장
2. `stop.sh` — PID로 graceful shutdown (SIGTERM → 30초 → SIGKILL)
3. `backup.sh` — PostgreSQL 덤프 → S3 업로드 (또는 로컬)
4. `health-check.sh` — 헬스 체크 실패 시 Slack 알림
5. `log-rotate.sh` — 7일 이상 된 로그 압축

---

### 과제 1-3. 프로세스와 시그널 (난이도 ★★)

**할 일**
1. Spring Boot 앱 띄우고 `SIGTERM` 보내기 — Graceful Shutdown 동작 확인
2. `server.shutdown=graceful` 옵션 켜고 종료 시 진행 중 요청 대기 확인
3. `kill -9` (SIGKILL)와의 차이 — 트랜잭션 중간에 끊기면?

---

## Week 2: Docker 기초 ~ 심화

### 과제 2-1. Spring Boot Dockerfile 작성 (난이도 ★★)

**할 일**
1. 가장 단순한 Dockerfile부터
   ```dockerfile
   FROM openjdk:21
   COPY build/libs/*.jar app.jar
   ENTRYPOINT ["java","-jar","/app.jar"]
   ```
2. **이미지 크기 최적화** — 단계별로 크기 측정
   - `openjdk:21` (700MB+) → `eclipse-temurin:21-jre-alpine` (200MB) → `distroless` (130MB)
3. **멀티 스테이지 빌드** — Gradle 빌드 단계 + 실행 단계 분리
4. **Layered Jar** 활용 (Spring Boot 2.3+)
   ```dockerfile
   FROM eclipse-temurin:21-jre AS builder
   COPY app.jar app.jar
   RUN java -Djarmode=layertools -jar app.jar extract

   FROM eclipse-temurin:21-jre
   COPY --from=builder dependencies/ ./
   COPY --from=builder spring-boot-loader/ ./
   COPY --from=builder snapshot-dependencies/ ./
   COPY --from=builder application/ ./
   ENTRYPOINT ["java","org.springframework.boot.loader.launch.JarLauncher"]
   ```
5. 의존성만 바뀌었을 때 vs 코드만 바뀌었을 때 빌드 시간 비교

---

### 과제 2-2. Docker Compose 풀스택 (난이도 ★★)

**할 일**
trader-bot 전체를 docker-compose로 띄우기

```yaml
services:
  backend:
    build: ./backend
    depends_on: [postgres, redis]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/trader
    ports: ["8080:8080"]

  frontend:
    build: ./frontend
    ports: ["3000:3000"]

  postgres:
    image: postgres:16-alpine
    volumes: ["postgres_data:/var/lib/postgresql/data"]
    environment:
      POSTGRES_DB: trader
      POSTGRES_PASSWORD: ${DB_PASSWORD}

  redis:
    image: redis:7-alpine
    volumes: ["redis_data:/data"]

volumes:
  postgres_data:
  redis_data:
```

**체크리스트**
- [ ] 한 줄(`docker compose up`)로 풀스택 실행
- [ ] 헬스체크로 의존성 순서 보장
- [ ] 비밀번호는 `.env` 파일로 분리
- [ ] 볼륨으로 데이터 유지 (재시작해도 살아남는지 확인)
- [ ] 개발용 / 운영용 compose 파일 분리

---

### 과제 2-3. Docker 네트워크 & 볼륨 (난이도 ★★)

**할 일**
1. `bridge`, `host`, `none` 네트워크 차이 실험
2. 두 컨테이너 간 통신 — 호스트명으로 가능한지
3. **볼륨 3가지** 모두 써보기
   - bind mount: `./data:/data`
   - named volume: `mydata:/data`
   - tmpfs: 메모리 볼륨
4. PostgreSQL 데이터 살리면서 컨테이너 재생성

---

### 과제 2-4. 모니터링 컨테이너 구성 (난이도 ★★)

**할 일**
compose에 다음 서비스 추가
- **Prometheus** — Spring Actuator `/actuator/prometheus` 스크랩
- **Grafana** — Prometheus 대시보드
- **Loki + Promtail** — 로그 수집 (또는 ELK)

대시보드 만들기
- JVM 메모리 / GC
- HTTP 요청 수 / 응답 시간 p50, p95, p99
- DB 커넥션 풀 사용량

---

## Week 3-4: Kubernetes

### 과제 3-1. 로컬 K8s 환경 (난이도 ★★)

**할 일**
1. **Kind** 또는 **Minikube** 설치 (Mac은 Docker Desktop K8s도 가능)
2. `kubectl` 기본 명령어 익히기
   - `get`, `describe`, `logs`, `exec`, `port-forward`
3. `k9s` (TUI) 설치 — 운영에 강추

---

### 과제 3-2. Pod부터 Deployment까지 (난이도 ★★★)

**목표:** "왜 Deployment가 필요한가" 직접 체감

**할 일**
1. trader-bot 이미지로 **Pod** 단독 실행
   ```yaml
   apiVersion: v1
   kind: Pod
   ```
2. Pod 죽이고 다시 안 살아나는 것 확인
3. **ReplicaSet**으로 감싸기 → 3개 유지, 하나 죽이면 새로 뜸
4. **Deployment**로 감싸기 → 롤링 업데이트 확인
5. 이미지 버전 바꿔서 `kubectl set image` → 무중단 배포 확인

---

### 과제 3-3. Service & Ingress (난이도 ★★★)

**할 일**
1. **ClusterIP Service** — 내부 통신만
2. **NodePort** — 외부에서 접근
3. **LoadBalancer** — 클라우드 LB 연동
4. **Ingress** — 도메인/경로 기반 라우팅
   - `/api/*` → backend
   - `/*` → frontend
5. `nginx-ingress-controller` 설치하고 적용

---

### 과제 3-4. ConfigMap & Secret (난이도 ★★)

**할 일**
1. Spring Boot `application.yml` 일부를 ConfigMap으로 외부화
2. DB 비밀번호, JWT 시크릿은 Secret으로
3. `kubectl create secret generic ... --from-literal=...`
4. 환경변수 주입 + 볼륨 마운트 둘 다 해보기
5. **Sealed Secrets** 또는 **External Secrets Operator** 맛보기 — Git에 비밀번호 안 넣는 법

---

### 과제 3-5. StatefulSet & PersistentVolume (난이도 ★★★)

**할 일**
1. PostgreSQL을 **StatefulSet**으로 배포
2. **PVC + PV** — 데이터 영속성
3. Pod 재시작해도 데이터 유지 확인
4. **Deployment vs StatefulSet** 차이 — Pod 이름, 식별성, 순서

---

### 과제 3-6. Helm 차트 작성 (난이도 ★★★)

**할 일**
1. trader-bot용 Helm 차트 직접 작성
2. `values.yaml`로 환경별 분리 (dev / staging / prod)
3. `helm install`, `helm upgrade`, `helm rollback` 모두 사용
4. **공개 차트 사용** — `bitnami/postgresql`, `bitnami/redis`

---

### 과제 3-7. 헬스체크 & 자동 복구 (난이도 ★★)

**할 일**
1. **Liveness Probe** — 죽으면 재시작
2. **Readiness Probe** — 준비 안 됐으면 트래픽 안 보냄
3. **Startup Probe** — 느린 시작 앱용 (Spring Boot에 적합)
4. Spring Actuator `/actuator/health/liveness`, `/readiness` 활용
5. 일부러 죽이는 코드 만들어서 자동 복구 확인

---

### 과제 3-8. 리소스 관리 & 자동 스케일링 (난이도 ★★★)

**할 일**
1. Pod `resources.requests` / `limits` 설정
2. 메모리 초과 시 **OOMKilled** 발생시키기 — JVM `-Xmx`와 충돌 주의
3. **HPA (Horizontal Pod Autoscaler)** — CPU 70% 넘으면 Pod 추가
4. 부하 테스트로 스케일 아웃 / 인 확인

---

## Week 5: 클라우드 (AWS)

### 과제 4-1. AWS 기초 (난이도 ★★)

**할 일** (Free Tier 사용)
1. EC2 t2.micro 띄우고 SSH 접속
2. trader-bot Docker로 실행
3. **VPC** 만들기 — Public/Private 서브넷
4. **Security Group** — 포트 화이트리스트
5. **RDS** PostgreSQL 띄우기 (db.t3.micro)
6. EC2에서 RDS로 접속

**비용 주의**: RDS는 Free Tier여도 750시간/월 제한. 안 쓸 때 stop 필수.

---

### 과제 4-2. IAM (난이도 ★★)

**할 일**
1. trader-bot용 IAM 역할 만들기
2. **최소 권한 원칙** — S3 특정 버킷만, RDS 특정 DB만
3. EC2에 역할 부여해서 액세스 키 없이 S3 접근
4. **AssumeRole**로 임시 자격증명

---

### 과제 4-3. ECS / EKS 중 하나 (난이도 ★★★)

**둘 중 하나만 선택**

#### ECS (Fargate) — 입문 추천
- Task Definition 작성
- Service로 자동 복구 + ALB 연동
- ECR에 이미지 푸시

#### EKS — K8s 경험 살리기
- `eksctl`로 클러스터 생성
- 위에서 만든 Helm 차트 그대로 배포
- AWS Load Balancer Controller로 ALB Ingress

---

### 과제 4-4. CloudWatch 모니터링 (난이도 ★★)

**할 일**
1. 컨테이너 로그를 CloudWatch Logs로
2. Spring Boot 메트릭 → CloudWatch Metrics
3. **알람 설정** — 에러율 5% 초과 시 SNS → Slack/Email
4. CloudWatch Logs Insights로 로그 쿼리

---

## 종합 과제

### "trader-bot 완전 클라우드 배포"

**아키텍처**
```
인터넷
   ↓
ALB / CloudFront
   ↓
EKS (또는 ECS)
   ├── frontend Pod x 2
   └── backend Pod x 3 (HPA)
        ↓
   RDS PostgreSQL (Multi-AZ)
   ElastiCache Redis
```

**체크리스트**
- [ ] 도메인 + Route53 + ACM(HTTPS)
- [ ] Private 서브넷에 DB, Public에는 ALB만
- [ ] Helm 차트로 배포
- [ ] HPA로 자동 스케일링
- [ ] CloudWatch 알람
- [ ] Secrets Manager로 비밀번호 관리
- [ ] 한 곳을 죽여도 서비스 유지(고가용성) 검증

---

## 추천 학습 자료

| 주제 | 자료 |
|---|---|
| Linux | "Linux 커맨드라인 셸 스크립트 바이블" |
| Docker | "도커 교과서" (엘튼 스톤맨) — 입문 최고 |
| Kubernetes | "쿠버네티스 인 액션" (마르코 룩샤) — 두꺼움, 정석 |
| K8s 실무 | "프로덕션 쿠버네티스" |
| AWS | "AWS Certified Solutions Architect Associate" 강의 (Stephane Maarek) |

---

## 진도 체크
- [ ] Week 1: Linux
- [ ] Week 2: Docker
- [ ] Week 3-4: Kubernetes
- [ ] Week 5: AWS
- [ ] 종합 과제: 완전 클라우드 배포
