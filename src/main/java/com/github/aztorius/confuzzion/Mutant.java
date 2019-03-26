package com.github.aztorius.confuzzion;

import soot.Local;
import soot.Modifier;
import soot.Printer;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.SourceLocator;
import soot.Type;
import soot.UnitPatchingChain;
import soot.Value;
import soot.VoidType;
import soot.jimple.JasminClass;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.options.Options;
import soot.util.Chain;
import soot.util.JasminOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;

public class Mutant {
    private SootClass sClass;
    private int counter;

    private static int MAX_FIELDS = 20;
    private static int MAX_METHODS = 20;

    public Mutant() {
        counter = 0;
    }

    public SootClass getSootClass() {
        return sClass;
    }

    public int nextInt() {
        counter = counter + 1;
        return counter;
    }

    private void genClass(RandomGenerator rand) {
        Scene.v().loadClassAndSupport("java.lang.Object");
        sClass = new SootClass("Test", Modifier.PUBLIC);
        sClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
        Scene.v().addClass(sClass);
    }

    private void genConstructor(RandomGenerator rand) {
        //Add constructor <init>
        String name = "<init>";
        ArrayList<Type> parameterTypes = new ArrayList<Type>();
        Type returnType = VoidType.v();
        int modifiers = Modifier.PUBLIC | Modifier.CONSTRUCTOR;
        ArrayList<SootClass> thrownExceptions = new ArrayList<SootClass>();
        SootMethod method = new SootMethod(
            name, parameterTypes, returnType, modifiers, thrownExceptions);
        JimpleBody body = Jimple.v().newBody(method);
        method.setActiveBody(body);
        sClass.addMethod(method);
        //Add Test r0; statement
        Local r0 = Jimple.v().newLocal("r0", sClass.getType());
        body.getLocals().add(r0);
        //Add r0 := @this: Test; statement
        body.getUnits().add(Jimple.v().newIdentityStmt(r0, Jimple.v().newThisRef(sClass.getType())));
        //Add specialinvoke r0.<java.lang.Object: void <init>()>(); statement
        body.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(r0, Scene.v().getSootClass("java.lang.Object").getMethod("<init>", new ArrayList<Type>()).makeRef())));
        //Add return; statement
        body.getUnits().add(Jimple.v().newReturnVoidStmt());

        //TODO: assign values to each static variables
        //Add initializer of static fields <clinit>
        name = "<clinit>";
        modifiers = Modifier.STATIC | Modifier.PUBLIC;
        method = new SootMethod(name, parameterTypes, returnType, modifiers);
        body = Jimple.v().newBody(method);
        method.setActiveBody(body);
        sClass.addMethod(method);
        //Add return; statement
        body.getUnits().add(Jimple.v().newReturnVoidStmt());
    }

    private void genMethod(RandomGenerator rand) {
        String name = "method" + this.nextInt();
        //TODO: random parameters
        ArrayList<Type> parameterTypes = new ArrayList<Type>();
        Type returnType = rand.randType(true);
        int modifiers = rand.randModifiers(true);
        //TODO: random (or not) exceptions thrown ?
        ArrayList<SootClass> thrownExceptions = new ArrayList<SootClass>();
        SootMethod method = new SootMethod(
            name, parameterTypes, returnType, modifiers, thrownExceptions);
        JimpleBody body = Jimple.v().newBody(method);
        method.setActiveBody(body);
        sClass.addMethod(method);

        this.genBody(rand, body, returnType, (modifiers & Modifier.STATIC) > 0);
    }

    private void genBody(RandomGenerator rand, JimpleBody body, Type returnType, Boolean isStatic) {
        Chain<Local> locals = body.getLocals();
        UnitPatchingChain units = body.getUnits();

        if (!isStatic) {
            //Add "this" local
            Local thisLocal = Jimple.v().newLocal("this", sClass.getType());
            locals.add(thisLocal);
            units.add(Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(sClass.getType())));
        }

        //TODO: random locals
        //TODO: random statements

        if (returnType == VoidType.v()) {
            //Add return; statement
            units.add(Jimple.v().newReturnVoidStmt());
        } else {
            //TODO: can use locals instead of constants
            Value val = null;
            if (returnType == soot.BooleanType.v()) {
                val = soot.jimple.IntConstant.v(rand.nextUint() % 2);
            } else if (returnType == soot.ByteType.v()) {
                val = soot.jimple.IntConstant.v(rand.nextUint() % 256);
            } else if (returnType == soot.CharType.v()) {
                val = soot.jimple.IntConstant.v(rand.nextUint() % 256);
            } else if (returnType == soot.DoubleType.v()) {
                val = soot.jimple.DoubleConstant.v(rand.nextDouble());
            } else if (returnType == soot.FloatType.v()) {
                val = soot.jimple.FloatConstant.v(rand.nextFloat());
            } else if (returnType == soot.IntType.v()) {
                val = soot.jimple.IntConstant.v(rand.nextInt());
            } else if (returnType == soot.LongType.v()) {
                val = soot.jimple.LongConstant.v(rand.nextLong());
            } else if (returnType == soot.ShortType.v()) {
                val = soot.jimple.IntConstant.v(rand.nextInt());
            } else {
                //TODO: not a primitive type
            }

            units.add(Jimple.v().newReturnStmt(val));
        }
    }

    private void genMethods(RandomGenerator rand) {
        int count = rand.nextUint() % this.MAX_METHODS;
        for (int i = 0; i < count; i++) {
            this.genMethod(rand);
        }
    }

    private void genField(RandomGenerator rand) {
        String name = "field" + this.nextInt();
        //TODO: random type can be an object
        Type type = rand.randPrimType();
        int modifiers = rand.randModifiers(true);
        this.sClass.addField(new SootField(name, type, modifiers));
    }

    private void genFields(RandomGenerator rand) {
        int count = rand.nextUint() % this.MAX_FIELDS;
        for (int i = 0; i < count; i++) {
            this.genField(rand);
        }
    }

    public void generate(RandomGenerator rand) {
        //Class
        this.genClass(rand);
        //Fields
        this.genFields(rand);
        //Methods
        this.genMethods(rand);
        //Constructors
        this.genConstructor(rand);
    }

    public String toClassFile(SootClass sClass) {
        String fileName = SourceLocator.v().getFileNameFor(sClass, Options.output_format_class);
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

    public Class<?> toClass(SootClass sClass) {
        String className = sClass.getShortName();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        OutputStream streamOut = new JasminOutputStream(stream);
        PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut));
        {
            JasminClass jasminClass = new soot.jimple.JasminClass(sClass);
            jasminClass.print(writerOut);
        }
        //TODO: fix issue where jasmin.main.<clinit> is called multiple times because of this flush()
        //TODO: fix issue where jasmin.Main could not initialize and throws NoClassDefFoundError
        writerOut.flush();
        byte[] classContent = stream.toByteArray();
        try {
            streamOut.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return null;
        }
        ByteClassLoader loader = new ByteClassLoader();
        return loader.load(className, classContent);
    }

    public String toJimple(SootClass sClass) {
        String fileName = SourceLocator.v().getFileNameFor(sClass, Options.output_format_jimple);
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
