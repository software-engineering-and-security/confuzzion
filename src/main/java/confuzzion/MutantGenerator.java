package confuzzion;

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
import soot.Unit;
import soot.UnitPatchingChain;
import soot.Value;
import soot.VoidType;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.NullConstant;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MutantGenerator {
    private RandomGenerator rand;
    private SootClass sClass;
    private int counter;

    private static final Logger logger = LoggerFactory.getLogger(MutantGenerator.class);

    public MutantGenerator(RandomGenerator rand, String className) {
        this.rand = rand;
        counter = 0;
        sClass = new SootClass(className, Modifier.PUBLIC);
        Scene.v().addClass(sClass);
    }

    public Mutant genMainLoader(List<Mutant> mutants) {
        //Class
        this.setSuperClass("java.lang.Object");
        //Constructor
        this.genConstructor("java.lang.Object");
        SootMethod constructor = sClass.getMethodByName("<init>");
        //Main
        SootMethod main = this.genMain();
        //Add call to Main constructor
        UnitPatchingChain mainUnits = main.getActiveBody().getUnits();
        Chain<Local> mainLocals = main.getActiveBody().getLocals();
        RefType mainType = sClass.getType();
        Local mainlocal = Jimple.v().newLocal("mainlocal", mainType);
        mainLocals.add(mainlocal);
        mainUnits.insertBefore(Jimple.v().newAssignStmt(mainlocal, Jimple.v().newNewExpr(mainType)), mainUnits.getLast());
        mainUnits.insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(mainlocal, constructor.makeRef())), mainUnits.getLast());

        //Add calls to each Mutant constructor
        UnitPatchingChain consUnits = constructor.getActiveBody().getUnits();
        Chain<Local> consLocals = constructor.getActiveBody().getLocals();
        Unit beginStmt = Jimple.v().newNopStmt();
        consUnits.insertBefore(beginStmt, consUnits.getLast());
        for (Mutant mut : mutants) {
            RefType mutType = mut.getSootClass().getType();
            Local mutlocal = Jimple.v().newLocal(mut.getClassName().toLowerCase(), mutType);
            consLocals.add(mutlocal);
            // TestX testx = new TestX();
            consUnits.insertBefore(Jimple.v().newAssignStmt(mutlocal, Jimple.v().newNewExpr(mutType)), consUnits.getLast());
            consUnits.insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(mutlocal, mut.getSootClass().getMethodByName("<init>").makeRef())), consUnits.getLast());
        }
        // return;
        Unit endStmt = Jimple.v().newReturnVoidStmt();
        consUnits.insertBefore(endStmt, consUnits.getLast());
        SootClass contractExceptionClass = Util.getOrLoadSootClass("confuzzion.ContractCheckException");
        SootClass throwableClass = Util.getOrLoadSootClass("java.lang.Throwable");
        // Throwable e := @caughtexception;
        Local caughtException = Jimple.v().newLocal("localexcept" + rand.nextIncrement(), throwableClass.getType());
        consLocals.add(caughtException);
        Unit handlerStmt = Jimple.v().newIdentityStmt(caughtException, Jimple.v().newCaughtExceptionRef());
        consUnits.insertBefore(handlerStmt, consUnits.getLast());
        // e.printStackTrace();
        consUnits.insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(caughtException, throwableClass.getMethod("printStackTrace", new ArrayList<Type>()).makeRef())), consUnits.getLast());
        SootClass runtimeClass = Util.getOrLoadSootClass("java.lang.Runtime");
        // Runtime.getRuntime().exit(ERRORCODE_VIOLATION);
        Local runtimeLocal = Jimple.v().newLocal("localexcept" + rand.nextIncrement(), runtimeClass.getType());
        consLocals.add(runtimeLocal);
        consUnits.insertBefore(Jimple.v().newAssignStmt(runtimeLocal, Jimple.v().newStaticInvokeExpr(runtimeClass.getMethodByName("getRuntime").makeRef())), consUnits.getLast());
        consUnits.insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(runtimeLocal, runtimeClass.getMethodByName("exit").makeRef(), soot.jimple.IntConstant.v(Util.ERRORCODE_VIOLATION))), consUnits.getLast());
        // Declare new try-catch
        constructor.getActiveBody().getTraps().add(Jimple.v().newTrap(contractExceptionClass, beginStmt, endStmt, handlerStmt));
        return new Mutant(sClass);
    }

    public void setSuperClass(String superClass) {
        sClass.setSuperclass(Util.getOrLoadSootClass(superClass));
    }

    public Mutant genEmptyClass(String superClass) {
        //Class
        this.setSuperClass(superClass);
        //Constructors
        this.genConstructor(superClass);
        //Override methods
        this.genOverrideMethods();

        return new Mutant(sClass);
    }

    private int nextInt() {
        return ++counter;
    }

    private void genConstructor(String superClass) {
        //Add constructor <init>
        String name = "<init>";
        ArrayList<Type> parameterTypes = new ArrayList<Type>();
        Type returnType = VoidType.v();
        int modifiers = Modifier.PUBLIC | Modifier.CONSTRUCTOR;
        SootMethod method =
            new SootMethod(name,
                           parameterTypes,
                           returnType,
                           modifiers);
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
        //Find super constructor
        SootMethod constructor = null;
        String classLoopStr = superClass;
        do {
            SootClass classLoop = Scene.v().getSootClass(classLoopStr);
            ArrayList<SootMethod> constructors = new ArrayList<SootMethod>(5);
            Iterator<SootMethod> iterMethods = classLoop.methodIterator();
            while (iterMethods.hasNext()) {
                SootMethod m = iterMethods.next();
                if (m.isConstructor()) {
                    constructors.add(m);
                }
            }
            if (constructors.size() > 0) {
                constructor = constructors.get(rand.nextUint(constructors.size()));
            }
            SootClass superClassLoop = classLoop.getSuperclassUnsafe();
            if (superClassLoop == null) {
                classLoopStr = "java.lang.Object";
            } else {
                classLoopStr = superClassLoop.getName();
            }
        } while (constructor == null);
        //Add specialinvoke r0.<superClass: void <init>(...)>(...); statement
        this.genMethodCall(body, r0, constructor);
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

    private void genOverrideMethods() {
        SootClass superClass = sClass.getSuperclass();
        Iterator<SootMethod> iterMethods = superClass.methodIterator();
        while (iterMethods.hasNext()) {
            SootMethod method = iterMethods.next();
            if (method.isAbstract() && !method.isConstructor()) {
                if (!sClass.declaresMethod(method.getSubSignature())) {
                    // Add a new method that implements method
                    int modifiers = method.getModifiers();
                    if (Modifier.isAbstract(modifiers)) {
                        modifiers -= Modifier.ABSTRACT;
                    }
                    this.addMethod(method.getName(), method.getParameterTypes(), method.getReturnType(), modifiers);
                }
            }
        }
    }

    private SootMethod genMain() {
        String name = "main";
        ArrayList<Type> parameterTypes = new ArrayList<Type>();
        SootClass stringClass = Util.getOrLoadSootClass("java.lang.String");
        parameterTypes.add(stringClass.getType().makeArrayType());
        Type returnType = VoidType.v();
        int modifiers = Modifier.PUBLIC + Modifier.STATIC;
        return this.addMethod(name, parameterTypes, returnType, modifiers);
    }

    private SootMethod addMethod(String name, List<Type> parameterTypes, Type returnType, int modifiers) {
        SootMethod method =
            new SootMethod(name,
                           parameterTypes,
                           returnType,
                           modifiers);

        sClass.addMethod(method);
        JimpleBody body = Jimple.v().newBody(method);
        method.setActiveBody(body);

        this.genBody(body,
                     returnType,
                     parameterTypes,
                     Modifier.isStatic(modifiers));
        return method;
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
            if (paramType instanceof PrimType) {
                // paramType is a Primitive Type
                locParam = rand.randConstant(paramType);
            } else if (paramType instanceof ArrayType) {
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
                         List<Type> params,
                         Boolean isStatic) {
        Chain<Local> locals = body.getLocals();
        UnitPatchingChain units = body.getUnits();

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

        SootClass clazz = Util.getOrLoadSootClass(strObj);
        if (!clazz.isPublic()) {
            logger.warn("Cannot build an object of the type {}", strObj);
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
        Iterator<SootMethod> iterMethods = clazz.methodIterator();
        while (iterMethods.hasNext()) {
            SootMethod method = iterMethods.next();
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
            iterMethods = clazz.methodIterator();
            while (iterMethods.hasNext()) {
                SootMethod method = iterMethods.next();
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
            if (param instanceof PrimType) {
                Local loc = Jimple.v().newLocal("local" + this.nextInt(), param);
                locals.add(loc);
                units.add(Jimple.v().newAssignStmt(loc,
                    rand.randConstant(param)));
                parameters.add(loc);
                continue;
            } else if (param instanceof ArrayType) {
                parameters.add(this.genArray(body, param));
                continue;
            }

            // Call this method to create another object
            Value loc = this.genObject(body, param.toString());
            if (loc == null) {
                // If a parameter cannot be built use a null value.
                loc = NullConstant.v();
            }
            parameters.add(loc);
        }

        Local loc = null;

        if (!constructor.isStatic()) {
            // Create local
            loc = Jimple.v().newLocal("local" + this.nextInt(), clazz.getType());
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

}
