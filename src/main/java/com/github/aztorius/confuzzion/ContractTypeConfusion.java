package com.github.aztorius.confuzzion;

import soot.Body;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.UnitPatchingChain;
import soot.Value;
import soot.jimple.ClassConstant;
import soot.jimple.Jimple;
import soot.jimple.NullConstant;
import soot.util.Chain;

import java.util.ArrayList;

public class ContractTypeConfusion implements Contract {
    public ContractTypeConfusion() {

    }

    @Override
    public BodyMutation applyCheck(Body body) {
        BodyMutation mutation = new BodyMutation(body);
        Chain<Local> locals = body.getLocals();
        SootClass exception = Scene.v().getSootClass(
            "com.github.aztorius.confuzzion.ContractCheckException");
        SootMethod mExceptionInit = exception.getMethodByName("<init>");
        SootClass clazz = Scene.v().getSootClass("java.lang.Class");
        int a = 0;
        ArrayList<Local> newLocals = new ArrayList<Local>();

        for (Local local : locals) {
            Type type = local.getType();
            if (!RefType.class.isInstance(type)) {
                continue;
            }
            // If type is a reference to an object, then check the reference
            RefType refType = (RefType)type;
            SootClass sClass = refType.getSootClass();

            Unit uNop = Jimple.v().newNopStmt();
            // Check that local is not null, else abort contract checking
            mutation.addUnit(
                Jimple.v().newIfStmt(
                    Jimple.v().newEqExpr(local, NullConstant.v()),
                    uNop));

            // Get the class
            Local locClass =
                Jimple.v().newLocal("contracttc" + a++, clazz.getType());
            newLocals.add(locClass);
            mutation.addUnit(
                Jimple.v().newAssignStmt(locClass,
                                         ClassConstant.v(
                                             refType.getClassName().replace(".", "/"))));
            SootMethod isInstance = clazz.getMethodByName("isInstance");

            // Call isInstance on the class to check
            Value vIsInstance =
                Jimple.v().newVirtualInvokeExpr(locClass,
                                                isInstance.makeRef(),
                                                local);
            Local locBoolResult =
                Jimple.v().newLocal("contracttc" + a++, soot.BooleanType.v());
            newLocals.add(locBoolResult);
            mutation.addUnit(
                Jimple.v().newAssignStmt(locBoolResult, vIsInstance));

            // if isInstance return 0 then jump at the end
            // else throw Exception
            mutation.addUnit(
                Jimple.v().newIfStmt(
                    Jimple.v().newNeExpr(locBoolResult,
                                         soot.jimple.IntConstant.v(0)),
                    uNop));
            Local locException =
                Jimple.v().newLocal("contracttc" + a++, exception.getType());
            newLocals.add(locException);
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
            Unit uThrow = Jimple.v().newThrowStmt(locException);
            mutation.addUnit(uThrow);
            mutation.addUnit(uNop);
        }

        // Add new locals to chain
        for (Local newLocal : newLocals) {
            mutation.addLocal(newLocal);
        }

        return mutation;
    }
}
