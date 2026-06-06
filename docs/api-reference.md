# API reference

A compact map of the public surface. For exact generics, edge cases and per-method
JavaDoc, see the full [JavaDoc](README.md#javadoc). Package: `be.theking90000.scope`.

## `Scope<C>`

`Scope<C>` is the lifetime-and-resolution container, parameterized by its context
type `C`. It implements `AutoCloseable`. Every registration method returns the scope
for chaining.

### Construction

| Member | Description |
| ------ | ----------- |
| `Scope(C context)` | Creates a scope and **seeds** the context object and the scope itself (`Scope.class`). |

### Registering values

| Member | Description |
| ------ | ----------- |
| `seed(Class<V> type, V value)` | Register an already-created instance for a type. |
| `seed(Key<V> key, V value)` | Same, for a qualified key. |
| `provide(Class<V> type, Provider<V> provider)` | Register a factory for a type; called lazily. |
| `provide(Key<V> key, Provider<V> provider)` | Same, for a qualified key. |
| `bind(Class<V> type)` | Declare a type to be constructed later; its dependency graph is **not** validated now. |
| `bind(Key<V> key)` | Same, for a qualified key. |

> There is no `bind(type, instance)` overload. To register an existing instance use
> `seed`.

### Resolving values

| Member | Description |
| ------ | ----------- |
| `get(Class<V> type)` | Resolve a single value, creating it by constructor injection if needed. |
| `get(Key<V> key)` | Same, for a qualified key. |
| `provider(Class<V> type)` | Resolve a `Provider<V>` (lazy handle) instead of the value. |
| `provider(Key<V> key)` | Same, for a qualified key. |
| `providers(Class<V> type)` | Resolve **all** nearest providers as a `MultiProvider<V>`. |
| `providers(Key<V> key)` | Same, for a qualified key. |
| `providers(Class<V> type, Collect mode)` | All providers using a [collection strategy](multi-parent.md#nearest-vs-deep). |
| `providers(Key<V> key, Collect mode)` | Same, for a qualified key. |

`get` / `provider` throw [`NoSuchBeanException`](exceptions.md) when nothing
resolves and [`AmbiguousBeanException`](exceptions.md) when more than one nearest
provider matches.

### Graph: ownership & visibility

| Member | Description |
| ------ | ----------- |
| `attach(Scope<?> parent, boolean owns, boolean visible)` | Add a parent edge with explicit ownership/visibility. |
| `ownedBy(Scope<?> parent)` | `attach(parent, true, true)` — visible and lifetime-bound. |
| `weakRef(Scope<?> parent)` | `attach(parent, false, true)` — visible, not owned. |
| `detachWeakRef(Scope<?> parent)` | Remove a weak reference added by `weakRef`. |
| `findParent(T context)` | Find a visible open ancestor by context object (or `null`). |
| `getChild(T context)` | Find an owned descendant by context object (or `null`). |

Invalid attachments throw [`ScopeCycleException`](exceptions.md) or
[`ScopeConflictException`](exceptions.md).

### Extension & lifecycle

| Member | Description |
| ------ | ----------- |
| `addHook(Class<? extends OnCreatedHook> hookType)` | Register a creation [hook](extension-hooks.md) by type. |
| `addHook(Key<? extends OnCreatedHook> hookKey)` | Register a hook by qualified key. |
| `beginInitialization()` | Open a [batch-initialization session](extension-hooks.md#initialization-order-and-begininitialization) (`ScopeInitialization`). |
| `close()` | Tear the scope down: owned children, disposers (LIFO), providers; detach from owners. |

### `Scope.Collect`

The parent-traversal strategy used by `providers(..., mode)`:

| Value | Meaning |
| ----- | ------- |
| `NEAREST` | Stop at the first scope that provides the key on each branch (default; lexical shadowing). |
| `DEEP` | Keep walking past a match to collect providers higher up too. |

## `Key<T>`

`record Key<T>(Class<T> type, Object qualifier)` — a bean identifier.

| Member | Description |
| ------ | ----------- |
| `Key.of(Class<T> type)` | Unqualified key. |
| `Key.of(Class<T> type, Object qualifier)` | Qualified key (qualifier is any object). |
| `type()` / `qualifier()` | Record accessors. |

## `Provider<T>`

`interface Provider<T>` — supplies values lazily.

| Member | Description |
| ------ | ----------- |
| `T get()` | Return a value (created on demand for constructor-injected providers). |

## `MultiProvider<T>`

`class MultiProvider<T> implements Provider<Iterable<Provider<T>>>` — the group
returned by `providers(...)`.

| Member | Description |
| ------ | ----------- |
| `Iterable<Provider<T>> get()` | The providers in the group. |
| `boolean isEmpty()` | True when the group has no providers. |
| `boolean isSingle()` | True when the group has exactly one provider. |

## Extension types

| Type | Description |
| ---- | ----------- |
| `OnCreatedHook` | `Disposer onCreated(BeanCreated event)` — see [Extension hooks](extension-hooks.md). |
| `BeanCreated` | `record BeanCreated(Scope<?> owner, Key<?> key, Object bean)`. |
| `Disposer` | `@FunctionalInterface void dispose() throws Exception` — cleanup run by `close()`. |
| `ScopeInitialization` | `AutoCloseable` session with `commit()`; from `beginInitialization()`. |
| `@Named(String value)` | Qualify an injected constructor parameter. |
| `@PostConstruct` | No-arg method run after construction. |
| `@PreDestroy` | No-arg method run when the owning scope closes. |

For the exception types, see [Exceptions](exceptions.md).
