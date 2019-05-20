package com.github.aztorius.confuzzion;

public class Repro {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: ./path/to/class/TestX.class");
        }
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            Class<?> clazz = loader.loadClass(args[0]);
            clazz.newInstance();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
