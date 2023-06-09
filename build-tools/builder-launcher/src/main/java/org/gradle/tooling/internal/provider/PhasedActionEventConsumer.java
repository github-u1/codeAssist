package org.gradle.tooling.internal.provider;

import org.gradle.initialization.BuildEventConsumer;
import org.gradle.tooling.internal.protocol.PhasedActionResult;
import org.gradle.tooling.internal.protocol.PhasedActionResultListener;
import org.gradle.tooling.internal.provider.serialization.PayloadSerializer;

/**
 * Consumer of events from phased actions. This consumer deserializes the results and forward them to the correct listener.
 */
public class PhasedActionEventConsumer implements BuildEventConsumer {
    private final PhasedActionResultListener phasedActionResultListener;
    private final PayloadSerializer payloadSerializer;
    private final BuildEventConsumer delegate;

    PhasedActionEventConsumer(PhasedActionResultListener phasedActionResultListener, PayloadSerializer payloadSerializer, BuildEventConsumer delegate) {
        this.phasedActionResultListener = phasedActionResultListener;
        this.payloadSerializer = payloadSerializer;
        this.delegate = delegate;
    }

    @Override
    public void dispatch(final Object event) {
        if (event instanceof PhasedBuildActionResult) {
            final PhasedBuildActionResult resultEvent = (PhasedBuildActionResult) event;
            final Object deserializedResult = payloadSerializer.deserialize(resultEvent.result);

            phasedActionResultListener.onResult(new PhasedActionResult<Object>() {
                @Override
                public Object getResult() {
                    return deserializedResult;
                }

                @Override
                public PhasedActionResult.Phase getPhase() {
                    return resultEvent.phase;
                }
            });
        } else {
            delegate.dispatch(event);
        }
    }
}