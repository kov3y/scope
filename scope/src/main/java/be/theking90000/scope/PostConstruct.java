package be.theking90000.scope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a no-argument lifecycle method to call after dependency injection has
 * constructed a bean.
 *
 * <p>Post-construct methods may return {@code void} or {@link Void}. Methods
 * declared on superclasses run before methods declared on subclasses.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PostConstruct {
}
