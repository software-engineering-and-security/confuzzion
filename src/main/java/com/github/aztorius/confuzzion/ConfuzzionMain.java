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

    private static long MAIN_LOOP_ITERATIONS = 1000;

    public ConfuzzionMain(Path resultFolder) {
        this.resultFolder = resultFolder;
    }

    public static void main(String args[]) {
        if (args.length < 1) {
            ConfuzzionMain.printHelp();
        } else if (args[0].equals("mut")) {
            int iterArgs = 1;
            long main_loop_iterations = ConfuzzionMain.MAIN_LOOP_ITERATIONS;

            String main_loop_iterations_str =
                ConfuzzionMain.parseOption(args, "-m");
            if (main_loop_iterations_str != null) {
                main_loop_iterations = Long.parseLong(main_loop_iterations_str);
            }

            boolean verbose = ConfuzzionMain.parseVerbose(args);
            Path resultFolder = ConfuzzionMain.parseDirectory(args);
            ConfuzzionMain conf = new ConfuzzionMain(resultFolder);
            conf.startMutation(main_loop_iterations, verbose);
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

    public void startMutation(long mainloop_turn, boolean verbose) {
        RandomGenerator rand = new RandomGenerator();
        ArrayList<Contract> contracts = new ArrayList<Contract>();
        contracts.add(new ContractTypeConfusion());

        Program currentProg = new Program("Test", rand);

        Stack<Mutation> mutationsStack = new Stack<Mutation>();

        // Refresh Status in command line each second
        Timer timer = new Timer();
        StatusScreen statusScreen = new StatusScreen();
        timer.schedule(statusScreen, 0, 1000);

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
                statusScreen.newMutation(e.getMutationClass(), Status.FAILED, 0);
                continue;
            }

            BodyMutation executedMutation =
                currentProg.addContractCheck(new ExecutedContract(), mutation);
            if (executedMutation != null) {
                // Check if the code is executed and without exception
                try {
                    currentProg.genAndLaunch(verbose);
                    currentProg.removeContractCheck(executedMutation);
                    mutation.undo();
                    statusScreen.newMutation(mutation.getClass(),
                        Status.NOTEXECUTED, 1);
                    continue;
                } catch(Throwable e) {
                    currentProg.removeContractCheck(executedMutation);
                    if (!ContractCheckException.class.isInstance(Util.getCause(e))) {
                        mutation.undo();
                        statusScreen.newMutation(mutation.getClass(),
                            Status.CRASHED, 1);
                        continue;
                    }
                }
            } else {
                // Check if the code does not throw exception
                try {
                    currentProg.genAndLaunch(verbose);
                } catch(Throwable e) {
                    if (verbose) {
                        e.printStackTrace();
                    }
                    mutation.undo();
                    statusScreen.newMutation(mutation.getClass(),
                        Status.CRASHED, 1);
                    continue;
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
                // Add mutation to the stack
                mutationsStack.push(mutation);
                // Update status screen
                statusScreen.newMutation(mutation.getClass(), Status.SUCCESS, 2);
            } catch(Throwable e) {
                if (verbose) {
                    e.printStackTrace();
                }
                Throwable cause = Util.getCause(e);
                if (ContractCheckException.class.isInstance(cause)) {
                    // Save current classes to a unique folder
                    Path folder = Paths.get(
                        resultFolder.toString(),
                        mutation.getClass().getSimpleName() + "-" + loop1);
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
                    statusScreen.newMutation(mutation.getClass(),
                        Status.VIOLATES, 2);
                } else if (InterruptedException.class.isInstance(cause)) {
                    // Update status screen
                    statusScreen.newMutation(mutation.getClass(),
                        Status.INTERRUPTED, 2);
                } else {
                    System.err.println("TOFIX: Unexpected exception with contract check");
                    cause.printStackTrace();
                    // Update status screen
                    statusScreen.newMutation(mutation.getClass(),
                        Status.CRASHED, 2);
                    // Exit properly
                    break;
                }
                // Remove contracts checks
                currentProg.removeContractsChecks(contractsMutations);
                // Bad sample, revert mutation
                mutation.undo();
            }

            if (statusScreen.isStalled() && mutationsStack.size() > 0) {
                // Revert a random number of mutations
                int toRevert = rand.nextUint(mutationsStack.size());
                while(toRevert-- > 0) {
                    mutationsStack.pop().undo();
                }
                // Refresh stack size on status screen
                statusScreen.newStackSize(mutationsStack.size());
            }
        }
        // Stop automatic call to status.run()
        timer.cancel();
        // Print a last time the status screen
        statusScreen.run();
    }
}
