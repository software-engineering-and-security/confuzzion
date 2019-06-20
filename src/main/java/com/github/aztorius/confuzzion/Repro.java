package com.github.aztorius.confuzzion;

import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;

public class Repro {
    private static final Logger logger = LoggerFactory.getLogger(Repro.class);

    public static void main(String[] args) {
        final Options options = configParameters();
        CommandLineParser parser = new DefaultParser();
        String input;

        try {
            CommandLine line = parser.parse(options, args);

            if (line.hasOption("h")) {
                Repro.printHelp(options);
            }

            input = line.getOptionValue("i");
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            String folder = Paths.get(input).getParent().toAbsolutePath().normalize().toString();

            // Init Soot
            Scene.v().loadBasicClasses();
            Scene.v().extendSootClassPath(folder);
            logger.info("Soot Classpath: {}", Scene.v().getSootClassPath());

            if (input.endsWith("jimple")) {
                String classname = Paths.get(input).getFileName().toString();
                classname = classname.substring(0, classname.lastIndexOf(".jimple"));
                Mutant mut = Repro.loadClass(classname);
                Repro.launchClass(mut, loader);

                if (line.hasOption("s")) {
                    mut.toClassFile(folder);
                }
            } else if (input.endsWith("class")) {
                String classname = Paths.get(input).getFileName().toString();
                classname = classname.substring(0, classname.lastIndexOf(".class"));
                Mutant mut = Repro.loadClass(classname);
                Repro.launchClass(mut, loader);

                if (line.hasOption("s")) {
                    mut.toJimpleFile(folder);
                }
            } else {
                Repro.printHelp(options);
            }
        } catch (ParseException e) {
            logger.error("Options parsing failed", e);
            Repro.printHelp(options);
        }
    }

    private static Mutant loadClass(String classname) {
        SootClass sClass = Scene.v().loadClassAndSupport(classname);
        sClass.setApplicationClass();
        for (SootMethod m : sClass.getMethods()) {
            // Load method body
            m.retrieveActiveBody();
            // Remove soot CastExpr from units
            for (Unit u : m.getActiveBody().getUnits()) {
                if (u instanceof AssignStmt) {
                    AssignStmt uA = (AssignStmt)u;
                    if (uA.getRightOp() instanceof CastExpr) {
                        CastExpr cExpr = (CastExpr)uA.getRightOp();
                        uA.setRightOp(cExpr.getOp());
                    }
                }
            }
        }
        return new Mutant(sClass);
    }

    private static void launchClass(Mutant mut, ClassLoader loader) {
        byte[] classContent = mut.toClass();
        ByteClassLoader subLoader = new ByteClassLoader(loader);
        Class<?> clazz;
        try {
            clazz = subLoader.load(mut.getClassName(), classContent);
            clazz.newInstance();
        } catch (Throwable e) {
            logger.error("Error loading and creating a new instance", e);
        }
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("repro", options);
        System.exit(1);
    }

    private static Options configParameters() {
        final Option inputOption = Option.builder("i")
                .longOpt("input")
                .desc("Input file, .class or .jimple")
                .hasArg(true)
                .argName("input")
                .required(true)
                .build();

        final Option saveOption = Option.builder("s")
                .longOpt("save")
                .desc("Save in the source opposite format, .jimple or .class")
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

        options.addOption(inputOption);
        options.addOption(saveOption);
        options.addOption(helpOption);

        return options;
    }
}
