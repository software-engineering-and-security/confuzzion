package com.github.aztorius.confuzzion;

import soot.SootClass;

public class AddClassMutation extends ProgramMutation {
    private Mutant addedClass;

    public AddClassMutation(RandomGenerator rand, Program program) {
        super(rand, program);

        addedClass = this.program.genNewClass();
    }

    @Override
    public void undo() {
        this.program.removeClass(addedClass);
    }

    @Override
    public void randomConstants() {
        // Nothing to do
    }
}
