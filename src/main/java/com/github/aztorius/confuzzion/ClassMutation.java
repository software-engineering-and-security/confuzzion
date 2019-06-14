package com.github.aztorius.confuzzion;

import soot.SootClass;

public abstract class ClassMutation extends Mutation {
    protected SootClass sootClass;

    protected ClassMutation(RandomGenerator rand, SootClass sootClass) {
        super(rand);
        this.sootClass = sootClass;
    }

    public SootClass getSootClass() {
        return sootClass;
    }
}
