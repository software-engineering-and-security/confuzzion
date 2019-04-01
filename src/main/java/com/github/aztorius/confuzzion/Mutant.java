package com.github.aztorius.confuzzion;

import soot.Local;
import soot.Modifier;
import soot.PrimType;
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
import java.util.List;
import java.util.Random;

public class Mutant {
    private SootClass sClass;
    private int counter;

    private static int MAX_FIELDS = 20;
    private static int MAX_METHODS = 20;
    private static int MAX_PARAMETERS = 3;
    private static int MAX_STATEMENTS = 20;

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
        //Initialize some fields
        for (SootField field: sClass.getFields()) {
            if (!field.isStatic()) {
                //TODO: don't call rand if not necessary
                Value val = rand.randConstant(field.getType());
                if (val != null) {
                    body.getUnits().add(Jimple.v().newAssignStmt(Jimple.v().newInstanceFieldRef(r0, field.makeRef()), val));
                }
            }
        }
        //Add return; statement
        body.getUnits().add(Jimple.v().newReturnVoidStmt());

        //Add initializer of static fields <clinit>
        name = "<clinit>";
        modifiers = Modifier.STATIC | Modifier.PUBLIC;
        method = new SootMethod(name, parameterTypes, returnType, modifiers);
        body = Jimple.v().newBody(method);
        method.setActiveBody(body);
        sClass.addMethod(method);
        //TODO: assign values to each static variables, even objects
        //Initialize some static fields
        for (SootField field: sClass.getFields()) {
            if (field.isStatic()) {
                //TODO: don't call rand if not necessary
                Value val = rand.randConstant(field.getType());
                if (val != null) {
                    body.getUnits().add(Jimple.v().newAssignStmt(Jimple.v().newStaticFieldRef(field.makeRef()), val));
                }
            }
        }
        //Add return; statement
        body.getUnits().add(Jimple.v().newReturnVoidStmt());
    }

    private void genMethod(RandomGenerator rand) {
        String name = "method" + this.nextInt();

        //TODO: add random object type as parameters
        ArrayList<Type> parameterTypes = new ArrayList<Type>();
        int numParams = rand.nextUint() % this.MAX_PARAMETERS;
        for (int i = 0; i < numParams; i++) {
            parameterTypes.add(rand.randType(false));
        }

        Type returnType = rand.randType(true);
        int modifiers = rand.randModifiers(true);
        //TODO: random (or not) exceptions thrown ?
        ArrayList<SootClass> thrownExceptions = new ArrayList<SootClass>();
        SootMethod method = new SootMethod(
            name, parameterTypes, returnType, modifiers, thrownExceptions);
        JimpleBody body = Jimple.v().newBody(method);
        method.setActiveBody(body);
        sClass.addMethod(method);

        this.genBody(rand, body, returnType, parameterTypes, (modifiers & Modifier.STATIC) > 0);
    }

    private void genBody(RandomGenerator rand,
        JimpleBody body, Type returnType, ArrayList<Type> params, Boolean isStatic) {
        Chain<Local> locals = body.getLocals();
        UnitPatchingChain units = body.getUnits();

        if (!isStatic) {
            //Add "this" local
            Local thisLocal = Jimple.v().newLocal("this", sClass.getType());
            locals.add(thisLocal);
            units.add(Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(sClass.getType())));
        }

        for (int i = 0; i < params.size(); i++) {
            Local paramLocal = Jimple.v().newLocal("param" + i, params.get(i));
            locals.add(paramLocal);
            units.add(Jimple.v().newIdentityStmt(paramLocal, Jimple.v().newParameterRef(params.get(i), i)));
        }

        //TODO: random statements (try catch if necessary)
        int nbStatements = rand.nextUint() % this.MAX_STATEMENTS;
        for (int i = 0; i < nbStatements; i++) {
            switch (rand.nextUint() % 4) {
                case 0: //Constructor call
                    //TODO: Local loc = Jimple.v().newlocal("local" + i, Type);
                    break;
                case 1: //Method call on a local
                    //TODO: Jimple.v().newInvokeStmt();
                    break;
                case 2: //Method call on this
                    //TODO: Jimple.v().newInvokeStmt();
                    break;
                default: //Cast
                    //TODO: Jimple.v().newCastExpr(Value, Type);
                    break;
            }
        }

        //TODO: remove
        this.genObject(rand, body, "java.io.ByteArrayOutputStream");
        this.genObject(rand, body, "java.util.concurrent.ThreadPoolExecutor");

        if (returnType == VoidType.v()) {
            //Add return; statement
            units.add(Jimple.v().newReturnVoidStmt());
        } else {
            Value val = null;
            for (Local loc : locals) {
                if (loc.getType() == returnType) {
                    val = loc;
                }
            }

            if (val == null) {
                val = rand.randConstant(returnType);
            }

            units.add(Jimple.v().newReturnStmt(val));
        }
    }

    private Local genObject(RandomGenerator rand, JimpleBody body, String strObj) {
        Chain<Local> locals = body.getLocals();
        UnitPatchingChain units = body.getUnits();

        Scene.v().loadClassAndSupport(strObj);

        SootClass clazz = Scene.v().getSootClass(strObj);
        if (!clazz.isPublic()) {
            //TODO: debug: cannot built an object of this type
            System.out.println("DEBUG: GEN: cannot build an object of the type");
            return null;
        }

        if (clazz.isEnum()) {
            int choice = rand.nextUint() % clazz.getFields().size();
            int i = 0;
            SootField selectedField = null;
            for (SootField field : clazz.getFields()) {
                selectedField = field;
                if (i == choice) {
                    break;
                }
                i++;
            }
            Local loc = Jimple.v().newLocal("local" + this.nextInt(), selectedField.getType());
            locals.add(loc);
            units.add(Jimple.v().newAssignStmt(loc, Jimple.v().newStaticFieldRef(selectedField.makeRef())));
            return loc;
        }

        ArrayList<SootMethod> constructors = new ArrayList<SootMethod>();
        for (SootMethod method : clazz.getMethods()) {
            if (method.isConstructor() && method.isPublic()) {
                constructors.add(method);
            } else if (method.isConstructor()) {
                System.out.println(method.toString());
            }
        }
        if (constructors.size() == 0) {
            //TODO: debug: no constructors found
            System.out.println("DEBUG: GEN: no constructors found");
            return null;
        }
        SootMethod constructor = constructors.get(rand.nextUint() % constructors.size());
        List<Type> parameterTypes = constructor.getParameterTypes();
        ArrayList<Value> parameters = new ArrayList<Value>();
        Boolean found = false;

        // Find or generate parameters
        for (Type param : parameterTypes) {
            found = false;
            // Find a local that can meet this type
            for (Local loc : locals) {
                if (loc.getType() == param) {
                    parameters.add(loc);
                    found = true;
                    break;
                }
            }
            if (found) {
                continue;
            }

            // Create a primitive typed local with a constant
            if (PrimType.class.isInstance(param)) {
                Local loc = Jimple.v().newLocal("local" + this.nextInt(), param);
                locals.add(loc);
                units.add(Jimple.v().newAssignStmt(loc, rand.randConstant(param)));
                parameters.add(loc);
                continue;
            }

            // Call this method to create another object
            Local loc = this.genObject(rand, body, param.toString());
            if (loc == null) {
                // If a parameter cannot be built
                return null;
            }
            parameters.add(loc);
        }
        // Create local
        Local loc = Jimple.v().newLocal("local" + this.nextInt(), clazz.getType());
        locals.add(loc);
        units.add(Jimple.v().newAssignStmt(loc, Jimple.v().newNewExpr(clazz.getType())));
        // units.add(Jimple.v().newIdentityStmt(loc, Jimple.v().newNewExpr(clazz.getType())));
        //TODO: call constructor
        units.add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(loc, constructor.makeRef(), parameters)));

        return loc;
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
