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
import soot.Unit;
import soot.UnitPatchingChain;
import soot.Value;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.List;

public class AddLocalMutation extends MethodMutation {
    private ArrayList<Local> addedLocals;
    private ArrayList<Unit> addedUnits;

    public AddLocalMutation(SootMethod method) {
        super(method);
        addedLocals = new ArrayList<Local>();
        addedUnits = new ArrayList<Unit>();
    }

    @Override
    public void apply(RandomGenerator rand) {
        Body body = method.getActiveBody();
        Local local = Jimple.v().newLocal("local" + rand.nextIncrement(),
            rand.randType(false));
        if (PrimType.class.isInstance(local)) {
            // Primitive type
            this.addLocal(body, local);
            Value rvalue = rand.randConstant(local.getType());
            this.addUnit(body, Jimple.v().newAssignStmt(local, rvalue));
        } else {
            // Reference of an object
            this.genObject(rand, body, local.getType().toString());
        }
    }

    @Override
    public void undo() {
        Body body = method.getActiveBody();
        for (Local local : addedLocals) {
            body.getLocals().remove(local);
        }
        for (Unit unit : addedUnits) {
            body.getUnits().remove(unit);
        }
    }

    private void addLocal(Body body, Local local) {
        Chain<Local> locals = body.getLocals();
        locals.add(local);
        addedLocals.add(local);
    }

    private void addUnit(Body body, Unit unit) {
        UnitPatchingChain units = body.getUnits();
        units.add(unit);
        addedUnits.add(unit);
    }

    private Local genArray(RandomGenerator rand, Body body, Type type) {
        ArrayType arrayType = (ArrayType)type;
        Type baseType = arrayType.baseType;
        Local loc = Jimple.v().newLocal("local" + rand.nextIncrement(), type);
        this.addLocal(body, loc);
        Value arraySize = soot.jimple.IntConstant.v(rand.nextUint(100) + 1);
        this.addUnit(body, Jimple.v().newAssignStmt(loc,
            Jimple.v().newNewArrayExpr(baseType, arraySize)));
        return loc;
    }

    private Local genObject(RandomGenerator rand, Body body, String strObj) {
        Chain<Local> locals = body.getLocals();

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
            Local loc = Jimple.v().newLocal("local" + rand.nextIncrement(),
                selectedField.getType());
            this.addLocal(body, loc);
            this.addUnit(body, Jimple.v().newAssignStmt(loc,
                Jimple.v().newStaticFieldRef(selectedField.makeRef())));
            return loc;
        }

        if (!clazz.isConcrete()) {
            // Find another class that implements this abstract class or interface
            String child = Util.abstractToConcrete(clazz.getName());
            if (child != null) {
                return this.genObject(rand, body, child);
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
                Local loc = Jimple.v().newLocal("local" + rand.nextIncrement(), param);
                this.addLocal(body, loc);
                this.addUnit(body, Jimple.v().newAssignStmt(loc,
                    rand.randConstant(param)));
                parameters.add(loc);
                continue;
            } else if (ArrayType.class.isInstance(param)) {
                parameters.add(this.genArray(rand, body, param));
                continue;
            }

            // Call this method to create another object
            Value loc = this.genObject(rand, body, param.toString());
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
            loc = Jimple.v().newLocal("local" + rand.nextIncrement(),
            clazz.getType());
            this.addLocal(body, loc);
            // Assign local value
            this.addUnit(body, Jimple.v().newAssignStmt(loc,
                Jimple.v().newNewExpr(clazz.getType())));
            // Call constructor
            this.addUnit(body, Jimple.v().newInvokeStmt(
                Jimple.v().newSpecialInvokeExpr(loc,
                    constructor.makeRef(), parameters)));
            return loc;
        } else { // Static method call
            // Create local
            loc = Jimple.v().newLocal("local" + rand.nextIncrement(),
                constructor.getReturnType());
            this.addLocal(body, loc);
            // Assign the static method call return value
            this.addUnit(body, Jimple.v().newAssignStmt(loc,
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
