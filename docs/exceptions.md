# Exceptions

All exceptions are unchecked and share a common root, **`DiException`** (extends
`RuntimeException`). You can catch `DiException` to handle any container error, or a
specific subtype for finer control.

## Hierarchy

```text
DiException
├── BeanResolutionException        // resolving or creating a bean failed
│   ├── NoSuchBeanException        // no provider available
│   ├── AmbiguousBeanException     // several providers for a single-value resolution
│   ├── UnsupportedInjectionException  // type not injectable / unsupported generic
│   └── BeanCreationException      // reflective instantiation failed
├── InitializationException        // misuse of a ScopeInitialization session
└── ScopeException                 // scope-graph / lifecycle error
    ├── ScopeCycleException        // attaching a parent would create a cycle
    ├── ScopeConflictException     // ownership conflict with an open child
    └── ScopeStateException        // operation on a closing/closed scope
```

## Resolution and creation

| Exception | Thrown when |
| --------- | ----------- |
| `BeanResolutionException` | Base for resolution/creation failures. |
| `NoSuchBeanException` | `get`/`provider` finds no provider and cannot create one. |
| `AmbiguousBeanException` | A single-value lookup matches more than one nearest provider. Qualify, shadow, or use `providers(...)`. |
| `UnsupportedInjectionException` | A type cannot be injected — e.g. no single public constructor, a non-concrete generic, a local/anonymous/non-static inner class. |
| `BeanCreationException` | The constructor or a `@PostConstruct` threw during instantiation. The bean is **not** exposed as a scope singleton. |

## Initialization

| Exception | Thrown when |
| --------- | ----------- |
| `InitializationException` | A `ScopeInitialization` session is misused — committing twice, or registering an event after the session ended. |

## Scopes

| Exception | Thrown when |
| --------- | ----------- |
| `ScopeException` | Base for scope errors. Also raised by `close()` (with the individual failures attached as **suppressed** exceptions) when cleanups fail. |
| `ScopeCycleException` | An `attach` / `ownedBy` / `weakRef` would make the graph cyclic. |
| `ScopeConflictException` | Attaching a second open owned child under the same context, or `detachWeakRef` with no matching weak reference. |
| `ScopeStateException` | Any operation on a scope that is `CLOSING` or `CLOSED`. |

---

See also: [Injection](injection.md) (what makes a type injectable),
[Multi-parent scopes](multi-parent.md) (ambiguity), and
[Scopes & lifecycle](scopes-and-lifecycle.md) (close semantics).
