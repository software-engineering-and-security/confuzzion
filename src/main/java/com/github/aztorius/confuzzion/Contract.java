package com.github.aztorius.confuzzion;

import soot.jimple.JimpleBody;

public interface Contract {
    public void applyCheck(JimpleBody body);
}
