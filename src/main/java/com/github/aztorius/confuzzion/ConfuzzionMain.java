package com.github.aztorius.confuzzion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Timer;

public class ConfuzzionMain {
    private Path resultFolder;

    private static long CONST_LOOP_ITERATIONS = 10;
    private static long MAIN_LOOP_ITERATIONS = 1000;

    public ConfuzzionMain(Path resultFolder) {
        this.resultFolder = resultFolder;
    }

    public static void main(String args[]) {
        if (args.length < 1) {
            ConfuzzionMain.printHelp();
        } else if (args[0].equals("mut")) {
            int iterArgs = 1;
            long const_loop_iterations = ConfuzzionMain.CONST_LOOP_ITERATIONS;
            long main_loop_iterations = ConfuzzionMain.MAIN_LOOP_ITERATIONS;

            String main_loop_iterations_str =
                ConfuzzionMain.parseOption(args, "-m");
            if (main_loop_iterations_str != null) {
                main_loop_iterations = Long.parseLong(main_loop_iterations_str);
            }

            String const_loop_iterations_str =
                ConfuzzionMain.parseOption(args, "-c");
            if (const_loop_iterations_str != null) {
                const_loop_iterations = Long.parseLong(const_loop_iterations_str);
            }

            boolean verbose = ConfuzzionMain.parseVerbose(args);
            Path resultFolder = ConfuzzionMain.parseDirectory(args);
            ConfuzzionMain conf = new ConfuzzionMain(resultFolder);
            conf.startMutation(const_loop_iterations,
                main_loop_iterations,
                verbose);
        } else if (args[0].equals("gen")) {
            long main_loop_iterations = 10;
            String main_loop_iterations_str =
                ConfuzzionMain.parseOption(args, "-m");
            if (main_loop_iterations_str != null) {
                main_loop_iterations = Long.parseLong(main_loop_iterations_str);
            }
            boolean verbose = ConfuzzionMain.parseVerbose(args);
            Path resultFolder = ConfuzzionMain.parseDirectory(args);
            ConfuzzionMain conf = new ConfuzzionMain(resultFolder);
            conf.startGeneration(main_loop_iterations, verbose);
        } else {
            ConfuzzionMain.printHelp();
        }
    }

    private static boolean parseVerbose(String[] args) {
        for (String arg : args) {
            if (arg.equals("-v")) {
                return true;
            }
        }
        return false;
    }

    private static Path parseDirectory(String[] args) {
        String folderOption = ConfuzzionMain.parseOption(args, "-o");
        Path resultFolder = null;

        if (folderOption == null) {
            resultFolder = Paths.get("confuzzionResults/");
        } else {
            resultFolder = Paths.get(folderOption);
        }

        if (!Files.exists(resultFolder)) {
            try {
                Files.createDirectories(resultFolder);
            } catch(IOException e) {
                System.err.println(
                "Error while creating result directory. " +
                "Check permissions and path.");
                System.err.println("Path: " + resultFolder);
            }
        }

        return resultFolder;
    }

    private static String parseOption(String[] args, String option) {
        for (int i = 1; i < args.length - 1; i++) {
            if (args[i].equals(option)) {
                i++;
                return args[i];
            }
        }

        return null;
    }

    private static void printHelp() {
        System.err.println(
            "Usage: mut [-v] [-o directory] [-m MAIN_LOOP_ITERATIONS] [-c CONST_LOOP_ITERATIONS]\n" +
            "       gen [-v] [-m MAIN_LOOP_ITERATIONS]"
        );
    }

    public void startGeneration(long mainloop_turn, boolean verbose) {
        RandomGenerator rand = new RandomGenerator();
        MutantGenerator generator = new MutantGenerator(rand, "Test");
        ArrayList<Contract> contracts = new ArrayList<Contract>();
        contracts.add(new ContractTypeConfusion());

        for (long loop1 = 0; loop1 < mainloop_turn; loop1++) {
            if (verbose) {
                System.out.println("===Loop " + loop1 + "===");
            }
            generator.generate();
            Mutant mutant = generator.addContractsChecks(contracts);
        }
    }

    public void startMutation(long constants_retry, long mainloop_turn, boolean verbose) {
        RandomGenerator rand = new RandomGenerator();
        ArrayList<Contract> contracts = new ArrayList<Contract>();
        contracts.add(new ContractTypeConfusion());

        Program currentProg = new Program("Test", rand);

        Stack<Mutation> mutationsStack = new Stack<Mutation>();

        // Refresh Status in command line each second
        Timer timer = new Timer();
        Status status = new Status();
        timer.schedule(status, 0, 1000);

        for (long loop1 = 0; loop1 < mainloop_turn || mainloop_turn < 0; loop1++) {
            Mutation mutation = null;

            try {
                // Random mutation (program level | class level | method level)
                mutation = currentProg.randomMutation();
            } catch (MutationException e) {
                if (verbose) {
                    e.printStackTrace();
                }
                e.undoMutation();
                status.newMutation(e.getMutationClass(), false, false, 0);
                continue;
            }

            boolean loop2NoException = true;
            long loop2 = 0;
            for (loop2 = 0; loop2 < constants_retry || constants_retry < 0; loop2++) {
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
                    loop2NoException = true;
                    // Continue if no exception else try other constants
                    break;
                } catch(Throwable e) {
                    loop2NoException = false;
                    if (verbose) {
                        e.printStackTrace();
                    }
                }
            }

            if (!loop2NoException) {
                // Try another mutation
                mutation.undo();
                status.newMutation(mutation.getClass(), false, false, loop2 + 1);
                continue;
            }

            // Add contracts checks
            ArrayList<BodyMutation> contractsMutations =
                currentProg.addContractsChecks(contracts, mutation);
            try {
                // Instantiation and launch
                currentProg.genAndLaunch(verbose);
                // Remove contracts checks for next turn
                currentProg.removeContractsChecks(contractsMutations);
                // Add mutation to the stack
                mutationsStack.push(mutation);
                // Update status screen
                status.newMutation(mutation.getClass(), true, false, loop2 + 2);
            } catch(Throwable e) {
                if (ContractCheckException.class.isInstance(e) ||
                    ContractCheckException.class.isInstance(e.getCause())) {
                    if (verbose) {
                        System.err.println("ContractCheckException raised");
                        e.printStackTrace();
                    }
                    // Save current classes to a unique folder
                    Path folder = Paths.get(
                        resultFolder.toString(),
                        loop1 + "-" + loop2);
                    try {
                        Files.createDirectories(folder);
                    } catch(IOException e2) {
                        e.printStackTrace();
                        System.err.println(
                            "Error while creating result directory." +
                            "Check permissions and path.");
                        System.err.println("Path: " + folder);
                        System.err.println("Printing last program generated");
                        System.err.println(currentProg.toString());
                        break;
                    }
                    currentProg.saveToFolder(folder.toString());
                    // Update status screen
                    status.newMutation(mutation.getClass(), false, true, loop2 + 2);
                } else {
                    System.err.println("TOFIX: Unexpected exception with contract check");
                    e.printStackTrace();
                    if (e.getCause() != null) {
                        e.getCause().printStackTrace();
                    }
                    // Update status screen
                    status.newMutation(mutation.getClass(), false, false, loop2 + 2);
                    // Exit properly
                    break;
                }
                // Remove contracts checks
                currentProg.removeContractsChecks(contractsMutations);
                // Bad sample, revert mutation
                mutation.undo();
            }

            if (status.isStalled() && mutationsStack.size() > 0) {
                // Revert a random number of mutations
                int toRevert = rand.nextUint(mutationsStack.size());
                while(toRevert-- > 0) {
                    mutationsStack.pop().undo();
                }
                // Refresh stack size on status screen
                status.newStackSize(mutationsStack.size());
            }
        }
        // Stop automatic call to status.run()
        timer.cancel();
        // Print a last time the status screen
        status.run();
    }
}
