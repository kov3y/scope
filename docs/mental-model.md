# Mental model

`scope` has one central idea: **an instance is created inside a `Scope`, and that
scope defines what it can see**. Everything else — dependency injection,
lifecycles, hooks — follows from this.

## Scopes as language blocks

A `Scope` reads like a lexical block in JavaScript, Rust or Java. A value defined
in an outer block is visible to inner blocks, unless an inner block defines a
nearer value under the same key — in which case the nearer one **shadows** it.

```text
// RootScope
{
    x = value;

    // PlayerScope  (nested inside RootScope)
    {
        player = value;
        classes = ...;

        // An object created here sees:
        // - player and classes locally;
        // - x from the parent;
        // - any local definition that shadows a parent definition.
    }

    // GameScope
    {
        game = value;
        players = List<Scope<Player>>;

        // Player1Scope
        { player = player1; }

        // Player2Scope
        { player = player2; }
    }
}
```

This classic view is a **tree**: a child inherits the providers visible from its
parent. Resolution uses the **nearest** definition by default (the `NEAREST`
strategy). Exactly like a local variable, a local value shadows a parent's value.

The difference with a compiler's lexical scope: here the blocks are **objects**.
They are opened, nested, shadowed and closed at runtime — and a scope may even have
[several parents](multi-parent.md), forming a directed acyclic graph rather than a
strict tree.

## The lifetime model

A scope is an **atomic unit of lifetime**: every object created or registered in a
scope lives exactly as long as that scope stays open. There is **no API to close or
remove a single object** inside a scope.

Concretely:

- Each provider in a scope returns a **singleton of that scope**.
- `get(...)` can be called dynamically, but the object it returns belongs to the
  scope until `scope.close()`.
- `close()` tears the whole scope down: owned children first, then cleanup hooks,
  then local providers (see [Scopes & lifecycle](scopes-and-lifecycle.md)).
- For a resource that must be released earlier, create a **dedicated child scope**
  for it, use it from there, and close that child when done.

## The asymmetry that makes it strict

A parent is visible from a child, but **not the other way around**. An object
allocated in a child scope never becomes a dependency of its parent. This asymmetry
forces a clear boundary of **visibility, state and lifecycle**.

In practice: **"1 scope = 1 container lifetime"** — dynamic allocation is allowed,
partial deallocation is not. If you find yourself wanting to free one object early,
that object wants its own scope.

## Why this matters

Systems that juggle many overlapping lifetimes map cleanly onto nested scopes. On a
game server, the same value can live at the scale of the plugin, a player, a running
game, or a single action — each with its own visibility boundary and its own
deterministic teardown:

```text
root → plugin → game → player-in-game → action
```

Model each as a scope, register what belongs there, and let resolution + `close()`
handle the wiring and the cleanup.

---

Next: [Getting started](getting-started.md).
