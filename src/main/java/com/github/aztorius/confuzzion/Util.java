package com.github.aztorius.confuzzion;

import soot.Local;
import soot.RefType;
import soot.util.Chain;

import java.util.ArrayList;
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

    public static Local randomLocal(Chain<Local> locals, RandomGenerator rand) {
        if (locals.size() <= 0) {
            return null;
        }
        int choice = rand.nextUint(locals.size());
        for (Local loc : locals) {
            if (choice <= 0) {
                return loc;
            }
            choice--;
        }
        return null;
    }

    public static Local randomLocalRef(Chain<Local> locals, RandomGenerator rand) {
        ArrayList<Local> localRefs = new ArrayList<Local>();
        for (Local loc : locals) {
            if (RefType.class.isInstance(loc.getType())) {
                localRefs.add(loc);
            }
        }
        if (localRefs.size() <= 0) {
            return null;
        }
        int choice = rand.nextUint(localRefs.size());
        return localRefs.get(choice);
    }
}
