package confuzzion;

import soot.Body;
import soot.Local;
import soot.RefType;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Jimple;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class CallMethodMutation extends MethodMutation {
    private SootMethod calledMethod;
    private HashSet<SootMethod> methodsSet;
    private boolean addsNewMethodCall;
    private InitializeMutation initializeMutation;

    public CallMethodMutation(RandomGenerator rand, SootMethod method,
            HashSet<SootMethod> methodsSet, ArrayList<Mutant> mutants) throws MutationException {
        super(rand, method);
        this.methodsSet = methodsSet;
        addsNewMethodCall = false;
        initializeMutation = null;

        Body body = method.getActiveBody();
        Local local = rand.randLocalRef(body.getLocals(), false);
        if (local == null) {
            local = Jimple.v().newLocal("local" + rand.nextIncrement(), rand.randRefType(method.getDeclaringClass().getName()));
            mutation.addLocal(local);
            try {
                initializeMutation = new InitializeMutation(rand, method, local);
            } catch(MutationException e) {
                e.undoMutation();
                mutation.undo();
                throw new MutationException(CallMethodMutation.class, e.getMessage());
            }
        }

        RefType type = (RefType)local.getType();
        SootClass sClass = type.getSootClass();
        List<SootMethod> methods = sClass.getMethods();
        if (methods.size() == 0) {
            this.undo();
            throw new MutationException(CallMethodMutation.class,
                    "No method available on object " + local.toString());
        }

        List<SootMethod> availableMethods = new ArrayList<SootMethod>();
        for (SootMethod m : methods) {
            // We should not call the constructor twice !
            // nor call a non-Public method on an external class
            if (!m.isConstructor() &&
                !m.getName().startsWith("<")) {
                if ((sClass == method.getDeclaringClass() && m != method) ||
                        (sClass != method.getDeclaringClass() && m.isPublic())) {
                    availableMethods.add(m);
                }
            }
        }

        if (availableMethods.size() == 0) {
            this.undo();
            throw new MutationException(CallMethodMutation.class,
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
        if (addsNewMethodCall) {
            methodsSet.remove(calledMethod);
        }
        if (initializeMutation != null) {
            initializeMutation.undo();
        }
        mutation.undo();
    }
}
