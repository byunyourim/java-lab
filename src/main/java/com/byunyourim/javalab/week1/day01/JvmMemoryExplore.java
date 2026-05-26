package com.byunyourim.javalab.week1.day01;

public class JvmMemoryExplore {

    public static void main(String[] args) {

        // 1. new Object() → Heap에 객체 할당, Stack에 참조 저장
        Object obj = new Object();
        System.out.println("obj class: " + obj.getClass().getName());
        System.out.println("obj hashCode: " + System.identityHashCode(obj));

        // 2. int vs Integer 저장 위치
        int primitiveX = 10;        // Stack (지역변수 슬롯에 값 자체 저장)
        Integer boxedX = 10;        // Heap에 Integer 객체, Stack에 참조
        Integer boxedY = 10;        // Integer 캐시(-128~127) → 같은 객체 재사용
        Integer boxedZ = 200;       // 캐시 범위 밖 → 새 객체 생성

        System.out.println("\n=== int vs Integer ===");
        System.out.println("primitiveX = " + primitiveX + " (Stack에 값 직접 저장)");
        System.out.println("boxedX == boxedY ? " + (boxedX == boxedY) + " (캐시 범위 내 → 같은 객체)");
        System.out.println("boxedX == boxedZ ? " + (boxedX == boxedZ) + " (값이 다르므로 다른 객체)");

        Integer a = 200;
        Integer b = 200;
        System.out.println("Integer(200) == Integer(200) ? " + (a == b) + " (캐시 범위 밖 → 다른 객체)");

        // 3. String 리터럴 vs new String
        String literal1 = "hello";              // String Pool (Heap 내부)
        String literal2 = "hello";              // 같은 Pool 참조
        String newStr = new String("hello");    // Heap에 새 객체 생성

        System.out.println("\n=== String 리터럴 vs new String ===");
        System.out.println("literal1 == literal2 ? " + (literal1 == literal2) + " (같은 String Pool 참조)");
        System.out.println("literal1 == newStr   ? " + (literal1 == newStr) + " (Pool vs Heap 별도 객체)");
        System.out.println("literal1.equals(newStr) ? " + literal1.equals(newStr) + " (값은 동일)");
        System.out.println("newStr.intern() == literal1 ? " + (newStr.intern() == literal1) + " (intern → Pool 참조 반환)");

        // 4. Heap 공유 vs Stack 스레드별 확인
        int[] shared = {0}; // Heap에 배열 객체 → 스레드 간 공유 가능

        System.out.println("\n=== Heap 공유 vs Stack 스레드별 ===");
        Thread t1 = new Thread(() -> {
            shared[0]++;
            int localVar = 100; // 이 스레드의 Stack에만 존재
            System.out.println("[T1] shared=" + shared[0] + ", localVar=" + localVar
                    + ", thread=" + Thread.currentThread().getName());
        }, "worker-1");

        Thread t2 = new Thread(() -> {
            shared[0]++;
            int localVar = 200; // 이 스레드의 Stack에만 존재 (T1의 localVar와 무관)
            System.out.println("[T2] shared=" + shared[0] + ", localVar=" + localVar
                    + ", thread=" + Thread.currentThread().getName());
        }, "worker-2");

        t1.start();
        t2.start();
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("[Main] shared 최종값=" + shared[0] + " (두 스레드가 Heap 객체를 공유)");

        // 5. Runtime 메모리 정보
        Runtime rt = Runtime.getRuntime();
        System.out.println("\n=== JVM 메모리 현황 ===");
        System.out.println("Max Heap   : " + rt.maxMemory() / 1024 / 1024 + " MB");
        System.out.println("Total Heap : " + rt.totalMemory() / 1024 / 1024 + " MB");
        System.out.println("Free Heap  : " + rt.freeMemory() / 1024 / 1024 + " MB");
        System.out.println("Used Heap  : " + (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024 + " MB");
    }
}
