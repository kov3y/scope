# Extension hooks

Hooks are the library's extension point. They let you attach side effects to the
**creation** and **disposal** of beans — event registration, instrumentation,
auditing — without the container knowing anything about your domain.

## `OnCreatedHook`

`OnCreatedHook` is a callback invoked every time the container creates a bean by
constructor injection:

```java
public interface OnCreatedHook {
    Disposer onCreated(BeanCreated event);
}
```

- [`BeanCreated`](#beancreated) carries the creation context.
- The returned [`Disposer`](#disposer) is registered on the owner scope and runs when
  that scope closes. Return `null` when no cleanup is needed.

### `BeanCreated`

A record with three fields:

```java
record BeanCreated(Scope<?> owner, Key<?> key, Object bean) {}
```

- `owner()` — the scope that owns and created the bean;
- `key()` — the key under which the bean is registered;
- `bean()` — the freshly created instance.

### `Disposer`

A functional cleanup callback, run by `Scope.close()`:

```java
@FunctionalInterface
public interface Disposer {
    void dispose() throws Exception;
}
```

Returning a `Disposer` from a hook is how you make a side effect **undo itself** when
the scope ends — the same deterministic, LIFO teardown as
[`@PreDestroy`](scopes-and-lifecycle.md#predestroy-and-autocloseable).

## A complete hook

A common pattern: auto-register any bean that implements `Listener` as a Bukkit
event listener, and unregister it when its scope closes. By convention a hook
**registers itself** in its constructor, so merely instantiating it activates it for
the scope:

```java
class BukkitEventHook implements OnCreatedHook {
    private final JavaPlugin plugin;

    public BukkitEventHook(Scope<?> s, JavaPlugin p) {
        this.plugin = p;
        s.addHook(BukkitEventHook.class); // register this hook in the scope
    }

    @Override
    public Disposer onCreated(BeanCreated event) {
        if (event.bean() instanceof Listener listener) {
            plugin.getServer().getPluginManager()
                .registerEvents(listener, plugin);
            return () -> HandlerList.unregisterAll(listener); // cleanup on close
        }
        return null; // not a Listener: nothing to do
    }
}
```

## Registering a hook

Register a hook with `addHook`, by type or by qualified key:

```java
scope.addHook(BukkitEventHook.class);
scope.addHook(Key.of(BukkitEventHook.class, "player")); // qualified
```

The container resolves the hook when each bean is created, following the **same
shadowing rules** as any other provider.

## Shadowing a hook

Because hooks obey shadowing, a child scope can **replace** a hook's behavior
locally by defining the same key nearer. Here a player-scoped subclass filters
events to one player and shadows the base hook with its own instance:

```java
class PlayerBukkitEventHook extends BukkitEventHook {
    private final Player player;

    public PlayerBukkitEventHook(Scope<?> s, JavaPlugin p, Player pp) {
        super(s, p);                          // registers BukkitEventHook.class as the hook key
        this.player = pp;
        s.seed(BukkitEventHook.class, this);  // shadow: in this scope BukkitEventHook.class → this
    }

    @Override
    public Disposer onCreated(BeanCreated event) {
        if (event.bean() instanceof Listener listener) {
            Listener filtered = filterByPlayer(listener, player);
            plugin.getServer().getPluginManager()
                .registerEvents(filtered, plugin);
            return () -> HandlerList.unregisterAll(filtered);
        }
        return null;
    }
}
```

> **Instance shadowing uses `seed`, not `bind`.** `seed(BukkitEventHook.class, this)`
> binds the key to an existing instance. (`bind(...)` only declares a type for later
> construction; it has no `(type, instance)` form.)

Under the hood, hook keys are collected with the
[`DEEP`](multi-parent.md#nearest-vs-deep) strategy across the whole visible scope
chain, deduplicated, then resolved in the creating scope — so the nearest definition
wins, exactly like ordinary resolution.

## Initialization order and `beginInitialization()`

During start-up the creation order of beans is undefined. If a hook is registered
**after** a bean was already created, that bean will never trigger `onCreated` for
it. `beginInitialization()` solves this: while the session is open, every
`BeanCreated` event is **buffered**, and the events are replayed in creation order
only when `commit()` is called.

```java
try (ScopeInitialization init = scope.beginInitialization()) {
    // Register all types in any order.
    for (Class<?> type : discoveredTypes) {
        scope.bind(type);
    }

    for (Class<?> type : discoveredTypes) {
        scope.get(type);   // eager instantiation — BeanCreated is buffered
    }
    init.commit();         // now every hook sees every bean, regardless of order
}
```

If the session is closed **without** `commit()` (an exception, an abort), the
buffered events are silently discarded and no hook fires.

### Full example: a plugin with dynamic player scopes

```java
record RootScope() {}
record JavaPluginScope() {}

Scope<RootScope> root = new Scope<>(new RootScope());

// In onEnable():
Scope<JavaPluginScope> jps = new Scope<>(new JavaPluginScope());
jps.ownedBy(root);
jps.seed(JavaPlugin.class, this);

List<Class<?>> types = List.of(MyListener.class, BukkitEventHook.class);

try (ScopeInitialization init = jps.beginInitialization()) {
    for (Class<?> type : types) {
        jps.bind(type);
    }

    for (Class<?> type : types) {
        jps.get(type);
    }
    init.commit(); // MyListener and BukkitEventHook see each other
}

// MyListener creates and destroys player scopes at runtime:
class MyListener implements Listener {
    private final JavaPlugin plugin;
    private final Scope<?> scope;
    private final Map<UUID, Scope<PlayerScope>> playerScopes = new HashMap<>();

    public MyListener(Scope<?> s, JavaPlugin p) { this.scope = s; this.plugin = p; }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        record PlayerScope(UUID uuid) {}
        Scope<PlayerScope> s = new Scope<>(new PlayerScope(event.getPlayer().getUniqueId()));
        s.ownedBy(scope);
        s.seed(Player.class, event.getPlayer());

        List<Class<?>> playerTypes = List.of(PlayerBukkitEventHook.class, MyPlayerListener.class);

        try (ScopeInitialization init = s.beginInitialization()) {
            for (Class<?> type : playerTypes) {
                s.bind(type);
            }

            for (Class<?> type : playerTypes) {
                s.get(type);
            }
            init.commit(); // PlayerBukkitEventHook shadows BukkitEventHook in this scope
        }

        playerScopes.put(event.getPlayer().getUniqueId(), s);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Scope<PlayerScope> s = playerScopes.remove(event.getPlayer().getUniqueId());
        if (s != null) s.close(); // unregisters every listener via the disposers
    }
}
```

Resulting scope structure:

```text
{ // Root
    { // PluginScope
        OnCreatedHookRegistration = [BukkitEventHook.class]
        Plugin          = JavaPlugin
        BukkitEventHook = BukkitEventHook
        MyListener      = MyListener

        { // PlayerScope (uuid=X)
            OnCreatedHookRegistration = [BukkitEventHook.class]  // merged, no duplicate
            BukkitEventHook           = PlayerBukkitEventHook    // shadows the parent
            MyPlayerListener          = MyPlayerListener
        }
    }
}
```

Each player scope shadows the base hook with a player-filtered version, and closing a
player scope on quit deterministically unregisters everything that scope created.

---

Next: [API reference](api-reference.md).
