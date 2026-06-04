package be.theking90000.di.core;

import org.junit.jupiter.api.Test;

import be.theking90000.di.core.LazyTest.A1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;



public class LazyTest {
    record RootScope(){};
    public static class A { static int i = 0; int j; public A() {
        j=++i;
    } };
    public static record B(Provider<A> a){}
    public static record B2(@Named("ok") Provider<A> a){};


    @Test
    void testInject() {
        A.i=0;
        Scope<RootScope> root = new Scope<>(new RootScope());

        Provider<B> b = root.provider(B.class);
        
        
        assertEquals(0, A.i);
        b.get();
        assertEquals(0,A.i);
        b.get().a().get();
        assertEquals(1, A.i);
        b.get().a().get();
        assertEquals(1, A.i);
        
        Provider<B2> b2 = root.provider(B2.class);
        assertEquals(1, A.i);
        b2.get();
        assertEquals(1,A.i);
        b2.get().a().get(); b.get();
        assertEquals(2, A.i);
        b2.get().a().get();
        assertEquals(2, A.i);
  
    }

    public record A1(Provider<A2> a2) { @Override
    public final String toString() {
        return "A1";
    }};
    public record A2(Provider<A1> a1) {@Override
    public final String toString() {
        return "A2";
    }};

    public record Counter(int i){};

    public record Counters(Provider<Iterable<Provider<Counter>>> counters) {
        public int sum() {
            int s= 0;
            for (Provider<Counter> c : counters.get()) {
                s+=c.get().i;
            }
            return s;
        }
    }

    @Test
    void testCircular() {
        Scope<RootScope> root = new Scope<>(new RootScope());

        A1 a1 = root.get(A1.class);

        a1.a2.get();
        assertEquals(a1.a2.get().a1.get(), a1);
    }

    public record C1(C2 c2) {}
    public record C2(C1 c1) {}

    public interface Plugin {}
    public record TestPlugin(String name) implements Plugin {}
    public record NeedsPlugin(Plugin plugin) {}
    public record NeedsNamedPlugin(@Named("plugin") Plugin plugin) {}

    @Test
    void testFailCircular() {
        Scope<RootScope> root = new Scope<>(new RootScope());

        assertThrows(BeanResolutionException.class, () -> root.provider(C1.class));
        assertThrows(BeanResolutionException.class, () -> root.get(C1.class));
    }

    @Test
    void testBindBeforeSeed() {
        Scope<RootScope> root = new Scope<>(new RootScope());
        TestPlugin plugin = new TestPlugin("test");

        root.bind(NeedsPlugin.class);
        root.seed(Plugin.class, plugin);

        assertEquals(plugin, root.get(NeedsPlugin.class).plugin());
    }

    @Test
    void testBindAfterSeed() {
        Scope<RootScope> root = new Scope<>(new RootScope());
        TestPlugin plugin = new TestPlugin("test");

        root.seed(Plugin.class, plugin);
        root.bind(NeedsPlugin.class);

        assertEquals(plugin, root.get(NeedsPlugin.class).plugin());
    }

    @Test
    void testBindFailsOnGet() {
        Scope<RootScope> root = new Scope<>(new RootScope());

        root.bind(NeedsPlugin.class);

        assertThrows(UnsupportedInjectionException.class, () -> root.get(NeedsPlugin.class));
    }

    @Test
    void testBindKeyKeepsQualifier() {
        Scope<RootScope> root = new Scope<>(new RootScope());
        TestPlugin plugin = new TestPlugin("test");
        Key<NeedsNamedPlugin> key = Key.of(NeedsNamedPlugin.class, "named");

        root.bind(key);
        root.seed(Key.of(Plugin.class, "plugin"), plugin);

        assertEquals(plugin, root.get(key).plugin());
    }

    @Test
    void testBindFailCircular() {
        Scope<RootScope> root = new Scope<>(new RootScope());

        root.bind(C1.class);

        assertThrows(BeanResolutionException.class, () -> root.get(C1.class));
    }

    @Test
    void testBindFailCircularWhenBothSidesAreBound() {
        Scope<RootScope> root = new Scope<>(new RootScope());

        root.bind(C1.class);
        root.bind(C2.class);

        assertThrows(BeanResolutionException.class, () -> root.get(C1.class));
    }

    @Test
    void testMultiple() {
        Scope<RootScope> root = new Scope<>(new RootScope());
        
        for (int i = 0; i <10;i++)
            root.seed(Counter.class, new Counter(i));
        for (int i = 10; i<20;i++) {
            int j =i;
            root.provide(Counter.class, () -> new Counter(j));
        }

        assertThrows(AmbiguousBeanException.class, ()->root.get(Counter.class));

        HashSet<Integer> h = new HashSet<>();
        for (Provider<Counter> p : root.providers(Counter.class).get()) {
            h.add(p.get().i);
        }
        assertEquals(20, h.size());

        Counters c = root.get(Counters.class);
        assertEquals(190, c.sum());
    }

}
