package com.github.aztorius.confuzzion;

import soot.ArrayType;
import soot.Local;
import soot.Modifier;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.UnitPatchingChain;
import soot.Value;
import soot.VoidType;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.util.Chain;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MutantGenerator {
    private Mutant mutant;
    private RandomGenerator rand;
    private int counter;

    private static int MAX_FIELDS = 20;
    private static int MAX_METHODS = 20;
    private static int MAX_PARAMETERS = 3;
    private static int MAX_STATEMENTS = 10;

    public MutantGenerator(RandomGenerator rand, String className) {
        this.rand = rand;
        mutant = new Mutant(className);
        counter = 0;
    }

    public Mutant genEmptyClass() {
        //Class
        this.genClass();
        //Constructors
        this.genConstructor();

        return this.mutant;
    }

    public Mutant generate() {
        //Class
        this.genClass();
        //Fields
        this.genFields();
        //Methods
        this.genMethods();
        //Constructors
        this.genConstructor();

        return this.mutant;
    }

    public Mutant addContractsChecks(ArrayList<Contract> contracts) {
        //For each Contract, apply all necessary checks on all methods
        //As we are a generator we don't need to revert mutations caused
        //by the contract check.
        for (Contract contract : contracts) {
            for (SootMethod method : mutant.getSootClass().getMethods()) {
                contract.applyCheck(method.getActiveBody());
            }
        }
        return this.mutant;
    }

    private int nextInt() {
        counter = counter + 1;
        return counter;
    }

    private void genClass() {
        Scene.v().loadClassAndSupport("java.lang.Object");
        try {
            // Add JAR path to SootClassPath
            String path =
                new File(
                    Mutant.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
            Scene.v().extendSootClassPath(path);
        } catch(java.net.URISyntaxException e) {
            e.printStackTrace();
        }
        Scene.v().loadClassAndSupport(
            "com.github.aztorius.confuzzion.ContractCheckException");
        SootClass sClass = new SootClass(mutant.getClassName(), Modifier.PUBLIC);
        sClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
        Scene.v().addClass(sClass);
        mutant.setSootClass(sClass);
    }

    private void genConstructor() {
        //Add constructor <init>
        SootClass sClass = mutant.getSootClass();
        String name = "<init>";
        ArrayList<Type> parameterTypes = new ArrayList<Type>();
        Type returnType = VoidType.v();
        int modifiers = Modifier.PUBLIC | Modifier.CONSTRUCTOR;
        ArrayList<SootClass> thrownExceptions = new ArrayList<SootClass>();
        SootMethod method =
            new SootMethod(name,
                           parameterTypes,
                           returnType,
                           modifiers,
                           thrownExceptions);
        JimpleBody body = Jimple.v().newBody(method);
        method.setActiveBody(body);
        sClass.addMethod(method);
        //Add Test r0; statement
        Local r0 = Jimple.v().newLocal("r0", sClass.getType());
        body.getLocals().add(r0);
        //Add r0 := @this: Test; statement
        body.getUnits().add(
            Jimple.v().newIdentityStmt(r0,
                                       Jimple.v().newThisRef(sClass.getType())));
        //Add specialinvoke r0.<java.lang.Object: void <init>()>(); statement
        body.getUnits().add(
            Jimple.v().newInvokeStmt(
                Jimple.v().newSpecialInvokeExpr(r0,
                    Scene.v().getSootClass("java.lang.Object").getMethod("<init>",
                        new ArrayList<Type>()).makeRef())));
        //Initialize some fields
        for (SootField field: sClass.getFields()) {
            if (!field.isStatic()) {
                Value val = rand.randConstant(field.getType());
                if (val != null) {
                    body.getUnits().add(
                        Jimple.v().newAssignStmt(
                            Jimple.v().newInstanceFieldRef(r0,
                                                           field.makeRef()),
                            val));
                } else {
                    Local loc =
                        this.genObject(body, field.getType().toString());
                    if (loc != null) {
                        body.getUnits().add(
                            Jimple.v().newAssignStmt(
                                Jimple.v().newInstanceFieldRef(r0,
                                                               field.makeRef()),
                                loc));
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

        //Initialize some static fields
        for (SootField field: sClass.getFields()) {
            if (field.isStatic()) {
                //TODO: call object constructors
                Value val = rand.randConstant(field.getType());
                if (val == null) {
                    // Assign to null if no value available
                    val = soot.jimple.NullConstant.v();
                }
                body.getUnits().add(
                    Jimple.v().newAssignStmt(
                        Jimple.v().newStaticFieldRef(field.makeRef()), val));
            }
        }
        //Add return; statement
        body.getUnits().add(Jimple.v().newReturnVoidStmt());
    }

    private void genMethod() {
        SootClass sClass = mutant.getSootClass();
        String name = "method" + this.nextInt();

        //TODO: add random object type as parameters
        ArrayList<Type> parameterTypes = new ArrayList<Type>();
        int numParams = rand.nextUint() % this.MAX_PARAMETERS;
        for (int i = 0; i < numParams; i++) {
            parameterTypes.add(rand.randType(sClass.getName(), false));
        }

        Type returnType = rand.randType(sClass.getName(), true);
        int modifiers = rand.randModifiers(true);
        //TODO: random (or not) exceptions thrown ?
        ArrayList<SootClass> thrownExceptions = new ArrayList<SootClass>();
        SootMethod method =
            new SootMethod(name,
                           parameterTypes,
                           returnType,
                           modifiers,
                           thrownExceptions);
        JimpleBody body = Jimple.v().newBody(method);
        method.setActiveBody(body);
        sClass.addMethod(method);

        this.genBody(body,
                     returnType,
                     parameterTypes,
                     (modifiers & Modifier.STATIC) > 0);
    }

    // Generate or find parameters for the specified method call with the local
    private void genMethodCall(JimpleBody body,
                               Local local,
                               SootMethod method) {
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
                locParam = this.genArray(body, paramType);
                if (locParam == null) {
                    // Cannot build the array
                    return;
                }
            } else {
                locParam = this.genObject(body, paramType.toString());
                if (locParam == null) {
                    // May happen if building the object is
                    // too difficult. Use null.
                    //TODO: catch IllegalArgumentException
                    locParam = soot.jimple.NullConstant.v();
                }
            }
            parameters.add(locParam);
        }

        // Add method call to units
        if (method.isStatic()) {
            units.add(
                Jimple.v().newInvokeStmt(
                    Jimple.v().newStaticInvokeExpr(method.makeRef(),
                                                   parameters)));
        } else if (method.isConstructor()) {
            units.add(
                Jimple.v().newInvokeStmt(
                    Jimple.v().newSpecialInvokeExpr(local,
                                                    method.makeRef(),
                                                    parameters)));
        } else if (method.getDeclaringClass().isInterface()) {
            units.add(
                Jimple.v().newInvokeStmt(
                    Jimple.v().newInterfaceInvokeExpr(local,
                                                      method.makeRef(),
                                                      parameters)));
        } else {
            units.add(
                Jimple.v().newInvokeStmt(
                    Jimple.v().newVirtualInvokeExpr(local,
                                                    method.makeRef(),
                                                    parameters)));
        }
    }

    private void genBody(JimpleBody body,
                         Type returnType,
                         ArrayList<Type> params,
                         Boolean isStatic) {
        Chain<Local> locals = body.getLocals();
        UnitPatchingChain units = body.getUnits();
        SootClass sClass = mutant.getSootClass();

        if (!isStatic) {
            //Add "this" local
            Local thisLocal = Jimple.v().newLocal("this", sClass.getType());
            locals.add(thisLocal);
            units.add(
                Jimple.v().newIdentityStmt(thisLocal,
                                           Jimple.v().newThisRef(sClass.getType())));
        }

        for (int i = 0; i < params.size(); i++) {
            Local paramLocal = Jimple.v().newLocal("param" + i, params.get(i));
            locals.add(paramLocal);
            units.add(
                Jimple.v().newIdentityStmt(paramLocal,
                                           Jimple.v().newParameterRef(params.get(i), i)));
        }

        // Add random statements
        // TODO: add try catch if necessary
        int nbStatements = rand.nextUint(this.MAX_STATEMENTS);
        for (int i = 0; i < nbStatements; i++) {
            switch (rand.nextUint(3)) {
                case 0: //Constructor call
                    String className = rand.getClassName();
                    this.genObject(body, className);
                    break;
                case 1: //Method call on a local
                    if (locals.size() <= 0) {
                        break;
                    }
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
                        this.genMethodCall(body, locSel, method);
                    }
                    break;
                default: //Cast
                    Local loc1 = rand.randLocal(locals);
                    if (loc1 == null) {
                        continue;
                    } else if (!(loc1.getType() instanceof RefType)) {
                        continue;
                    }
                    //TODO; cast to other types
                    Local loc2 = Jimple.v().newLocal("local" + this.nextInt(),
                        loc1.getType());
                    locals.add(loc2);
                    units.add(
                        Jimple.v().newAssignStmt(loc2,
                            Jimple.v().newCastExpr(loc1, loc2.getType())));
                    //TODO: catch exception if cast should not work
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

                if (val == null) {
                    val = this.genObject(body, returnType.toString());
                }
            }

            units.add(Jimple.v().newReturnStmt(val));
        }
    }

    private Local genArray(JimpleBody body, Type type) {
        Chain<Local> locals = body.getLocals();
        UnitPatchingChain units = body.getUnits();
        ArrayType arrayType = (ArrayType)type;
        Type baseType = arrayType.baseType;
        Local loc = Jimple.v().newLocal("local" + this.nextInt(), type);
        locals.add(loc);
        Value arraySize = soot.jimple.IntConstant.v(rand.nextUint(100) + 1);
        units.add(Jimple.v().newAssignStmt(loc,
            Jimple.v().newNewArrayExpr(baseType, arraySize)));
        return loc;
    }

    private Local genObject(JimpleBody body, String strObj) {
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
            int choice = rand.nextUint(clazz.getFields().size());
            int i = 0;
            SootField selectedField = null;
            for (SootField field : clazz.getFields()) {
                selectedField = field;
                if (i == choice) {
                    break;
                }
                i++;
            }
            Local loc = Jimple.v().newLocal("local" + this.nextInt(),
                selectedField.getType());
            locals.add(loc);
            units.add(Jimple.v().newAssignStmt(loc,
                Jimple.v().newStaticFieldRef(selectedField.makeRef())));
            return loc;
        }

        if (!clazz.isConcrete()) {
            // Find another class that implements this abstract class or interface
            String child = Util.abstractToConcrete(clazz.getName());
            if (child != null) {
                return this.genObject(body, child);
            }
            // try to continue anyway
        }

        ArrayList<SootMethod> constructors = new ArrayList<SootMethod>();
        for (SootMethod method : clazz.getMethods()) {
            if (method.isConstructor() && method.isPublic()) {
                constructors.add(method);
            }
        }

        if (constructors.size() == 0) {
            // No constructors found. Try invoking static methods to get other
            // type of objects.
            // But first try to find a static method of Class<T> that return
            // a type T.
            ArrayList<SootMethod> methodsSameType = new ArrayList<SootMethod>();
            for (SootMethod method : clazz.getMethods()) {
                if (method.isPublic() && method.isStatic()) {
                    if (method.getReturnType() == clazz.getType()) {
                        methodsSameType.add(method);
                    }
                    constructors.add(method);
                }
            }

            if (constructors.size() == 0) {
                return null;
            } else if (methodsSameType.size() > 0) {
                constructors = methodsSameType;
            }
            // else: continue with an other static method call that return an
            // other type of object
        }

        SootMethod constructor = constructors.get(rand.nextUint(constructors.size()));
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
                units.add(Jimple.v().newAssignStmt(loc,
                    rand.randConstant(param)));
                parameters.add(loc);
                continue;
            } else if (ArrayType.class.isInstance(param)) {
                parameters.add(this.genArray(body, param));
                continue;
            }

            // Call this method to create another object
            Value loc = this.genObject(body, param.toString());
            if (loc == null) {
                // If a parameter cannot be built use a null value.
                // TODO: catch IllegalArgumentException
                loc = soot.jimple.NullConstant.v();
            }
            parameters.add(loc);
        }

        Local loc = null;

        if (!constructor.isStatic()) {
            // Create local
            loc = Jimple.v().newLocal("local" + this.nextInt(),
            clazz.getType());
            locals.add(loc);
            // Assign local value
            units.add(Jimple.v().newAssignStmt(loc,
                Jimple.v().newNewExpr(clazz.getType())));
            // Call constructor
            units.add(Jimple.v().newInvokeStmt(
                Jimple.v().newSpecialInvokeExpr(loc,
                    constructor.makeRef(), parameters)));
            return loc;
        } else { // Static method call
            // Create local
            loc = Jimple.v().newLocal("local" + this.nextInt(),
                constructor.getReturnType());
            locals.add(loc);
            // Assign the static method call return value
            units.add(Jimple.v().newAssignStmt(loc,
                Jimple.v().newStaticInvokeExpr(constructor.makeRef(), parameters)));

            if (constructor.getReturnType() == clazz.getType()) {
                return loc;
            } else {
                // Even if we succeeded at building an object, it is not the correct
                // type as specified by the caller.
                return null;
            }
        }
    }

    private void genMethods() {
        int count = rand.nextUint(this.MAX_METHODS);
        for (int i = 0; i < count; i++) {
            this.genMethod();
        }
    }

    private void genField() {
        String name = "field" + this.nextInt();
        // Type can be a primitive type...
        Type type = rand.randPrimType();
        // ...or a target object
        if (rand.nextBoolean()) {
            String className = rand.getClassName();
            Scene.v().loadClassAndSupport(className);
            SootClass clazz = Scene.v().getSootClass(className);
            type = clazz.getType();
        }
        int modifiers = rand.randModifiers(true);
        mutant.getSootClass().addField(new SootField(name, type, modifiers));
    }

    private void genFields() {
        int count = rand.nextUint(this.MAX_FIELDS);
        for (int i = 0; i < count; i++) {
            this.genField();
        }
    }
}
