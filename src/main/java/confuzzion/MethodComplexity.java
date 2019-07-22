package confuzzion;

import soot.SootMethod;

import org.apache.commons.math3.distribution.BinomialDistribution;

public class MethodComplexity {
    private final static double confidence = 0.95;

    private SootMethod method;
    private double failureRate;
    private long all;
    private long failures;
    private double probability;

    public MethodComplexity(SootMethod method) {
        this.method = method;
        failureRate = 1.0;
        all = 1;
        failures = 1;
        probability = 0.0;
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

    public double getScore() {
        BinomialDistribution bin = new BinomialDistribution((int) all, failureRate);
        double score = ((double)bin.inverseCumulativeProbability(confidence) / all);
        return score;
    }

    public void setProbability(double p) {
        probability = p;
    }

    public double getProbability() {
        return probability;
    }
}
