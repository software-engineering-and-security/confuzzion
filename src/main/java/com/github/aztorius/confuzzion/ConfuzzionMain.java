package com.github.aztorius.confuzzion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Scene;

public class ConfuzzionMain {
    private Path resultFolder;

    private static long MAIN_LOOP_ITERATIONS = 1000;
    private static int TIMEOUT = 1000;
    private static boolean WITH_JVM = true;
    private static final Logger logger = LoggerFactory.getLogger(ConfuzzionMain.class);

    public ConfuzzionMain(Path resultFolder) {
        this.resultFolder = resultFolder;
    }

    public static void main(String args[]) {
        if (args.length < 1) {
            ConfuzzionMain.printHelp();
        } else if (args[0].equals("mut")) {
            long main_loop_iterations = ConfuzzionMain.MAIN_LOOP_ITERATIONS;
            int timeout = ConfuzzionMain.TIMEOUT;
            boolean withJVM = ConfuzzionMain.WITH_JVM;

            String main_loop_iterations_str =
                ConfuzzionMain.parseOption(args, "-m");
            if (main_loop_iterations_str != null) {
                main_loop_iterations = Long.parseLong(main_loop_iterations_str);
            }

            Path resultFolder = ConfuzzionMain.parseDirectory(args);
            ConfuzzionMain conf = new ConfuzzionMain(resultFolder);

            String timeout_str = ConfuzzionMain.parseOption(args, "-t");
            if (timeout_str != null) {
                timeout = Integer.parseInt(timeout_str);
            }

            String withjvm_str = ConfuzzionMain.parseOption(args, "-r");
            if (withjvm_str != null) {
                withJVM = !withjvm_str.equalsIgnoreCase("thread");
            }

            conf.startMutation(main_loop_iterations, timeout, withJVM);
        } else if (args[0].equals("gen")) {
            long main_loop_iterations = 10;
            String main_loop_iterations_str =
                ConfuzzionMain.parseOption(args, "-m");
            if (main_loop_iterations_str != null) {
                main_loop_iterations = Long.parseLong(main_loop_iterations_str);
            }
            Path resultFolder = ConfuzzionMain.parseDirectory(args);
            ConfuzzionMain conf = new ConfuzzionMain(resultFolder);
            conf.startGeneration(main_loop_iterations);
        } else {
            ConfuzzionMain.printHelp();
        }
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
                logger.error(
                        "Error while creating result directory.\n" +
                        "Check permissions and path: {}", resultFolder, e);
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
            "Usage: mut [-o directory] [-m MAIN_LOOP_ITERATIONS] [-c CONST_LOOP_ITERATIONS] [-t TIMEOUT_PER_PROGRAM] [-r RUNNER]\n" +
            "       gen [-m MAIN_LOOP_ITERATIONS]\n" +
            "RUNNER : thread or jvm"
        );
    }

    public void startGeneration(long mainloop_turn) {
        RandomGenerator rand = new RandomGenerator();
        MutantGenerator generator = new MutantGenerator(rand, "Test");
        ArrayList<Contract> contracts = new ArrayList<Contract>();
        contracts.add(new ContractTypeConfusion());

        for (long loop1 = 0; loop1 < mainloop_turn; loop1++) {
            logger.info("===Loop {}===", loop1);
            generator.generate("java.lang.Object");
            Mutant mutant = generator.addContractsChecks(contracts);
        }
    }

    public void startMutation(long mainloop_turn, int timeout, boolean withJVM) {
        Scene.v().loadBasicClasses();
        Scene.v().extendSootClassPath(Util.getJarPath());
        logger.info("Soot Class Path: {}", Scene.v().getSootClassPath());

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
                logger.warn("Exception while applying mutation", e);
                e.undoMutation();
                statusScreen.newMutation(e.getMutationClass(), Status.FAILED, 0);
                continue;
            }

            logger.info("Mutation: {}", mutation.getClass().toString());

            // Add contracts checks
            ArrayList<BodyMutation> contractsMutations =
                    currentProg.addContractsChecks(contracts, mutation);
            // Save current classes to unique folder
            Path folder = Paths.get(
                    resultFolder.toAbsolutePath().toString(),
                    mutation.getClass().getSimpleName() + "-" + loop1);
            try {
                // Instantiation and launch
                if (withJVM) {
                    try {
                        Files.createDirectories(folder);
                    } catch(IOException e2) {
                        logger.error("Printing last program generated:\n{}", currentProg.toString(), e2);
                        break;
                    }
                    currentProg.genAndLaunchWithJVM(folder.toString(), timeout);
                } else { //with threads
                    currentProg.genAndLaunch(timeout);
                }
                // Remove contracts checks for next turn
                currentProg.removeContractsChecks(contractsMutations);
                // Add mutation to the stack
                mutationsStack.push(mutation);
                // Update status screen
                statusScreen.newMutation(mutation.getClass(), Status.SUCCESS, 2);
            } catch(Throwable e) {
                logger.warn("Exception while executing program", e);
                Throwable cause = Util.getCause(e);
                if (ContractCheckException.class.isInstance(cause)) {
                    // Save current classes to unique folder
                    Path folder2 = Paths.get(
                            resultFolder.toAbsolutePath().toString(),
                            mutation.getClass().getSimpleName() + "-" + loop1 + "-ContractCheckException");
                    try {
                        Files.createDirectories(folder2);
                    } catch(IOException e2) {
                        logger.error("Printing last program generated:\n{}", currentProg.toString(), e2);
                        break;
                    }
                    // Update status screen
                    statusScreen.newMutation(mutation.getClass(),
                        Status.VIOLATES, 2);
                } else if (InterruptedException.class.isInstance(cause)) {
                    // Update status screen
                    statusScreen.newMutation(mutation.getClass(),
                        Status.INTERRUPTED, 2);
                } else {
                    // Update status screen
                    statusScreen.newMutation(mutation.getClass(),
                        Status.CRASHED, 2);
                }
                // Remove contracts checks
                currentProg.removeContractsChecks(contractsMutations);
                // Bad sample, revert mutation
                mutation.undo();
            } finally {
                if (withJVM) {
                    // Remove folder
                    try {
                        Util.deleteDirectory(folder);
                    } catch(IOException e2) {
                        logger.error("Error while deleting directory {}", folder, e2);
                        break;
                    }
                }
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
