package com.github.aztorius.confuzzion;

import soot.Body;
import soot.SootMethod;
import soot.Value;
import soot.ValueBox;
import soot.jimple.Constant;

import java.util.List;

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

    public void randomConstants() {
        List<ValueBox> boxes = mutation.getUseBoxes();
        for (ValueBox box : boxes) {
            if (Constant.class.isInstance(box.getValue())) {
                Value val = rand.randConstant(box.getValue().getType());
                if (val == null) {
                    continue;
                }
                box.setValue(val);
            }
        }
     }
}
