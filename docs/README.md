# `scope` documentation

`scope` is a small JVM library that treats **scopes as first-class runtime
objects**. A scope is a lexical block you can build, nest, shadow and dispose
_while the program runs_ — and a thin dependency injection layer wires objects
together by walking the scope graph.

If you have five minutes, read **[Mental model](mental-model.md)** first: every
other page builds on it.

```java
import be.theking90000.scope.Scope;

record RootScope() {}
record Config(String value) {}
record Service(Config config) {}

Scope<RootScope> root = new Scope<>(new RootScope());
root.seed(Config.class, new Config("prod"));

Service service = root.get(Service.class); // built and cached as a scope singleton
```

## Table of contents

| Page                                                      | What it covers                                                                                              |
| --------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------- |
| [Mental model](mental-model.md)                           | Scopes as language blocks, the lifetime model, the visibility/ownership asymmetry.                          |
| [Getting started](getting-started.md)                     | First example, step by step, and how `get()` resolves.                                                      |
| [Injection](injection.md)                                 | Constructor injection rules, supported parameter shapes, lazy `Provider<T>`, cycles.                        |
| [Qualifiers & collections](qualifiers-and-collections.md) | `Key<T>`, `@Named`, and injecting all providers of a type.                                                  |
| [Scopes & lifecycle](scopes-and-lifecycle.md)             | Parents, shadowing, ownership vs visibility, `close()`, `@PostConstruct` / `@PreDestroy` / `AutoCloseable`. |
| [Multi-parent scopes](multi-parent.md)                    | DAG scopes, ambiguity, `NEAREST` vs `DEEP` resolution.                                                      |
| [Extension hooks](extension-hooks.md)                     | `OnCreatedHook`, `BeanCreated`, `Disposer`, hook shadowing, batch initialization.                           |
| [API reference](api-reference.md)                         | Every public member of `Scope`, `Key`, `Provider`, `MultiProvider`.                                         |
| [Exceptions](exceptions.md)                               | The `DiException` hierarchy and when each is thrown.                                                        |
| [Recipes](recipes.md)                                     | Common patterns and a best-practices checklist.                                                             |

## Installation

See the [main README](../README.md#quick-start) for Gradle / Maven coordinates.
The library targets **Java 21**. The base package is:

```java
package be.theking90000.scope;
```

## JavaDoc

Every public type and method ships with thorough JavaDoc — it is the most precise
reference for exact signatures, edge cases and behavior. Browse it online:

**<https://theking90000.github.io/scope/javadoc/>**

_(Published from the `javadoc` jar produced by the build; if the hosted site is not
up yet, the same JavaDoc is attached to every release on GitHub Packages.)_

## Other references

- [Main README](../README.md) — the project landing page (pitch and comparison).
- [`scope/README.md`](../scope/README.md) — the original, in-depth **French**
  reference for the same module.
