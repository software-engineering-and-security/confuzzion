package com.github.aztorius.confuzzion;

import java.util.List;

public abstract class ProgramMutation extends Mutation {
    protected Program program;

    protected ProgramMutation(RandomGenerator rand, Program program) {
        super(rand);
        this.program = program;
    }
}
