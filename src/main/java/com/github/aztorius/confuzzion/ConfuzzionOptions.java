package com.github.aztorius.confuzzion;

public class ConfuzzionOptions {
    private static final ConfuzzionOptions instance = new ConfuzzionOptions();

    public volatile boolean allow_unsafe_assignment;
    public volatile boolean use_jasmin_backend;
    public volatile int java_version;

    private ConfuzzionOptions() {
        allow_unsafe_assignment = false;
        use_jasmin_backend = false;
        java_version = soot.options.Options.java_version_default;
    }

    public static ConfuzzionOptions v() {
        return instance;
    }
}
