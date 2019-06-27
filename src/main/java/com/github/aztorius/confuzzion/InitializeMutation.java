package com.github.aztorius.confuzzion;

import soot.Local;
import soot.RefType;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.jimple.Jimple;

/**
 * Class InitializeMutation describes an Initialization of a Local or Field
 * It is used internally only
 */
public class InitializeMutation extends MethodMutation {
    public InitializeMutation(RandomGenerator rand,
                              SootMethod method,
                              Local local) throws MutationException {
        super(rand, method);
        Type type = local.getType();
        Value val = null;
        if (type instanceof RefType) {
            // Assign a reference
            val = this.genObject(method.getActiveBody(), type.toString());
        } else {
            // Assign a constant
            val = rand.randConstant(type);
        }

        if (val == null) {
            throw new MutationException(InitializeMutation.class,
                    this.mutation,
                    "Cannot generate a value for type " + type.toString());
        }

        mutation.addUnit(Jimple.v().newAssignStmt(local, val));
    }

    public InitializeMutation(RandomGenerator rand,
                              SootMethod method,
                              SootField field) throws MutationException {
        super(rand, method);
        Type type = field.getType();
        Value val = null;
        if (type instanceof RefType) {
            // Assign a reference
            val = this.genObject(method.getActiveBody(), type.toString());
        } else {
            // Assign a constant
            val = rand.randConstant(type);
        }

        if (val == null) {
            throw new MutationException(InitializeMutation.class,
                    this.mutation,
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
