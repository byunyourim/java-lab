# 5단계 실습 가이드: CI/CD & DevOps

> **기간:** 3~4주
> **선수 과정:** 1~4단계 (특히 Docker, K8s)
> **이 단계의 목표:** `git push` 한 번으로 배포까지 완전 자동화

---

## 학습 원칙

1. **GitHub Actions 먼저** — 가장 쉽고 무료
2. **테스트 없는 CI는 무의미** — 테스트 코드 작성과 병행
3. **점진적 적용** — 빌드 → 테스트 → 이미지 빌드 → 배포 → 모니터링 순서

---

## Week 1: Git 심화 & 브랜치 전략

### 과제 1-1. Git 고급 기능 (난이도 ★★)

**할 일**
1. `rebase -i`로 커밋 squash, reword, drop
2. `cherry-pick` — 특정 커밋만 가져오기
3. `reset --soft / mixed / hard` 차이 직접 실험
4. `reflog`로 실수 복구
5. `stash` 활용 — 중간에 다른 브랜치 작업

> ⚠️ 공유 브랜치에는 절대 rebase / force push 금지

---

### 과제 1-2. 브랜치 전략 선택 (난이도 ★★)

**할 일** — trader-bot에 맞는 전략 결정

| 전략 | 적합 | 비고 |
|---|---|---|
| **Git Flow** | 릴리즈 주기 명확한 큰 프로젝트 | 복잡함 |
| **GitHub Flow** | CD가 활성화된 작은 팀 | main + feature만 |
| **Trunk Based** | 매일 배포하는 팀 | feature flag 필요 |

→ trader-bot 같은 개인 프로젝트는 **GitHub Flow** 추천

**문서화**: `/CONTRIBUTING.md`에 브랜치 규칙, 커밋 메시지 컨벤션, PR 템플릿

---

### 과제 1-3. Conventional Commits + 자동 버저닝 (난이도 ★★)

**할 일**
1. **Conventional Commits** 컨벤션 적용
   ```
   feat: 새 기능
   fix: 버그 수정
   chore: 잡일
   refactor: 리팩토링
   docs: 문서
   test: 테스트
   ```
2. **commitlint** + **husky**로 컨벤션 강제
3. **semantic-release**로 버전 자동 증가 + CHANGELOG 자동 생성

---

## Week 2: GitHub Actions로 CI 구축

### 과제 2-1. 기본 CI 파이프라인 (난이도 ★★)

**할 일**
`.github/workflows/ci.yml` 작성

```yaml
name: CI
on:
  pull_request:
  push:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - uses: gradle/actions/setup-gradle@v3
      - run: ./gradlew build
      - uses: actions/upload-artifact@v4
        with:
          name: jar
          path: backend/build/libs/*.jar
```

**체크리스트**
- [ ] PR마다 자동 실행
- [ ] 빌드 실패 시 PR 머지 차단 (Branch Protection Rule)
- [ ] Gradle 캐시로 빌드 시간 단축

---

### 과제 2-2. 테스트 + 커버리지 (난이도 ★★)

**할 일**
1. JUnit5 + Mockito + AssertJ로 테스트 작성
2. **Testcontainers** — 실제 PostgreSQL + Redis로 통합 테스트
   ```java
   @Testcontainers
   class OrderServiceTest {
       @Container
       static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
   }
   ```
3. **JaCoCo** 커버리지 측정 → 80% 미만이면 빌드 실패
4. **Codecov** 또는 GitHub Action으로 PR에 커버리지 댓글

---

### 과제 2-3. 정적 분석 & 보안 스캔 (난이도 ★★)

**할 일**
1. **Spotless** (이미 적용됨) — 포맷 체크 단계 추가
2. **SonarCloud** — 코드 스멜, 버그, 보안 취약점
3. **Dependabot** — 의존성 자동 업데이트 PR
4. **Trivy** — Docker 이미지 취약점 스캔
5. **gitleaks** — 비밀번호/API 키 커밋 방지

---

### 과제 2-4. 매트릭스 빌드 (난이도 ★)

**할 일**
- Java 17 / 21 두 버전 동시 빌드
- Ubuntu / Mac 두 OS 동시 테스트
- 한 환경 실패해도 다른 환경 계속 진행 (`fail-fast: false`)

---

## Week 3: CD (배포 자동화)

### 과제 3-1. Docker 이미지 빌드 & 푸시 (난이도 ★★)

**할 일**
1. main 브랜치 머지 시 자동으로
   - Docker 이미지 빌드
   - 태그 부여 (Git SHA + semver)
   - **GHCR (GitHub Container Registry)** 또는 **AWS ECR** 푸시
2. **멀티 아키텍처 빌드** — `linux/amd64` + `linux/arm64`
3. **이미지 캐시 활용** — `cache-from`, `cache-to`로 빌드 시간 90% 단축

---

### 과제 3-2. 환경별 배포 (난이도 ★★★)

**할 일**
1. **dev / staging / prod** 세 환경 분리
2. 브랜치 전략과 연동
   - `develop` 머지 → dev 환경
   - `release/*` → staging
   - `main` + Git tag → prod (수동 승인 필요)
3. GitHub **Environments** 기능 — prod 배포 시 수동 승인
4. **Secrets** 환경별로 분리

---

### 과제 3-3. K8s 배포 자동화 (난이도 ★★★)

선택 1: **kubectl 직접**
```yaml
- run: |
    aws eks update-kubeconfig --name trader-cluster
    kubectl set image deployment/backend backend=$IMAGE_TAG
```

선택 2: **Helm 업그레이드**
```yaml
- run: helm upgrade --install trader ./charts/trader --set image.tag=$IMAGE_TAG
```

선택 3: **ArgoCD (GitOps)** — 강추
- Git 저장소가 원본 진실(Single Source of Truth)
- 매니페스트 변경 → ArgoCD가 자동 sync
- 롤백도 Git 되돌리기로

---

### 과제 3-4. 배포 전략 (난이도 ★★★)

**할 일**
1. **Rolling Update** — K8s 기본, 점진 교체
2. **Blue/Green** — 두 환경 동시 운영 후 트래픽 전환
   - Argo Rollouts 또는 직접 Service selector 변경
3. **Canary** — 트래픽 10% → 50% → 100% 점진 증가
   - Argo Rollouts + Prometheus 메트릭 기반 자동 promote
4. **롤백 훈련** — 일부러 버그 배포 → 5분 내 롤백

---

## Week 4: 모니터링 & 옵저버빌리티

### 과제 4-1. 메트릭 (Prometheus + Grafana) (난이도 ★★★)

**할 일**
1. Spring Actuator + Micrometer로 커스텀 메트릭
   ```java
   meterRegistry.counter("trade.executed", "symbol", symbol).increment();
   ```
2. **RED 메트릭** (Rate, Errors, Duration) 대시보드
3. **USE 메트릭** (Utilization, Saturation, Errors) — 인프라용
4. **AlertManager**로 Slack 알람
   - 에러율 1% 초과
   - p99 응답시간 500ms 초과
   - DB 커넥션 풀 90% 초과

---

### 과제 4-2. 로그 (Loki / ELK) (난이도 ★★)

**할 일**
1. **구조화된 로그** — JSON 포맷, Logback 설정
2. **MDC**로 traceId, userId 자동 포함
3. **Loki + Promtail** 또는 **ELK** 구축
4. Grafana에서 메트릭과 로그 연계 — "이 시점 에러 로그 보기"

---

### 과제 4-3. 분산 추적 (난이도 ★★★)

**할 일**
1. **OpenTelemetry** 적용 (Spring Boot 3 기본 지원)
2. **Jaeger** 또는 **Tempo**로 trace 수집
3. 외부 거래소 API 호출까지 trace 연결
4. Grafana에서 메트릭 + 로그 + 트레이스 통합 뷰

---

### 과제 4-4. 부하 테스트 자동화 (난이도 ★★)

**할 일**
1. **k6** 스크립트 작성 — 시나리오 기반
2. GitHub Actions에서 야간 부하 테스트 (cron)
3. 결과를 Grafana에 저장 → 성능 회귀 감지
4. **Performance Budget** — p99 > 200ms면 CI 실패

---

## 종합 과제

### "완전 자동화 파이프라인 구축"

**목표:** `git push origin main` → 5분 후 prod 배포 완료

**플로우**
```
1. PR 생성
   ├── Lint (Spotless)
   ├── Test (JUnit + Testcontainers)
   ├── Coverage (JaCoCo, 80% 이상)
   ├── SonarCloud 분석
   └── Trivy 이미지 스캔
2. PR 머지 (main)
   ├── 시맨틱 버저닝
   ├── Docker 이미지 빌드 + ECR 푸시
   ├── Helm 차트 버전 업
   └── ArgoCD 자동 sync → staging
3. Smoke test (k6, 30초)
4. Slack 알림 → 수동 승인
5. Canary 배포 (10% → 50% → 100%)
6. Prometheus 메트릭 자동 검증
7. 실패 시 자동 롤백
```

**체크리스트**
- [ ] PR부터 prod까지 사람 손은 "승인" 한 번만
- [ ] 빌드 + 테스트 + 배포 총 10분 이하
- [ ] 실패 시 자동 롤백 (5분 이내)
- [ ] 배포 알림 Slack
- [ ] Grafana 대시보드에서 배포 마커 표시

---

## 추천 학습 자료

| 주제 | 자료 |
|---|---|
| CI/CD 일반 | "Continuous Delivery" (Jez Humble) — 고전 |
| GitHub Actions | 공식 문서 |
| GitOps | "GitOps and Kubernetes" |
| 모니터링 | "관측 가능성 엔지니어링" (오라일리) |
| SRE | "Site Reliability Engineering" (Google) — 무료 공개 |

---

## 진도 체크
- [ ] Week 1: Git & 브랜치 전략
- [ ] Week 2: CI 구축
- [ ] Week 3: CD 구축
- [ ] Week 4: 모니터링 & 옵저버빌리티
- [ ] 종합 과제: 완전 자동화 파이프라인
