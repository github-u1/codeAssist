package org.gradle.tooling.internal.provider.runner;

import org.gradle.api.internal.tasks.testing.operations.TestListenerBuildOperationAdapter;
import org.gradle.api.tasks.testing.TestOutputEvent;
import org.gradle.internal.build.event.types.DefaultTestOutputDescriptor;
import org.gradle.internal.build.event.types.DefaultTestOutputEvent;
import org.gradle.internal.build.event.types.DefaultTestOutputResult;
import org.gradle.internal.operations.BuildOperationDescriptor;
import org.gradle.internal.operations.BuildOperationIdFactory;
import org.gradle.internal.operations.BuildOperationListener;
import org.gradle.internal.operations.OperationFinishEvent;
import org.gradle.internal.operations.OperationIdentifier;
import org.gradle.internal.operations.OperationProgressEvent;
import org.gradle.internal.operations.OperationStartEvent;
import org.gradle.tooling.events.test.Destination;
import org.gradle.tooling.internal.protocol.events.InternalTestOutputDescriptor;

/**
 * Test listener that forwards the test output events.
 */
class ClientForwardingTestOutputOperationListener implements BuildOperationListener {
    private final ProgressEventConsumer eventConsumer;
    private final BuildOperationIdFactory idFactory;

    ClientForwardingTestOutputOperationListener(ProgressEventConsumer eventConsumer, BuildOperationIdFactory idFactory) {
        this.eventConsumer = eventConsumer;
        this.idFactory = idFactory;
    }

    @Override
    public void started(BuildOperationDescriptor buildOperation, OperationStartEvent startEvent) {
    }

    @Override
    public void progress(OperationIdentifier buildOperationId, OperationProgressEvent progressEvent) {
        if (progressEvent.getDetails() instanceof TestListenerBuildOperationAdapter.OutputProgress) {
            TestListenerBuildOperationAdapter.OutputProgress progress = (TestListenerBuildOperationAdapter.OutputProgress) progressEvent.getDetails();
            InternalTestOutputDescriptor descriptor = new DefaultTestOutputDescriptor(new OperationIdentifier(idFactory.nextId()), buildOperationId);
            DefaultTestOutputResult result = new DefaultTestOutputResult(progressEvent.getTime(), progressEvent.getTime(), getDestination(progress.getOutput().getDestination()), progress.getOutput().getMessage());
            eventConsumer.progress(new DefaultTestOutputEvent(progressEvent.getTime(), descriptor, result));
        }
    }

    private int getDestination(TestOutputEvent.Destination destination) {
        switch (destination) {
            case StdOut: return Destination.StdOut.getCode();
            case StdErr: return Destination.StdErr.getCode();
            default: throw new IllegalStateException("Unknown output destination type: " + destination);
        }
    }

    @Override
    public void finished(BuildOperationDescriptor buildOperation, OperationFinishEvent finishEvent) {
    }
}