# Recipes

Short, copy-pasteable patterns, followed by a best-practices checklist.

## Declare first, provide later

When the registration order matters, `bind` a type before its dependencies exist —
the graph is only inspected on the first `get`:

```java
scope.bind(MyService.class);

// later, before the first resolution:
scope.seed(JavaPlugin.class, plugin);

MyService service = scope.get(MyService.class); // graph inspected here
```

Without `bind`, calling `provider(MyService.class)` or `get(MyService.class)` too
early forces the graph to build and may try to create a not-yet-registered
dependency locally.

## Lazy injection

Defer construction (or break a cycle) with `Provider<T>`:

```java
record Controller(Provider<Service> service) {
    void handle() {
        service.get().run(); // Service built on first use
    }
}
```

## Multiple implementations

Register many, consume all:

```java
interface Command {}
record StartCommand() implements Command {}
record StopCommand()  implements Command {}

scope.seed(Command.class, new StartCommand());
scope.seed(Command.class, new StopCommand());

record CommandRegistry(Provider<Iterable<Provider<Command>>> commands) {}
```

See [Qualifiers & collections](qualifiers-and-collections.md#collection-injection-by-constructor).

## A player scope

```java
record RootScope() {}
record Player(String name) {}
record PlayerData(Player player, RootScope root, Scope<?> scope) {}

Scope<RootScope> root = new Scope<>(new RootScope());
Scope<Player> player = new Scope<>(new Player("Ada"));

player.ownedBy(root);

PlayerData data = player.get(PlayerData.class);
```

`data.player()` comes from the player scope, `data.root()` from the parent, and
`data.scope()` is the player scope (the nearest `Scope`).

## A short-lived resource

For anything that must live *less* long than its parent, give it its own child scope
and close that child when done — never try to free a single object inside a scope:

```java
Scope<RequestCtx> request = new Scope<>(new RequestCtx(id));
request.ownedBy(serverScope);
try {
    handle(request.get(Handler.class));
} finally {
    request.close(); // deterministic cleanup of everything created for this request
}
```

## Best-practices checklist

- Use **`seed`** for instances your application already owns.
- Use **`bind`** when a type must be created later and its dependencies are
  registered afterward.
- Use **`Provider<T>`** to break cycles or defer an instantiation cost.
- Use **`@Named`** / **`Key.of(type, qualifier)`** as soon as there are several
  conceptual values of the same type.
- Avoid multi-parent scopes sharing the **same unqualified key**, unless the
  ambiguity is intentional and handled via `providers(...)`.
- Prefer **small, explicit scopes**: root, player, game, request, session, tenant,
  job.
- Create a **temporary child scope** for any resource that must outlive less than its
  parent, then close that child.
- Let the container **own** resources that need disposal: create them via
  `provide`/`bind`, not `seed`, so `close()` cleans them up.

---

Back to the [documentation index](README.md).
