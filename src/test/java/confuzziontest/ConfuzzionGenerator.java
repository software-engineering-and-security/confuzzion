package confuzziontest;

import com.github.aztorius.confuzzion.GenerationResult;
import com.github.aztorius.confuzzion.Mutant;
import com.github.aztorius.confuzzion.RandomGenerator;

import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import com.pholser.junit.quickcheck.generator.GenerationStatus;

public class ConfuzzionGenerator extends Generator<GenerationResult> {
    public ConfuzzionGenerator() {
        super(GenerationResult.class);
    }

    @Override
    public GenerationResult generate(SourceOfRandomness random, GenerationStatus status) {
        Mutant mut = new Mutant();
        mut.mutate(new RandomGenerator(random.toJDKRandom()));
        //Class<?> clazz = mut.toClass(mut.getSootClass());
        //return new GenerationResult(clazz);
        return new GenerationResult(mut);
    }
}
