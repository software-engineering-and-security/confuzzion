package com.github.aztorius.confuzzion;

enum MutationType {
    ADD, CHANGE, REMOVE;
}

public interface Mutation {
    public abstract void apply(Mutant mut, RandomGenerator rand, MutationType mtype);
}
