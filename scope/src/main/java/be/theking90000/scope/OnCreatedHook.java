package be.theking90000.scope;

/**
 * Callback interface for observing bean creation events within a scope tree.
 *
 * <p>Implementing classes are registered via {@link Scope#addHook(Class)} and are called
 * with a {@link BeanCreated} event every time the container creates a bean by constructor
 * injection.  The hook may return a {@link Disposer} that the container will register on
 * the owner scope, so that cleanup is tied to that scope's lifetime.
 *
 * <h2>Hook registration and shadowing</h2>
 *
 * <p>Each scope stores its registered hook keys as {@code OnCreatedHookRegistration} seeds.
 * When a bean is created, the container collects all hook keys visible from the creating
 * scope using {@link Scope.Collect#DEEP}, deduplicates them, and resolves each key in the
 * creating scope.  This means a child scope can shadow a parent's hook with a local
 * implementation simply by binding the same key:
 *
 * <pre>
 * { // parent scope
 *     OnCreatedHookRegistration = [Hook1.class, Hook2.class]
 *     Hook1 = &lt;object1&gt;
 *     Hook2 = &lt;object2&gt;
 *
 *     { // child scope
 *         OnCreatedHookRegistration = [Hook1.class, Hook3.class]
 *         Hook2 = &lt;object2local&gt;  // redefine (shadow)
 *         Hook3 = &lt;object3&gt;       // new hook
 *
 *         { // grandchild scope
 *             Hook3 = &lt;object3local&gt;  // shadow via bind() / seed() / provide()
 *
 *             // When a bean is created here, hook keys are merged uniquely:
 *             // [Hook1.class, Hook2.class, Hook3.class]
 *             // resolved in this scope:
 *             //   Hook1 → &lt;object1&gt;       (from parent)
 *             //   Hook2 → &lt;object2local&gt;  (shadowed by child)
 *             //   Hook3 → &lt;object3local&gt;  (shadowed by grandchild)
 *         }
 *     }
 * }
 * </pre>
 *
 * <h2>Example: Bukkit event auto-registration</h2>
 *
 * <p>A common pattern is to automatically register any bean that implements
 * {@code Listener} as a Bukkit event listener, and unregister it when its scope closes.
 * The hook calls {@code addHook} on itself during construction, so simply instantiating
 * it is enough to activate it for the scope:
 *
 * <pre>{@code
 * class BukkitEventHook implements OnCreatedHook {
 *     private final JavaPlugin plugin;
 *
 *     public BukkitEventHook(Scope<?> s, JavaPlugin p) {
 *         this.plugin = p;
 *         s.addHook(BukkitEventHook.class); // register this hook in the scope
 *     }
 *
 *     @Override
 *     public Disposer onCreated(BeanCreated event) {
 *         if (event.bean() instanceof Listener listener) {
 *             plugin.getServer().getPluginManager()
 *                 .registerEvents(listener, plugin);
 *             return () -> HandlerList.unregisterAll(listener);
 *         }
 *         return null; // no cleanup needed
 *     }
 * }
 *
 * // A player-scoped subclass filters events to a specific player.
 * // It shadows BukkitEventHook in its scope via s.bind(BukkitEventHook.class, this),
 * // so any scope that has both hooks registered will use PlayerBukkitEventHook locally.
 * class PlayerBukkitEventHook extends BukkitEventHook {
 *     private final Player player;
 *
 *     public PlayerBukkitEventHook(Scope<?> s, JavaPlugin p, Player pp) {
 *         super(s, p);       // registers BukkitEventHook.class as the hook key
 *         this.player = pp;
 *         // Shadow the parent hook: in this scope BukkitEventHook.class → this instance
 *         s.bind(BukkitEventHook.class, this);
 *     }
 *
 *     @Override
 *     public Disposer onCreated(BeanCreated event) {
 *         if (event.bean() instanceof Listener listener) {
 *             Listener filtered = filterByPlayer(listener, player);
 *             plugin.getServer().getPluginManager()
 *                 .registerEvents(filtered, plugin);
 *             return () -> HandlerList.unregisterAll(filtered);
 *         }
 *         return null;
 *     }
 * }
 * }</pre>
 *
 * <h2>Initialization order and {@link ScopeInitialization}</h2>
 *
 * <p>Because there is no defined creation order during boot, registering a hook
 * <em>after</em> some beans have already been created means those earlier beans will
 * never trigger {@code onCreated}.  Use {@link Scope#beginInitialization()} to open a
 * batch-initialization session: all {@link BeanCreated} events fired while the session
 * is open are buffered, and replayed in order only when {@link ScopeInitialization#commit()}
 * is called.  This guarantees every hook sees every bean regardless of which was
 * instantiated first.
 *
 * <p>Full plugin startup example:
 *
 * <pre>{@code
 * record RootScope() {}
 * record JavaPluginScope() {}
 *
 * Scope<RootScope> root = new Scope<>(new RootScope());
 *
 * // Called in onEnable():
 * Scope<JavaPluginScope> jps = new Scope<>(new JavaPluginScope());
 * jps.ownedBy(root);
 * jps.seed(JavaPlugin.class, this);
 *
 * // discoveredTypes comes from reflection / annotation processor / explicit list.
 * List<Class<?>> discoveredTypes = List.of(MyListener.class, BukkitEventHook.class);
 *
 * try (ScopeInitialization init = jps.beginInitialization()) {
 *     for (Class<?> type : discoveredTypes) {
 *         jps.bind(type);
 *         jps.get(type);   // eager instantiation — BeanCreated events are buffered
 *     }
 *     // MyListener was instantiated before BukkitEventHook, but commit() replays
 *     // all events after BukkitEventHook is registered, so it still fires for MyListener.
 *     init.commit();
 * }
 *
 * // After initialization, MyListener can create and destroy PlayerScopes dynamically:
 * class MyListener implements Listener {
 *     private final JavaPlugin plugin;
 *     private final Scope<?> scope;
 *     private final Map<UUID, Scope<PlayerScope>> playerScopes = new HashMap<>();
 *
 *     public MyListener(Scope<?> s, JavaPlugin p) { this.scope = s; this.plugin = p; }
 *
 *     @EventHandler
 *     public void onJoin(PlayerJoinEvent event) {
 *         record PlayerScope(UUID uuid) {}
 *         PlayerScope ps = new PlayerScope(event.getPlayer().getUniqueId());
 *
 *         Scope<PlayerScope> s = new Scope<>(ps);
 *         s.ownedBy(scope);                          // player scope owned by plugin scope
 *         s.seed(Player.class, event.getPlayer());   // seed the player instance
 *
 *         List<Class<?>> playerTypes = List.of(PlayerBukkitEventHook.class, MyPlayerListener.class);
 *
 *         try (ScopeInitialization init = s.beginInitialization()) {
 *             for (Class<?> type : playerTypes) {
 *                 s.bind(type);
 *                 s.get(type);
 *             }
 *             // PlayerBukkitEventHook shadows BukkitEventHook in this scope,
 *             // so MyPlayerListener gets a player-filtered event hook.
 *             init.commit();
 *         }
 *
 *         playerScopes.put(ps.uuid(), s);
 *     }
 *
 *     @EventHandler
 *     public void onQuit(PlayerQuitEvent event) {
 *         Scope<PlayerScope> s = playerScopes.remove(event.getPlayer().getUniqueId());
 *         if (s != null) s.close(); // unregisters all listeners via disposers
 *     }
 * }
 * }</pre>
 *
 * <p>Resulting scope structure:
 *
 * <pre>
 * { // Root
 *     { // PluginScope
 *         OnCreatedHookRegistration = [BukkitEventHook.class]
 *         Plugin              = JavaPlugin
 *         BukkitEventHook     = BukkitEventHook
 *         MyListener          = MyListener
 *
 *         { // PlayerScope (uuid=X)
 *             OnCreatedHookRegistration = [BukkitEventHook.class]  // merged, no duplicate
 *             BukkitEventHook           = PlayerBukkitEventHook    // shadows parent hook
 *             MyPlayerListener          = MyPlayerListener
 *         }
 *     }
 * }
 * </pre>
 *
 * @see BeanCreated
 * @see Disposer
 * @see ScopeInitialization
 * @see Scope#addHook(Class)
 */
public interface OnCreatedHook {

    /**
     * Called when a bean is created in a scope that has this hook registered.
     *
     * @param event the creation event carrying the owner scope, key, and bean instance
     * @return a {@link Disposer} to be registered on the owner scope, or {@code null}
     *         if no cleanup is needed
     */
    Disposer onCreated(BeanCreated event);
}
