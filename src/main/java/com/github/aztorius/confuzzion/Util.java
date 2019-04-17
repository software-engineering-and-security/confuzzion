package com.github.aztorius.confuzzion;

import soot.Local;
import soot.util.Chain;

public class Util {
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
}
