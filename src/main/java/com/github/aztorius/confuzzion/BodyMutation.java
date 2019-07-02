package com.github.aztorius.confuzzion;

import soot.Body;
import soot.Local;
import soot.Unit;
import soot.UnitPatchingChain;
import soot.ValueBox;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.List;

public class BodyMutation {
    protected Body body;
    private ArrayList<Local> addedLocals;
    private ArrayList<Unit> addedUnits;
    private Unit uReturn;

    public BodyMutation(Body body) {
        this.body = body;
        this.addedLocals = new ArrayList<Local>(10);
        this.addedUnits = new ArrayList<Unit>(10);
        this.uReturn = body.getUnits().getLast();
    }

    public void undo() {
        Chain<Local> locals = this.body.getLocals();
        for (Local local : addedLocals) {
            locals.remove(local);
        }
        addedLocals.clear();
        UnitPatchingChain units = this.body.getUnits();
        for (Unit unit : addedUnits) {
            units.remove(unit);
        }
        addedUnits.clear();
    }

    public void addLocal(Local local) {
        this.addedLocals.add(local);
        this.body.getLocals().add(local);
    }

    public void addUnit(Unit unit) {
        this.addUnitBefore(unit, uReturn);
    }

    public void addUnitBefore(Unit toInsert, Unit point) {
        this.addedUnits.add(toInsert);
        body.getUnits().insertBefore(toInsert, point);
    }

    public void addUnitAfter(Unit toInsert, Unit point) {
        this.addedUnits.add(toInsert);
        body.getUnits().insertAfter(toInsert, point);
    }

    public List<Local> getLocals() {
        return addedLocals;
    }

    public List<Unit> getUnits() {
        return addedUnits;
    }

    public List<ValueBox> getUseBoxes() {
        List<ValueBox> boxes = new ArrayList<ValueBox>();
        for (Unit unit : addedUnits) {
            boxes.addAll(unit.getUseBoxes());
        }
        return boxes;
    }
}
