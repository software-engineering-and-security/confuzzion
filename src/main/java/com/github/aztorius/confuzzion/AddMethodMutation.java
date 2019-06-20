package com.github.aztorius.confuzzion;

import soot.Local;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.VoidType;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.NullConstant;

import java.util.List;

public class AddMethodMutation extends ClassMutation {
    private static int MAX_PARAMETERS = 4;

    private SootMethod addedMethod;
    private AddLocalMutation addedMutation;

    public AddMethodMutation(RandomGenerator rand, SootClass sootClass) throws MutationException {
        this(rand,
                sootClass,
                "method" + rand.nextIncrement(),
                rand.randTypes(sootClass.getName(), AddMethodMutation.MAX_PARAMETERS),
                rand.randType(sootClass.getName(), true),
                rand.randModifiers(true, false));
    }

    public AddMethodMutation(RandomGenerator rand, SootClass sootClass, String name,
            List<Type> parameters, Type returnType, int modifiers) throws MutationException {
        super(rand, sootClass);

        addedMethod = new SootMethod(name,
                                     parameters,
                                     returnType,
                                     modifiers);
        JimpleBody body = Jimple.v().newBody(addedMethod);
        addedMethod.setActiveBody(body);

        if (!addedMethod.isStatic()) {
            //Add "this" local
            Local thisLocal = Jimple.v().newLocal("this", sootClass.getType());
            body.getLocals().add(thisLocal);
            body.getUnits().add(
                Jimple.v().newIdentityStmt(thisLocal,
                                           Jimple.v().newThisRef(sootClass.getType())));
        }

        // Add locals for parameters
        for (int i = 0; i < parameters.size(); i++) {
            Local paramLocal = Jimple.v().newLocal("param" + i, parameters.get(i));
            body.getLocals().add(paramLocal);
            body.getUnits().add(
                Jimple.v().newIdentityStmt(paramLocal,
                                           Jimple.v().newParameterRef(parameters.get(i), i)));
        }

        if (returnType == VoidType.v()) {
            body.getUnits().add(Jimple.v().newReturnVoidStmt());
        } else {
            Value val = null;
            for (Local loc : body.getLocals()) {
                if (loc.getType() == returnType) {
                    val = loc;
                }
            }

            if (val == null) {
                val = rand.randConstant(returnType);
                if (val == null) {
                    // Add temporary return for BodyMutation
                    body.getUnits().add(Jimple.v().newReturnStmt(NullConstant.v()));
                    // Add method to class. Necessary for AddLocalMutation
                    sootClass.addMethod(addedMethod);
                    addedMutation = new AddLocalMutation(rand, addedMethod, returnType);
                    // Remove temporary method
                    sootClass.removeMethod(addedMethod);
                    val = addedMutation.getAddedLocal();
                    if (val == null) {
                        throw new MutationException(AddMethodMutation.class,
                            "Cannot build object of type " + returnType);
                    }
                    // Remove temporary return
                    body.getUnits().removeLast();
                }
            }

            // Add final return
            body.getUnits().add(Jimple.v().newReturnStmt(val));
        }

        sootClass.addMethod(addedMethod);
    }

    public AddMethodMutation(RandomGenerator rand, SootClass sootClass, SootMethod superMethod) throws MutationException {
        this(rand, sootClass, superMethod.getName(), superMethod.getParameterTypes(), superMethod.getReturnType(), rand.randModifiers(true, false));
    }

    @Override
    public void undo() {
        sootClass.removeMethod(addedMethod);
    }

    @Override
    public void randomConstants() {
        if (addedMutation != null) {
            addedMutation.randomConstants();
        }
    }
}
