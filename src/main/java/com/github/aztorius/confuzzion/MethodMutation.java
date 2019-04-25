package com.github.aztorius.confuzzion;

import soot.Body;
import soot.SootMethod;

/* A Method Mutation describes how a method will be changed (adding,
 * modifying, removing a local , parameter, return value or method call ...)
 */
public abstract class MethodMutation extends Mutation {
    protected SootMethod method;
    protected BodyMutation mutation;

    protected MethodMutation(RandomGenerator rand, SootMethod method) {
        super(rand);
        this.method = method;
        this.mutation = new BodyMutation(method.getActiveBody());
    }

    public Body getBody() {
        return method.getActiveBody();
    }

    /* Remove the mutation from method body.
     */
    public void undo() {
        mutation.undo();
    }
}
