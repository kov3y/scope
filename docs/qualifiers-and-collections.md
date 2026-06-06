# Qualifiers & collections

Two related needs: telling apart several beans of the **same type**, and injecting
**all** providers of a type at once.

## `Key<T>`: type + optional qualifier

Every bean is identified by a `Key<T>`, which is a **type** plus an optional
**qualifier**:

```java
import be.theking90000.scope.Key;

Key<Message> hello = Key.of(Message.class, "hello");
Key<Message> bye   = Key.of(Message.class, "bye");

scope.seed(hello, new Message("Hello"));
scope.seed(bye,   new Message("Bye"));
```

`Key.of(type)` builds an unqualified key; `Key.of(type, qualifier)` a qualified one.
The qualifier is any object (commonly a `String`). Every `seed` / `provide` / `bind`
/ `get` / `provider` / `providers` method has a `Key` overload.

## `@Named`: qualify an injected parameter

To inject a qualified bean by constructor, annotate the parameter:

```java
import be.theking90000.scope.Named;

record Greeter(@Named("hello") Message message) {}

Greeter greeter = scope.get(Greeter.class); // gets the "hello" Message
```

`bind` accepts qualified keys too:

```java
Key<Service> serviceKey = Key.of(Service.class, "game");

scope.bind(serviceKey);
Service service = scope.get(serviceKey);
```

Reach for a qualifier whenever there is more than one conceptual value of the same
type (e.g. two `DataSource`s, several `Message`s).

## Injecting all providers of a type

Sometimes you want **every** provider of a type, not a single one. Register several:

```java
record Counter(int value) {}

scope.seed(Counter.class, new Counter(1));
scope.provide(Counter.class, () -> new Counter(2));
```

Asking for a single `Counter` now fails with
[`AmbiguousBeanException`](exceptions.md). Instead, request the collection:

```java
for (Provider<Counter> counter : scope.providers(Counter.class).get()) {
    System.out.println(counter.get().value());
}
```

`providers(...)` returns a [`MultiProvider<Counter>`](api-reference.md#multiprovider),
which is itself a `Provider<Iterable<Provider<Counter>>>`.

## Collection injection by constructor

A class can declare that it wants all providers of a type:

```java
record Counters(Provider<Iterable<Provider<Counter>>> counters) {
    int sum() {
        int result = 0;
        for (Provider<Counter> counter : counters.get()) {
            result += counter.get().value();
        }
        return result;
    }
}
```

The exact type matters — read it inside out:

```java
Provider<Iterable<Provider<Counter>>>
//  └ outer Provider : the collection itself is lazy
//        └ Iterable : several providers are requested
//              └ Provider<Counter> : each element creates or returns a Counter
```

This is the canonical way to model plugin-style "register many, consume all"
patterns (commands, listeners, validators…). See
[Recipes](recipes.md#multiple-implementations).

> By default the collection is gathered with the `NEAREST` strategy. To gather
> contributions from *every* visible scope (not just the nearest one per branch),
> use the `DEEP` strategy — see [Multi-parent scopes](multi-parent.md#nearest-vs-deep).

---

Next: [Scopes & lifecycle](scopes-and-lifecycle.md).
