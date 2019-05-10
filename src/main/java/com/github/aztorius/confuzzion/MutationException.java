package com.github.aztorius.confuzzion;

public class MutationException extends Exception {
    private Class<?> mutationClass;

    public MutationException(Class<?> mutationClass, String reason) {
        super(reason);
        this.mutationClass = mutationClass;
    }

    public Class<?> getMutationClass() {
        return mutationClass;
    }
}
