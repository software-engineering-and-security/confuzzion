package com.github.aztorius.confuzzion;

import soot.Body;
import soot.Local;
import soot.PrimType;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Jimple;

public class AddLocalMutation extends MethodMutation {
    public AddLocalMutation(RandomGenerator rand, SootMethod method) {
        super(rand, method);
        Body body = method.getActiveBody();
        Local local = Jimple.v().newLocal("local" + rand.nextIncrement(),
            rand.randType(false));
        if (PrimType.class.isInstance(local.getType())) {
            // Primitive type
            mutation.addLocal(local);
            Value rvalue = rand.randConstant(local.getType());
            mutation.addUnit(Jimple.v().newAssignStmt(local, rvalue));
        } else {
            // Reference of an object
            this.genObject(body, local.getType().toString());
        }
    }
}
