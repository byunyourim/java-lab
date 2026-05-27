package com.byunyourim.javalab.week1.day03;

import java.util.*;

/**
 * GC 기초 실험 — Minor / Major / Full GC 관찰
 *
 * 실행 옵션:
 *   -Xms128m -Xmx128m -Xlog:gc*  (Java 21)
 *   또는 -verbose:gc -XX:+PrintGCDetails (Java 8)
 *
 * ── Mark-Sweep-Compact 알고리즘 ──
 *
 *   1) Mark   : GC Root(스택 변수, static 변수, JNI 참조)에서 시작해
 *               참조 그래프를 따라가며 "살아있는 객체"를 표시
 *   2) Sweep  : 표시되지 않은 객체의 메모리를 해제
 *   3) Compact: 살아남은 객체를 메모리 한쪽으로 밀어서 단편화 제거
 *               → 새 객체 할당 시 bump-the-pointer 방식으로 빠르게 할당 가능
 *
 * ── Minor GC vs Major GC vs Full GC ──
 *
 *   Minor GC:
 *     - Young Generation(Eden + Survivor)만 수집
 *     - Eden이 가득 차면 발생
 *     - 대부분의 객체가 금방 죽으므로(Weak Generational Hypothesis) 매우 빠름
 *     - Stop-the-World 시간: 보통 수 ms
 *
 *   Major GC:
 *     - Old Generation만 수집
 *     - CMS, G1의 Old 영역 수집 시 사용하는 용어
 *     - Minor GC보다 오래 걸림
 *
 *   Full GC:
 *     - Young + Old + Metaspace 전체 수집
 *     - System.gc() 호출, Old 영역 부족, Metaspace 부족 시 발생
 *     - 가장 긴 STW(Stop-the-World) → 운영 환경에서 피해야 함
 *
 * ── 왜 Young 영역을 Eden + Survivor 두 개로 나눴는가? ──
 *
 *   복사 수집기(Copying Collector)의 원리:
 *     - 메모리를 두 영역(from, to)으로 나누고, 살아남은 객체만 to로 복사
 *     - 장점: 단편화 없음, 할당이 빠름(bump pointer)
 *     - 단점: 메모리 절반만 사용 가능
 *
 *   Eden + Survivor(S0, S1)로 개선:
 *     - 대부분 객체는 Eden에서 한 번의 Minor GC 안에 죽음 (IBM 연구: 98%)
 *     - 따라서 Eden을 크게, Survivor를 작게 설정 (기본 비율 8:1:1)
 *     - Minor GC 시: Eden + 현재 Survivor에서 살아남은 객체 → 반대쪽 Survivor로 복사
 *     - 결과: 메모리 낭비가 전체의 10%(Survivor 하나)로 줄어듦
 *             cf. 순수 복사 수집기는 50% 낭비
 *
 * ── 객체가 Old로 넘어가는 조건 ──
 *
 *   1) Age 임계값 도달:
 *      - 객체가 Minor GC에서 살아남을 때마다 age +1
 *      - age가 MaxTenuringThreshold(기본 15)에 도달하면 Old로 승격
 *      - -XX:MaxTenuringThreshold=N 으로 조정 가능
 *
 *   2) 동적 나이 판단 (Dynamic Age):
 *      - Survivor 영역에서 같은 age 이하의 객체 크기 합이
 *        Survivor 공간의 50%(-XX:TargetSurvivorRatio)를 넘으면
 *        그 age 이상의 객체를 조기 승격
 *
 *   3) 대형 객체 직접 할당:
 *      - Eden에 넣기엔 너무 큰 객체 → 바로 Old 영역에 할당
 *      - G1GC에서는 Region 크기의 50% 이상이면 Humongous Region에 할당
 *
 *   4) Survivor 공간 부족:
 *      - Minor GC 후 살아남은 객체가 Survivor에 안 들어가면 Old로 직접 이동
 */
public class GcBasic {

    public static void main(String[] args) {
        System.out.println("=== GC 기초 실험 ===");
        System.out.println("PID: " + ProcessHandle.current().pid());
        System.out.println("실행 옵션: -Xms128m -Xmx128m -Xlog:gc*");
        System.out.println();

        System.out.println("── 1단계: Minor GC 유발 (단명 객체 대량 생성) ──");
        triggerMinorGc();

        System.out.println();
        System.out.println("── 2단계: 객체 승격 관찰 (장수 객체 유지) ──");
        List<byte[]> tenured = observePromotion();

        System.out.println();
        System.out.println("── 3단계: Full GC 유발 (Old 영역 압박) ──");
        triggerFullGc(tenured);

        System.out.println();
        System.out.println("=== 실험 종료 ===");
    }

    /**
     * Eden을 반복 채워서 Minor GC를 여러 번 유발.
     * 참조를 유지하지 않으므로 대부분 Eden에서 즉시 수거됨.
     */
    static void triggerMinorGc() {
        for (int i = 0; i < 30; i++) {
            byte[] garbage = new byte[4 * 1024 * 1024]; // 4MB, 곧바로 GC 대상
        }
        System.out.println("  → 단명 객체 30개(총 120MB) 생성 완료. GC 로그에서 Minor GC 확인.");
    }

    /**
     * 객체를 리스트에 유지하면서 Minor GC를 반복 유발.
     * MaxTenuringThreshold를 넘은 객체가 Old 영역으로 승격되는 과정을 관찰.
     */
    static List<byte[]> observePromotion() {
        List<byte[]> survivors = new ArrayList<>();

        // 장수할 객체 5개 (각 1MB) — Survivor를 거쳐 Old로 승격될 예정
        for (int i = 0; i < 5; i++) {
            survivors.add(new byte[1024 * 1024]);
        }
        System.out.println("  → 장수 객체 5개(5MB) 생성. 이 객체들은 GC에서 살아남아 age 증가.");

        // Minor GC를 여러 번 유발하여 age 증가 → 승격
        for (int i = 0; i < 20; i++) {
            byte[] temp = new byte[4 * 1024 * 1024];
        }
        System.out.println("  → Minor GC 반복 유발. -Xlog:gc*에서 'Pause Young' 확인.");
        System.out.println("  → survivors 객체의 age가 증가하여 Old 영역으로 승격됨.");

        return survivors;
    }

    /**
     * Old 영역을 가득 채워 Full GC를 유발.
     */
    static void triggerFullGc(List<byte[]> existing) {
        List<byte[]> pressure = new ArrayList<>();
        int count = 0;
        try {
            while (true) {
                pressure.add(new byte[1024 * 1024]); // 1MB씩 추가
                count++;
            }
        } catch (OutOfMemoryError e) {
            pressure.clear();
            pressure = null;
            existing.clear();
            System.out.println("  → Old 영역 소진. " + count + "MB 할당 후 OOM.");
            System.out.println("  → GC 로그에서 'Full GC' 또는 'Pause Full' 확인.");
            System.out.println("  → Full GC는 Young + Old + Metaspace 전체를 수집.");
        }
    }
}
