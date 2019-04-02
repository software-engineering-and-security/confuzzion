package com.github.aztorius.confuzzion;

public class GenerationResult {
    private Mutant mut;

    public GenerationResult(Mutant mut) {
        this.mut = mut;
    }

    public Mutant getMutant() {
        return mut;
    }
}
