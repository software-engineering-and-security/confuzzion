package confuzziontest;

import com.github.aztorius.confuzzion.ByteClassLoader;
import com.github.aztorius.confuzzion.GenerationResult;
import com.github.aztorius.confuzzion.Mutant;
import com.github.aztorius.confuzzion.RandomGenerator;

import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import com.pholser.junit.quickcheck.generator.GenerationStatus;

public class ConfuzzionGenerator extends Generator<GenerationResult> {
    private ByteClassLoader loader;

    public ConfuzzionGenerator() {
        super(GenerationResult.class);

        loader = new ByteClassLoader();
    }

    @Override
    public GenerationResult generate(SourceOfRandomness random, GenerationStatus status) {
        Mutant mut = new Mutant();
        mut.generate(new RandomGenerator(random.toJDKRandom()));
        try {
            Class<?> clazz = mut.toClass(this.loader, mut.getSootClass());
            return new GenerationResult(clazz);
        } catch (Throwable e) {
            //TODO: DEBUG print e
            return new GenerationResult(null);
        }
    }
}
