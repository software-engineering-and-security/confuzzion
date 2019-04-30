package com.github.aztorius.confuzzion;

public abstract class Mutation {
    protected RandomGenerator rand;

    protected Mutation(RandomGenerator rand) {
        this.rand = rand;
    }

    public abstract void undo();

    public abstract void randomConstants();
}
