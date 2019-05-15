package com.github.aztorius.confuzzion;

import soot.Body;
import soot.Local;
import soot.PrimType;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.jimple.Jimple;

public class AddLocalMutation extends MethodMutation {
    private Local addedLocal;

    public AddLocalMutation(RandomGenerator rand, SootMethod method) {
        this(rand, method, rand.randType(false));
    }

    public AddLocalMutation(RandomGenerator rand, SootMethod method, Type type) {
        super(rand, method);
        Body body = method.getActiveBody();
        Local local = Jimple.v().newLocal("local" + rand.nextIncrement(), type);
        if (PrimType.class.isInstance(local.getType())) {
            // Primitive type
            mutation.addLocal(local);
            addedLocal = local;
            Value rvalue = rand.randConstant(local.getType());
            mutation.addUnit(Jimple.v().newAssignStmt(local, rvalue));
        } else {
            // Reference of an object
            addedLocal = this.genObject(body, local.getType().toString());
        }
    }

    public Local getAddedLocal() {
        return addedLocal;
    }
}
