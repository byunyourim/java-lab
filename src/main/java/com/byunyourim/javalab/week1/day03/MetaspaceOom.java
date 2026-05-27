package com.byunyourim.javalab.week1.day03;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Metaspace OOM 재현 실험
 *
 * 실행 옵션:
 *   -XX:MaxMetaspaceSize=64m -XX:+PrintGCDetails
 *
 * ByteBuddy로 런타임에 클래스를 1만 개 이상 동적 생성하여
 * Metaspace 영역이 소진되면 OutOfMemoryError: Metaspace 발생.
 *
 * ── 왜 Java 8에서 PermGen → Metaspace로 바꿨는가? ──
 *
 * PermGen 문제점:
 *   1) 고정 크기(-XX:MaxPermSize)로 설정해야 해서 예측이 어렵고,
 *      적으면 OOM, 크면 메모리 낭비.
 *   2) String.intern()도 PermGen에 저장 → 런타임 문자열이 많으면 PermGen이 터짐.
 *   3) 클래스 메타데이터가 Java Heap 안(PermGen)에 있어서 GC 복잡도 증가.
 *
 * Metaspace 개선점:
 *   1) Native Memory 사용 → OS가 허용하는 만큼 자동 확장 가능.
 *   2) MaxMetaspaceSize를 설정하지 않으면 물리 메모리까지 사용 가능.
 *   3) String.intern()은 Heap으로 이동 → Metaspace 부담 감소.
 *   4) 클래스별 메타데이터를 Chunk 단위로 관리 → ClassLoader 해제 시 일괄 반환.
 *
 * ── 클래스 언로딩은 언제 일어나는가? ──
 *
 *   클래스 언로딩 조건 (세 가지 모두 충족해야 함):
 *     1) 해당 클래스의 모든 인스턴스가 GC됨
 *     2) 해당 클래스를 로드한 ClassLoader가 GC됨
 *     3) 해당 Class 객체에 대한 참조가 없음 (리플렉션 등)
 *
 *   GC와의 관계:
 *     - Full GC 시점에 Metaspace 정리가 발생
 *     - G1GC에서는 Concurrent Cycle의 cleanup 단계에서도 가능
 *     - -XX:MaxMetaspaceSize 도달 시 Metaspace GC가 트리거됨
 *     - 시스템 ClassLoader가 로드한 클래스(java.lang.*, 등)는 JVM 종료까지 언로딩 불가
 *
 *   이 실험에서 클래스가 언로딩되지 않는 이유:
 *     ClassLoadingStrategy.Default.WRAPPER는 매번 새 ClassLoader를 만들지만,
 *     생성된 Class 객체를 classes 리스트가 참조하고 있으므로
 *     ClassLoader가 GC 대상이 될 수 없음 → 클래스 언로딩 불가 → Metaspace 계속 증가
 */
public class MetaspaceOom {

    public static void main(String[] args) {
        System.out.println("=== Metaspace OOM 재현 실험 ===");
        System.out.println("PID: " + ProcessHandle.current().pid());
        System.out.println("jstat -gc <PID> 1000 으로 MC/MU 컬럼 관찰");
        System.out.println("실행 옵션: -XX:MaxMetaspaceSize=64m");
        System.out.println();
        sleep(3000);

        List<Class<?>> classes = new ArrayList<>();
        int count = 0;
        try {
            while (true) {
                DynamicType.Unloaded<?> unloaded = new ByteBuddy()
                        .subclass(Object.class)
                        .name("com.generated.DynClass_" + count)
                        .make();

                Class<?> loaded = unloaded.load(
                        MetaspaceOom.class.getClassLoader(),
                        ClassLoadingStrategy.Default.WRAPPER
                ).getLoaded();

                classes.add(loaded);
                count++;
                if (count % 500 == 0) {
                    Runtime rt = Runtime.getRuntime();
                    System.out.printf("[%,d개 생성] Heap used: %,dKB / Heap max: %,dKB%n",
                            count,
                            (rt.totalMemory() - rt.freeMemory()) / 1024,
                            rt.maxMemory() / 1024);
                }
            }
        } catch (OutOfMemoryError e) {
            classes.clear();
            classes = null;

            System.out.println();
            System.out.println("=== OOM 발생! ===");
            System.out.println("생성된 클래스 수: " + count);
            System.out.println("에러 타입: " + e.getClass().getName());
            System.out.println("에러 메시지: " + e.getMessage());
            System.out.println();
            System.out.println("── 분석 ──");
            System.out.println("각 클래스의 메타데이터(클래스 이름, 메서드 테이블, 상수 풀 등)가");
            System.out.println("Metaspace(Native Memory)에 저장됨.");
            System.out.println("MaxMetaspaceSize=64m 제한에 도달하여 OOM 발생.");
        }
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
