# Multi-parent scopes

A scope is not limited to a strict tree. It can have **several visible parents**,
forming a directed acyclic graph (DAG). This is what lets an object live at the
intersection of several contexts.

## A scope at a crossroads

A player *inside a game* lives at the intersection of two contexts — the game and
the player's global presence:

```java
record RootScope() {}
record Player(String name) {}
record GameScope(String name) {}
record GamePlayerScope(Player player, Game game) {}

Scope<RootScope> root = new Scope<>(new RootScope());

Scope<Player> globalPlayerScope = new Scope<>(new Player("Ada"));
globalPlayerScope.ownedBy(root);

Scope<GameScope> gameScope = new Scope<>(new GameScope("Arena"));
gameScope.ownedBy(root);

Scope<GamePlayerScope> gamePlayerScope =
    new Scope<>(new GamePlayerScope(new Player("Ada"), game));

gamePlayerScope.ownedBy(gameScope);          // owned by the game
gamePlayerScope.ownedBy(globalPlayerScope);  // and visible from the global player scope
```

A class created in `gamePlayerScope` can see:

- the player-in-game local values;
- the `gameScope` values;
- the global player-scope values;
- the `root` values, visible through either parent.

This is powerful for cross-cutting contexts: **player + game**, **request + tenant**,
**job + user**, **session + module**.

> `ownedBy` ties lifetime *and* visibility. When you want a parent to be **visible
> without owning** the child (or being owned by it), use `weakRef` instead — see
> [ownership vs visibility](scopes-and-lifecycle.md#ownership-vs-visibility).

## The ambiguity problem

With several visible parents, two of them can provide the **same key**. Resolving a
single value is then ambiguous:

```java
english.seed(Message.class, new Message("Hello"));
french.seed(Message.class, new Message("Bonjour"));

quebec.ownedBy(english);
quebec.ownedBy(french);

quebec.get(Message.class); // AmbiguousBeanException
```

Three ways to resolve it:

**1. Shadow it locally** — define the key nearer, so it wins:

```java
quebec.seed(Message.class, new Message("Salut"));
quebec.get(Message.class); // "Salut"
```

**2. Qualify each side** with a [`Key`](qualifiers-and-collections.md):

```java
english.seed(Key.of(Message.class, "en"), new Message("Hello"));
french.seed(Key.of(Message.class, "fr"), new Message("Bonjour"));
```

**3. Ask for all of them** instead of one:

```java
for (Provider<Message> message : quebec.providers(Message.class).get()) {
    System.out.println(message.get().text());
}
```

## `NEAREST` vs `DEEP`

Provider lookup walks the visible parents using a `Scope.Collect` strategy.

By default, `Scope` uses **`NEAREST`**: on each visible branch, the search stops as
soon as a definition is found. This is the lexical shadowing model — the nearest
definition wins.

```java
scope.providers(Message.class);                       // NEAREST (default)
scope.providers(Message.class, Scope.Collect.NEAREST);
```

**`DEEP`** keeps walking *past* a definition to also collect providers higher up:

```java
scope.providers(Message.class, Scope.Collect.DEEP);
```

Use `NEAREST` for normal resolution. Use `DEEP` when the caller wants to **inspect or
aggregate every visible contribution** of a type across the whole graph — for
example, collecting all registered handlers from every enclosing scope. (`DEEP` is
also how the container itself merges [extension hooks](extension-hooks.md) across
the scope chain.)

---

Next: [Extension hooks](extension-hooks.md).
