package com.github.aztorius.confuzzion;

import soot.Body;
import soot.BooleanType;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.ClassConstant;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.NullConstant;

import java.util.ArrayList;

public class ContractTypeConfusion implements Contract {
    public ContractTypeConfusion() {

    }

    @Override
    public BodyMutation applyCheck(Body body) {
        BodyMutation mutation = new BodyMutation(body);
        SootClass exception = Util.getOrLoadSootClass("com.github.aztorius.confuzzion.ContractCheckException");
        SootMethod mExceptionInit = exception.getMethodByName("<init>");
        SootClass clazz = Scene.v().getSootClass("java.lang.Class");
        SootMethod isInstance = clazz.getMethodByName("isInstance");
        int a = 0;
        ArrayList<Local> newLocals = new ArrayList<Local>();
        ArrayList<Value> values = new ArrayList<Value>(10);

        for (Local local : body.getLocals()) {
            if (local.getType() instanceof RefType) {
                values.add(local);
            }
        }

        if (!body.getMethod().isStatic()) {
            // Check also all fields
            Local thisLocal = body.getThisLocal();
            for (SootField field : body.getMethod().getDeclaringClass().getFields()) {
                if (field.getType() instanceof RefType) {
                    Local local = Jimple.v().newLocal("contracttc" + a++, field.getType());
                    if (field.isStatic()) {
                        mutation.addUnit(
                                Jimple.v().newAssignStmt(local,
                                        Jimple.v().newStaticFieldRef(field.makeRef())));
                    } else {
                        mutation.addUnit(
                                Jimple.v().newAssignStmt(local,
                                        Jimple.v().newInstanceFieldRef(thisLocal, field.makeRef())));
                    }
                    newLocals.add(local);
                    values.add(local);
                }
            }
        }

        for (Value value : values) {
            Type type = value.getType();
            // If type is a reference to an object, then check the reference
            RefType refType = (RefType)type;

            Unit uNop = Jimple.v().newNopStmt();
            // Check that local is not null, else abort contract checking
            mutation.addUnit(
                    Jimple.v().newIfStmt(
                            Jimple.v().newEqExpr(value, NullConstant.v()),
                            uNop));

            // Get the class
            Local locClass = Jimple.v().newLocal("contracttc" + a++, clazz.getType());
            newLocals.add(locClass);
            mutation.addUnit(
                    Jimple.v().newAssignStmt(locClass,
                            ClassConstant.v(refType.getClassName().replace(".", "/"))));

            // Call isInstance on the class to check
            Value vIsInstance =
                    Jimple.v().newVirtualInvokeExpr(locClass,
                            isInstance.makeRef(),
                            value);
            Local locBoolResult =
                    Jimple.v().newLocal("contracttc" + a++, BooleanType.v());
            newLocals.add(locBoolResult);
            mutation.addUnit(Jimple.v().newAssignStmt(locBoolResult, vIsInstance));

            // if isInstance return 1 then jump at the end
            // else throw Exception
            mutation.addUnit(
                    Jimple.v().newIfStmt(
                            Jimple.v().newNeExpr(locBoolResult, IntConstant.v(0)),
                            uNop));
            Local locException =
                    Jimple.v().newLocal("contracttc" + a++, exception.getType());
            newLocals.add(locException);
            mutation.addUnit(
                    Jimple.v().newAssignStmt(locException,
                            Jimple.v().newNewExpr(exception.getType())));
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
