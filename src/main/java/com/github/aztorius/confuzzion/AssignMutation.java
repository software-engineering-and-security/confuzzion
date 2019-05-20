package com.github.aztorius.confuzzion;

import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootMethod;
import soot.Type;
import soot.jimple.Jimple;

public class AssignMutation extends MethodMutation {
    public AssignMutation(RandomGenerator rand, SootMethod method)
            throws MutationException {
        super(rand, method);

        Body body = method.getActiveBody();
        if (body.getLocals().size() == 0) {
            throw new MutationException(AssignMutation.class, "No local inside body.");
        }
        Local localBefore = rand.randLocalRef(body.getLocals());
        if (localBefore == null) {
            throw new MutationException(AssignMutation.class, "No reference inside body.");
        }
        Type typeBefore = localBefore.getType();
        Type typeAfter = null;
        if (rand.nextBoolean()) {
            // Pick a type from RandomGenerator
            typeAfter = rand.randRefType(method.getDeclaringClass().getName());
        } else {
            // Pick a type from locals
            typeAfter = rand.randLocalRef(body.getLocals()).getType();
        }
        if (rand.nextBoolean()) {
            // Do a valid assignment to a common parent class
            typeAfter = typeBefore.merge(typeAfter, Scene.v());
        } //else: do a direct assignment between typeBefore and typeAfter
        Local localAfter =
            Jimple.v().newLocal("local" + rand.nextIncrement(), typeAfter);
        mutation.addLocal(localAfter);
        mutation.addUnit(Jimple.v().newAssignStmt(localAfter, localBefore));
    }
}
