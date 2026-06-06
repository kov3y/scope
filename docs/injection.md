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

## Where the bean ends up

A constructor-injected bean is created **in the scope that resolved it** and cached
there as a scope singleton. Its own dependencies therefore see that scope first,
then visible parents — the same shadowing rules as everything else
([Scopes & lifecycle](scopes-and-lifecycle.md)). It is owned by that scope and
cleaned up when the scope closes.

---

Next: [Qualifiers & collections](qualifiers-and-collections.md).
