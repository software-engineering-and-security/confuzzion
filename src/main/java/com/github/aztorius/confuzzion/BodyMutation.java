package com.github.aztorius.confuzzion;

import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Jimple;

import java.util.List;

public class BodyMutation implements Mutation {
    @Override
    public void apply(Mutant mut, RandomGenerator rand, MutationType mtype) {
        switch (mtype) {
            case ADD:
                List<SootMethod> methods = mut.getSootClass().getMethods();
                if (methods.size() == 0) {
                    return;
                }
                SootMethod method = methods.get(rand.randUint() % methods.size());
                Body body = method.getActiveBody();
                int value = rand.randLimits(1.0);
                switch (value) {
                    case 0:
                        this.addLocal(mut, body, rand);
                        break;
                    case 1:
                        //TODO
                        break;
                    case 2:
                        //TODO
                        break;
                    default:
                        break;
                }
                break;
            case CHANGE:
                break;
            case REMOVE:
                break;
            default:
                break;
        }
    }

    public void addLocal(Mutant mut, Body body, RandomGenerator rand) {
        //TODO: add objects
        body.getLocals().add(Jimple.v().newLocal("l" + mut.nextInt(), rand.randPrimType()));
    }

    public void addUnit(Body body, Unit unit) {
        body.getUnits().addLast(unit);
    }

    public void addAssign(Mutant mut, Body body, RandomGenerator rand) {
        //body.getUnits().add(Jimple.v().newAssignStmt());
    }

    //TODO: new Jimple.v().newInvokeStmt();

    //TODO: new try catch statements
}
