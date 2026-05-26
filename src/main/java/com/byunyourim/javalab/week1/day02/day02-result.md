# Day 2 — Heap OOM + StackOverflowError 재현 결과

> 실행 환경: OpenJDK 21.0.10, macOS (Apple Silicon), G1 GC (기본)

---

## 1. Heap OOM 재현

### 실행 조건

```bash
java -Xmx128m -cp build/classes/java/main com.byunyourim.javalab.week1.memory.day02
```

- 1MB `byte[]`를 `List`에 무한 추가
- Heap 최대 128MB로 제한

### 결과

- **62MB 할당 후 63번째에서 OOM 발생**
- 128MB 중 약 62MB만 사용 가능한 이유: JVM 내부 구조(G1 Region 메타데이터, ArrayList 내부 배열, GC 오버헤드 등)가 힙을 차지

```
62MB 할당 완료
=== OOM 발생 ===
총 할당량: 62MB → 63번째에서 터짐
error class: java.lang.OutOfMemoryError
error message: Java heap space
```

### jstat -gcutil 관찰 결과

```
  S0     S1     E      O      M     CCS    YGC  YGCT  FGC  FGCT  CGC  CGCT    GCT
   -      -   0.00  13.73  66.25  16.38    0   0.000   0  0.000   0  0.000  0.000
   -      -   0.00  26.08  66.25  16.38    0   0.000   0  0.000   0  0.000  0.000
   -      -   0.00  38.42  66.27  16.38    0   0.000   0  0.000   0  0.000  0.000
   -      -   0.00  50.77  66.40  16.38    0   0.000   0  0.000   0  0.000  0.000
   -      -   0.00  63.12  66.68  16.38    0   0.000   0  0.000   0  0.000  0.000
   - 28.12   0.00  63.01  66.92  16.38    1   0.001   0  0.000   2  0.000  0.001
   - 28.12   0.00  73.32  66.92  16.38    1   0.001   0  0.000   2  0.000  0.001
   - 28.12   0.00  83.63  66.92  16.38    1   0.001   0  0.000   2  0.000  0.001
   - 28.12   0.00  93.94  66.92  16.38    1   0.001   0  0.000   2  0.000  0.001
   - 28.12   0.00  99.14  66.92  16.38    1   0.001   0  0.000   2  0.000  0.001
   - 28.12   0.00  99.22  66.92  16.38    1   0.001   0  0.000   2  0.000  0.001
   - 28.12   0.00  99.27  66.92  16.38    1   0.001   0  0.000   2  0.000  0.001
```

- **O (Old Gen)**: 13% → 99%까지 꾸준히 증가
- **S0/S1 (Survivor)**: `-`로 표시 — G1에서는 Survivor를 동적 관리하므로 대부분 비어 있음
- **YGC=1, FGC=0**: Young GC 1회만 발생하고 Old로 직행 (1MB 청크가 Humongous Object이기 때문)

### GC 로그 분석 (`-Xlog:gc*`)

```
GC(0)  Pause Young (Humongous Allocation)  58M→57M(128M)   0.830ms
GC(1)  Concurrent Mark Cycle                                4.806ms

GC(2)  Pause Young (Humongous Allocation)  125M→125M(128M)  ← 회수 실패
GC(3)  Pause Full (Compaction)             125M→125M(128M)  ← Full GC도 실패
GC(4)  Pause Full (Compaction)             125M→125M(128M)  ← 또 실패
...
GC(10) Pause Full (Compaction)             127M→127M(128M)  ← 계속 반복
GC(11) Pause Full (Compaction)             127M→127M(128M)  ← JVM 포기 → OOM

GC(14) Pause Full (Compaction)             127M→1M(8M)      ← catch에서 clear() 후 회수 성공
```

### 왜 질문: OOM이 터질 때 GC는 뭘 하고 있었나?

**GC가 안 돌아서 OOM이 아니라, GC가 미친 듯이 돌았는데 회수할 객체가 없어서 OOM이 터진 것.**

1. 1MB `byte[]`는 G1 Region 크기(1MB)의 50% 이상 → **Humongous Object**로 분류되어 Eden을 건너뛰고 Old에 직접 할당
2. Old가 가득 차면 Young GC 시도 → 회수 불가 (모든 객체가 `List`에서 참조 중)
3. **Full GC를 연속 5~6회** 시도 → 매번 `125M→125M` (회수 대상 없음)
4. JVM이 "더 이상 GC로 해결 불가"로 판단 → `OutOfMemoryError` 던짐
5. catch에서 `chunks.clear()` → 참조 끊김 → `GC(14)`에서 `127M→1M`으로 깔끔하게 회수

---

## 2. StackOverflowError 재현

### 실행 조건

```java
static void recursiveCall(int[] depth) {
    depth[0]++;
    long a = 1, b = 2, c = 3; // 지역변수 3개 → 프레임 크기 증가
    recursiveCall(depth);
}
```

### 결과: -Xss 옵션별 비교

| 옵션 | 스택 크기 | 도달 깊이 | 프레임당 크기 (추정) |
|------|-----------|-----------|---------------------|
| `-Xss256k` | 256 KB | **887** | ~295 bytes |
| `-Xss512k` | 512 KB | **3,666** | ~143 bytes |
| `-Xss1m` | 1,024 KB | **13,132** | ~80 bytes |

- 스택 크기에 비례하여 프레임 수 증가
- 256k에서 프레임당 크기가 더 큰 이유: JVM 내부 오버헤드(스레드 메타데이터 등)가 고정 비용으로 차지하는 비율이 작은 스택에서 더 크기 때문

### 에러 메시지

```
error class: java.lang.StackOverflowError
error message: null
```

- `OutOfMemoryError`와 달리 **message가 null** — JVM이 별도 메시지를 넣지 않음

### 왜 질문: 스택 프레임 하나의 크기는 뭐가 결정하는가?

스택 프레임의 크기를 결정하는 요소:

1. **지역변수 배열 (Local Variable Array)**: 메서드 안의 지역변수 + `this` 참조. `long`/`double`은 2슬롯(16바이트), `int`/`float`/참조는 1슬롯(8바이트)
2. **파라미터**: 지역변수 배열의 앞부분에 포함됨
3. **피연산자 스택 (Operand Stack)**: 연산 중간값을 쌓는 공간. 복잡한 표현식일수록 커짐
4. **프레임 데이터**: return address, 이전 프레임 포인터, constant pool 참조 등 고정 오버헤드

→ 지역변수를 늘리거나 파라미터를 늘리면 프레임이 커지고, 같은 스택 크기에서 재귀 깊이가 줄어든다.

---

## 3. 에러 메시지 비교 정리

| 에러 | 클래스 | 메시지 | 발생 원인 |
|------|--------|--------|-----------|
| Heap OOM | `java.lang.OutOfMemoryError` | `"Java heap space"` | Heap에 객체를 할당할 공간이 없고, GC로도 확보 불가 |
| StackOverflow | `java.lang.StackOverflowError` | `null` | 스레드의 스택 공간이 가득 차서 새 프레임을 push 불가 |

둘 다 `Error`의 하위 클래스 (= `Exception`이 아님). catch 가능하지만, 프로그램이 정상 상태가 아니므로 보통 로깅 후 종료하는 것이 안전하다.

### 주의: catch 블록에서의 메모리

- **Heap OOM**: catch 블록 실행 자체도 메모리가 필요 → `chunks.clear()`로 힙 확보 안 하면 catch 블록도 OOM으로 실패
- **StackOverflow**: catch는 호출 스택이 풀리면서(unwinding) 프레임이 해제되므로 정상 실행 가능
