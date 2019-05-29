package com.github.aztorius.confuzzion;

public enum Status {
    SUCCESS, /* Mutation success, and execution return no error */
    FAILED, /* Mutation failed, no execution. */
    NOTEXECUTED, /* Mutation succeed but the code is not executed. */
    CRASHED, /* Mutation success but execution crashed. */
    INTERRUPTED, /* Mutation succeed but the execution has been interrupted. */
    VIOLATES /* Mutation and execution success, but violates a contract. */
}
