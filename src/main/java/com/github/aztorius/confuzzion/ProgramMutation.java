package com.github.aztorius.confuzzion;

import java.util.List;

/**
 * Class ProgramMutation describes a mutation on a Program
 */
public abstract class ProgramMutation extends Mutation {
    protected Program program;

    /**
     * Constructor
     * @param rand    the RandomGenerator to use
     * @param program the Program to mutate
     */
    protected ProgramMutation(RandomGenerator rand, Program program) {
        super(rand);
        this.program = program;
    }
}
