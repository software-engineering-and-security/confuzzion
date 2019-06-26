package com.github.aztorius.confuzzion;

import soot.Printer;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.baf.BafASMBackend;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.JasminClass;
import soot.util.JasminOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
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
    private SootClass sClass;

    private static final Logger logger = LoggerFactory.getLogger(Mutant.class);

    /**
     * Constructor
     * @param className ex: Test0
     */
    public Mutant(SootClass sClass) {
        this.sClass = sClass;
    }

    public String getClassName() {
        return sClass.getShortName();
    }

    public SootClass getSootClass() {
        return sClass;
    }

    public void setSootClass(SootClass clazz) {
        sClass = clazz;
    }

    /**
     * Generate bytecode to an output stream
     * @param stream output
     */
    public void toBytecode(OutputStream stream) {
        try {
            if (ConfuzzionOptions.v().use_jasmin_backend) {
                OutputStream streamOut = new JasminOutputStream(stream);
                PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
                JasminClass jasminClass = new JasminClass(sClass);
                jasminClass.print(writerOut);
                writerOut.flush();
                streamOut.close();
                writerOut.close();
            } else {
                BafASMBackend backend = new BafASMBackend(sClass, ConfuzzionOptions.v().java_version);
                backend.generateClassFile(stream);
            }
        } catch (IOException e) {
            logger.error("OutputStream {}", stream.toString(), e);
        }
    }

    /**
     * Save the SootClass as a .class file
     * @param  folder destination folder that already exists
     * @return filepath
     */
    public String toClassFile(String folder) {
        String fileName = Paths.get(folder, sClass.getShortName() + ".class").toString();
        try {
            this.toBytecode(new FileOutputStream(fileName));
        } catch (FileNotFoundException e) {
            logger.error("File {}", fileName, e);
        }
        return fileName;
    }

    /**
     * Build the bytecode of the class in memory
     * @return bytecode of the class as an array or byte
     */
    public byte[] toClass() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        this.toBytecode(stream);
        byte[] classContent = stream.toByteArray();
        return classContent;
    }

    /**
     * Generate Jimple to output stream. Does not close output stream.
     * @param stream output
     */
    public void toJimple(OutputStream stream) {
        PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(stream));
        Printer.v().printTo(sClass, writerOut);
        writerOut.flush();
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
            this.toJimple(streamOut);
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
        this.toJimple(streamOut);
    }

    @Override
    public String toString() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        this.toJimple(buffer);
        return new String(buffer.toByteArray(), Charset.forName("UTF-8"));
    }

    /**
     * Load a class from SootClassPath folders, remove CastExpr, validate and return Mutant
     * @param classname in java.lang.Object format
     * @return
     */
    public static Mutant loadClass(String classname) {
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
        sClass.validate();
        return new Mutant(sClass);
    }
}
