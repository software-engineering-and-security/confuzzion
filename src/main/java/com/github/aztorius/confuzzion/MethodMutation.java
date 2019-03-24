package com.github.aztorius.confuzzion;

import soot.Modifier;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.VoidType;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;

import java.util.ArrayList;

public class MethodMutation implements Mutation {
    @Override
    public void apply(Mutant mut, RandomGenerator rand, MutationType mtype) {
        switch (mtype) {
            case ADD:
                String name = "method" + mut.nextInt();
                //TODO: random parameters
                ArrayList<Type> parameterTypes = new ArrayList<Type>();
                //TODO: Type returnType = rand.randPrimType();
                Type returnType = VoidType.v();
                int modifiers = Modifier.STATIC; //TODO: random
                //TODO: random exceptions thrown ?
                ArrayList<SootClass> thrownExceptions = new ArrayList<SootClass>();
                SootMethod method = new SootMethod(name, parameterTypes, returnType, modifiers, thrownExceptions);
                JimpleBody body = Jimple.v().newBody(method);
                //Add return; statement
                body.getUnits().add(Jimple.v().newReturnVoidStmt());
                method.setActiveBody(body);
                mut.getSootClass().addMethod(method);
                break;
            case CHANGE:
                break;
            case REMOVE:
                break;
            default:
                break;
        }
    }
}
