package com.github.aztorius.confuzzion;

import java.io.File;
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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class ConfuzzionMain {
    private Path resultFolder;

    private static final long MAIN_LOOP_ITERATIONS = 1000;
    private static final long TIMEOUT = 1000L;
    private static final int STACK_LIMIT = Integer.MAX_VALUE;
    private static final boolean WITH_JVM = true;
    private static final Logger logger = LoggerFactory.getLogger(ConfuzzionMain.class);

    public ConfuzzionMain(Path resultFolder) {
        this.resultFolder = resultFolder;
    }

    public static void main(String args[]) {
        final Options options = configParameters();
        CommandLineParser parser = new DefaultParser();

        Path resultFolder = Paths.get("confuzzionResults/");
        long main_loop_iterations = ConfuzzionMain.MAIN_LOOP_ITERATIONS;
        long timeout = ConfuzzionMain.TIMEOUT;
        int stackLimit = ConfuzzionMain.STACK_LIMIT;
        boolean withJVM = ConfuzzionMain.WITH_JVM;
        String javahome = System.getProperty("java.home");
        Path seedFile = null;

        try {
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("ConfuzzionMain [options]", options);
                return;
            }
            if (line.hasOption("o")) {
                resultFolder = Paths.get(line.getOptionValue("o"));
            }
            if (line.hasOption("i")) {
                main_loop_iterations = Long.parseLong(line.getOptionValue("i"));
            }
            if (line.hasOption("t")) {
                timeout = Long.parseLong(line.getOptionValue("t"));
            }
            if (line.hasOption("l")) {
                stackLimit = Integer.parseInt(line.getOptionValue("l"));
            }
            withJVM = !line.hasOption("threads");
            if (line.hasOption("j")) {
                javahome = line.getOptionValue("j");
            }
            if (line.hasOption("s")) {
                seedFile = Paths.get(line.getOptionValue("s"));
            }
            if (line.hasOption("java-version")) {
                // soot.options.Options java_version corresponds to java_version + 1
                ConfuzzionOptions.v().java_version = Integer.parseInt(line.getOptionValue("java-version")) + 1;
            }
            if (line.hasOption("classes-limit")) {
                ConfuzzionOptions.v().class_number_limit = Integer.parseInt(line.getOptionValue("classes-limit"));
            }
            ConfuzzionOptions.v().use_jasmin_backend = line.hasOption("jasmin");
            ConfuzzionOptions.v().allow_unsafe_assignment = line.hasOption("unsafe-assignment");
            ConfuzzionOptions.v().fixed_number_of_classes = !line.hasOption("one-class");

            if (!Files.exists(resultFolder)) {
                Files.createDirectories(resultFolder);
            }
        } catch (ParseException e) {
            logger.error("Options parsing failed", e);
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("confuzzion", options);
            System.exit(1);
        } catch (IOException e) {
            logger.error("Error", e);
            System.exit(1);
        }

        ConfuzzionMain conf = new ConfuzzionMain(resultFolder);

        conf.startMutation(main_loop_iterations, timeout, stackLimit, withJVM, javahome, seedFile);
    }

    private static Options configParameters() {
        final Option outputOption = Option.builder("o")
                .longOpt("output")
                .desc("Output directory")
                .hasArg(true)
                .argName("output")
                .required(false)
                .build();

        final Option iterationsOption = Option.builder("i")
                .longOpt("iterations")
                .desc("Main loop iterations / -1 by default")
                .hasArg(true)
                .argName("iterations")
                .required(false)
                .build();

        final Option timeoutOption = Option.builder("t")
                .longOpt("timeout")
                .desc("Timeout per program execution / 1000 ms by default")
                .hasArg(true)
                .argName("timeout")
                .required(false)
                .build();

        final Option runnerOption = Option.builder()
                .longOpt("threads")
                .desc("Use threads in spite of JVM to run programs")
                .hasArg(false)
                .required(false)
                .build();

        final Option jvmOption = Option.builder("j")
                .longOpt("jvm")
                .desc("JAVA_HOME for execution when not using --threads")
                .hasArg(true)
                .argName("jvm")
                .required(false)
                .build();

        final Option stackLimitOption = Option.builder("l")
                .longOpt("stack-limit")
                .desc("Mutations stack size limit / default no limit")
                .hasArg(true)
                .argName("stacklimit")
                .required(false)
                .build();

        final Option seedOption = Option.builder("s")
                .longOpt("seed")
                .desc("Seed file to start mutations from")
                .hasArg(true)
                .argName("seed")
                .required(false)
                .build();

        final Option jasminOption = Option.builder()
                .longOpt("jasmin")
                .desc("Use Jasmin backend instead of ASM")
                .hasArg(false)
                .required(false)
                .build();

        final Option javaVersionOption = Option.builder()
                .longOpt("java-version")
                .desc("Force Java version of output bytecode to : 1-9")
                .hasArg(true)
                .argName("java-version")
                .required(false)
                .build();

        final Option unsafeAssignment = Option.builder()
                .longOpt("unsafe-assignment")
                .desc("Allow unsafe assignment in bytecode without checkcast")
                .hasArg(false)
                .required(false)
                .build();

        final Option classNumberOption = Option.builder()
                .longOpt("classes-limit")
                .desc("Max number of classes to create for the program / default 3")
                .hasArg(true)
                .argName("number-of-classes")
                .required(false)
                .build();

        final Option startWithOneClass = Option.builder()
                .longOpt("one-class")
                .desc("Start with only one class. Else immediately creates the max number of classes.")
                .hasArg(false)
                .required(false)
                .build();

        final Option helpOption = Option.builder("h")
                .longOpt("help")
                .desc("Print this message")
                .hasArg(false)
                .required(false)
                .build();

        final Options options = new Options();

        options.addOption(outputOption);
        options.addOption(iterationsOption);
        options.addOption(timeoutOption);
        options.addOption(runnerOption);
        options.addOption(jvmOption);
        options.addOption(stackLimitOption);
        options.addOption(seedOption);
        options.addOption(jasminOption);
        options.addOption(javaVersionOption);
        options.addOption(unsafeAssignment);
        options.addOption(classNumberOption);
        options.addOption(startWithOneClass);
        options.addOption(helpOption);

        return options;
    }

    public void startGeneration(long mainloop_turn) {
        RandomGenerator rand = new RandomGenerator();
        MutantGenerator generator = new MutantGenerator(rand, "Test");
        ArrayList<Contract> contracts = new ArrayList<Contract>();
        contracts.add(new ContractTypeConfusion());

        for (long loop1 = 0; loop1 < mainloop_turn; loop1++) {
            logger.info("===Loop {}===", loop1);
            generator.generate("java.lang.Object");
            generator.addContractsChecks(contracts);
        }
    }

    public void startMutation(long mainloop_turn, long timeout, int stackLimit, boolean withJVM, String javahome, Path seedFile) {
        Scene.v().loadBasicClasses();
        Scene.v().extendSootClassPath(Util.getJarPath());
        logger.info("Soot Class Path: {}", Scene.v().getSootClassPath());
        logger.info("Default java.home: {}", System.getProperty("java.home"));
        logger.info("Target java.home: {}", javahome);

        RandomGenerator rand = new RandomGenerator();

        Program currentProg = null;
        if (seedFile != null) {
            logger.info("Seed file: {}", seedFile);
            Path seedFolder = seedFile.getParent().toAbsolutePath().normalize();
            Scene.v().extendSootClassPath(seedFolder.toString());
            String seedName = seedFile.getFileName().toString();
            seedName = seedName.substring(0, seedName.lastIndexOf("."));
            Mutant seedMutant = Mutant.loadClass(seedName);
            currentProg = new Program(rand, seedMutant);
            if (logger.isDebugEnabled()) {
                logger.debug(seedMutant.toString());
            }
            // Load all classes (.jimple/.class) in folder except the seed
            File folderFile = seedFolder.toFile();
            File[] listFiles = folderFile.listFiles();
            for (int i = listFiles.length - 1; i >= 0; i--) {
                File fileEntry = listFiles[i];
                if (fileEntry.isFile()) {
                    String filename = fileEntry.getName();

                    if (filename.endsWith(".jimple") || filename.endsWith(".class")) {
                        filename = filename.substring(0, filename.lastIndexOf("."));
                    } else {
                        continue;
                    }

                    if (filename.equals(seedName)) {
                        // Same class, do not reload
                        continue;
                    }

                    logger.info("Loading class {}", filename);
                    Mutant mut = Mutant.loadClass(filename);
                    currentProg.insertSeedDependency(mut);
                    if (logger.isDebugEnabled()) {
                        logger.debug(mut.toString());
                    }
                }
            }
        } else {
            currentProg = new Program(rand, "Test");
        }

        if (ConfuzzionOptions.v().fixed_number_of_classes) {
            // Generate new classes (with java.lang.Object as super type) until class_number_limit is reached
            for (int i = 1; i < ConfuzzionOptions.v().class_number_limit; i++) {
                currentProg.genNewClass(true);
            }
        } //else: classes are added with AddClassMutation with any super type

        ArrayList<Contract> contracts = new ArrayList<Contract>(1);
        contracts.add(new ContractTypeConfusion());
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
            } catch (Throwable e) {
                logger.error("Error while applying mutation", e);
                break;
            }

            logger.info("Mutation: {}", mutation.getClass().toString());

            // Add contracts checks
            ArrayList<BodyMutation> contractsMutations =
                    currentProg.addContractsChecks(contracts, mutation);
            // Save current classes to unique folder
            Path folder = Paths.get(
                    resultFolder.toAbsolutePath().toString(),
                    loop1 + "-" + mutation.getClass().getSimpleName());
            Boolean keepFolder = false;
            try {
                // Instantiation and launch
                if (withJVM) {
                    try {
                        Files.createDirectories(folder);
                    } catch(IOException e2) {
                        logger.error("Printing last program generated:\n{}", currentProg.toString(), e2);
                        break;
                    }
                    currentProg.genAndLaunchWithJVM(javahome, folder.toString(), timeout);
                } else { //with threads
                    currentProg.genAndLaunch(timeout);
                }
                // Remove contracts checks for next turn
                currentProg.removeContractsChecks(contractsMutations);
                // Add mutation to the stack
                mutationsStack.push(mutation);
                // Update status screen
                statusScreen.newMutation(mutation.getClass(), Status.SUCCESS, 1);
            } catch(Throwable e) {
                logger.warn("Exception while executing program", e);
                Throwable cause = Util.getCause(e);
                if (cause instanceof ContractCheckException) {
                    keepFolder = true;
                    // Save current classes also as jimple files
                    currentProg.saveAsJimpleFiles(folder.toString());
                    // Update status screen
                    statusScreen.newMutation(mutation.getClass(), Status.VIOLATES, 1);
                } else if (cause instanceof InterruptedException) {
                    // Update status screen
                    statusScreen.newMutation(mutation.getClass(), Status.INTERRUPTED, 1);
                } else {
                    // Update status screen
                    statusScreen.newMutation(mutation.getClass(), Status.CRASHED, 1);
                }
                // Remove contracts checks
                currentProg.removeContractsChecks(contractsMutations);
                // Bad sample, revert mutation
                mutation.undo();
            } finally {
                if (withJVM && !keepFolder) {
                    // Remove folder
                    try {
                        Util.deleteDirectory(folder);
                    } catch(IOException e2) {
                        logger.error("Error while deleting directory {}", folder, e2);
                        break;
                    }
                }
            }

            if ((statusScreen.isStalled() && mutationsStack.size() > 0) || mutationsStack.size() >= stackLimit) {
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
