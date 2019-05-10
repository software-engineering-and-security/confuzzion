package com.github.aztorius.confuzzion;

import soot.Body;
import soot.Local;
import soot.RefType;
import soot.SootClass;
import soot.SootMethod;

import java.util.ArrayList;
import java.util.List;

public class CallMethodMutation extends MethodMutation {
    public CallMethodMutation(RandomGenerator rand, SootMethod method) throws MutationException {
        super(rand, method);
        Body body = method.getActiveBody();
        Local local = Util.randomLocalRef(body.getLocals(), rand);
        if (local == null) {
            throw new MutationException(CallMethodMutation.class,
                    this.mutation,
                    "No local reference found");
        }

        RefType type = (RefType)local.getType();
        SootClass sClass = type.getSootClass();
        List<SootMethod> methods = sClass.getMethods();
        if (methods.size() == 0) {
            throw new MutationException(CallMethodMutation.class,
                    this.mutation,
                    "No method available on object " + local.toString());
        }

        List<SootMethod> availableMethods = new ArrayList<SootMethod>();
        for (SootMethod m : methods) {
            // We should not call the constructor twice !
            // nor call a non-Public method
            if (!m.isConstructor() &&
                m.isPublic() &&
                !m.getName().startsWith("<")) {
                availableMethods.add(m);
            }
        }

        if (availableMethods.size() == 0) {
            throw new MutationException(CallMethodMutation.class,
                    this.mutation,
                    "No public method available on object " + local.toString());
        }

        int methodSel = rand.nextUint(availableMethods.size());
        SootMethod methodTarget = availableMethods.get(methodSel);

        // Call method
        this.genMethodCall(body, local, methodTarget);
    }
}
