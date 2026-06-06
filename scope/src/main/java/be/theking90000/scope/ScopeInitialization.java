package be.theking90000.scope;

/**
 * A try-with-resources handle for batch-initializing a scope.
 *
 * <p>While a session is open, {@link OnCreatedHook} callbacks are buffered instead of
 * being fired immediately.  This allows beans and hooks to be registered in any order:
 * once {@link #commit()} is called, all buffered {@link BeanCreated} events are replayed
 * in creation order so that every hook sees every bean.
 *
 * <p>Internally the session creates a temporary outer scope attached to the target scope
 * via a weak reference (no ownership):
 *
 * <pre>
 * { // initScope (temporary, weakRef only) — holds the ScopeInitialization instance
 *     { // child scope — normal binds/seeds/gets happen here
 *     }
 * }
 * </pre>
 *
 * <p>The child scope detects the active session via provider lookup on
 * {@code InitializationSession.class} and routes new {@link BeanCreated} events into
 * the buffer.  On {@link #commit()} or {@link #close()}, the temporary scope is detached
 * and closed.
 *
 * <p>If the session is closed <em>without</em> committing (for example because an
 * exception aborted initialization), buffered events are discarded and no hooks fire.
 *
 * @see Scope#beginInitialization()
 * @see OnCreatedHook
 * @see BeanCreated
 */
public interface ScopeInitialization extends AutoCloseable {

    /**
     * Detaches the temporary initialization scope and replays all buffered
     * {@link BeanCreated} events through the registered {@link OnCreatedHook}s.
     *
     * @throws InitializationException if this session has already been committed
     */
    void commit();

    /**
     * Closes the initialization session.
     *
     * <p>If {@link #commit()} has not been called, buffered events are discarded.
     * The temporary initialization scope is always detached and closed.
     */
    @Override
    void close() throws Exception;
}
