# 5단계: CI/CD & DevOps — 하루 단위 커리큘럼

> **기간:** 4주 (Week 22–25)
> **선수 과정:** 1~4단계 (특히 Docker, K8s)
> **목표:** "`git push` 한 번으로 배포까지 완전 자동화" 그 과정을 설명할 수 있는 수준
> **하루:** 평일 2~3h / 토 5~6h / 일 2~3h (오후 휴식)
> **코딩 장소:** java-lab (실험) / trader-bot (적용)
> **매주 필수:** PR 1개 + 테스트 + 측정 + 블로그 1편

> **학습 원칙:**
> 1. **GitHub Actions 먼저** — 가장 쉽고 무료
> 2. **테스트 없는 CI는 무의미** — 테스트 코드 작성과 병행
> 3. **점진적 적용** — 빌드 → 테스트 → 이미지 빌드 → 배포 → 모니터링 순서

---

# Week 22 — Git 심화 & 브랜치 전략

---

## Day 148 (월) — Git 내부 동작 이해

**이해 (2h)**
- Git 내부 구조 그려보기
  - `.git/objects/` — blob, tree, commit, tag
  - **blob**: 파일 내용 (SHA-1 해시)
  - **tree**: 디렉토리 구조
  - **commit**: tree 스냅샷 + 부모 커밋 + 메타데이터
  - HEAD → branch → commit → tree → blob
- **왜 질문:**
  - Git이 "분산" 버전 관리인 이유는? (전체 히스토리를 로컬에 가짐)
  - Git이 diff가 아니라 snapshot을 저장하는 이유는? (빠른 브랜치 전환, 무결성)
  - SHA-1 해시가 의미하는 것은? (내용이 같으면 같은 해시 → 무결성 보장)
  - **왜 질문:** `git add`하면 내부적으로 뭐가 생기나? (blob object 생성 + staging area 갱신)
- [ ] `git cat-file -p <hash>`로 객체 직접 확인
- [ ] `git log --graph --oneline`으로 커밋 그래프 시각화

---

## Day 149 (화) — Git 고급 명령어

**코드 (2.5h)**
- [ ] **Interactive Rebase** (`rebase -i`)
  - 커밋 squash: 여러 커밋 합치기
  - reword: 메시지 수정
  - drop: 커밋 제거
  - reorder: 순서 변경
- [ ] **cherry-pick** — 특정 커밋만 가져오기
  - 다른 브랜치의 버그 수정 커밋 하나만 가져올 때
- [ ] **reset** 3가지 모드 실험
  - `--soft`: 커밋만 취소 (staged 유지)
  - `--mixed` (기본): 커밋+staging 취소 (working dir 유지)
  - `--hard`: 전부 취소 (⚠️ 위험)
- [ ] **reflog** — 실수 복구
  - `git reflog` → 모든 HEAD 이동 기록
  - hard reset 후에도 복구 가능
- [ ] **stash** — 작업 임시 저장
  - `git stash`, `git stash pop`, `git stash list`
- **왜 질문:**
  - rebase vs merge 차이는? (rebase: 선형 히스토리 / merge: 브랜치 기록 유지)
  - **왜 질문:** 공유 브랜치에 rebase하면 왜 위험한가? (다른 사람 커밋 기반이 바뀜)
  - reflog은 영원히 남나? (`gc.reflogExpire` — 기본 90일)
  - force push가 왜 위험한가? (다른 사람 작업 덮어쓰기)

---

## Day 150 (수) — 브랜치 전략 선택 & 적용

**이해 + 코드 (2.5h)**
- [ ] 브랜치 전략 3가지 비교

| 전략 | 적합 | 특징 |
|---|---|---|
| **Git Flow** | 릴리즈 주기 명확한 큰 프로젝트 | main, develop, feature, release, hotfix |
| **GitHub Flow** | CD 활성화된 작은 팀 | main + feature only |
| **Trunk Based** | 매일 배포하는 팀 | main 직접 + feature flag |

- [ ] trader-bot에 **GitHub Flow** 적용
  - main: 항상 배포 가능 상태
  - feature/xxx: 기능 개발 → PR → 리뷰 → merge
  - 핫픽스도 feature branch에서
- [ ] Branch Protection Rule 설정
  - PR 필수, 리뷰 1명 이상, CI 통과 필수
  - **왜 질문:** main에 직접 push를 막는 이유는? (리뷰 없는 코드 → 버그, 장애)
- [ ] PR 템플릿 작성 (`.github/pull_request_template.md`)
- **왜 질문:**
  - Git Flow가 복잡한데도 쓰이는 이유는? (여러 버전 동시 관리 필요할 때)
  - Trunk Based에서 feature flag란? (미완성 기능을 main에 넣되, 끄고 배포)
  - **왜 질문:** feature branch가 오래 살면 왜 문제인가? (merge conflict 폭발, 통합 비용 증가)

---

## Day 151 (목) — Conventional Commits & 자동 버저닝

**코드 (2.5h)**
- [ ] **Conventional Commits** 컨벤션 적용
  ```
  feat: 새 기능 (minor 버전 ↑)
  fix: 버그 수정 (patch 버전 ↑)
  feat!: 또는 BREAKING CHANGE: (major 버전 ↑)
  chore: 빌드/설정 변경
  refactor: 리팩토링
  docs: 문서
  test: 테스트
  perf: 성능 개선
  ```
- [ ] **commitlint** 설정 — 컨벤션 강제
  - Git Hook (pre-commit)으로 메시지 검증
  - 또는 CI에서 PR 커밋 메시지 검증
- [ ] **Semantic Versioning** 이해
  - `MAJOR.MINOR.PATCH` (예: 2.1.3)
  - MAJOR: 호환 안 되는 변경
  - MINOR: 하위 호환 기능 추가
  - PATCH: 하위 호환 버그 수정
- [ ] (선택) **semantic-release** 설정
  - 커밋 메시지 분석 → 자동 버전 증가 + CHANGELOG 생성 + Git tag
- **왜 질문:**
  - 커밋 메시지 컨벤션이 왜 중요한가? (자동화 가능 + 히스토리 가독성)
  - **왜 질문:** CHANGELOG를 수동으로 관리하면 왜 문제인가? (빠뜨림, 일관성 없음)
  - Git tag의 용도는? (릴리즈 시점 표시, 이미지 태그와 연동)

---

## Day 152 (금) — Git Hooks & 코드 품질 자동화

**코드 (2.5h)**
- [ ] **Git Hooks** 이해
  - `pre-commit`: 커밋 전 검사 (포맷, 린트)
  - `commit-msg`: 메시지 검증
  - `pre-push`: push 전 검사 (테스트)
- [ ] **pre-commit 훅** 설정
  - Spotless 포맷 체크 (Java)
  - 민감 정보 검출 (gitleaks)
  - 테스트 실행 (빠른 단위 테스트만)
- [ ] `.gitignore` 정비
  - `build/`, `.gradle/`, `.idea/`, `.env`, `*.p12`
  - **왜 질문:** `.env`를 왜 커밋하면 안 되나? (비밀번호, API 키 노출)
- [ ] `.gitattributes` — 줄바꿈 통일
  - `* text=auto eol=lf`
  - **왜 질문:** Windows CRLF vs Unix LF 문제가 왜 발생하나?
- **왜 질문:**
  - Hook을 팀원 모두 적용하려면? (husky, lefthook 같은 도구)
  - CI에서도 같은 검사를 하는데 Hook이 왜 필요한가? (빠른 피드백 — push 전에 잡기)
  - **왜 질문:** `--no-verify`로 Hook 건너뛰기를 허용해야 하나? (긴급 시에만)

---

## Day 153 (토) — GitHub Actions CI 기본

**코드 (5h)**

오전 (3h) — 기본 CI 파이프라인
- [ ] `.github/workflows/ci.yml` 작성
  ```yaml
  name: CI
  on:
    pull_request:
    push:
      branches: [main]

  jobs:
    build:
      runs-on: ubuntu-latest
      steps:
        - uses: actions/checkout@v4
        - uses: actions/setup-java@v4
          with:
            java-version: '21'
            distribution: 'temurin'
            cache: 'gradle'
        - run: ./gradlew build
        - uses: actions/upload-artifact@v4
          with:
            name: jar
            path: build/libs/*.jar
  ```
- [ ] PR 생성 → CI 자동 실행 확인
- [ ] 빌드 실패 → PR 머지 차단 확인
- **왜 질문:**
  - `actions/checkout@v4`가 하는 일은? (repo 코드를 runner에 가져오기)
  - `cache: 'gradle'`이 왜 중요한가? (의존성 다시 다운 안 받음 → 빌드 시간 단축)
  - **왜 질문:** `on: pull_request`와 `on: push`를 둘 다 쓰는 이유는?
  - GitHub Actions runner는 어디서 실행되나? (GitHub이 관리하는 VM)

오후 (2h) — Gradle 캐시 최적화
- [ ] Gradle Build Cache 활성화
  ```yaml
  - uses: gradle/actions/setup-gradle@v3
    with:
      cache-read-only: ${{ github.ref != 'refs/heads/main' }}
  ```
- [ ] 캐시 있을 때 vs 없을 때 빌드 시간 비교
- [ ] Gradle `--parallel` + `--build-cache` 조합
- **왜 질문:**
  - Build Cache vs Dependency Cache 차이는?
  - `cache-read-only`를 PR에서 true로 하는 이유는? (캐시 오염 방지)

---

## Day 154 (일) — Week 22 정리 + 블로그

**오전 (2.5h)**
- [ ] Git 내부 구조 다이어그램 정리
- [ ] 브랜치 전략 비교표 최종 정리
- [ ] **블로그 작성:** "Git 내부 동작 — blob, tree, commit 직접 까보기"
- [ ] 또는: "GitHub Actions CI 첫 구축기"
- 오후: 휴식

**Week 22 PR:** Branch Protection + Conventional Commits + 기본 CI 파이프라인

---

# Week 23 — CI 고도화

---

## Day 155 (월) — 테스트 자동화 (JUnit + Testcontainers)

**코드 (2.5h)**
- [ ] 테스트 피라미드 이해
  - 단위 테스트 (많이, 빠르게)
  - 통합 테스트 (적당히)
  - E2E 테스트 (적게)
- [ ] CI에서 테스트 실행
  ```yaml
  - run: ./gradlew test
  - uses: dorny/test-reporter@v1
    with:
      name: Test Results
      path: build/test-results/test/*.xml
      reporter: java-junit
  ```
- [ ] **Testcontainers** — 실제 DB로 통합 테스트
  ```java
  @Testcontainers
  @SpringBootTest
  class OrderServiceIntegrationTest {
      @Container
      static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

      @Container
      static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
          .withExposedPorts(6379);
  }
  ```
- **왜 질문:**
  - H2로 테스트하면 왜 위험한가? (PostgreSQL 전용 기능 못 테스트, 동작 차이)
  - Testcontainers가 CI에서 돌아가려면? (Docker in Docker 또는 Docker 소켓 마운트)
  - **왜 질문:** 테스트가 느려지면 CI 전체가 느려진다. 어떻게 빠르게? (병렬화, 필요한 것만)

---

## Day 156 (화) — JaCoCo 커버리지 + 리포트

**코드 (2.5h)**
- [ ] **JaCoCo** 설정
  ```groovy
  plugins {
      id 'jacoco'
  }
  jacocoTestReport {
      reports {
          xml.required = true
      }
  }
  jacocoTestCoverageVerification {
      violationRules {
          rule {
              limit {
                  minimum = 0.80
              }
          }
      }
  }
  ```
- [ ] CI에서 커버리지 80% 미만이면 빌드 실패
- [ ] **Codecov** 연동 — PR에 커버리지 댓글
  ```yaml
  - uses: codecov/codecov-action@v3
    with:
      files: build/reports/jacoco/test/jacocoTestReport.xml
  ```
- **왜 질문:**
  - 커버리지 80%가 적절한가? (높을수록 좋지만 100%는 비현실적 — ROI)
  - 라인 커버리지 vs 브랜치 커버리지 차이는?
  - **왜 질문:** 커버리지가 높으면 버그가 없는 건가? (아님 — 테스트 품질이 중요)
  - 커버리지를 낮추는 PR을 어떻게 막나? (Codecov diff coverage 체크)

---

## Day 157 (수) — 정적 분석 & 보안 스캔

**코드 (2.5h)**
- [ ] **Spotless** — 코드 포맷 체크
  ```yaml
  - run: ./gradlew spotlessCheck
  ```
  - 실패 시 `./gradlew spotlessApply`로 자동 수정
- [ ] **SonarCloud** — 코드 품질 분석
  - 코드 스멜, 버그, 보안 취약점 자동 탐지
  - Quality Gate: 통과 못 하면 머지 차단
  - **왜 질문:** 정적 분석이 런타임 테스트를 대체할 수 있나? (아님 — 보완 관계)
- [ ] **Dependabot** — 의존성 자동 업데이트
  - `.github/dependabot.yml` 설정
  - 보안 취약점 있는 의존성 자동 PR 생성
- [ ] **gitleaks** — 비밀번호/API 키 커밋 방지
  ```yaml
  - uses: gitleaks/gitleaks-action@v2
  ```
- [ ] **Trivy** — Docker 이미지 취약점 스캔
  ```yaml
  - uses: aquasecurity/trivy-action@master
    with:
      image-ref: trader-bot:${{ github.sha }}
      severity: 'CRITICAL,HIGH'
  ```
- **왜 질문:**
  - Dependabot PR이 너무 많으면? (그룹핑, 주간 스케줄)
  - **왜 질문:** 이미지 취약점이 CRITICAL인데 패치가 없으면? (베이스 이미지 변경, 또는 위험 수용 기록)
  - SAST vs DAST 차이는? (정적 분석 vs 실행 중 분석)

---

## Day 158 (목) — 매트릭스 빌드 & 병렬화

**코드 (2.5h)**
- [ ] **매트릭스 빌드** — 여러 환경 동시 테스트
  ```yaml
  strategy:
    matrix:
      java: [17, 21]
      os: [ubuntu-latest, macos-latest]
    fail-fast: false
  ```
  - Java 17/21 두 버전 동시 빌드
  - 한 환경 실패해도 다른 환경 계속 (`fail-fast: false`)
- [ ] **Job 병렬화** — 독립적인 작업 동시 실행
  ```yaml
  jobs:
    lint:
      runs-on: ubuntu-latest
      steps: [spotlessCheck]
    test:
      runs-on: ubuntu-latest
      steps: [test + coverage]
    security:
      runs-on: ubuntu-latest
      steps: [gitleaks + trivy]
    build:
      needs: [lint, test, security]
      steps: [build jar]
  ```
- [ ] 전체 CI 시간 측정: 직렬 vs 병렬
- **왜 질문:**
  - `needs`로 의존성을 거는 이유는? (test 실패하면 build 할 필요 없음)
  - `fail-fast: false`를 쓰는 이유는? (하나 실패해도 다른 환경 결과 확인)
  - **왜 질문:** CI 시간을 5분 이내로 유지하는 게 왜 중요한가? (개발자 피드백 루프)
  - self-hosted runner는 언제 필요한가? (무료 한도 초과, 특수 환경)

---

## Day 159 (금) — Docker 이미지 빌드 & Push (CD 시작)

**코드 (2.5h)**
- [ ] main 머지 시 자동 이미지 빌드 + 푸시
  ```yaml
  name: CD
  on:
    push:
      branches: [main]

  jobs:
    build-and-push:
      runs-on: ubuntu-latest
      steps:
        - uses: actions/checkout@v4
        - uses: docker/setup-buildx-action@v3
        - uses: docker/login-action@v3
          with:
            registry: ghcr.io
            username: ${{ github.actor }}
            password: ${{ secrets.GITHUB_TOKEN }}
        - uses: docker/build-push-action@v5
          with:
            push: true
            tags: |
              ghcr.io/${{ github.repository }}:${{ github.sha }}
              ghcr.io/${{ github.repository }}:latest
            cache-from: type=gha
            cache-to: type=gha,mode=max
  ```
- [ ] **이미지 태그 전략**
  - `git SHA`: 정확한 커밋 추적
  - `semver`: 릴리즈 버전
  - `latest`: 최신 (운영에서는 쓰지 마!)
- [ ] **빌드 캐시** (`cache-from`, `cache-to`)로 빌드 시간 90% 단축
- **왜 질문:**
  - GHCR vs ECR vs DockerHub 선택 기준은?
  - `latest` 태그를 운영에서 왜 쓰면 안 되나? (어떤 버전인지 모름, 롤백 불가)
  - **왜 질문:** 이미지 태그에 git SHA를 쓰면 좋은 점은? (정확한 코드 ↔ 이미지 매핑)
  - multi-arch 빌드는 왜 필요한가? (ARM Mac + AMD64 서버)

---

## Day 160 (토) — 환경별 배포 파이프라인

**코드 (5h)**

오전 (3h) — 환경 분리 + 배포 자동화
- [ ] **dev / staging / prod** 3환경 분리
  - dev: feature 브랜치 머지 시 자동 배포
  - staging: main 머지 시 자동 배포
  - prod: Git tag + 수동 승인 후 배포
- [ ] GitHub **Environments** 기능
  ```yaml
  deploy-prod:
    environment:
      name: production
      url: https://trader-bot.example.com
    steps:
      - run: echo "Deploying to prod"
  ```
  - production 환경에 **Required reviewers** 설정 → 수동 승인
- [ ] **Secrets** 환경별 분리
  - dev: `DEV_DB_PASSWORD`
  - prod: `PROD_DB_PASSWORD`
  - 환경에 바인딩된 Secret은 해당 환경 Job에서만 접근

오후 (2h) — K8s 배포 자동화
- [ ] Helm으로 배포
  ```yaml
  - run: |
      helm upgrade --install trader-bot ./charts/trader-bot \
        --set image.tag=${{ github.sha }} \
        --namespace ${{ env.NAMESPACE }} \
        -f charts/trader-bot/values-${{ env.ENV }}.yaml
  ```
- [ ] 또는 **ArgoCD** (GitOps)
  - Git 저장소의 매니페스트가 진실의 원천
  - 매니페스트 변경 → ArgoCD가 자동 sync
  - **왜 질문:** GitOps가 `kubectl apply`보다 나은 이유는? (감사 추적, 선언적, 롤백 = git revert)
- **왜 질문:**
  - Push 방식(CI가 배포) vs Pull 방식(ArgoCD가 감지) 차이는?
  - **왜 질문:** Pull 방식이 보안에 더 좋은 이유는? (CI에 클러스터 접근 권한 안 줘도 됨)

---

## Day 161 (일) — Week 23 정리 + 블로그

**오전 (2.5h)**
- [ ] CI 파이프라인 전체 다이어그램 (lint → test → security → build → push)
- [ ] 환경별 배포 플로우 정리
- [ ] **블로그 작성:** "GitHub Actions CI/CD 완전 구축기 — PR부터 배포까지"
- [ ] 다음 주 예습: Canary 배포, Argo Rollouts 문서
- 오후: 휴식

**Week 23 PR:** 전체 CI 파이프라인 + 이미지 빌드 + 환경별 배포

---

# Week 24 — 배포 전략 & 안전장치

---

## Day 162 (월) — 배포 전략 자동화

**코드 (2.5h)**
- [ ] **Rolling Update** CI 연동
  - `helm upgrade` → maxSurge/maxUnavailable 설정
  - 배포 진행 상황 모니터링
  ```bash
  kubectl rollout status deployment/trader-bot --timeout=300s
  ```
- [ ] **Canary 배포** (Argo Rollouts)
  ```yaml
  apiVersion: argoproj.io/v1alpha1
  kind: Rollout
  spec:
    strategy:
      canary:
        steps:
        - setWeight: 10
        - pause: {duration: 60s}
        - setWeight: 50
        - pause: {duration: 60s}
        - setWeight: 100
  ```
  - 10% → 50% → 100% 점진 증가
  - 각 단계에서 메트릭 확인
- **왜 질문:**
  - Canary에서 "이 버전이 괜찮다"는 판단 기준은? (에러율, 응답 시간, 비즈니스 메트릭)
  - 자동 promote vs 수동 promote 각각 언제?
  - **왜 질문:** 10% 트래픽으로 문제를 감지하려면 최소 얼마나 기다려야 하나?

---

## Day 163 (화) — 자동 롤백 & Smoke Test

**코드 (2.5h)**
- [ ] **자동 롤백** 구현
  - 배포 후 헬스체크 실패 → 자동 롤백
  ```yaml
  - run: |
      helm upgrade ... || helm rollback trader-bot
  ```
  - K8s `progressDeadlineSeconds` — 배포가 이 시간 내 완료 안 되면 실패
- [ ] **Smoke Test** — 배포 직후 빠른 검증
  ```yaml
  smoke-test:
    needs: deploy
    steps:
      - run: |
          for i in {1..10}; do
            STATUS=$(curl -s -o /dev/null -w "%{http_code}" https://api.trader-bot.com/health)
            if [ "$STATUS" = "200" ]; then exit 0; fi
            sleep 5
          done
          exit 1
  ```
  - 주요 API 엔드포인트 호출 + 응답 확인
  - 실패 시 → 자동 롤백 트리거
- **왜 질문:**
  - Smoke Test vs Integration Test 차이는? (Smoke: 기본 동작 확인, 30초 이내)
  - 롤백 시 DB 마이그레이션은 어떻게? (backward compatible migration 필수!)
  - **왜 질문:** 롤백 불가능한 배포란? (DB 스키마 변경 — 어떻게 안전하게?)
  - expand-and-contract 패턴이란?

---

## Day 164 (수) — Database Migration 자동화

**코드 (2.5h)**
- [ ] **Flyway** (또는 Liquibase) CI/CD 연동
  - 마이그레이션 파일: `V1__create_users.sql`, `V2__add_column.sql`
  - 앱 시작 시 자동 실행 vs CI에서 별도 실행
- [ ] **Backward Compatible Migration** 원칙
  - 컬럼 추가: OK (nullable 또는 default)
  - 컬럼 삭제: 2단계 (1. 코드에서 안 씀 배포 → 2. 컬럼 삭제)
  - 컬럼 이름 변경: 3단계 (새 컬럼 추가 → 양쪽 쓰기 → 구 컬럼 삭제)
  - **왜 질문:** 왜 한 번에 컬럼을 삭제하면 안 되나? (롤링 배포 중 구 버전이 참조)
- [ ] 마이그레이션 실패 시 처리
  - `spring.flyway.repair-on-failure`
  - **왜 질문:** 마이그레이션이 반쯤 실행되면? (트랜잭션 — DDL은 자동 커밋이라 위험)
- **왜 질문:**
  - JPA auto-ddl을 운영에서 쓰면 왜 위험한가? (의도치 않은 스키마 변경)
  - 마이그레이션을 CI에서 테스트하는 방법은? (Testcontainers + 마이그레이션 실행)
  - **왜 질문:** 대형 테이블에 ALTER TABLE이 왜 위험한가? (락, 오래 걸림 → `pg_repack`)

---

## Day 165 (목) — Feature Flag & 안전한 릴리즈

**이해 + 코드 (2.5h)**
- [ ] **Feature Flag** 이해
  - 코드를 배포하되, 기능은 끈 상태로
  - 특정 사용자/퍼센트에게만 켜기
  - 문제 발견 시 플래그 OFF → 즉시 비활성화 (롤백 없이!)
- [ ] 간단한 Feature Flag 구현
  ```java
  @Value("${feature.new-algorithm.enabled:false}")
  private boolean newAlgorithmEnabled;

  public void execute() {
      if (newAlgorithmEnabled) {
          newAlgorithm();
      } else {
          oldAlgorithm();
      }
  }
  ```
- [ ] ConfigMap으로 런타임 변경 (재시작 없이)
- [ ] (선택) **GrowthBook** / **Unleash** 같은 도구 연동
- **왜 질문:**
  - Feature Flag가 Canary보다 유연한 점은? (특정 사용자/조건에만 적용 가능)
  - 오래된 Feature Flag를 왜 정리해야 하나? (코드 복잡성 증가, dead code)
  - **왜 질문:** Trunk Based Development에서 Feature Flag가 필수인 이유는?
  - Dark Launch란? (기능 켜되 사용자에게 보이지 않게 — 성능 테스트용)

---

## Day 166 (금) — 배포 알림 & 감사 로그

**코드 (2.5h)**
- [ ] **Slack 배포 알림**
  ```yaml
  - uses: slackapi/slack-github-action@v1
    with:
      payload: |
        {
          "text": "🚀 ${{ github.repository }} v${{ env.VERSION }} deployed to ${{ env.ENV }}"
        }
  ```
  - 배포 성공/실패 알림
  - 배포한 사람, 커밋 목록 포함
- [ ] **배포 감사 로그**
  - 누가, 언제, 어떤 버전을 배포했는지 기록
  - GitHub Deployments API 활용
  - Grafana에서 배포 시점 마커 표시 (Annotation)
- [ ] **Grafana Annotation** — 배포와 메트릭 연결
  - 배포 후 에러율 상승 → "이 배포 때문?" 즉시 확인
- **왜 질문:**
  - 배포 알림이 왜 중요한가? (누가 뭘 배포했는지 모르면 장애 원인 추적 불가)
  - **왜 질문:** 배포 마커가 모니터링 대시보드에 있으면 뭐가 좋은가?
  - `git blame`과 배포 기록의 관계는?

---

## Day 167 (토) — 모니터링: Prometheus + Grafana 심화

**코드 (5h)**

오전 (3h) — 커스텀 메트릭 + 대시보드
- [ ] **RED 메트릭** (모든 서비스에 필수)
  - **R**ate: 초당 요청 수
  - **E**rrors: 에러율
  - **D**uration: 응답 시간 (p50, p95, p99)
- [ ] Spring Micrometer 커스텀 메트릭
  ```java
  meterRegistry.counter("trade.executed", "symbol", symbol).increment();
  meterRegistry.timer("trade.execution.time").record(duration);
  ```
- [ ] Grafana 대시보드 구성
  - 서비스 RED 메트릭 패널
  - JVM 메트릭 (Heap, GC, Threads)
  - DB 커넥션 풀 (Active, Idle, Waiting)
  - Redis 캐시 히트율
- [ ] **SLI/SLO** 설정
  - SLI: p99 응답시간 < 200ms
  - SLO: 99.9% 요청이 SLI 만족
  - **왜 질문:** SLI, SLO, SLA 차이는?

오후 (2h) — 알림 설정
- [ ] **AlertManager** 또는 Grafana Alerting
  - 에러율 1% 초과 → Slack 경고
  - p99 응답시간 500ms 초과 → Slack 경고
  - DB 커넥션 풀 90% 초과 → Slack 경고
  - Pod OOMKilled → Slack 긴급
- [ ] 알림 레벨 분리
  - **Warning**: 주의 필요 (대시보드 확인)
  - **Critical**: 즉시 대응 (on-call 호출)
- **왜 질문:**
  - 알람이 너무 많으면? (알람 피로 → 진짜 문제 놓침)
  - 좋은 알람의 조건은? (actionable, 즉시 대응 가능한 것만)
  - **왜 질문:** "에러가 발생했다" vs "에러율이 평소보다 높다" — 어느 게 좋은 알람인가?

---

## Day 168 (일) — Week 24 정리 + 블로그

**오전 (2.5h)**
- [ ] 배포 전략 비교 다이어그램 (Rolling / Blue-Green / Canary)
- [ ] CI/CD 전체 파이프라인 아키텍처 도
- [ ] **블로그 작성:** "안전한 배포 — Canary + 자동 롤백 + Feature Flag"
- [ ] 다음 주 예습: 분산 추적 (OpenTelemetry) 문서
- 오후: 휴식

**Week 24 PR:** Canary 배포 + Smoke Test + 자동 롤백 + Flyway + 배포 알림

---

# Week 25 — 옵저버빌리티 & 종합

---

## Day 169 (월) — 구조화된 로깅

**코드 (2.5h)**
- [ ] **JSON 포맷 로그** (운영 환경)
  - logback-spring.xml에 JSON 인코더 설정
  ```xml
  <encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <includeMdcKeyName>traceId</includeMdcKeyName>
    <includeMdcKeyName>userId</includeMdcKeyName>
  </encoder>
  ```
- [ ] **MDC(Mapped Diagnostic Context)** 활용
  - traceId: 요청 추적
  - userId: 사용자별 로그 필터링
  - spanId: 분산 추적 연동
- [ ] Loki에서 라벨 기반 쿼리
  ```
  {service="trader-bot"} |= "ERROR" | json | userId="user123"
  ```
- **왜 질문:**
  - 왜 구조화된 로그(JSON)가 필요한가? (파싱 쉬움, 필드별 검색/필터)
  - 텍스트 로그의 문제점은? (정규식 파싱 필요, 필드 추가 어려움)
  - **왜 질문:** 로그 레벨을 런타임에 변경할 수 있나? (Spring Actuator `/loggers` 엔드포인트)
  - 민감 정보가 로그에 찍히면? (마스킹 필터 필수)

---

## Day 170 (화) — 분산 추적 (OpenTelemetry)

**코드 (2.5h)**
- [ ] **OpenTelemetry** 이해
  - Trace: 요청의 전체 여정
  - Span: 개별 작업 단위
  - Context Propagation: 서비스 간 trace ID 전달
- [ ] Spring Boot 3 + Micrometer Tracing 설정
  ```properties
  management.tracing.sampling.probability=1.0
  management.otlp.tracing.endpoint=http://tempo:4318/v1/traces
  ```
- [ ] **Tempo** (또는 Jaeger)로 trace 수집
- [ ] Grafana에서 trace 시각화
  - 요청 → 서비스A → DB → 외부 API → 서비스B 전체 흐름
  - 각 구간별 소요 시간 확인
- **왜 질문:**
  - 분산 추적이 왜 MSA에서 필수인가? (로그만으로는 요청 흐름 파악 불가)
  - sampling.probability=1.0은 운영에서 왜 위험한가? (성능 영향 + 저장 비용)
  - **왜 질문:** W3C Trace Context 표준이란? (`traceparent` 헤더)
  - 메트릭 → 로그 → 트레이스 상호 연결이 왜 중요한가?

---

## Day 171 (수) — 옵저버빌리티 3대 축 통합

**코드 (2.5h)**
- [ ] **메트릭 + 로그 + 트레이스** 통합 시나리오
  1. Grafana 대시보드에서 에러율 급증 감지 (메트릭)
  2. 해당 시간대 에러 로그 확인 (로그)
  3. 특정 에러의 trace ID로 분산 추적 (트레이스)
  4. 병목 구간 식별 → 원인 파악
- [ ] Grafana에서 연결 설정
  - 메트릭 패널 → "View logs" 링크
  - 로그 → traceId 클릭 → Trace 뷰어
- [ ] **Exemplar** — 메트릭에 trace ID 연결
  - 느린 요청의 정확한 trace를 메트릭에서 바로 접근
- **왜 질문:**
  - 셋 중 하나만 있으면 왜 부족한가?
  - 메트릭만: "문제 있다"는 알지만 원인 모름
  - 로그만: 양이 너무 많아 찾기 어려움
  - 트레이스만: 어떤 요청을 볼지 모름
  - **왜 질문:** "옵저버빌리티"와 "모니터링"의 차이는? (모니터링: 알려진 문제 감시 / 옵저버빌리티: 미지의 문제 탐색)

---

## Day 172 (목) — 부하 테스트 자동화

**코드 (2.5h)**
- [ ] **k6** 스크립트 작성
  ```javascript
  import http from 'k6/http';
  import { check, sleep } from 'k6';

  export const options = {
    stages: [
      { duration: '30s', target: 100 },
      { duration: '1m', target: 500 },
      { duration: '30s', target: 0 },
    ],
    thresholds: {
      http_req_duration: ['p(99)<200'],
      http_req_failed: ['rate<0.01'],
    },
  };

  export default function () {
    const res = http.get('https://api.trader-bot.com/api/orders');
    check(res, { 'status 200': (r) => r.status === 200 });
    sleep(1);
  }
  ```
- [ ] CI에서 야간 부하 테스트 (cron)
  ```yaml
  on:
    schedule:
      - cron: '0 2 * * *'  # 매일 새벽 2시
  ```
- [ ] **Performance Budget** — p99 > 200ms면 CI 실패
- [ ] 결과를 Grafana에 저장 → 성능 회귀 트렌드 확인
- **왜 질문:**
  - 부하 테스트를 CI에 넣는 이유는? (성능 회귀 자동 감지)
  - 부하 테스트 환경은 운영과 같아야 하나? (가능한 비슷하게 — 다르면 결과 의미 없음)
  - **왜 질문:** Soak Test(장시간 테스트)는 왜 필요한가? (메모리 누수, 커넥션 누수 발견)

---

## Day 173 (금) — 인시던트 대응 프로세스

**이해 (2.5h)**
- [ ] **On-Call & 인시던트 관리**
  - 감지 → 분류 → 대응 → 복구 → 포스트모텔
  - PagerDuty / Opsgenie 같은 도구
  - 에스컬레이션 정책
- [ ] **Runbook** 작성
  - 알람별 대응 매뉴얼
  - 예: "DB 커넥션 풀 90% → 1) 쿼리 확인 2) 풀 증가 3) 재시작"
- [ ] **포스트모텔** (장애 후 회고)
  - 타임라인
  - 근본 원인 (Root Cause)
  - 재발 방지 Action Items
  - **Blameless** 문화
- **왜 질문:**
  - Blameless 포스트모텀이 왜 중요한가? (숨기지 않고 배우기 위해)
  - MTTR(Mean Time To Recovery)를 줄이는 핵심은? (좋은 모니터링 + 자동 롤백 + Runbook)
  - **왜 질문:** SRE에서 Error Budget이란? (SLO 초과 여유분 — 소진되면 기능 개발 중단하고 안정성에 집중)
  - 카오스 엔지니어링(Chaos Monkey)이란? 왜 필요한가?

---

## Day 174 (토) — 종합 파이프라인 완성

**코드 + 측정 (5h)**

오전 (3h) — 전체 파이프라인 검증
- [ ] **End-to-End 플로우 확인**
  ```
  1. PR 생성
     ├── Lint (Spotless)
     ├── Test (JUnit + Testcontainers)
     ├── Coverage (JaCoCo ≥ 80%)
     ├── Security (gitleaks + Trivy)
     └── SonarCloud
  2. PR 머지 (main)
     ├── 시맨틱 버저닝 (tag)
     ├── Docker 이미지 빌드 + Push (GHCR)
     ├── Helm 업그레이드 → staging
     └── Smoke Test
  3. Production 배포
     ├── 수동 승인
     ├── Canary (10% → 50% → 100%)
     ├── 메트릭 자동 검증
     └── 실패 시 자동 롤백
  ```
- [ ] PR 생성부터 prod 배포까지 전체 시간 측정 (목표: 10분)
- [ ] 일부러 버그 배포 → 자동 롤백 확인

오후 (2h) — 5단계 종합 정리
- [ ] CI/CD 전체 아키텍처 다이어그램
- [ ] 각 단계별 도구 + 역할 정리
- [ ] 면접 대비 "왜" 질문 10개 셀프 답변
- [ ] 비용 정리: GitHub Actions 무료 한도, AWS 비용

---

## Day 175 (일) — 5단계 졸업 + 블로그

**오전 (2.5h)**
- [ ] **블로그 작성:** "CI/CD 완전 자동화 — git push부터 prod 배포까지 10분"
- [ ] 5단계 회고
  - 수동 배포의 고통 → 자동화의 쾌감
  - 모니터링 없이 배포하는 건 눈 감고 운전하는 것
  - 가장 가치 있었던 자동화 단계
- [ ] 6단계(아키텍처 & 분산 시스템) 예습: "데이터 중심 애플리케이션 설계" 1장
- 오후: 휴식

**Week 25 PR:** 옵저버빌리티 스택 + 부하 테스트 자동화 + 종합 파이프라인

---

## 5단계 완료 체크리스트

### PR 목록 (4개)
- [ ] W22: Branch Protection + Conventional Commits + 기본 CI
- [ ] W23: 테스트/커버리지/보안 스캔 + Docker Push + 환경별 배포
- [ ] W24: Canary + 자동 롤백 + Flyway + Feature Flag + 알림
- [ ] W25: 옵저버빌리티 + 부하 테스트 + 종합 파이프라인

### 블로그 (4편)
- [ ] W22: Git 내부 구조 또는 CI 첫 구축기
- [ ] W23: GitHub Actions CI/CD 완전 구축
- [ ] W24: 안전한 배포 전략
- [ ] W25: CI/CD 완전 자동화 종합

### "왜"에 답할 수 있어야 하는 것들 (면접 대비)
- [ ] Git 내부 객체 구조 (blob, tree, commit)
- [ ] rebase vs merge 차이와 사용 시점
- [ ] CI에서 테스트/린트/보안 스캔이 각각 잡는 문제
- [ ] Docker 이미지 빌드 캐시 원리
- [ ] Rolling vs Blue/Green vs Canary 장단점
- [ ] 자동 롤백이 동작하려면 무엇이 필요한가
- [ ] Feature Flag의 장점과 위험
- [ ] 옵저버빌리티 3대 축 (메트릭/로그/트레이스) 역할
- [ ] SLI/SLO/SLA 차이
- [ ] GitOps(Pull 방식)가 Push 방식보다 안전한 이유
