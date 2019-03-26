package com.github.aztorius.confuzzion;

public class GenerationResult {
    private Class<?> clazz;
    private Mutant mutant;

    public GenerationResult() {
        this.clazz = null;
    }

    public GenerationResult(Class<?> clazz) {
        this.clazz = clazz;
    }

    public GenerationResult(Mutant mutant) {
        this.mutant = mutant;
    }

    public Class<?> getClassResult() {
        return clazz;
    }

    public Mutant getMutant() {
        return mutant;
    }
}
