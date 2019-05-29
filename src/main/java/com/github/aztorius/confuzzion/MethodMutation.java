package com.github.aztorius.confuzzion;

import soot.ArrayType;
import soot.Body;
import soot.Local;
import soot.PrimType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.ValueBox;
import soot.jimple.Constant;
import soot.jimple.Jimple;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.List;

/**
 * A MethodMutation describes how a method will be changed (adding,
 * modifying, removing a local , parameter, return value or method call ...)
 */
public abstract class MethodMutation extends Mutation {
    protected SootMethod method;
    protected BodyMutation mutation;

    /**
     * Constructor
     * @param rand   the RandomGenerator to use
     * @param method the method which will be mutated
     */
    protected MethodMutation(RandomGenerator rand, SootMethod method) {
        super(rand);
        this.method = method;
        this.mutation = new BodyMutation(method.getActiveBody());
    }

    public Body getBody() {
        return method.getActiveBody();
    }

    /**
     * Remove the mutation from method body.
     */
    @Override
    public void undo() {
        mutation.undo();
    }

    /**
     * Change all constants added by the mutation
     */
    @Override
    public void randomConstants() {
        List<ValueBox> boxes = mutation.getUseBoxes();
        for (ValueBox box : boxes) {
            if (Constant.class.isInstance(box.getValue())) {
                Value val = rand.randConstant(box.getValue().getType());
                if (val == null) {
                    continue;
                }
                box.setValue(val);
            }
        }
    }

    protected Local genArray(Body body, Type type) {
        ArrayType arrayType = (ArrayType)type;
        Type baseType = arrayType.baseType;
        Local loc = Jimple.v().newLocal("local" + rand.nextIncrement(), type);
        mutation.addLocal(loc);
        Value arraySize = soot.jimple.IntConstant.v(rand.nextUint(100) + 1);
        mutation.addUnit(Jimple.v().newAssignStmt(loc,
            Jimple.v().newNewArrayExpr(baseType, arraySize)));
        return loc;
    }

    protected Local genObject(Body body, String strObj) {
        Chain<Local> locals = body.getLocals();

        Scene.v().loadClassAndSupport(strObj);

        SootClass clazz = Scene.v().getSootClass(strObj);
        if (!clazz.isPublic()) {
            return null;
        }

        // In case its a special Class object or String object
        if (clazz.getName().equals("java.lang.Class") || clazz.getName().equals("java.lang.String")) {
            // Create local
            Local loc = Jimple.v().newLocal("local" + rand.nextIncrement(),
                                            clazz.getType());
            mutation.addLocal(loc);
            String className = method.getDeclaringClass().getName();
            Value value = null;
            if (clazz.getName().equals("java.lang.Class")) {
                value = rand.randClassConstant(className);
            } else {
                value = rand.randString(className);
            }
            // Assign local value
            mutation.addUnit(Jimple.v().newAssignStmt(loc, value));

            return loc;
        }

        if (clazz.isEnum()) {
            ArrayList<SootField> staticFields = new ArrayList<SootField>();
            for (SootField field : clazz.getFields()) {
                if (field.isStatic()) {
                    staticFields.add(field);
                }
            }
            if (staticFields.size() == 0) {
                return null;
            }
            int choice = rand.nextUint(staticFields.size());
            SootField selectedField = staticFields.get(choice);

            Local loc = Jimple.v().newLocal("local" + rand.nextIncrement(),
                selectedField.getType());
            mutation.addLocal(loc);
            mutation.addUnit(Jimple.v().newAssignStmt(loc,
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

        SootMethod constructor =
            constructors.get(rand.nextUint(constructors.size()));
        List<Type> parameterTypes = constructor.getParameterTypes();
        ArrayList<Value> parameters = new ArrayList<Value>(parameterTypes.size());
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
                Local loc =
                    Jimple.v().newLocal("local" + rand.nextIncrement(), param);
                mutation.addLocal(loc);
                mutation.addUnit(
                    Jimple.v().newAssignStmt(loc, rand.randConstant(param)));
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
                loc = soot.jimple.NullConstant.v();
            }
            parameters.add(loc);
        }

        Local loc = null;

        if (!constructor.isStatic()) {
            // Create local
            loc = Jimple.v().newLocal("local" + rand.nextIncrement(),
                                      clazz.getType());
            mutation.addLocal(loc);
            // Assign local value
            mutation.addUnit(
                Jimple.v().newAssignStmt(loc,
                                         Jimple.v().newNewExpr(clazz.getType())));
            // Call constructor
            mutation.addUnit(Jimple.v().newInvokeStmt(
                Jimple.v().newSpecialInvokeExpr(loc,
                                                constructor.makeRef(),
                                                parameters)));
            return loc;
        } else { // Static method call
            // Create local
            loc = Jimple.v().newLocal("local" + rand.nextIncrement(),
                constructor.getReturnType());
            mutation.addLocal(loc);
            // Assign the static method call return value
            mutation.addUnit(
                Jimple.v().newAssignStmt(loc,
                                         Jimple.v().newStaticInvokeExpr(constructor.makeRef(),
                                                                        parameters)));

            if (constructor.getReturnType() == clazz.getType()) {
                return loc;
            } else {
                // Even if we succeeded at building an object, it is not the correct
                // type as specified by the caller.
                return null;
            }
        }
    }

    // Generate or find parameters for the specified method call with the local
    protected void genMethodCall(Body body,
                                 Local local,
                                 SootMethod method) {
        Chain<Local> locals = body.getLocals();

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
                    locParam = soot.jimple.NullConstant.v();
                }
            }
            parameters.add(locParam);
        }

        // Add method call to units
        if (method.isStatic()) {
            mutation.addUnit(
                Jimple.v().newInvokeStmt(
                    Jimple.v().newStaticInvokeExpr(method.makeRef(),
                                                   parameters)));
        } else if (method.isConstructor()) {
            mutation.addUnit(
                Jimple.v().newInvokeStmt(
                    Jimple.v().newSpecialInvokeExpr(local,
                                                    method.makeRef(),
                                                    parameters)));
        } else if (method.getDeclaringClass().isInterface()) {
            mutation.addUnit(
                Jimple.v().newInvokeStmt(
                    Jimple.v().newInterfaceInvokeExpr(local,
                                                      method.makeRef(),
                                                      parameters)));
        } else {
            mutation.addUnit(
                Jimple.v().newInvokeStmt(
                    Jimple.v().newVirtualInvokeExpr(local,
                                                    method.makeRef(),
                                                    parameters)));
        }
    }
}
