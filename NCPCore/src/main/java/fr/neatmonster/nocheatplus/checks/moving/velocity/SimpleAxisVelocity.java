package fr.neatmonster.nocheatplus.checks.moving.velocity;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Simple per-axis velocity (positive + negative), only accounting for queuing
 * and invalidation. Since entries just wrap values for one time use, no extra
 * ticking is done, invalidation mechanics and activation count decreasing takes
 * place in removeInvalid.
 * 
 * @author asofold
 *
 */
public class SimpleAxisVelocity {

    /** Margin for accepting a demanded 0.0 amount, regardless sign. */
    private static final double marginAcceptZero = 0.005;

    private final List<SimpleEntry> queued = new LinkedList<SimpleEntry>();

    public void add(SimpleEntry entry) {
        queued.add(entry);
    }

    public boolean hasQueued() {
        return !queued.isEmpty();
    }

    /**
     * Use the next matching entry.
     * 
     * @param amount
     * @param tolerance
     *            Allow using entries with less amount (still sign-specific).
     *            Must be equal or greater than 0.0.
     * @return The first matching entry. Returns null if no entry is available.
     *         This will directly invalidate leading entries with the wrong
     *         sign.
     */
    public SimpleEntry use(final double amount, final double tolerance) {
        final Iterator<SimpleEntry> it = queued.iterator();
        while (it.hasNext()) {
            final SimpleEntry entry = it.next();
            it.remove();
            if (matchesEntry(entry, amount, tolerance)) {
                // Success.
                return entry;
            } 
            // (Entry can not be used.)
            // TODO: Note unused velocity.
        }
        // None found.
        return null;
    }

    /**
     * Check if the demanded amount can be covered by this velocity entry. Might
     * return an entry with a small value with a different sign, if amount is
     * set to 0.0. Needed also for testing stored entries.
     * 
     * @param entry
     * @param amount
     * @param tolerance
     *            Allow using entries with less amount (still sign-specific).
     *            Must be equal or greater than 0.0.
     * @return
     */
    public boolean matchesEntry(final SimpleEntry entry, final double amount, final double tolerance) {
        return Math.abs(amount) <= Math.abs(entry.value) + tolerance && 
                (amount > 0.0 && entry.value > 0.0 && amount <= entry.value + tolerance 
                || amount < 0.0 && entry.value < 0.0 && entry.value - tolerance <= amount 
                || amount == 0.0 && Math.abs(entry.value) <= marginAcceptZero);
    }

    /**
     * Remove all entries that have been added before the given tick, or for which the activation count has reached 0.
     * @param tick
     */
    public void removeInvalid(final int tick) {
        // Note: clear invalidated here, append unused to invalidated.
        final Iterator<SimpleEntry> it = queued.iterator();
        while (it.hasNext()) {
            final SimpleEntry entry = it.next();
            entry.actCount --; // Let others optimize this.
            if (entry.actCount <= 0 || entry.tick < tick) {
                it.remove();
            }
        }
    }

    public void clear() {
        queued.clear();
    }

    /**
     * Debugging.
     * @param builder
     */
    public void addQueued(final StringBuilder builder) {
        for (final SimpleEntry vel: queued) {
            builder.append(" ");
            builder.append(vel);
        }
    }

}
