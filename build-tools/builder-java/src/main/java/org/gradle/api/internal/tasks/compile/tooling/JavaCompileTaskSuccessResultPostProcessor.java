package org.gradle.api.internal.tasks.compile.tooling;

import org.gradle.api.internal.tasks.compile.CompileJavaBuildOperationType;
import org.gradle.api.internal.tasks.compile.CompileJavaBuildOperationType.Result.AnnotationProcessorDetails;
import org.gradle.api.internal.tasks.execution.ExecuteTaskBuildOperationType;
import org.gradle.internal.build.event.OperationResultPostProcessor;
import org.gradle.internal.build.event.types.AbstractTaskResult;
import org.gradle.internal.build.event.types.DefaultAnnotationProcessorResult;
import org.gradle.internal.build.event.types.DefaultJavaCompileTaskSuccessResult;
import org.gradle.internal.build.event.types.DefaultTaskSuccessResult;
import org.gradle.internal.operations.BuildOperationDescriptor;
import org.gradle.internal.operations.OperationFinishEvent;
import org.gradle.internal.operations.OperationIdentifier;
import org.gradle.internal.operations.OperationStartEvent;
import org.gradle.tooling.internal.protocol.events.InternalJavaCompileTaskOperationResult;
import org.gradle.tooling.internal.protocol.events.InternalJavaCompileTaskOperationResult.InternalAnnotationProcessorResult;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JavaCompileTaskSuccessResultPostProcessor implements OperationResultPostProcessor {

    private static final Object TASK_MARKER = new Object();
    private final Map<OperationIdentifier, CompileJavaBuildOperationType.Result> results =
            new ConcurrentHashMap<>();
    private final Map<OperationIdentifier, Object> parentsOfOperationsWithJavaCompileTaskAncestor =
            new ConcurrentHashMap<>();

    @Override
    public void started(BuildOperationDescriptor buildOperation, OperationStartEvent startEvent) {
        if (buildOperation.getDetails() instanceof ExecuteTaskBuildOperationType.Details) {
            parentsOfOperationsWithJavaCompileTaskAncestor.put(buildOperation.getId(), TASK_MARKER);
        } else if (buildOperation.getParentId() != null &&
                   parentsOfOperationsWithJavaCompileTaskAncestor
                           .containsKey(buildOperation.getParentId())) {
            parentsOfOperationsWithJavaCompileTaskAncestor
                    .put(buildOperation.getId(), buildOperation.getParentId());
        }
    }

    @Override
    public void finished(BuildOperationDescriptor buildOperation,
                         OperationFinishEvent finishEvent) {
        if (finishEvent.getResult() instanceof CompileJavaBuildOperationType.Result) {
            CompileJavaBuildOperationType.Result result =
                    (CompileJavaBuildOperationType.Result) finishEvent.getResult();
            OperationIdentifier taskBuildOperationId =
                    findTaskOperationId(buildOperation.getParentId());
            results.put(taskBuildOperationId, result);
        }
        parentsOfOperationsWithJavaCompileTaskAncestor.remove(buildOperation.getId());
    }

    private OperationIdentifier findTaskOperationId(OperationIdentifier id) {
        Object parent = parentsOfOperationsWithJavaCompileTaskAncestor.get(id);
        if (parent == TASK_MARKER) {
            return id;
        }
        return findTaskOperationId((OperationIdentifier) parent);
    }

    @Override
    public AbstractTaskResult process(AbstractTaskResult taskResult,
                                      OperationIdentifier taskBuildOperationId) {
        CompileJavaBuildOperationType.Result compileResult = results.remove(taskBuildOperationId);
        if (taskResult instanceof DefaultTaskSuccessResult && compileResult != null) {
            return new DefaultJavaCompileTaskSuccessResult((DefaultTaskSuccessResult) taskResult,
                    toAnnotationProcessorResults(compileResult.getAnnotationProcessorDetails()));
        }
        return taskResult;
    }

    private List<InternalAnnotationProcessorResult> toAnnotationProcessorResults(List<AnnotationProcessorDetails> allDetails) {
        if (allDetails == null) {
            return null;
        }
        List<InternalAnnotationProcessorResult> results =
                new ArrayList<InternalAnnotationProcessorResult>(allDetails.size());
        for (AnnotationProcessorDetails details : allDetails) {
            results.add(toAnnotationProcessorResult(details));
        }
        return results;
    }

    private InternalAnnotationProcessorResult toAnnotationProcessorResult(AnnotationProcessorDetails details) {
        return new DefaultAnnotationProcessorResult(details.getClassName(),
                toAnnotationProcessorType(details.getType()),
                Duration.ofMillis(details.getExecutionTimeInMillis()));
    }

    private String toAnnotationProcessorType(AnnotationProcessorDetails.Type type) {
        switch (type) {
            case AGGREGATING:
                return InternalAnnotationProcessorResult.TYPE_AGGREGATING;
            case ISOLATING:
                return InternalAnnotationProcessorResult.TYPE_ISOLATING;
            case UNKNOWN:
                return InternalAnnotationProcessorResult.TYPE_UNKNOWN;
        }
        throw new IllegalArgumentException("Missing conversion for enum constant " + type);
    }
}
