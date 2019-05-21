package com.github.aztorius.confuzzion;

import java.util.ArrayList;
import java.util.TimerTask;

public class Status extends TimerTask {
    private long totalMutations;
    private long totalExecutions;
    private long mutationsFromLastSecond;
    private long executionsFromLastSecond;
    private ArrayList<Long> successMutations;
    private ArrayList<Long> failedMutations;
    private ArrayList<Long> contractViolations;
    private ArrayList<Class<?>> mutations;
    private boolean stalled;
    private int mutationsStackSize;
    private long time;

    private static String template =
        "\033[H\033[2J" +
        "Confuzzion%n%n" +
        "            %4d:%02d:%02d |%n" +
        "%10d total execs | %10d total mutations%n" +
        "%10d     execs/s | %10d     mutations/s%n" +
        "               %7s | %10d    stacked muts%n%n" +
        "       Mutation type |    Success |      Fails | Violations |%n";

    public Status() {
        this.mutations = new ArrayList<Class<?>>();
        successMutations = new ArrayList<Long>();
        failedMutations = new ArrayList<Long>();
        contractViolations = new ArrayList<Long>();
        stalled = false;
        mutationsStackSize = 0;
        time = 0;
    }

    public synchronized void newMutation(Class<?> mutation,
            boolean success,
            boolean violates,
            long numberOfExecutions) {
        int index = mutations.indexOf(mutation);
        if (index < 0) {
            index = mutations.size();
            mutations.add(mutation);
            successMutations.add(0L);
            failedMutations.add(0L);
            contractViolations.add(0L);
        }

        mutationsFromLastSecond++;
        executionsFromLastSecond += numberOfExecutions;

        if (success) {
            Long lSuccess = successMutations.get(index);
            lSuccess++;
            successMutations.set(index, lSuccess);
            mutationsStackSize++;
        } else {
            Long lFailed = failedMutations.get(index);
            lFailed++;
            failedMutations.set(index, lFailed);
        }
        if (violates) {
            Long lViolations = contractViolations.get(index);
            lViolations++;
            contractViolations.set(index, lViolations);
        }
    }

    public synchronized boolean isStalled() {
        boolean stalledValue = stalled;
        stalled = false;
        return stalledValue;
    }

    public synchronized void newStackSize(int size) {
        mutationsStackSize = size;
    }

    public synchronized void run() {
        System.out.print(this.toString());
    }

    @Override
    public String toString() {
        totalMutations += mutationsFromLastSecond;
        totalExecutions += executionsFromLastSecond;
        if (mutationsFromLastSecond == 0) {
            stalled = true;
        }
        time++;
        String str = String.format(Status.template,
            time / 3600,
            (time % 3600) / 60,
            time % 60,
            totalExecutions,
            totalMutations,
            executionsFromLastSecond,
            mutationsFromLastSecond,
            stalled ? "STALLED" : "",
            mutationsStackSize);
        for (int i = 0; i < mutations.size(); i++) {
            str += String.format("%20s | %10d | %10d | %10d |%n",
                    mutations.get(i).getSimpleName(),
                    successMutations.get(i),
                    failedMutations.get(i),
                    contractViolations.get(i));
        }

        mutationsFromLastSecond = 0;
        executionsFromLastSecond = 0;
        return str;
    }
}
