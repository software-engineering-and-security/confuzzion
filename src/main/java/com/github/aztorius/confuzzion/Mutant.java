package com.github.aztorius.confuzzion;

import soot.Printer;
import soot.SootClass;
import soot.jimple.JasminClass;
import soot.util.JasminOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class Mutant corresponds to a SootClass with some methods to build the
 * class file
 */
public class Mutant {
    private String className;
    private SootClass sClass;

    private static final Logger logger = LoggerFactory.getLogger(Mutant.class);

    /**
     * Constructor
     * @param className ex: Test0
     */
    public Mutant(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public SootClass getSootClass() {
        return sClass;
    }

    public void setSootClass(SootClass clazz) {
        sClass = clazz;
    }

    /**
     * Save the SootClass as a .class file
     * @param  folder destination folder that already exists
     * @return        filepath
     */
    public String toClassFile(String folder) {
        String fileName = Paths.get(folder, sClass.getShortName() + ".class").toString();
        try {
            OutputStream streamOut = new JasminOutputStream(new FileOutputStream(fileName));
            PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
            JasminClass jasminClass = new JasminClass(sClass);
            jasminClass.print(writerOut);
            writerOut.flush();
            streamOut.close();
        } catch (IOException e) {
            logger.error("Writing file {}", fileName, e);
        }
        return fileName;
    }

    /**
     * Build the bytecode of the class in memory
     * @return bytecode of the class as an array or byte
     */
    public byte[] toClass() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        OutputStream streamOut = new JasminOutputStream(stream);
        PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
        {
            JasminClass jasminClass = new JasminClass(sClass);
            jasminClass.print(writerOut);
        }
        writerOut.flush();
        byte[] classContent = stream.toByteArray();
        try {
            streamOut.close();
        } catch (IOException e) {
            logger.error("Converting to class bytecode in memory", e);
            return null;
        }
        return classContent;
    }

    /**
     * Save class to a plaintext jimple file
     * @param  folder destination folder
     * @return        filepath
     */
    public String toJimpleFile(String folder) {
        String fileName = Paths.get(folder, sClass.getShortName() + ".jimple").toString();
        try {
            OutputStream streamOut = new FileOutputStream(fileName);
            PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
            Printer.v().printTo(sClass, writerOut);
            writerOut.flush();
            streamOut.close();
        } catch (IOException e) {
            logger.error("Writing file {}", fileName, e);
        }
        return fileName;
    }

    /**
     * Print class to FileDescriptor.out as a Jimple class
     */
    public void toStdOut() {
        OutputStream streamOut = new FileOutputStream(FileDescriptor.out);
        PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
        Printer.v().printTo(sClass, writerOut);
        writerOut.flush();
    }

    @Override
    public String toString() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(buffer));
        Printer.v().printTo(sClass, writerOut);
        writerOut.flush();
        return new String(buffer.toByteArray(), Charset.forName("UTF-8"));
    }
}
