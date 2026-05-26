package com.byunyourim.javalab.week1.day02;

import java.util.*;

public class HeapOom {

    public static void main(String[] args) {
        System.out.println("PID: " + ProcessHandle.current().pid());
        System.out.println("jstat 붙일 시간 3초 대기...");
        sleep(3000);

        List<byte[]> chunks = new ArrayList<>();
        int count = 0;
        try {
            while (true) {
                chunks.add(new byte[1024 * 1024]); // 1MB
                count++;
                System.out.println(count + "MB 할당 완료");
                sleep(200);
            }
        } catch (OutOfMemoryError e) {
            chunks.clear();
            chunks = null;
            System.out.println("=== OOM 발생 ===");
            System.out.println("총 할당량: " + count + "MB");
            System.out.println("error class: " + e.getClass().getName());
            System.out.println("error message: " + e.getMessage());
        }
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
