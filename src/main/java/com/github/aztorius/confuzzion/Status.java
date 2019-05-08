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

    public Status() {
        this.mutations = new ArrayList<Class<?>>();
        successMutations = new ArrayList<Long>();
        failedMutations = new ArrayList<Long>();
        contractViolations = new ArrayList<Long>();
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

    public synchronized void run() {
        System.out.print(this.toString());
        System.out.flush();
    }

    @Override
    public String toString() {
        totalMutations += mutationsFromLastSecond;
        totalExecutions += executionsFromLastSecond;

        String template =
            "\033[H\033[2J" +
            "Confuzzion :\n" +
            "%d total execs | %d total mutations\n" +
            "%d execs/s | %d mutations/s\n";
        String str = String.format(template,
            totalExecutions,
            totalMutations,
            executionsFromLastSecond,
            mutationsFromLastSecond);
        for (int i = 0; i < mutations.size(); i++) {
            str += String.format("%s | %d | %d | %d%n",
                    mutations.get(i).getName(),
                    successMutations.get(i),
                    failedMutations.get(i),
                    contractViolations.get(i));
        }

        mutationsFromLastSecond = 0;
        executionsFromLastSecond = 0;
        return str;
    }
}
