package org.gradle.tooling.internal.provider.runner;

import org.gradle.internal.UncheckedException;
import org.gradle.internal.build.event.BuildEventSubscriptions;
import org.gradle.internal.build.event.types.AbstractOperationResult;
import org.gradle.internal.build.event.types.DefaultFailure;
import org.gradle.internal.build.event.types.DefaultFileDownloadDescriptor;
import org.gradle.internal.build.event.types.DefaultFileDownloadFailureResult;
import org.gradle.internal.build.event.types.DefaultFileDownloadSuccessResult;
import org.gradle.internal.build.event.types.DefaultOperationFinishedProgressEvent;
import org.gradle.internal.build.event.types.DefaultOperationStartedProgressEvent;
import org.gradle.internal.build.event.types.DefaultStatusEvent;
import org.gradle.internal.operations.BuildOperationDescriptor;
import org.gradle.internal.operations.OperationFinishEvent;
import org.gradle.internal.operations.OperationIdentifier;
import org.gradle.internal.operations.OperationProgressDetails;
import org.gradle.internal.operations.OperationProgressEvent;
import org.gradle.internal.operations.OperationStartEvent;
import org.gradle.internal.resource.ExternalResourceReadBuildOperationType;
import org.gradle.tooling.events.OperationType;
import org.gradle.tooling.internal.protocol.events.InternalOperationFinishedProgressEvent;
import org.gradle.tooling.internal.protocol.events.InternalOperationStartedProgressEvent;
import org.gradle.tooling.internal.protocol.events.InternalProgressEvent;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

public class FileDownloadOperationMapper implements BuildOperationMapper<ExternalResourceReadBuildOperationType.Details, DefaultFileDownloadDescriptor> {
    @Override
    public boolean isEnabled(BuildEventSubscriptions subscriptions) {
        return subscriptions.isRequested(OperationType.FILE_DOWNLOAD);
    }

    @Override
    public Class<ExternalResourceReadBuildOperationType.Details> getDetailsType() {
        return ExternalResourceReadBuildOperationType.Details.class;
    }

    @Override
    public DefaultFileDownloadDescriptor createDescriptor(ExternalResourceReadBuildOperationType.Details details, BuildOperationDescriptor buildOperation, @Nullable OperationIdentifier parent) {
        try {
            return new DefaultFileDownloadDescriptor(buildOperation.getId(), buildOperation.getName(), buildOperation.getDisplayName(), parent, new URI(details.getLocation()));
        } catch (URISyntaxException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }
    }

    @Override
    public InternalOperationStartedProgressEvent createStartedEvent(DefaultFileDownloadDescriptor descriptor, ExternalResourceReadBuildOperationType.Details details, OperationStartEvent startEvent) {
        return new DefaultOperationStartedProgressEvent(startEvent.getStartTime(), descriptor);
    }

    @Nullable
    @Override
    public InternalProgressEvent createProgressEvent(DefaultFileDownloadDescriptor descriptor, OperationProgressEvent progressEvent) {
        if (progressEvent.getDetails() instanceof OperationProgressDetails) {
            OperationProgressDetails details = (OperationProgressDetails) progressEvent.getDetails();
            return new DefaultStatusEvent(progressEvent.getTime(), descriptor, details.getProgress(), details.getTotal(), details.getUnits());
        } else {
            return null;
        }
    }

    @Override
    public InternalOperationFinishedProgressEvent createFinishedEvent(DefaultFileDownloadDescriptor descriptor, ExternalResourceReadBuildOperationType.Details details, OperationFinishEvent finishEvent) {
        ExternalResourceReadBuildOperationType.Result operationResult = (ExternalResourceReadBuildOperationType.Result) finishEvent.getResult();
        Throwable failure = finishEvent.getFailure();
        long startTime = finishEvent.getStartTime();
        long endTime = finishEvent.getEndTime();
        AbstractOperationResult result;
        if (failure != null) {
            result = new DefaultFileDownloadFailureResult(startTime, endTime, Collections.singletonList(DefaultFailure.fromThrowable(failure)), operationResult.getBytesRead());
        } else {
            result = new DefaultFileDownloadSuccessResult(startTime, endTime, operationResult.getBytesRead());
        }
        return new DefaultOperationFinishedProgressEvent(finishEvent.getEndTime(), descriptor, result);
    }
}