package com.github.aztorius.confuzzion;

import soot.Body;
import soot.Local;
import soot.Unit;
import soot.ValueBox;

import java.util.ArrayList;
import java.util.List;

public class BodyMutation {
    protected Body body;
    private ArrayList<Local> addedLocals;
    private ArrayList<Unit> addedUnits;
    private Unit uReturn;

    public BodyMutation(Body body) {
        this.body = body;
        this.addedLocals = new ArrayList<Local>();
        this.addedUnits = new ArrayList<Unit>();
        this.uReturn = body.getUnits().getLast();
    }

    public void undo() {
        for (Local local : addedLocals) {
            this.body.getLocals().remove(local);
        }
        for (Unit unit : addedUnits) {
            this.body.getUnits().remove(unit);
        }
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

    public List<ValueBox> getUseBoxes() {
        List<ValueBox> boxes = new ArrayList<ValueBox>();
        for (Unit unit : addedUnits) {
            boxes.addAll(unit.getUseBoxes());
        }
        return boxes;
    }
}
