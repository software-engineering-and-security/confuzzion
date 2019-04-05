package com.github.aztorius.confuzzion;

public class GenerationResult {
    private Class<?> clazz;

    public GenerationResult(Class<?> clazz) {
        this.clazz = clazz;
    }

    public Class<?> getSootClass() {
        return clazz;
    }
}
