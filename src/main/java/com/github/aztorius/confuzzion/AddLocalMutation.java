package com.github.aztorius.confuzzion;

import soot.Local;
import soot.PrimType;
import soot.SootMethod;
import soot.Type;
import soot.jimple.Jimple;

/**
 * Class describing a mutation which adds a local inside the body of a method
 */
public class AddLocalMutation extends MethodMutation {
    private Local addedLocal;

    /**
     * Add a new local of a random type in the body of the method and call
     * a constructor if necessary
     * @param rand   the RandomGenerator to use
     * @param method the method where to add a local
     */
    public AddLocalMutation(RandomGenerator rand, SootMethod method) {
        this(rand, method, rand.randType(method.getDeclaringClass().getName(), false));
    }

    /**
     * Add a new local of the specified type in the body of the method and call
     * a constructor if necessary
     * @param rand   the RandomGenerator to use
     * @param method the method where to add a local
     * @param type   the type of the new local
     */
    public AddLocalMutation(RandomGenerator rand, SootMethod method, Type type) {
        super(rand, method);

        if (PrimType.class.isInstance(type)) {
            // Primitive type
            Local local = Jimple.v().newLocal("local" + rand.nextIncrement(), type);
            mutation.addLocal(local);
            addedLocal = local;
            mutation.addUnit(Jimple.v().newAssignStmt(local, rand.randConstant(type)));
        } else {
            // Reference of an object
            addedLocal = this.genObject(method.getActiveBody(), type.toString());
        }
    }

    /**
     * Get the added local
     * @return the local added by the mutation
     */
    public Local getAddedLocal() {
        return addedLocal;
    }
}
