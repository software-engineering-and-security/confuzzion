package com.github.aztorius.confuzzion;

import soot.SootMethod;

import java.util.List;

/* A Method Mutation describes how a method will be changed (adding,
 * modifying, removing a local , parameter, return value or method call ...)
 */
public abstract class MethodMutation {
    SootMethod method;

    protected MethodMutation(SootMethod method) {
        this.method = method;
    }

    /* Apply the mutation on a method
     */
    public abstract void apply(RandomGenerator rand);

    /* Remove the mutation from method. Only available after a call to apply.
     */
    public abstract void undo();

    /* Apply the mutation on a copy of method and add all necessary contracts
     * checkers, and then return it
     */
    // public SootMethod applyAndCheck(
    //     SootMethod method,
    //     List<Contract> contracts);
}
