package com.github.aztorius.confuzzion;

import soot.ArrayType;
import soot.Local;
import soot.Modifier;
import soot.PrimType;
import soot.Printer;
import soot.RefType;
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
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class Mutant {
    private SootClass sClass;
    private int counter;
    private ArrayList<String> strClasses;
    private HashMap<String, String> childMap;

    private static int MAX_FIELDS = 20;
    private static int MAX_METHODS = 20;
    private static int MAX_PARAMETERS = 3;
    private static int MAX_STATEMENTS = 10;

    public Mutant() {
        counter = 0;
        strClasses = new ArrayList<String>();
        strClasses.add("java.io.ByteArrayOutputStream");
        strClasses.add("java.util.concurrent.ForkJoinPool");
        childMap = new HashMap<String, String>();
        childMap.put("java.util.concurrent.BlockingQueue",
                     "java.util.concurrent.ArrayBlockingQueue");
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
                Value val = rand.randConstant(field.getType());
                if (val != null) {
                    body.getUnits().add(Jimple.v().newAssignStmt(Jimple.v().newInstanceFieldRef(r0, field.makeRef()), val));
                } else {
                    Local loc = this.genObject(rand, body, field.getType().toString());
                    if (loc != null) {
                        body.getUnits().add(Jimple.v().newAssignStmt(Jimple.v().newInstanceFieldRef(r0, field.makeRef()), loc));
                    }
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

    // Generate or find parameters for the specified method call with the local
    private void genMethodCall(RandomGenerator rand, JimpleBody body,
        Local local, SootMethod method) {
        Chain<Local> locals = body.getLocals();
        UnitPatchingChain units = body.getUnits();

        // Generate parameters
        List<Type> parameterTypes = method.getParameterTypes();
        ArrayList<Value> parameters = new ArrayList<Value>();
        Boolean found = false;
        for (Type paramType : parameterTypes) {
            for (Local loc : locals) {
                if (loc.getType() == paramType) {
                    found = true;
                    parameters.add(loc);
                    break;
                }
            }
            if (found) {
                found = false;
                continue;
            }
            Value locParam = null;
            if (soot.PrimType.class.isInstance(paramType)) {
                // paramType is a Primitive Type
                locParam = rand.randConstant(paramType);
            } else if (soot.ArrayType.class.isInstance(paramType)) {
                // paramType is an Array Type
                //TODO: java.lang.IllegalArgumentException: value may not be null
                //locParam = ;
                return;
            } else {
                locParam = this.genObject(rand, body, paramType.toString());
                if (locParam == null) {
                    // May happen if building the object is
                    // too difficult. Abort.
                    //TODO: debug message
                    return;
                }
            }
            parameters.add(locParam);
        }

        // Add method call to units
        if (method.isStatic()) {
            units.add(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(method.makeRef(), parameters)));
        } else if (method.isConstructor()) {
            units.add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(local, method.makeRef(), parameters)));
        } else {
            units.add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(local, method.makeRef(), parameters)));
        }
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

        // Add random statements
        // TODO: add try catch if necessary
        int nbStatements = rand.nextUint(this.MAX_STATEMENTS);
        for (int i = 0; i < nbStatements; i++) {
            switch (rand.nextUint(3)) {
                case 0: //Constructor call
                    String className = strClasses.get(rand.nextUint(strClasses.size()));
                    this.genObject(rand, body, className);
                    break;
                case 1: //Method call on a local
                    if (locals.size() > 0) {
                        Local locSel = null;
                        int localSel = rand.nextUint(locals.size());
                        for (Local loc : locals) {
                            if (localSel > 0) {
                                localSel--;
                            } else {
                                locSel = loc;
                                break;
                            }
                        }
                        Type type = locSel.getType();
                        if (type instanceof RefType) {
                            RefType refType = (RefType)type;
                            List<SootMethod> methods = refType.getSootClass().getMethods();
                            if (methods.size() == 0) {
                                break;
                            }
                            int methodSel = rand.nextUint(methods.size());
                            SootMethod method = methods.get(methodSel);
                            if (method.isConstructor() || !method.isPublic()) {
                                // We should not call the constructor twice !
                                // nor call a non-Public method
                                break;
                            }

                            // Call method
                            this.genMethodCall(rand, body, locSel, method);
                        }
                        // else: cannot call a method on a non-RefType object
                        break;
                    }
                default: //Cast
                    //TODO: units.add(Jimple.v().newCastExpr(Value, Type));
                    //TODO: random Value/Local and random Type
                    continue;
            }
        }

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
            // System.out.println("DEBUG: GEN: cannot build an object of the type");
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

        if (!clazz.isConcrete()) {
            // Find another class that implements this abstract class or interface
            String child = this.childMap.get(clazz.getName());
            if (child != null) {
                return this.genObject(rand, body, child);
            }
            // System.out.println("DEBUG: GEN: " + clazz.getName() + " is not concrete");
            return null;
        }

        ArrayList<SootMethod> constructors = new ArrayList<SootMethod>();
        for (SootMethod method : clazz.getMethods()) {
            if (method.isConstructor() && method.isPublic()) {
                constructors.add(method);
            }
        }
        if (constructors.size() == 0) {
            //TODO: debug: no constructors found
            // System.out.println("DEBUG: GEN: no constructors found");
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
            } else if (ArrayType.class.isInstance(param)) {
                ArrayType paramAT = (ArrayType)param;
                Local loc = Jimple.v().newLocal("local" + this.nextInt(), param);
                locals.add(loc);
                units.add(Jimple.v().newAssignStmt(loc, Jimple.v().newNewArrayExpr(paramAT.baseType, soot.jimple.IntConstant.v(rand.nextUint(100) + 1))));
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
        // Assign local value
        units.add(Jimple.v().newAssignStmt(loc, Jimple.v().newNewExpr(clazz.getType())));
        // Call constructor
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
        // Type can be a primitive type...
        Type type = rand.randPrimType();
        // ...or a target object
        if (rand.nextBoolean()) {
            String className = strClasses.get(rand.nextUint(strClasses.size()));
            Scene.v().loadClassAndSupport(className);
            SootClass clazz = Scene.v().getSootClass(className);
            type = clazz.getType();
        }
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
