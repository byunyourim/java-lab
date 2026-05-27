# Day 03 — Metaspace OOM 재현 & GC 기초

---

## 1. Metaspace OOM 재현 (MetaspaceOom.java)

### 실행 방법
```bash
java -XX:MaxMetaspaceSize=64m -cp "build/classes/java/main:<byte-buddy-jar경로>" \
  com.byunyourim.javalab.week1.day03.MetaspaceOom
```

### 실행 결과
```
[500개 생성]    Heap used: 18,751KB / Heap max: 6,291,456KB
[1,000개 생성]  Heap used: 62,925KB / Heap max: 6,291,456KB
[2,000개 생성]  Heap used: 149,823KB / Heap max: 6,291,456KB
[2,500개 생성]  Heap used: 34,337KB    ← GC 발생! Heap은 수거됨
[5,000개 생성]  Heap used: 109,641KB
[5,500개 생성]  Heap used: 43,866KB    ← 또 GC 발생! Heap은 계속 안정적
...
[10,000개 생성] Heap used: 80,921KB

=== OOM 발생! ===
Exception in thread "main" java.lang.OutOfMemoryError: Metaspace
```

### 결과 해석 — 숫자로 읽기

**Heap은 안정적이다:**
- 2,000개 시점에서 149MB까지 올라갔다가 → 2,500개 시점에 34MB로 뚝 떨어짐
- 이건 GC가 ByteBuddy가 만든 중간 바이트코드 객체(Heap에 있는)를 잘 수거했다는 뜻
- Heap max는 6,291,456KB(≈6GB)인데 실제 사용량은 30~150MB 사이를 왔다갔다
- → **Heap은 전혀 문제없음**

**그런데 왜 OOM이 났는가:**
- 클래스를 로드할 때 클래스의 **메타데이터**는 Heap이 아니라 **Metaspace**(Native Memory)에 저장됨
- 메타데이터 = 클래스 이름, 부모 클래스 정보, 메서드 테이블(vtable), 상수 풀, 필드 레이아웃 등
- 클래스 1개당 약 6~7KB의 Metaspace 사용 (64MB ÷ 10,000개 ≈ 6.5KB)
- 64MB 제한에 도달하자 더 이상 클래스를 로드할 수 없어서 OOM

**Heap과 Metaspace의 차이를 그림으로:**
```
┌─────────────────────────────────────────────┐
│                 JVM 프로세스                  │
│                                             │
│  ┌─────────────────────┐  ┌──────────────┐  │
│  │      Java Heap       │  │  Metaspace   │  │
│  │                     │  │ (Native Mem) │  │
│  │  new byte[1024]     │  │              │  │
│  │  new ArrayList()    │  │  클래스 이름    │  │
│  │  ByteBuddy 중간객체   │  │  메서드 테이블  │  │
│  │                     │  │  상수 풀       │  │
│  │  → GC가 수거 가능 ✓  │  │  필드 레이아웃  │  │
│  │                     │  │              │  │
│  │  -Xmx로 크기 제한    │  │  -XX:MaxMeta │  │
│  │                     │  │  spaceSize   │  │
│  └─────────────────────┘  └──────────────┘  │
└─────────────────────────────────────────────┘
```

### 참조 유지 vs 미유지 — 왜 결과가 달라지는가

**참조를 유지한 경우 (이번 실험):**
```java
List<Class<?>> classes = new ArrayList<>();
classes.add(loaded);  // Class 객체 참조를 리스트가 잡고 있음
```
```
classes 리스트 → Class 객체 → ClassLoader → Metaspace의 메타데이터
     ↑ GC Root(스택 변수)에서 도달 가능 → GC 불가 → 언로딩 불가 → Metaspace 증가만 함
```
결과: 10,000개에서 OOM

**참조를 유지하지 않은 경우 (첫 번째 시도):**
```java
unloaded.load(...);  // 로드만 하고 반환값을 안 잡음
```
```
아무도 Class 객체를 참조 안 함
→ Class 객체 GC 대상 → ClassLoader도 GC 대상 → 클래스 언로딩 발생
→ Metaspace 반환 → 다시 여유 생김
```
결과: 223만 개를 넘겨도 OOM 안 남 (Metaspace가 계속 재활용됨)

---

### 왜 Java 8에서 PermGen → Metaspace로 바꿨는가?

**PermGen의 문제점 (Java 7 이하):**

1. **고정 크기 문제**
   - `-XX:MaxPermSize=256m` 같이 개발자가 직접 크기를 정해야 함
   - 너무 작게 잡으면 → `java.lang.OutOfMemoryError: PermGen space`
   - 너무 크게 잡으면 → 메모리 낭비
   - 앱마다 필요한 크기가 달라서 "적정값"을 예측하기 어려움
   - 특히 Spring, Hibernate 같은 프레임워크는 런타임에 프록시 클래스를 대량 생성 → 예측 불가

2. **String.intern() 폭탄**
   - Java 7 이하에서 `String.intern()`의 문자열이 PermGen에 저장됨
   - 웹 앱에서 사용자 입력을 intern하면 PermGen이 터짐
   - 클래스 메타데이터와 문자열이 같은 공간을 놓고 경쟁

3. **GC 복잡도**
   - PermGen은 Heap 안에 있지만 일반 객체와 다른 수명을 가짐
   - Full GC에서만 정리 가능 → 빈번한 Full GC 유발 → 성능 저하

**Metaspace의 개선 (Java 8+):**

1. **Native Memory 사용**
   - JVM Heap 바깥, OS가 관리하는 네이티브 메모리 사용
   - `MaxMetaspaceSize`를 안 설정하면 OS 물리 메모리까지 자동 확장
   - "미리 크기를 예측해야 하는" 부담이 사라짐

2. **ClassLoader 단위 Chunk 관리**
   - 각 ClassLoader가 자기만의 메모리 Chunk를 가짐
   - ClassLoader가 GC되면 → 해당 Chunk 전체를 OS에 일괄 반환
   - PermGen 시절처럼 단편화 문제가 없음

3. **String.intern()은 Heap으로 이동**
   - Java 7부터 String Pool이 Heap으로 이동
   - Metaspace에는 순수하게 클래스 메타데이터만 저장
   - 각 영역의 역할이 명확해짐

| 항목 | PermGen (Java 7 이하) | Metaspace (Java 8+) |
|------|----------------------|---------------------|
| 위치 | Java Heap 안 (고정 크기) | Native Memory (자동 확장) |
| 크기 설정 | `-XX:MaxPermSize` 필수 | 설정 안 하면 OS 메모리까지 사용 |
| String.intern() | PermGen에 저장 → 터지기 쉬움 | Heap으로 이동 |
| 클래스 해제 | Full GC에서만, 단편화 발생 | ClassLoader 단위 Chunk 일괄 반환 |
| 실무 영향 | Spring/Hibernate에서 자주 OOM | 거의 OOM 안 남 (제한 안 걸면) |

---

### 클래스 언로딩은 언제 일어나는가?

**언로딩 조건 — 세 가지 모두 충족해야 함:**

```
조건 1: 해당 클래스의 모든 인스턴스가 GC됨
         → new DynClass_0() 으로 만든 객체가 전부 사라져야 함

조건 2: 해당 클래스를 로드한 ClassLoader가 GC됨
         → ClassLoader 자체도 아무도 참조하지 않아야 함

조건 3: 해당 Class 객체(java.lang.Class)에 대한 참조가 없음
         → 리플렉션으로 Class.forName() 등으로 잡고 있으면 안 됨
```

**하나라도 안 되면 언로딩 불가:**
```
DynClass_0.class ←── classes 리스트가 참조 (조건 3 위반)
       ↓
  ClassLoader  ←── Class 객체가 classLoader 필드로 참조 (조건 2 위반)
       ↓
  Metaspace 메타데이터  →  해제 불가!
```

**GC와의 관계:**
- 클래스 언로딩은 **GC의 일부**로 발생함
- Minor GC에서는 클래스 언로딩 안 함 (Young 영역만 보니까)
- **Full GC** 또는 **G1의 Concurrent Cycle cleanup** 단계에서 발생
- `-XX:MaxMetaspaceSize` 한계 도달 시 → JVM이 Metaspace GC를 강제 트리거
- 그래도 언로딩 조건이 안 맞으면 → OOM

**절대 언로딩 안 되는 클래스:**
- Bootstrap ClassLoader가 로드한 클래스 (`java.lang.Object`, `java.lang.String` 등)
- System ClassLoader가 로드한 애플리케이션 클래스
- → 이들의 ClassLoader는 JVM이 종료될 때까지 살아있으므로 조건 2를 절대 충족 못 함

---

## 2. GC 기초 실험 (GcBasic.java)

### 실행 방법
```bash
java -Xms128m -Xmx128m '-Xlog:gc*' -cp build/classes/java/main \
  com.byunyourim.javalab.week1.day03.GcBasic
```

### GC 로그 한 줄 해부

```
GC(0) Pause Young (Concurrent Start) (G1 Humongous Allocation) 57M->1M(128M) 2.546ms
│  │      │              │                    │                  │      │       │
│  │      │              │                    │                  │      │       └ STW 시간
│  │      │              │                    │                  │      └ 전체 Heap 크기
│  │      │              │                    │                  └ GC 후 Heap 사용량
│  │      │              │                    │                  (57MB→1MB: 56MB 수거됨!)
│  │      │              │                    └ 원인: 큰 객체 할당 시도
│  │      │              └ Concurrent Mark Cycle도 시작
│  │      └ Young 영역 GC
│  └ 0번째 GC
└ GC 이벤트
```

**Heap 변화 상세:**
```
GC(0) Eden regions: 1->0(55)      ← Eden: 1개 Region 사용 중 → 0개로 비움 (최대 55개)
GC(0) Survivor regions: 0->1(6)   ← Survivor: 비어있었음 → 1개로 (살아남은 객체)
GC(0) Old regions: 2->2           ← Old: 변화 없음 (아직 승격된 객체 없음)
GC(0) Humongous regions: 55->0    ← Humongous: 55개 → 0 (대형 객체가 전부 수거됨)
GC(0) Metaspace: 354K->354K       ← Metaspace: 변화 없음 (클래스 로드/언로드 없음)
```

### 1단계 결과: Minor GC — 단명 객체 (triggerMinorGc)

```java
for (int i = 0; i < 30; i++) {
    byte[] garbage = new byte[4 * 1024 * 1024]; // 4MB
}
```

**무슨 일이 일어나는가:**
```
반복 1:  garbage → [4MB 배열] 할당 (Eden에 들어가기엔 큼 → Humongous)
반복 2:  garbage → [새 4MB 배열] 할당
         이전 배열은 garbage 변수가 더 이상 안 가리킴 → GC 대상
         ...Eden/Humongous가 차면 Minor GC 발생
반복 30: 총 120MB 할당했지만 실제 살아있는 건 마지막 1개뿐
```

**GC 로그에서 보이는 것:**
- `57M->1M` : 57MB 사용 중이었는데 GC 후 1MB만 남음 (56MB 수거)
- STW 2.5ms : 모든 스레드가 멈춘 시간이 고작 2.5밀리초
- Humongous regions: 55->0 : 대형 객체 55개가 전부 수거됨

**왜 Humongous인가:**
- G1GC는 Heap을 동일 크기 Region(이 실험에서 1MB)으로 나눔
- 객체가 Region의 50% 이상(= 512KB 이상)이면 Humongous로 분류
- 4MB 배열 = 4개 Region을 연속으로 차지하는 Humongous Object

### 2단계 결과: 객체 승격 관찰 (observePromotion)

```java
List<byte[]> survivors = new ArrayList<>();
for (int i = 0; i < 5; i++) {
    survivors.add(new byte[1024 * 1024]); // 1MB × 5 = 5MB, 리스트가 참조 유지
}
// 이후 Minor GC를 여러 번 유발
for (int i = 0; i < 20; i++) {
    byte[] temp = new byte[4 * 1024 * 1024]; // 단명 객체
}
```

**무슨 일이 일어나는가:**
```
Minor GC 1회차: survivors 5개 살아남음 → age 1
Minor GC 2회차: survivors 5개 살아남음 → age 2
Minor GC 3회차: survivors 5개 살아남음 → age 3
...
Minor GC 15회차: age 15 도달 → MaxTenuringThreshold(기본 15) → Old 영역으로 승격!
```

**GC 로그에서 보이는 것:**
```
GC(4) Eden regions: 1->0(65)
GC(4) Survivor regions: 1->1(7)   ← Survivor에 객체가 계속 있음 = 살아남고 있음
GC(4) Old regions: 2->2           ← 아직 Old로 안 넘어감
GC(4) Humongous regions: 60->10   ← survivors 5개(각 1MB)는 Humongous로 남아있음
```

### 3단계 결과: Full GC (triggerFullGc)

```java
List<byte[]> pressure = new ArrayList<>();
while (true) {
    pressure.add(new byte[1024 * 1024]); // 1MB씩, 절대 안 버림
}
```

**무슨 일이 일어나는가:**
```
1MB 할당 → 참조 유지 → GC해도 수거 불가
1MB 할당 → 참조 유지 → GC해도 수거 불가
...
Eden 가득 → Minor GC → 근데 살아있으니 Survivor로
Survivor 가득 → Old로 승격
Old 가득 → Full GC 시도 → 근데 다 살아있음 → 수거할 게 없음
→ OutOfMemoryError: Java heap space
```

**Minor GC와 Full GC의 STW 시간 비교:**
```
Minor GC: 2~3ms    (Young만 봄, 대부분 죽은 객체라 빠름)
Full GC:  수십~수백ms (Young + Old + Metaspace 전체를 Mark-Sweep-Compact)
```

---

### Mark-Sweep-Compact 알고리즘 상세

```
  GC 전 메모리 상태 (■ = 살아있음, □ = 죽음)
  ┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐
  │ ■ │ □ │ ■ │ □ │ □ │ ■ │ □ │ □ │ ■ │ □ │
  └───┴───┴───┴───┴───┴───┴───┴───┴───┴───┘

  1) Mark — GC Root에서 참조 체인을 따라가며 살아있는 객체 표시
     GC Root: 스택의 지역변수, static 변수, JNI 참조
     ┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐
     │ ✓ │   │ ✓ │   │   │ ✓ │   │   │ ✓ │   │
     └───┴───┴───┴───┴───┴───┴───┴───┴───┴───┘

  2) Sweep — 표시 안 된 객체의 메모리 해제
     ┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐
     │ ■ │   │ ■ │   │   │ ■ │   │   │ ■ │   │
     └───┴───┴───┴───┴───┴───┴───┴───┴───┴───┘
     → 문제: 빈 공간이 듬성듬성 → "메모리 단편화"
     → 연속 5칸짜리 객체를 할당하고 싶어도 불가능

  3) Compact — 살아남은 객체를 한쪽으로 밀착
     ┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐
     │ ■ │ ■ │ ■ │ ■ │   │   │   │   │   │   │
     └───┴───┴───┴───┴───┴───┴───┴───┴───┴───┘
     → 빈 공간이 연속됨 → 큰 객체도 할당 가능
     → 새 객체는 빈 영역 시작점에 바로 할당 (bump-the-pointer)
```

---

### 왜 Eden + Survivor 두 개로 나눴는가?

**배경: Weak Generational Hypothesis (약한 세대 가설)**
> "대부분의 객체는 생성 직후 곧바로 죽는다"
> IBM 연구 결과: 전체 객체의 약 98%가 첫 번째 GC 이전에 죽음

**순수 복사 수집기의 문제:**
```
┌──────────────────┬──────────────────┐
│    From 영역      │     To 영역       │
│  (사용 중)        │   (비어있음)       │
│                  │                  │
│  50% 사용 가능    │  50% 낭비!       │
└──────────────────┴──────────────────┘
GC 시: From에서 살아남은 것만 To로 복사 → From과 To 역할 교대
장점: 단편화 없음 (복사하면서 밀착 배치)
단점: 메모리의 절반을 항상 비워둬야 함
```

**Eden + S0 + S1 개선:**
```
┌────────────────────────────────────┬────────┬────────┐
│          Eden (80%)                │ S0(10%)│ S1(10%)│
│  새 객체는 여기에 할당              │ from   │  to    │
│                                    │        │        │
│  98%가 여기서 바로 죽음             │ 2%만   │        │
│                                    │ 여기로  │        │
└────────────────────────────────────┴────────┴────────┘

Minor GC 동작:
  1. Eden + S0(from)에서 살아남은 객체 → S1(to)로 복사
  2. Eden과 S0 전체를 비움
  3. S0과 S1의 역할을 교대 (다음 GC에서는 S1이 from)

낭비: S1 하나 = 전체의 10%만 낭비 (순수 복사 수집기의 50% → 10%로 개선!)
```

**왜 이게 가능한가:**
- 98%가 Eden에서 바로 죽으니까 Eden을 크게 잡아도 됨
- Survivor로 넘어오는 건 2%뿐 → 작은 Survivor로 충분
- 결국 "대부분 죽는다"는 가설이 맞기 때문에 이 비대칭 구조가 효율적

---

### 객체가 Old로 넘어가는 조건 상세

#### 조건 1: Age 임계값 도달
```
Minor GC 1회 살아남음 → age 1 (Object Header의 4bit에 저장)
Minor GC 2회 살아남음 → age 2
...
Minor GC 15회 살아남음 → age 15 = MaxTenuringThreshold 도달
→ 다음 Minor GC에서 Old 영역으로 복사 (승격/Promotion)

조정: -XX:MaxTenuringThreshold=5 → 5회만 살아남으면 바로 승격
     (최대값: 15, 4bit로 표현하므로)
```

#### 조건 2: 동적 나이 판단 (Dynamic Age Decision)
```
Survivor 영역에서:
  age 1 객체들 합계: 2MB
  age 2 객체들 합계: 3MB
  age 3 객체들 합계: 4MB
  ─────────────────────
  누적 합: 9MB

  Survivor 크기의 50%(TargetSurvivorRatio) = 8MB

  age 3 시점에서 누적 합(9MB) > 8MB → age 3 이상 객체 전부 Old로 조기 승격
  → MaxTenuringThreshold가 15여도 age 3에서 승격될 수 있음!
```

#### 조건 3: 대형 객체 직접 할당
```
일반 GC:      Eden에 못 넣을 크기 → 바로 Old로
G1GC:         Region(보통 1~32MB)의 50% 이상 → Humongous Region에 할당
              → Humongous는 Old 취급, Full GC에서만 수거 (Java 8)
              → Java 11+부터는 Young GC에서도 수거 가능하도록 개선
```

#### 조건 4: Survivor 공간 부족 (Promotion Failure)
```
Minor GC 후 살아남은 객체가 10MB인데 Survivor가 8MB밖에 없으면
→ 넘치는 2MB가 바로 Old로 이동
→ 이게 반복되면 Old가 빨리 차서 Full GC 빈도 증가
→ 해결: Survivor 비율 조정 (-XX:SurvivorRatio)
```
