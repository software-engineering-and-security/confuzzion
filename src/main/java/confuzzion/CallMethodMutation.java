package confuzzion;

import soot.Body;
import soot.Local;
import soot.RefType;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Jimple;

import java.util.ArrayList;
import java.util.HashSet;

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
        calledMethod = rand.getRandomMethod(method.getDeclaringClass().getName());
        SootClass sClass = calledMethod.getDeclaringClass();
        RefType type = sClass.getType();

        Local local = rand.randLocal(body.getLocals(), type);

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
