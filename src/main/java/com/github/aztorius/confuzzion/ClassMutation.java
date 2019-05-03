package com.github.aztorius.confuzzion;

import soot.SootClass;

import java.util.List;

public abstract class ClassMutation extends Mutation {
    protected SootClass sootClass;

    protected ClassMutation(RandomGenerator rand, SootClass sootClass) {
        super(rand);
        this.sootClass = sootClass;
    }
}
