# Scopes & lifecycle

This page covers how scopes connect to one another, how resolution shadows parent
values, and how a scope is torn down deterministically when it closes.

## Parents and shadowing

A scope can see its **visible parents**. A class created in a child resolves its
dependencies against the child first, then the parents.

```java
record RootScope() {}
record Player(String name) {}
record Service(Player player, RootScope root) {}

Scope<RootScope> root = new Scope<>(new RootScope());

Scope<Player> playerScope = new Scope<>(new Player("Ada"));
playerScope.ownedBy(root);

Service service = playerScope.get(Service.class);
```

`Service` sees:

- `Player` from `playerScope`;
- `RootScope` from `root`;
- `Scope.class` shadowed by the nearest scope, so injecting `Scope<?>` yields
  `playerScope`, not `root`.

When a child defines the **same key** as a parent, the child wins:

```java
root.seed(Config.class, new Config("root"));
playerScope.seed(Config.class, new Config("player"));

assert playerScope.get(Config.class).value().equals("player");
```

This is lexical shadowing: the nearest definition along a branch is the one used.

## Ownership vs visibility

`attach(parent, owns, visible)` configures two **independent** flags on the edge
from a child to a parent:

- `owns` — the parent closes this scope when the parent closes;
- `visible` — this scope may resolve providers through that parent.

Helpers cover the common combinations:

```java
child.ownedBy(parent);        // owns = true,  visible = true
child.weakRef(parent);        // owns = false, visible = true
child.detachWeakRef(parent);  // remove a visible weak reference (the inverse of weakRef)
```

- **`ownedBy`** — the normal parent/child relationship: visible *and* lifetime-bound.
- **`weakRef`** — "I want to *see* this scope, but it does not own me and I do not
  own it." Useful for cross-context visibility without coupling lifetimes (see
  [Multi-parent scopes](multi-parent.md)).
- **`detachWeakRef`** — removes a weak reference added by `weakRef`. Throws
  [`ScopeConflictException`](exceptions.md) if no matching weak reference exists.

The graph must stay **acyclic**: an attach that would create a cycle throws
[`ScopeCycleException`](exceptions.md). Attaching a second open owned child under
the same context throws [`ScopeConflictException`](exceptions.md).

### Navigating the graph

```java
Scope<RootScope> r = playerScope.findParent(new RootScope()); // visible ancestor by context
Scope<Player>    p = root.getChild(new Player("Ada"));        // owned descendant by context
```

`findParent` searches visible (open) ancestors; `getChild` searches owned
descendants. Both match on the context object and return `null` when nothing
matches.

## Closing a scope

`close()` is **blocking and sequential**. When a scope closes:

1. its **owned children are closed first** (in reverse order of attachment);
2. the **cleanup callbacks** (disposers) of beans it created run in **LIFO** order
   (reverse order of allocation);
3. its **local providers are cleared**;
4. it is **detached** from the parents that own it;
5. any further operation on it throws [`ScopeStateException`](exceptions.md).

```java
scope.close();
```

During teardown the scope is in state `CLOSING`; it becomes `CLOSED` only once every
owned child and every local disposer has finished. `close()` is idempotent — calling
it again on a `CLOSED` scope is a no-op.

If a cleanup fails, the scope still attempts all the other cleanups, then `close()`
throws a [`ScopeException`](exceptions.md) carrying the failures as **suppressed
exceptions**.

## Bean lifecycle hooks

Beans created automatically by constructor injection are owned by the scope that
creates them. The container scans the bean's actual class — including superclasses
and private methods — for lifecycle hooks.

### `@PostConstruct`

Marks a no-argument method to call **right after** construction:

```java
public class Service {
    private boolean started;

    public Service() {}

    @PostConstruct
    private void start() {
        started = true;
    }
}
```

- Supported signatures: `void method()` or `Void method()`.
- Superclass hooks run **before** subclass hooks.
- If a `@PostConstruct` throws, bean creation fails with
  [`BeanCreationException`](exceptions.md) and the instance is **not** exposed as a
  scope singleton.

### `@PreDestroy` and `AutoCloseable`

Marks a no-argument method to call when the **owning scope closes**:

```java
public class Service {
    @PreDestroy
    private void stop() {
        flush();
    }
}
```

- Supported signatures: `void method()` or `Void method()`.
- If a bean implements `AutoCloseable` (or `Closeable`), its `close()` is registered
  automatically, even without `@PreDestroy`. If `close()` is itself annotated
  `@PreDestroy`, it is not registered twice.
- Cleanups run **LIFO**. For an inheritance chain, hooks declared on the subclass run
  before those on the superclass. Annotated hooks run before the automatic `close()`.

### Seeded values are *not* auto-disposed

Values registered with `seed(...)` stay owned by the caller — they are **not** part
of the scope's cleanup. If a resource should be owned and closed by the container,
create it through a `provide(...)` factory or `bind(...)` in the scope that
represents its lifetime, so the container — not your calling code — owns it.

---

Next: [Multi-parent scopes](multi-parent.md).
