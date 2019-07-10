package confuzzion;

public class ConfuzzionOptions {
    private static final ConfuzzionOptions instance = new ConfuzzionOptions();

    public volatile boolean allow_unsafe_assignment;
    public volatile boolean use_jasmin_backend;
    public volatile int java_version;
    public volatile int class_number_limit;
    public volatile boolean fixed_number_of_classes;
    public volatile boolean use_uniform_distribution_for_methods;

    private ConfuzzionOptions() {
        allow_unsafe_assignment = false;
        use_jasmin_backend = false;
        java_version = soot.options.Options.java_version_default;
        class_number_limit = 3;
        fixed_number_of_classes = true;
        use_uniform_distribution_for_methods = false;
    }

    public static ConfuzzionOptions v() {
        return instance;
    }
}
