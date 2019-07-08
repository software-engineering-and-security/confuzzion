package confuzzion;

import soot.ArrayType;
import soot.Body;
import soot.Local;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.ValueBox;
import soot.VoidType;
import soot.jimple.Constant;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.NullConstant;

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
            if (box.getValue() instanceof Constant) {
                Value val = rand.randConstant(box.getValue().getType());
                if (val == null) {
                    continue;
                }
                box.setValue(val);
            }
        }
    }

    /**
     * Find or build a Value from other locals or accessible fields
     * @param body
     * @param type target type of the local
     * @return NullConstant or an appropriate Value
     */
    protected Value getOrGenValue(Body body, Type type) {
        ArrayList<Local> compatibleLocals = new ArrayList<Local>(5);
        ArrayList<SootField> compatibleFields = new ArrayList<SootField>(5);
        ArrayList<Local> correspondingLocals = new ArrayList<Local>(5);
        for (Local loc : body.getLocals()) {
            // Same type or sub-type is compatible for assignment
            if (loc.getType() == type ||
                    (type instanceof RefType &&
                            loc.getType() instanceof RefType &&
                            type.merge(loc.getType(), Scene.v()) == type)) {
                compatibleLocals.add(loc);
            }
            if (loc.getType() instanceof RefType) {
                SootClass sClass = Util.getOrLoadSootClass(loc.getType().toString());
                for (SootField field : sClass.getFields()) {
                    // Is field accessible ?
                    if (sClass == body.getMethod().getDeclaringClass() ||
                            field.isPublic() ||
                            field.isProtected()) {
                        // Is field compatible ?
                        if (field.getType() == type ||
                                (type instanceof RefType &&
                                        field.getType() instanceof RefType &&
                                        type.merge(field.getType(), Scene.v()) == type)) {
                            compatibleFields.add(field);
                            correspondingLocals.add(loc);
                        }
                    }
                }
            }
        }

        Value val = null;
        if (compatibleLocals.size() > 0 && rand.nextBoolean()) {
            val = compatibleLocals.get(rand.nextUint(compatibleLocals.size()));
        } else if (compatibleFields.size() > 0 && rand.nextBoolean()) {
            int index = rand.nextUint(compatibleFields.size());
            SootField field = compatibleFields.get(index);
            Local loc = Jimple.v().newLocal("local" + rand.nextIncrement(), type);
            mutation.addLocal(loc);
            if (field.isStatic()) {
                mutation.addUnit(
                        Jimple.v().newAssignStmt(loc,
                                Jimple.v().newStaticFieldRef(field.makeRef())));
            } else {
                mutation.addUnit(
                        Jimple.v().newAssignStmt(loc,
                                Jimple.v().newInstanceFieldRef(correspondingLocals.get(index),
                                        field.makeRef())));
            }
            val = loc;
        } else {
            // Create a new Value of the appropriate type
            if (type instanceof PrimType) {
                val = rand.randConstant(type);
            } else if (type instanceof ArrayType) {
                val = this.genArray(body, type);
            } else {
                // Call this method to create another object
                val = this.genObject(body, type.toString());
            }
        }
        if (val == null) {
            // If type cannot be found or generated use a proper null value.
            val = NullConstant.v();
        }
        return val;
    }

    protected Local genArray(Body body, Type type) {
        ArrayType arrayType = (ArrayType)type;
        Type baseType = arrayType.baseType;
        Local loc = Jimple.v().newLocal("local" + rand.nextIncrement(), type);
        mutation.addLocal(loc);
        Value arraySize = IntConstant.v(rand.nextUint(100) + 1);
        mutation.addUnit(Jimple.v().newAssignStmt(loc,
                Jimple.v().newNewArrayExpr(baseType, arraySize)));
        return loc;
    }

    protected Local genObject(Body body, String strObj) {
        SootClass clazz = Util.getOrLoadSootClass(strObj);
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

        // Find or generate parameters
        for (Type param : parameterTypes) {
            // Find a local that can meet this type
            Value value = this.getOrGenValue(body, param);
            parameters.add(value);
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
        // Generate parameters
        List<Type> parameterTypes = method.getParameterTypes();
        ArrayList<Value> parameters = new ArrayList<Value>();
        for (Type paramType : parameterTypes) {
            parameters.add(this.getOrGenValue(body, paramType));
        }

        InvokeExpr methodCallExpr = null;

        // Add method call to units
        if (method.isStatic()) {
            methodCallExpr = Jimple.v().newStaticInvokeExpr(method.makeRef(), parameters);
        } else if (method.isConstructor()) {
            methodCallExpr = Jimple.v().newSpecialInvokeExpr(local, method.makeRef(), parameters);
        } else if (method.getDeclaringClass().isInterface()) {
            methodCallExpr = Jimple.v().newInterfaceInvokeExpr(local, method.makeRef(), parameters);
        } else {
            methodCallExpr = Jimple.v().newVirtualInvokeExpr(local, method.makeRef(), parameters);
        }

        Type returnType = method.getReturnType();
        if (returnType.equals(VoidType.v())) {
            mutation.addUnit(Jimple.v().newInvokeStmt(methodCallExpr));
        } else {
            Local loc = Jimple.v().newLocal("local" + rand.nextIncrement(), returnType);
            mutation.addLocal(loc);
            mutation.addUnit(Jimple.v().newAssignStmt(loc, methodCallExpr));
        }
    }
}
