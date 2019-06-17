package com.github.aztorius.confuzzion;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.Scene;
import soot.SootClass;

public class Util {
    public static final int ERRORCODE_VIOLATION = 200;

    private static HashMap<String, String> childMap;
    private static final Logger logger = LoggerFactory.getLogger(Util.class);

    static {
        childMap = new HashMap<String, String>();
        childMap.put("java.util.concurrent.BlockingQueue",
                     "java.util.concurrent.ArrayBlockingQueue");
    }

    public static String abstractToConcrete(String className) {
        return childMap.get(className);
    }

    public static Throwable getCause(Throwable e) {
        Throwable cause = null;
        Throwable result = e;

        while((cause = result.getCause()) != null && (result != cause)) {
            result = cause;
        }
        return result;
    }

    public static SootClass getOrLoadSootClass(String name) {
        SootClass sootClass = Scene.v().getSootClassUnsafe(name);
        if (sootClass == null) {
            sootClass = Scene.v().loadClassAndSupport(name);
        }
        return sootClass;
    }

    public static void deleteDirectory(Path path) throws IOException {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteDirectory(entry);
                }
            }
        }
        Files.delete(path);
    }

    public static void writeToFile(String fileName, String content) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(fileName);
        byte[] strToBytes = content.getBytes();
        outputStream.write(strToBytes);
        outputStream.close();
    }

    public static void startJVM(String javahome, String classpath, String className, int timeoutMiliseconds) throws Exception {
        String path = Paths.get(javahome, "bin", "java").toString();
        ProcessBuilder processBuilder = new ProcessBuilder(path, "-cp", classpath + ":" + Util.getJarPath(), className);
        logger.info("{}", processBuilder.command());
        processBuilder.redirectOutput(new File(Paths.get(classpath, "stdout.txt").toString()));
        processBuilder.redirectError(new File(Paths.get(classpath, "stderr.txt").toString()));
        Process process = processBuilder.start();
        process.waitFor(timeoutMiliseconds, TimeUnit.MILLISECONDS);
        if (process.isAlive()) {
            process.destroyForcibly();
            throw new InterruptedException();
        }
        int errorCode = process.exitValue();
        if (errorCode != 0) {
            if (errorCode == Util.ERRORCODE_VIOLATION) {
                throw new ContractCheckException();
            }
            throw new RuntimeException("Error code " + errorCode);
        }
    }

    public static String getJarPath() {
        try {
            // JAR path
            String path =
                    new File(Util.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
            return path;
        } catch(java.net.URISyntaxException e) {
            logger.error("Error while getting JAR path", e);
            return null;
        }
    }
}
