package be.theking90000.di.core;

import java.util.concurrent.CompletionStage;

/**
 * Cleanup callback that can finish synchronously or asynchronously.
 */
@FunctionalInterface
interface AsyncDisposer {
    /**
     * Runs this cleanup callback.
     *
     * @return completion stage that finishes when cleanup is done
     * @throws Exception if cleanup fails before returning a stage
     */
    CompletionStage<Void> dispose() throws Exception;
}
