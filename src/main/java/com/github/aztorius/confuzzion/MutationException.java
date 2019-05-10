package com.github.aztorius.confuzzion;

public class MutationException extends Exception {
    private Class<?> mutationClass;
    private BodyMutation mutation;

    public MutationException(Class<?> mutationClass, String reason) {
        super(reason);
        this.mutationClass = mutationClass;
        this.mutation = null;
    }

    public MutationException(Class<?> mutationClass,
            BodyMutation mutation, String reason) {
        this(mutationClass, reason);
        this.mutation = mutation;
    }

    public Class<?> getMutationClass() {
        return mutationClass;
    }

    public void undoMutation() {
        if (mutation != null) {
            mutation.undo();
        }
    }
}
