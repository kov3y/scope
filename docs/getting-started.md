# Getting started

This page walks through the smallest useful program and explains exactly what the
container does. For installation coordinates, see the
[main README](https://github.com/theking90000/scope#quick-start). The library
targets **Java 21** and lives under `be.theking90000.scope`.

## A first scope

```java
import be.theking90000.scope.Scope;

record RootScope() {}
record Config(String value) {}
record Service(Config config) {}

Scope<RootScope> root = new Scope<>(new RootScope());
root.seed(Config.class, new Config("prod"));

Service service = root.get(Service.class);
```

Every scope is parameterized by a **context object** (`RootScope` here). The
context identifies the scope and is itself injectable.

## What `get(Service.class)` does

When `root.get(Service.class)` is called:

1. The scope looks for a provider of `Service`.
2. It finds none, so it **creates `Service` automatically**.
3. The injector inspects the single public constructor of `Service`.
4. It resolves `Config` (found via the `seed` above).
5. It instantiates `Service(config)`.
6. The new instance is cached in the scope as a **scope singleton** — the next
   `get(Service.class)` returns the same object.

That is the whole loop: _look up, otherwise build by constructor injection, cache_.

Read as the lexical mental model, the scope is a block and `get` fills in the one
missing name:

```text
┌─ root : RootScope ───────────────────────────────────────────────┐
│                                                                  │
│  RootScope = (context)      ┐ auto-seeded                        │
│  Scope     = root           ┘ on creation                        │
│                                                                  │
│  Config    = Config("prod")   (seeded)                           │
│                                                                  │
│  Service   = Service(config)  ◄── built by get(), cached here    │
│              └─ needs Config ─► Config above                     │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

A type you never registered (like `Service` here) is built **in the scope you call
`get` on**. That choice decides its lifetime — see
[where an auto-created bean lands](injection.md#where-an-auto-created-bean-lands).

## What every scope already knows

A freshly constructed scope seeds two things automatically:

- its **context object** (here `RootScope`);
- **itself**, under `Scope.class`.

So a class created in this scope can inject either of them:

```java
record NeedsScope(RootScope root, Scope<?> scope) {}

NeedsScope n = root.get(NeedsScope.class);
// n.root()  == the RootScope context
// n.scope() == the nearest Scope (see shadowing rules)
```

## Three ways to put something in a scope

| Method                                                          | Use it for                                                                                  |
| --------------------------------------------------------------- | ------------------------------------------------------------------------------------------- |
| [`seed(type, instance)`](api-reference.md#registering-values)   | An object already created by your code (plugin, config, player, server handle).             |
| [`provide(type, factory)`](api-reference.md#registering-values) | A factory the container calls lazily the first time the value is needed.                    |
| [`bind(type)`](api-reference.md#registering-values)             | A type the container should construct later, _without_ validating its dependency graph now. |

```java
root.seed(Config.class, new Config("prod"));          // existing instance
root.provide(Clock.class, () -> new Clock(now()));    // lazy factory
root.bind(Service.class);                             // construct on first get()
```

All three return the scope, so calls chain:

```java
root.seed(Config.class, cfg)
    .provide(Clock.class, () -> new Clock(now()))
    .bind(Service.class);
```

---

Next: [Injection](injection.md) — the rules the container follows when it builds a
class for you.
