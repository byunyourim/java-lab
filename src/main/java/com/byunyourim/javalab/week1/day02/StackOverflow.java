package com.byunyourim.javalab.week1.day02;

public class StackOverflow {

    public static void main(String[] args) {
        int[] depth = {0};
        try {
            recursiveCall(depth);
        } catch (StackOverflowError e) {
            System.out.println("=== StackOverflow 발생 ===");
            System.out.println("도달 깊이: " + depth[0]);
            System.out.println("error class: " + e.getClass().getName());
            System.out.println("error message: " + e.getMessage());
        }
    }

    static void recursiveCall(int[] depth) {
        depth[0]++;
        long a = 1, b = 2, c = 3;
        recursiveCall(depth);
    }
}
