package com.github.aztorius.confuzzion;

import soot.Local;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.VoidType;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;

import java.util.ArrayList;

public class AddMethodMutation extends ClassMutation {
    private static int MAX_PARAMETERS = 4;

    private SootMethod addedMethod;
    private AddLocalMutation addedMutation;

    public AddMethodMutation(RandomGenerator rand, SootClass sootClass) throws MutationException {
        super(rand, sootClass);

        String name = "method" + rand.nextIncrement();

        // Add random object type as parameters
        ArrayList<Type> parameterTypes = new ArrayList<Type>();
        int numParams = rand.nextUint(this.MAX_PARAMETERS);
        for (int i = 0; i < numParams; i++) {
            parameterTypes.add(rand.randType(false));
        }

        Type returnType = rand.randType(true);
        int modifiers = rand.randModifiers(true);

        //TODO: random (or not) exceptions thrown ?
        ArrayList<SootClass> thrownExceptions = new ArrayList<SootClass>();
        addedMethod = new SootMethod(name,
                                     parameterTypes,
                                     returnType,
                                     modifiers,
                                     thrownExceptions);
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
        for (int i = 0; i < parameterTypes.size(); i++) {
            Local paramLocal = Jimple.v().newLocal("param" + i, parameterTypes.get(i));
            body.getLocals().add(paramLocal);
            body.getUnits().add(
                Jimple.v().newIdentityStmt(paramLocal,
                                           Jimple.v().newParameterRef(parameterTypes.get(i), i)));
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
                    body.getUnits().add(Jimple.v().newReturnVoidStmt());
                    addedMutation = new AddLocalMutation(rand, addedMethod, returnType);
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
