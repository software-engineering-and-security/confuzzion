package com.github.aztorius.confuzzion;

import soot.Printer;
import soot.SootClass;
import soot.SourceLocator;
import soot.Type;
import soot.jimple.JasminClass;
import soot.jimple.Jimple;
import soot.options.Options;
import soot.util.JasminOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Paths;

/**
 * Class Mutant corresponds to a SootClass with some methods to build the
 * class file
 */
public class Mutant {
    private String className;
    private SootClass sClass;

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
        String fileName = Paths.get(folder, sClass.getShortName()).toString() + ".class";
        try {
            OutputStream streamOut = new JasminOutputStream(new FileOutputStream(fileName));
            PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
            JasminClass jasminClass = new JasminClass(sClass);
            jasminClass.print(writerOut);
            writerOut.flush();
            streamOut.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return fileName;
    }

    /**
     * Build the bytecode of the class in memory
     * @return bytecode of the class as an array or byte
     */
    public byte[] toClass() {
        String className = sClass.getShortName();
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
        } catch (java.io.IOException e) {
            e.printStackTrace();
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
        String fileName = Paths.get(folder, sClass.getShortName()).toString() + ".jimple";
        try {
            OutputStream streamOut = new FileOutputStream(fileName);
            PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
            Printer.v().printTo(sClass, writerOut);
            writerOut.flush();
            streamOut.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
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
