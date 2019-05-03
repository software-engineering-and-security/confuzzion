package com.github.aztorius.confuzzion;

import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;

import java.util.ArrayList;

public class AddMethodMutation extends ClassMutation {
    private static int MAX_PARAMETERS = 4;

    private SootMethod addedMethod;

    public AddMethodMutation(RandomGenerator rand, SootClass sootClass) {
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
        body.getUnits().add(Jimple.v().newReturnVoidStmt());
        sootClass.addMethod(addedMethod);
    }

    @Override
    public void undo() {
        sootClass.removeMethod(addedMethod);
    }

    @Override
    public void randomConstants() {
        // Nothing to do
    }
}
