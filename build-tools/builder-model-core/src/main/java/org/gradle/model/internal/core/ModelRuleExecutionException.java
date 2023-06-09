package org.gradle.model.internal.core;

import org.gradle.api.GradleException;
import org.gradle.internal.exceptions.Contextual;
import org.gradle.model.internal.core.rule.describe.ModelRuleDescriptor;

@Contextual
// TODO should include some context on what the rule was trying to do (create vs. mutate)
public class ModelRuleExecutionException extends GradleException {

    public ModelRuleExecutionException(ModelRuleDescriptor descriptor, Throwable cause) {
        super(toMessage(descriptor), cause);
    }

    public ModelRuleExecutionException(ModelRuleDescriptor descriptor, String error) {
        super(toMessage(descriptor, error));
    }

    private static String toMessage(ModelRuleDescriptor descriptor) {
        StringBuilder builder = new StringBuilder("Exception thrown while executing model rule: ");
        descriptor.describeTo(builder);
        return builder.toString();
    }

    private static String toMessage(ModelRuleDescriptor descriptor, String error) {
        StringBuilder builder = new StringBuilder("error executing model rule: ");
        descriptor.describeTo(builder);
        builder.append(" - ");
        builder.append(error);
        return builder.toString();
    }

}
