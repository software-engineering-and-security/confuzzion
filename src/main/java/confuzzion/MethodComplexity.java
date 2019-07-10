package confuzzion;

import soot.SootMethod;

public class MethodComplexity {
    private SootMethod method;
    private double failureRate;
    private long all;
    private long failures;

    public MethodComplexity(SootMethod method) {
        this.method = method;
        failureRate = 1.0;
        all = 0;
        failures = 0;
    }

    public SootMethod getMethod() {
        return method;
    }

    public void newSuccess() {
        all++;
        failureRate = (double)failures / all;
    }

    public void newFailure() {
        failures++;
        all++;
        failureRate = (double)failures / all;
    }

    public double getFailureRate() {
        return failureRate;
    }
}
