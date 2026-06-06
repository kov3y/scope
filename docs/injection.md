# Injection

When a scope has no provider for a requested key, it builds the value by
**constructor injection**. This page covers the rules, the parameter shapes you can
inject, and how lazy injection breaks cycles.

## The one-constructor rule

An injectable class must have **exactly one public constructor**. The container
inspects that constructor, resolves each parameter through the scope, and calls it.

```java
public class Service {
    private final Repository repository;

    public Service(Repository repository) {
        this.repository = repository;
    }
}
```

Records work naturally — their canonical constructor is that single public
constructor:

```java
public record Service(Repository repository) {}
```

## Supported parameter shapes

Each constructor parameter may take any of these forms:

```java
TypeX value                                  // resolve a single TypeX
Provider<TypeX> lazyValue                     // resolve lazily, on first get()
Provider<Iterable<Provider<TypeX>>> all       // resolve all providers of TypeX
@Named("main") TypeX namedValue               // resolve a qualified TypeX
@Named("main") Provider<TypeX> namedLazyValue // qualified + lazy
```

- `Provider<T>` — see [Lazy injection](#lazy-injection) below.
- `Provider<Iterable<Provider<T>>>` — see
  [Qualifiers & collections](qualifiers-and-collections.md).
- `@Named(...)` — see [Qualifiers & collections](qualifiers-and-collections.md).

## Unsupported cases

The container will refuse (with [`UnsupportedInjectionException`](exceptions.md) or
a resolution error) to build:

- local or anonymous classes;
- non-static inner classes;
- non-concrete generic types such as `Provider<List<T>>`;
- a class without a single public constructor;
- a **direct, non-lazy cycle** (see below).

## Lazy injection

A `Provider<T>` parameter is resolved lazily: the dependency is not created until
`provider.get()` is called.

```java
record Controller(Provider<Service> service) {
    void handle() {
        service.get().run(); // Service is built here, not at Controller construction
    }
}
```

Use it to defer an expensive construction, or to break a cycle.

## Cycles

A **direct cycle** cannot be constructed — neither side can be built first:

```java
record C1(C2 c2) {}
record C2(C1 c1) {}

scope.get(C1.class); // BeanResolutionException
```

A **lazy cycle** through `Provider<T>` is allowed, because the provider defers
construction of the other side:

```java
record A1(Provider<A2> a2) {}
record A2(Provider<A1> a1) {}

A1 a1 = scope.get(A1.class);
A2 a2 = a1.a2().get();

assert a2.a1().get() == a1; // same scope singletons
```

The rule of thumb: if two types must reference each other, put a `Provider<>` on at
least one side.

## Where an auto-created bean lands

This is the part people miss. When you call `get(T)` / `provider(T)` and **no
provider for `T` exists anywhere in the visible graph** — you never `seed`,
`provide` or `bind` it — the container builds `T` by constructor injection and
registers it **in the current scope**: the exact scope you called `get` on. The
same goes for any of `T`'s dependencies that are _also_ unregistered — each missing
one is created locally too. Dependencies that _do_ exist in a parent are reused from
there.

```java
record RootScope() {}
record Config(String value) {}
record Service(Config config) {}

Scope<RootScope> root = new Scope<>(new RootScope());
root.seed(Config.class, new Config("prod"));

Service service = root.get(Service.class); // Service is registered nowhere
```

Mental model — `Service` materializes _inside_ the scope that asked for it:

```text
root.get(Service.class)

┌─ root : RootScope ─────────────────────────────────────────────────────────────┐
│                                                                                │
│  Config  = Config("prod")        (seeded)                                      │
│                                                                                │
│  Service = Service(config)   ◄── created HERE  the scope that called get()     │
│             └─ needs Config ─┐                                                 │
│                              └─► Config above  (reused, already local)         │
│                                                                                │
└────────────────────────────────────────────────────────────────────────────────┘
```

### Why the _current_ scope matters

The bean is **owned by** the scope that created it, so it dies when that scope
closes — even if all of its dependencies came from a parent. Resolve from a child
and the bean lives and dies with the child:

```java
Scope<RootScope> root  = new Scope<>(new RootScope());
root.seed(Config.class, new Config("prod"));

Scope<Player> child = new Scope<>(new Player("Ada"));
child.ownedBy(root);

Service s = child.get(Service.class); // Service unregistered; Config lives in root
```

```text
┌─ root ────────────────────────────┐
│                                   │
│  Config = Config("prod")          │   ← Config stays in root
│                                   │
└───────────────────────────────────┘
            ▲
            │ visible (ownedBy)
            │  reuses Config
            │
┌─ child : Player ──────────────────┐
│                                   │
│  Service = Service(config)        │   ← created in CHILD (the caller),
│            └─ Config from root    │      owned by child, gone on child.close()
│                                   │
└───────────────────────────────────┘
```

So _where you call `get` from_ decides the bean's lifetime. If a type should be a
singleton at the root, resolve it from the root (or `bind`/`provide` it there) — not
from a short-lived child. This is the same shadowing model as everything else: a
bean's dependencies see its scope first, then visible parents
([Scopes & lifecycle](scopes-and-lifecycle.md)).

---

Next: [Qualifiers & collections](qualifiers-and-collections.md).
