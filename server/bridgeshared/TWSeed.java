import java.util.Random;

/**
 * Seeds the terrain generators of inf-20100227-1433 and inf-20100313.
 *
 * Those builds create their terrain noise with "new Random()", so every run of the game makes a different
 * world, and chunks made after a server restart do not fit the saved ones. The server's class loader replaces
 * those calls (in the generator classes only) with {@link #random()}: the generators now get their random
 * numbers from the world's seed, so the same seed always gives the same terrain.
 */
public final class TWSeed {

    private static long base;
    /** One counter per class: the seeds do not depend on the order in which the classes start. */
    private static final java.util.Map<String, Integer> counters = new java.util.HashMap<String, Integer>();

    private TWSeed() {
    }

    /** Called by the bridge before the world is created. */
    public static synchronized void reset(long seed) {
        base = seed;
        counters.clear();
    }

    /** Replaces "new Random()" in the generator classes (owner: the name of the class that asks). */
    public static synchronized Random random(String owner) {
        Integer n = counters.get(owner);
        int count = n == null ? 1 : n + 1;
        counters.put(owner, count);
        return new Random(base + 0x9E3779B97F4A7C15L * count + owner.hashCode() * 0x632BE59BD9B4E019L);
    }
}
