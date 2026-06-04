package be.theking90000.di.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Scanned lifecycle hooks for one injectable type.
 */
final class LifecycleHooks {
    private enum Hook {
        POST_CONSTRUCT,
        PRE_DESTROY
    }

    private final List<Method> postConstruct;
    private final List<Method> preDestroy;
    private final boolean autoCloseable;
    private final boolean closeAlreadyAnnotated;

    private LifecycleHooks(
            List<Method> postConstruct,
            List<Method> preDestroy,
            boolean autoCloseable,
            boolean closeAlreadyAnnotated
    ) {
        this.postConstruct = postConstruct;
        this.preDestroy = preDestroy;
        this.autoCloseable = autoCloseable;
        this.closeAlreadyAnnotated = closeAlreadyAnnotated;
    }

    static LifecycleHooks of(Class<?> type) {
        List<Class<?>> hierarchy = hierarchy(type);
        List<Method> postConstruct = new ArrayList<>();
        List<Method> preDestroy = new ArrayList<>();
        boolean closeAlreadyAnnotated = false;

        for (Class<?> current : hierarchy) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.isAnnotationPresent(PostConstruct.class)) {
                    validate(method, Hook.POST_CONSTRUCT);
                    makeAccessible(method);
                    postConstruct.add(method);
                }

                if (method.isAnnotationPresent(PreDestroy.class)) {
                    validate(method, Hook.PRE_DESTROY);
                    makeAccessible(method);
                    preDestroy.add(method);
                    if (isCloseMethod(method)) {
                        closeAlreadyAnnotated = true;
                    }
                }
            }
        }

        return new LifecycleHooks(
                List.copyOf(postConstruct),
                List.copyOf(preDestroy),
                AutoCloseable.class.isAssignableFrom(type),
                closeAlreadyAnnotated
        );
    }

    void postConstruct(Object instance) {
        for (Method method : postConstruct) {
            invokePostConstruct(method, instance);
        }
    }

    List<AsyncDisposer> disposers(Object instance) {
        List<AsyncDisposer> disposers = new ArrayList<>();

        if (autoCloseable && !closeAlreadyAnnotated) {
            disposers.add(() -> {
                ((AutoCloseable) instance).close();
                return CompletableFuture.completedFuture(null);
            });
        }

        for (Method method : preDestroy) {
            disposers.add(() -> invokePreDestroy(method, instance));
        }

        return disposers;
    }

    private static List<Class<?>> hierarchy(Class<?> type) {
        List<Class<?>> hierarchy = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            hierarchy.add(current);
            current = current.getSuperclass();
        }
        Collections.reverse(hierarchy);
        return hierarchy;
    }

    private static void validate(Method method, Hook hook) {
        if (method.getParameterCount() != 0) {
            throw new UnsupportedInjectionException(
                    hook + " method " + method + " must not declare parameters"
            );
        }

        Class<?> returnType = method.getReturnType();
        if (returnType == void.class || returnType == Void.class) {
            return;
        }

        if (hook == Hook.PRE_DESTROY && isCompletionStageOfVoid(method)) {
            return;
        }

        throw new UnsupportedInjectionException(
                hook + " method " + method + " has unsupported return type " + returnType.getTypeName()
        );
    }

    private static void makeAccessible(Method method) {
        if (!method.trySetAccessible()) {
            throw new UnsupportedInjectionException("Cannot access lifecycle method " + method);
        }
    }

    private static boolean isCloseMethod(Method method) {
        return method.getName().equals("close") && method.getParameterCount() == 0;
    }

    private static boolean isCompletionStageOfVoid(Method method) {
        if (!CompletionStage.class.isAssignableFrom(method.getReturnType())) {
            return false;
        }

        Type genericReturnType = method.getGenericReturnType();
        if (genericReturnType instanceof ParameterizedType parameterizedType) {
            Type[] args = parameterizedType.getActualTypeArguments();
            return args.length == 1 && args[0] == Void.class;
        }

        return false;
    }

    private static void invokePostConstruct(Method method, Object instance) {
        try {
            Object result = method.invoke(instance);
            if (method.getReturnType() == Void.class && result != null) {
                throw new BeanCreationException(method + " must return null");
            }
        } catch (IllegalAccessException e) {
            throw new BeanCreationException(e);
        } catch (InvocationTargetException e) {
            throw new BeanCreationException(e.getCause());
        }
    }

    @SuppressWarnings("unchecked")
    private static CompletionStage<Void> invokePreDestroy(Method method, Object instance) {
        try {
            Object result = method.invoke(instance);

            if (method.getReturnType() == Void.class) {
                if (result != null) {
                    return CompletableFuture.failedFuture(
                            new BeanCreationException(method + " must return null")
                    );
                }
                return CompletableFuture.completedFuture(null);
            }

            if (CompletionStage.class.isAssignableFrom(method.getReturnType())) {
                if (result == null) {
                    return CompletableFuture.failedFuture(
                            new BeanCreationException(method + " must not return null")
                    );
                }
                return (CompletionStage<Void>) result;
            }

            return CompletableFuture.completedFuture(null);
        } catch (IllegalAccessException e) {
            return CompletableFuture.failedFuture(e);
        } catch (InvocationTargetException e) {
            return CompletableFuture.failedFuture(e.getCause());
        }
    }
}
