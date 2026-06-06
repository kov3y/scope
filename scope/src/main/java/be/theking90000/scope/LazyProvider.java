package be.theking90000.scope;

/**
 * Provider placeholder that materializes a bound key only when first requested.
 *
 * @param <T> provided value type
 */
class LazyProvider<T> implements Provider<T> {
    private final Scope<?> scope;
    private final Key<T> key;
    private Provider<T> provider;
    private boolean resolving = false;

    /**
     * Creates a lazy provider for a key in a scope.
     *
     * @param scope scope that owns the binding
     * @param key bound key to materialize
     */
    LazyProvider(Scope<?> scope, Key<T> key) {
        this.scope = scope;
        this.key = key;
    }

    /**
     * Materializes the bound key on first use, then delegates to the real provider.
     *
     * @return provided value
     */
    @Override
    public T get() {
        if (resolving) {
            throw new BeanResolutionException("Circular dependency detected while resolving bound key " + key);
        }

        resolving = true;
        try {
            if (provider == null) {
                provider = scope.materializeBound(key);
            }

            return provider.get();
        } finally {
            resolving = false;
        }
    }

    /**
     * Returns a debug representation of this provider.
     *
     * @return debug representation
     */
    @Override
    public String toString() {
        return "LazyProvider[key=" + key + ",provider=" + provider + "]";
    }
}
