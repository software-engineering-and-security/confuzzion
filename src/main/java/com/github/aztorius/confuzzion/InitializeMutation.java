package com.github.aztorius.confuzzion;

import soot.Local;
import soot.RefType;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.jimple.Jimple;

public class InitializeMutation extends MethodMutation {
    public InitializeMutation(RandomGenerator rand,
                              SootMethod method,
                              Local local) {
        super(rand, method);
        Type type = local.getType();
        Value val = null;
        if (RefType.class.isInstance(type)) {
            // Assign a reference
            val = this.genObject(method.getActiveBody(), type.toString());
        } else {
            // Assign a constant
            val = rand.randConstant(type);
        }

        mutation.addUnit(Jimple.v().newAssignStmt(local, val));
    }

    public InitializeMutation(RandomGenerator rand,
                              SootMethod method,
                              SootField field) throws MutationException {
        super(rand, method);
        Type type = field.getType();
        Value val = null;
        if (RefType.class.isInstance(type)) {
            // Assign a reference
            val = this.genObject(method.getActiveBody(), type.toString());
        } else {
            // Assign a constant
            val = rand.randConstant(type);
        }

        if (val == null) {
            throw new MutationException(
                "Cannot generate a value for type " + type.toString());
        }

        // Assign to field
        if (field.isStatic()) {
            mutation.addUnit(
                Jimple.v().newAssignStmt(
                    Jimple.v().newStaticFieldRef(field.makeRef()),
                    val));
        } else {
            // Get local this
            Local lThis = method.getActiveBody().getThisLocal();
            mutation.addUnit(
                Jimple.v().newAssignStmt(
                    Jimple.v().newInstanceFieldRef(lThis, field.makeRef()),
                    val));
        }
    }
}
