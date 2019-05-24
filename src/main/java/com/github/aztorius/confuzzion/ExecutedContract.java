package com.github.aztorius.confuzzion;

import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.Jimple;

public class ExecutedContract implements Contract {
    public ExecutedContract() {

    }

    @Override
    public BodyMutation applyCheck(Body body) {
        BodyMutation mutation = new BodyMutation(body);
        SootClass exception = Scene.v().getSootClass(
            "com.github.aztorius.confuzzion.ContractCheckException");
        SootMethod mExceptionInit = exception.getMethodByName("<init>");
        Local locException =
            Jimple.v().newLocal("executedException", exception.getType());
        mutation.addLocal(locException);
        mutation.addUnit(
            Jimple.v().newAssignStmt(locException,
                                     Jimple.v().newNewExpr(
                                        exception.getType())));
        // Call locException constructor
        mutation.addUnit(
            Jimple.v().newInvokeStmt(
                Jimple.v().newSpecialInvokeExpr(locException,
                                                mExceptionInit.makeRef())));
        // Add throw statement
        mutation.addUnit(Jimple.v().newThrowStmt(locException));
        return mutation;
    }
}
