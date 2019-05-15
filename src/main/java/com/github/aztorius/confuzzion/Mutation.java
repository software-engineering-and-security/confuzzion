package com.github.aztorius.confuzzion;

/**
 * Class Mutation describes a change
 */
public abstract class Mutation {
    protected RandomGenerator rand;

    /**
     * Constructor
     * @param rand the RandomGenerator to use
     */
    protected Mutation(RandomGenerator rand) {
        this.rand = rand;
    }

    /**
     * Revert mutation
     */
    public abstract void undo();

    /**
     * Change added constants
     */
    public abstract void randomConstants();
}
