package com.github.aztorius.confuzzion;

import soot.Body;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.UnitPatchingChain;
import soot.util.Chain;

import java.util.ArrayList;

public class CallMethodMutation extends MethodMutation {
    public CallMethodMutation(RandomGenerator rand, SootMethod method) {
        super(rand, method);
    }
}
