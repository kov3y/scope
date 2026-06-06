package be.theking90000.scope;

/**
 * Thrown when a {@link ScopeInitialization} session is used incorrectly,
 * for example committing twice or registering events after the session has ended.
 *
 * @see ScopeInitialization
 */
@SuppressWarnings("serial")
public class InitializationException extends DiException {

    /** @param message detail message */
    public InitializationException(String message) {
        super(message);
    }

    /** @param cause root cause */
    public InitializationException(Throwable cause) {
        super(cause);
    }
}
