package confuzzion;

import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;

public class AddFieldMutation extends ClassMutation {
    private SootField addedField;
    private InitializeMutation initializeMutation;

    public AddFieldMutation(RandomGenerator rand, SootClass sootClass) throws MutationException {
        super(rand, sootClass);

        String name = "field" + rand.nextIncrement();

        // Type can be a reference type
        Type type = rand.randType(sootClass.getName(), false, false);
        int modifiers = rand.randModifiers(true, true);
        this.addedField = new SootField(name, type, modifiers);
        sootClass.addField(this.addedField);

        // Call constructor inside <clinit> or <init>
        SootMethod meth = null;
        if (addedField.isStatic()) {
            meth = sootClass.getMethodByName("<clinit>");
        } else {
            meth = sootClass.getMethodByName("<init>");
        }

        try {
            initializeMutation = new InitializeMutation(rand, meth, addedField);
        } catch(MutationException e) {
            sootClass.removeField(this.addedField);
            throw e;
        }
    }

    @Override
    public void undo() {
        initializeMutation.undo();
        sootClass.removeField(this.addedField);
    }

    @Override
    public void randomConstants() {
        initializeMutation.randomConstants();
    }
}
