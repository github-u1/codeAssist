package org.gradle.tooling.internal.provider.runner;

import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.Transformer;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.internal.project.ProjectState;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.testing.Test;
import org.gradle.api.tasks.testing.TestExecutionException;
import org.gradle.api.tasks.testing.TestFilter;
import org.gradle.execution.BuildConfigurationAction;
import org.gradle.execution.BuildExecutionContext;
import org.gradle.execution.TaskSelection;
import org.gradle.execution.TaskSelectionException;
import org.gradle.execution.TaskSelector;
import org.gradle.internal.build.event.types.DefaultTestDescriptor;
import org.gradle.process.JavaDebugOptions;
import org.gradle.process.internal.DefaultJavaDebugOptions;
import org.gradle.tooling.internal.protocol.events.InternalTestDescriptor;
import org.gradle.tooling.internal.protocol.test.InternalDebugOptions;
import org.gradle.tooling.internal.protocol.test.InternalJvmTestRequest;
import org.gradle.tooling.internal.protocol.test.InternalTestPatternSpec;
import org.gradle.tooling.internal.provider.action.TestExecutionRequestAction;
import org.gradle.util.internal.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class TestExecutionBuildConfigurationAction implements BuildConfigurationAction {
    private final TestExecutionRequestAction testExecutionRequest;
    private final TaskSelector taskSelector;

    public TestExecutionBuildConfigurationAction(TestExecutionRequestAction testExecutionRequest, GradleInternal gradle) {
        this.testExecutionRequest = testExecutionRequest;
        this.taskSelector = gradle.getServices().get(TaskSelector.class);
    }

    @Override
    public void configure(BuildExecutionContext context) {
        final Set<Test> allTestTasksToRun = new LinkedHashSet<>();
        final GradleInternal gradleInternal = context.getGradle();
        allTestTasksToRun.addAll(configureBuildForTestDescriptors(testExecutionRequest));
        allTestTasksToRun.addAll(configureBuildForInternalJvmTestRequest(gradleInternal, testExecutionRequest));
        allTestTasksToRun.addAll(configureBuildForTestTasks(testExecutionRequest));
        configureTestTasks(allTestTasksToRun);
        context.getExecutionPlan().addEntryTasks(allTestTasksToRun, 0);
        for (String task : testExecutionRequest.getTasks()) {
            context.getExecutionPlan().addEntryTasks(queryTasks(task));
        }
        if (testExecutionRequest.isRunDefaultTasks()) {
            for (String defaultTask : gradleInternal.getDefaultProject().getDefaultTasks()) {
                context.getExecutionPlan().addEntryTasks(queryTasks(defaultTask));
            }
        }
    }

    private void configureTestTasks(Set<Test> allTestTasksToRun) {
        for (Test task : allTestTasksToRun) {
            task.setIgnoreFailures(true);
            task.getFilter().setFailOnNoMatchingTests(false);
            task.getOutputs().upToDateWhen(Specs.SATISFIES_NONE);
            InternalDebugOptions debugOptions = testExecutionRequest.getDebugOptions();
            if (debugOptions.isDebugMode()) {
                task.debugOptions(new Action<JavaDebugOptions>() {
                    @Override
                    public void execute(JavaDebugOptions javaDebugOptions) {
                        DefaultJavaDebugOptions options = (DefaultJavaDebugOptions) javaDebugOptions;
                        options.getEnabled().set(true);
                        options.getPort().set(debugOptions.getPort());
                        options.getServer().set(false);
                        options.getSuspend().set(false);
                    }
                });
            }
        }
    }

    private List<Test> configureBuildForTestDescriptors(TestExecutionRequestAction testExecutionRequest) {
        Map<String, List<InternalJvmTestRequest>> taskAndTests = testExecutionRequest.getTaskAndTests();

        List<Test> testTasksToRun = new ArrayList<Test>();
        for (final Map.Entry<String, List<InternalJvmTestRequest>> entry : taskAndTests.entrySet()) {
            String testTaskPath = entry.getKey();
            for (Test testTask : queryTestTasks(testTaskPath)) {
                for (InternalJvmTestRequest jvmTestRequest : entry.getValue()) {
                    final TestFilter filter = testTask.getFilter();
                    filter.includeTest(jvmTestRequest.getClassName(), jvmTestRequest.getMethodName());
                }
                testTasksToRun.add(testTask);
            }
        }

        for (InternalTestPatternSpec patternSpec : testExecutionRequest.getTestPatternSpecs()) {
            for (Test task : queryTestTasks(patternSpec.getTaskPath())) {
                testTasksToRun.add(task);
                TestFilter filter = task.getFilter();
                for (String cls : patternSpec.getClasses()) {
                    filter.includeTest(cls, null);
                }
                for (Map.Entry<String, List<String>> entry : patternSpec.getMethods().entrySet()) {
                    String cls = entry.getKey();
                    for (String method : entry.getValue()) {
                        filter.includeTest(cls, method);
                    }
                }
                filter.getIncludePatterns().addAll(patternSpec.getPatterns());
                for (String pkg : patternSpec.getPackages()) {
                    filter.getIncludePatterns().add(pkg + ".*");
                }
            }
        }

        return testTasksToRun;
    }

    private List<Test> configureBuildForTestTasks(TestExecutionRequestAction testExecutionRequest) {
        final Collection<InternalTestDescriptor> testDescriptors = testExecutionRequest.getTestExecutionDescriptors();

        final List<String> testTaskPaths = CollectionUtils.collect(testDescriptors, new Transformer<String, InternalTestDescriptor>() {
            @Override
            public String transform(InternalTestDescriptor testDescriptor) {
                return ((DefaultTestDescriptor) testDescriptor).getTaskPath();
            }
        });

        List<Test> testTasksToRun = new ArrayList<>();
        for (final String testTaskPath : testTaskPaths) {
            for (Test testTask : queryTestTasks(testTaskPath)) {
                for (InternalTestDescriptor testDescriptor : testDescriptors) {
                    DefaultTestDescriptor defaultTestDescriptor = (DefaultTestDescriptor) testDescriptor;
                    if (defaultTestDescriptor.getTaskPath().equals(testTaskPath)) {
                        String className = defaultTestDescriptor.getClassName();
                        String methodName = defaultTestDescriptor.getMethodName();
                        if (className == null && methodName == null) {
                            testTask.getFilter().includeTestsMatching("*");
                        } else {
                            testTask.getFilter().includeTest(className, methodName);
                        }
                    }
                }
                testTasksToRun.add(testTask);
            }
        }
        return testTasksToRun;
    }

    private Set<Task> queryTasks(String testTaskPath) {
        TaskSelection taskSelection;
        try {
            taskSelection = taskSelector.getSelection(testTaskPath);
        } catch (TaskSelectionException e) {
            throw new TestExecutionException(String.format("Requested test task with path '%s' cannot be found.", testTaskPath));
        }

        Set<Task> tasks = taskSelection.getTasks();
        if (tasks.isEmpty()) {
            throw new TestExecutionException(String.format("Requested test task with path '%s' cannot be found.", testTaskPath));
        }

        return tasks;
    }

    private Set<Test> queryTestTasks(String testTaskPath) {
        Set<Test> result = new LinkedHashSet<>();
        for (Task task : queryTasks(testTaskPath)) {
            if (!(task instanceof Test)) {
                throw new TestExecutionException(String.format("Task '%s' of type '%s' not supported for executing tests via TestLauncher API.", testTaskPath, task.getClass().getName()));
            }
            result.add((Test) task);
        }
        return result;
    }

    private List<Test> configureBuildForInternalJvmTestRequest(GradleInternal gradle, TestExecutionRequestAction testExecutionRequest) {
        final Collection<InternalJvmTestRequest> internalJvmTestRequests = testExecutionRequest.getInternalJvmTestRequests();
        if (internalJvmTestRequests.isEmpty()) {
            return Collections.emptyList();
        }

        List<Test> tasksToExecute = new ArrayList<Test>();

        gradle.getOwner().ensureProjectsConfigured();
        for (ProjectState projectState : gradle.getOwner().getProjects().getAllProjects()) {
            projectState.ensureConfigured();
            projectState.applyToMutableState(project -> {
                final Collection<Test> testTasks = project.getTasks().withType(Test.class);
                for (Test testTask : testTasks) {
                    for (InternalJvmTestRequest jvmTestRequest : internalJvmTestRequests) {
                        final TestFilter filter = testTask.getFilter();
                        filter.includeTest(jvmTestRequest.getClassName(), jvmTestRequest.getMethodName());
                    }
                }
                tasksToExecute.addAll(testTasks);
            });
        }
        return tasksToExecute;
    }
}