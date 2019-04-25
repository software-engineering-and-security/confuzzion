package com.github.aztorius.confuzzion;

import soot.Body;

public abstract class Contract {
    public abstract BodyMutation applyCheck(Body body);
}
