package confuzzion;

import java.io.File;
import java.nio.file.Path;
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

public class Repro {
    private static final Logger logger = LoggerFactory.getLogger(Repro.class);

    public static void main(String[] args) {
        final Options options = configParameters();
        CommandLineParser parser = new DefaultParser();
        String input;

        try {
            CommandLine line = parser.parse(options, args);

            ConfuzzionOptions.v().use_jasmin_backend = line.hasOption("jasmin");
            ConfuzzionOptions.v().allow_unsafe_assignment = line.hasOption("uassign");

            if (line.hasOption("jversion")) {
                // soot.options.Options java_version corresponds to java_version + 1
                ConfuzzionOptions.v().java_version = Integer.parseInt(line.getOptionValue("jversion")) + 1;
            }

            if (line.hasOption("h")) {
                Repro.printHelp(options);
            }

            input = line.getOptionValue("i");
            ByteClassLoader loader = new ByteClassLoader(Thread.currentThread().getContextClassLoader());
            Path path = Paths.get(input).getParent().toAbsolutePath().normalize();
            String folder = path.toString();

            // Init Soot
            Scene.v().loadBasicClasses();
            Scene.v().extendSootClassPath(folder);
            Scene.v().extendSootClassPath(Util.getJarPath());
            logger.info("Soot Classpath: {}", Scene.v().getSootClassPath());
            logger.info("java.home: {}", System.getProperty("java.home"));

            if (!(input.endsWith(".jimple") || input.endsWith(".class"))) {
                Repro.printHelp(options);
            }
            String classname = Paths.get(input).getFileName().toString();
            // Load all classes (.jimple/.class) except classname inside folder
            File folderFile = new File(folder);
            File[] listFiles = folderFile.listFiles();
            for (int i = listFiles.length - 1; i >= 0; i--) {
                File fileEntry = listFiles[i];
                if (fileEntry.isFile() && !fileEntry.getName().equals(classname)) {
                    String filename = fileEntry.getName();

                    if (filename.endsWith(".jimple") || filename.endsWith(".class")) {
                        filename = filename.substring(0, filename.lastIndexOf("."));
                    } else {
                        continue;
                    }
                    logger.info("Loading class {}", filename);
                    Mutant mut = Mutant.loadClass(filename);
                    if (logger.isDebugEnabled()) {
                        logger.debug(mut.toString());
                    }
                    Repro.loadClass(mut, loader);
                }
            }

            classname = classname.substring(0, classname.lastIndexOf("."));

            Mutant mut = Mutant.loadClass(classname);
            if (logger.isDebugEnabled()) {
                logger.debug(mut.toString());
            }
            if (line.hasOption("s")) {
                if (input.endsWith(".jimple")) {
                    mut.toClassFile(folder);
                } else if (input.endsWith(".class")) {
                    mut.toJimpleFile(folder);
                }
            }
            Class<?> clazz = Repro.loadClass(mut, loader);
            Repro.launchClass(clazz);
        } catch (ParseException e) {
            logger.error("Options parsing failed", e);
            Repro.printHelp(options);
        }
    }

    private static Class<?> loadClass(Mutant mut, ByteClassLoader loader) {
        byte[] classContent = mut.toClass();
        Class<?> clazz = null;
        try {
            clazz = loader.load(mut.getClassName(), classContent);
        } catch (Throwable e) {
            logger.error("Error loading class {}", mut.getClassName(), e);
        }
        return clazz;
    }

    private static void launchClass(Class<?> clazz) {
        try {
            clazz.newInstance();
        } catch (Throwable e) {
            logger.error("Error newInstance {}", clazz.getSimpleName(), e);
        }
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Repro [options]", options);
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

        final Option jasminOption = Option.builder("jasmin")
                .desc("Use Jasmin backend instead of ASM")
                .hasArg(false)
                .required(false)
                .build();

        final Option javaVersionOption = Option.builder("jversion")
                .longOpt("java-version")
                .desc("Force Java version of output bytecode to : 1-9")
                .hasArg(true)
                .argName("java-version")
                .required(false)
                .build();

        final Option unsafeAssignment = Option.builder("uassign")
                .longOpt("unsafe-assignment")
                .desc("Allow unsafe assignment in bytecode without checkcast")
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
        options.addOption(jasminOption);
        options.addOption(javaVersionOption);
        options.addOption(unsafeAssignment);
        options.addOption(helpOption);

        return options;
    }
}
