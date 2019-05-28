package com.github.aztorius.confuzzion;

import java.util.HashMap;

public class Util {
    private static HashMap<String, String> childMap;

    static {
        childMap = new HashMap<String, String>();
        childMap.put("java.util.concurrent.BlockingQueue",
                     "java.util.concurrent.ArrayBlockingQueue");
    }

    public static String abstractToConcrete(String className) {
        return childMap.get(className);
    }

    public static Throwable getCause(Throwable e) {
        Throwable cause = null;
        Throwable result = e;

        while((cause = result.getCause()) != null && (result != cause)) {
            result = cause;
        }
        return result;
    }
}
