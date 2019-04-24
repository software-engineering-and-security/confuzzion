package com.github.aztorius.confuzzion;

import soot.SootClass;
import soot.SootMethod;

import java.util.ArrayList;

/* A Program is a collection of classes with at least one "main" class.
 * Each class is represented as a Mutant that can evolve.
 * The evolution is represented as a Mutation and apply only to one of the
 * Mutant.
 */
public class Program {
    private String classBaseName;
    private ArrayList<Mutant> mutants;
    private RandomGenerator rand;

    public Program(String classBaseName, RandomGenerator rand) {
        // Initialize class fields
        this.classBaseName = classBaseName;
        this.rand = rand;
        this.mutants = new ArrayList<Mutant>();

        // Create first empty Mutant (main class)
        Mutant firstMutant = new Mutant(classBaseName + "0");
        firstMutant.genEmptyClass(rand);
        this.mutants.add(firstMutant);
    }

    /* Instatiate the main class and call all methods on it.
     */
    public void launch() {
        //TODO
    }
}
