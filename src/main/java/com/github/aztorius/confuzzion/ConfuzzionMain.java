package com.github.aztorius.confuzzion;

import java.lang.IllegalAccessException;
import java.lang.InstantiationException;
import java.lang.NoSuchMethodException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class ConfuzzionMain {
    public static void main(String args[]) {
        //TODO: add support for verbose option
        if (args.length < 1 || args.length > 3) {
            ConfuzzionMain.printHelp();
        } else if (args[0].equals("mutate")) {
            long const_loop_iterations = 10;
            long main_loop_iterations = 10;
            if (args.length == 3) {
                try {
                    const_loop_iterations = Long.parseLong(args[2]);
                } catch(NumberFormatException e) {
                    ConfuzzionMain.printHelp();
                    return;
                }
            }
            if (args.length >= 2) {
                try {
                    main_loop_iterations = Long.parseLong(args[1]);
                } catch(NumberFormatException e) {
                    ConfuzzionMain.printHelp();
                    return;
                }
            }
            ConfuzzionMain conf = new ConfuzzionMain();
            conf.startMutation(const_loop_iterations, main_loop_iterations, true);
        } else if (args[0].equals("generate")) {
            long main_loop_iterations = 10;
            ConfuzzionMain conf = new ConfuzzionMain();
            conf.startGeneration(main_loop_iterations, true);
        } else {
            ConfuzzionMain.printHelp();
        }
    }

    private static void printHelp() {
        System.err.println(
            "Usage: mutate [MAIN_LOOP_ITERATIONS] [CONST_LOOP_ITERATIONS]\n" +
            "       generate [MAIN_LOOP_ITERATIONS]"
        );
    }

    public void startGeneration(long mainloop_turn, boolean verbose) {
        //TODO
        // RandomGenerator rand = new RandomGenerator();
        // MutantGenerator generator = new MutantGenerator(rand, "Test");
        // ArrayList<Contract> contracts = new ArrayList<Contract>();
        // contracts.add(new ContractTypeConfusion());
        //
        // for (long loop1 = 0; loop1 < mainloop_turn; loop1++) {
        //     generator.generate();
        //     Mutant mutant = generator.addContractsChecks(contracts);
        // }
    }

    public void startMutation(long constants_retry, long mainloop_turn, boolean verbose) {
        RandomGenerator rand = new RandomGenerator();
        ArrayList<Contract> contracts = new ArrayList<Contract>();
        contracts.add(new ContractTypeConfusion());

        Program currentProg = new Program("Test", rand);

        for (long loop1 = 0; loop1 < mainloop_turn || mainloop_turn < 0; loop1++) {
            Mutation mutation = null;

            try {
                // Random mutation (program level | class level | method level)
                mutation = currentProg.randomMutation();
            } catch (MutationException e) {
                if (verbose) {
                    e.printStackTrace();
                }
                continue;
            }

            for (long loop2 = 0; loop2 < constants_retry || constants_retry < 0; loop2++) {
                // Change constants in mutation units taken from a pool
                mutation.randomConstants();
                try {
                    if (verbose) {
                        System.err.println(
                            "===Program Test: iter1 " + loop1 +
                            " iter2 " + loop2 + "===");
                    }
                    // Instantiation and launch
                    currentProg.genAndLaunch(verbose);
                    // Continue if no exception else try other constants
                    break;
                } catch(Throwable e) {
                    e.printStackTrace();
                }
            }
            // Add contracts checks
            ArrayList<BodyMutation> contractsMutations =
                currentProg.addContractsChecks(contracts, mutation);
            try {
                // Instantiation and launch
                currentProg.genAndLaunch(verbose);
                // Remove contracts checks for next turn
                currentProg.removeContractsChecks(contractsMutations);
            } catch(ContractCheckException e) {
                // TODO: add the program source code to the result list
                // save the resulting class file ?
                // Remove contracts checks
                currentProg.removeContractsChecks(contractsMutations);
                e.printStackTrace();
            } catch(Throwable e) {
                // Remove contracts checks
                currentProg.removeContractsChecks(contractsMutations);
                // Bad sample, revert mutation
                mutation.undo();
                e.printStackTrace();
            }
        }
    }
}
