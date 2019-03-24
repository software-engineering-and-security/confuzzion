package confuzziontest;

import com.github.aztorius.confuzzion.Mutant;
import com.github.aztorius.confuzzion.RandomGenerator;

import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import com.pholser.junit.quickcheck.generator.GenerationStatus;

public class ConfuzzionGenerator extends Generator<Mutant> {
    public ConfuzzionGenerator(Class<Mutant> clazz) {
        super(clazz);
    }

    @Override
    public Mutant generate(SourceOfRandomness random, GenerationStatus status) {
        Mutant mut = new Mutant();
        mut.mutate(new RandomGenerator(random.toJDKRandom()));
        return mut;
    }
}
