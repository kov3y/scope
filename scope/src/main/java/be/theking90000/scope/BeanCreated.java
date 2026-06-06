package be.theking90000.scope;

/**
 * Event record emitted by the container whenever a bean is created by constructor injection.
 *
 * <p>Delivered to registered {@link OnCreatedHook} implementations either immediately
 * (normal mode) or batched via a {@link ScopeInitialization} session.
 *
 * @param owner the scope that owns and created the bean
 * @param key   the key under which the bean is registered
 * @param bean  the newly created instance
 * @see OnCreatedHook
 * @see ScopeInitialization
 */
public record BeanCreated(
    Scope<?> owner,
    Key<?> key,
    Object bean)
{ }
