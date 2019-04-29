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
import java.util.ArrayList;
import java.util.List;

public class Mutant {
    private String className;
    private SootClass sClass;

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

    public String toClassFile(SootClass sClass) {
        String fileName = SourceLocator.v().getFileNameFor(sClass,
            Options.output_format_class);
        try {
            OutputStream streamOut = new JasminOutputStream(new FileOutputStream(fileName));
            PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
            JasminClass jasminClass = new soot.jimple.JasminClass(sClass);
            jasminClass.print(writerOut);
            writerOut.flush();
            streamOut.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return fileName;
    }

    public byte[] toClass(SootClass sClass) {
        String className = sClass.getShortName();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        OutputStream streamOut = new JasminOutputStream(stream);
        PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
        {
            JasminClass jasminClass = new soot.jimple.JasminClass(sClass);
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

    public String toJimple(SootClass sClass) {
        String fileName = SourceLocator.v().getFileNameFor(sClass,
            Options.output_format_jimple);
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

    public void toStdOut() {
        OutputStream streamOut = new FileOutputStream(FileDescriptor.out);
        PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
        Printer.v().printTo(sClass, writerOut);
        writerOut.flush();
    }
}
