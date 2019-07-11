package confuzzion;

import soot.SootMethod;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;

public class MethodComplexity {
    private final static double inverseConfidence = 0.05;

    private SootMethod method;
    private double failureRate;
    private long all;
    private long failures;
    private double probability;

    public MethodComplexity(SootMethod method) {
        this.method = method;
        failureRate = 1.0;
        all = 1;
        failures = 0;
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
        double score = failureRate;
        double degreeOfFreedom = 2 * (failures + 1);
        ChiSquaredDistribution chi2 = new ChiSquaredDistribution(null, degreeOfFreedom);
        score += ((chi2.inverseCumulativeProbability(inverseConfidence) / 2.0) / (double)all);
        return score;
    }

    public void setProbability(double p) {
        probability = p;
    }

    public double getProbability() {
        return probability;
    }
}
