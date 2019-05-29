package com.github.aztorius.confuzzion;

import soot.Body;
import soot.Local;
import soot.RefType;
import soot.SootClass;
import soot.SootMethod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class CallMethodMutation extends MethodMutation {
    private SootMethod calledMethod;
    private HashSet<SootMethod> methodsSet;
    private boolean addsNewMethodCall;

    public CallMethodMutation(RandomGenerator rand, SootMethod method,
            HashSet<SootMethod> methodsSet, ArrayList<Mutant> mutants) throws MutationException {
        super(rand, method);
        this.methodsSet = methodsSet;
        this.addsNewMethodCall = false;

        Body body = method.getActiveBody();
        Local local = rand.randLocalRef(body.getLocals());
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
            // nor call a non-Public method on an external class
            if (!m.isConstructor() &&
                !m.getName().startsWith("<")) {
                if (sClass == method.getDeclaringClass() || m.isPublic()) {
                    availableMethods.add(m);
                }
            }
        }

        if (availableMethods.size() == 0) {
            throw new MutationException(CallMethodMutation.class,
                    this.mutation,
                    "No public method available on object " + local.toString());
        }

        int methodSel = rand.nextUint(availableMethods.size());
        calledMethod = availableMethods.get(methodSel);

        // Call method
        this.genMethodCall(body, local, calledMethod);

        // If the method concerns one of the program class and is not currently executed, mark it as executed
        if (!methodsSet.contains(calledMethod)) {
            for (Mutant mut : mutants) {
                if (mut.getSootClass().equals(calledMethod.getDeclaringClass())) {
                    methodsSet.add(calledMethod);
                    addsNewMethodCall = true;
                    break;
                }
            }
        }
    }

    public SootMethod getCalledMethod() {
        return calledMethod;
    }

    @Override
    public void undo() {
        super.undo();
        if (addsNewMethodCall) {
            methodsSet.remove(calledMethod);
        }
    }
}
