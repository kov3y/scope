package be.theking90000.di.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.CompletionStage;

/**
 * Marks a no-argument lifecycle method to call when the owning {@link Scope}
 * closes.
 *
 * <p>Pre-destroy methods may return {@code void}, {@link Void}, or
 * {@link CompletionStage}{@code <Void>}. Methods declared on subclasses run
 * before methods declared on superclasses during cleanup.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PreDestroy {
}
