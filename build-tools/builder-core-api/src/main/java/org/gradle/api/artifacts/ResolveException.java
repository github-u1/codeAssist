package org.gradle.api.artifacts;

import org.gradle.internal.exceptions.Contextual;
import org.gradle.internal.exceptions.DefaultMultiCauseException;

/**
 * <p>A <code>ResolveException</code> is thrown when dependency resolution fails for some reason.</p>
 */
@Contextual
public class ResolveException extends DefaultMultiCauseException {
    public ResolveException(String resolveContext, Throwable cause) {
        super(buildMessage(resolveContext), cause);
    }

    public ResolveException(String resolveContext, Iterable<? extends Throwable> causes) {
        super(buildMessage(resolveContext), causes);
    }

    private static String buildMessage(String resolveContext) {
        return String.format("Could not resolve all dependencies for %s.", resolveContext);
    }
}
