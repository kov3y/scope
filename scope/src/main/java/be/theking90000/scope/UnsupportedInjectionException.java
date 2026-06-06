package be.theking90000.scope;

/**
 * Thrown when a type or constructor parameter is not supported by the injector.
 */
@SuppressWarnings("serial")
public class UnsupportedInjectionException extends BeanResolutionException {
    /**
     * Creates an unsupported-injection exception.
     *
     * @param message detail message
     */
    public UnsupportedInjectionException(String message) {
        super(message);
    }
}
