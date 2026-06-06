package be.theking90000.scope;

/**
 * Supplies values to scopes and injected constructor parameters.
 *
 * @param <T> provided value type
 */
public interface Provider<T> {
    /**
     * Returns a value from this provider.
     *
     * @return provided value
     */
    T get();
}
