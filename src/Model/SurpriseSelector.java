package Model;

import java.util.Objects;
import java.util.Random;

/** Draws one surprise according to the probabilities required by FR-38. */
public final class SurpriseSelector {
    private final Random random;

    /** Create a selector for normal gameplay. */
    public SurpriseSelector() {
        this(new Random());
    }

    /** Inject the random source so tests can verify every outcome reliably. */
    public SurpriseSelector(Random random) {
        this.random = Objects.requireNonNull(random);
    }

    /** Draw one outcome using its percentage weight. */
    public SurpriseType draw() {
        int ticket = random.nextInt(100);
        int cumulativeWeight = 0;

        for (SurpriseType type : SurpriseType.values()) {
            cumulativeWeight += type.probabilityPercent();
            if (ticket < cumulativeWeight) {
                return type;
            }
        }

        throw new IllegalStateException(
                "Surprise probabilities must cover all 100 tickets.");
    }
}