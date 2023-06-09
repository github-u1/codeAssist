package org.gradle.api.internal.file;

import org.gradle.api.Task;
import org.gradle.api.Transformer;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.internal.Factory;
import org.gradle.api.internal.file.collections.MinimalFileSet;
import org.gradle.api.internal.provider.MappingProvider;
import org.gradle.api.internal.provider.PropertyHost;
import org.gradle.api.internal.provider.Providers;
import org.gradle.api.internal.tasks.TaskDependencyFactory;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.util.PatternSet;

import java.io.File;

public class DefaultProjectLayout implements ProjectLayout, TaskFileVarFactory {
    private final Directory projectDir;
    private final DirectoryProperty buildDir;
    private final FileResolver fileResolver;
    private final TaskDependencyFactory taskDependencyFactory;
    private final Factory<PatternSet> patternSetFactory;
    private final PropertyHost propertyHost;
    private final FileCollectionFactory fileCollectionFactory;
    private final FileFactory fileFactory;

    public DefaultProjectLayout(File projectDir, FileResolver fileResolver, TaskDependencyFactory taskDependencyFactory, Factory<PatternSet> patternSetFactory, PropertyHost propertyHost, FileCollectionFactory fileCollectionFactory, FilePropertyFactory filePropertyFactory, FileFactory fileFactory) {
        this.fileResolver = fileResolver;
        this.taskDependencyFactory = taskDependencyFactory;
        this.patternSetFactory = patternSetFactory;
        this.propertyHost = propertyHost;
        this.fileCollectionFactory = fileCollectionFactory;
        this.fileFactory = fileFactory;
        this.projectDir = fileFactory.dir(projectDir);
        this.buildDir = filePropertyFactory.newDirectoryProperty().convention(fileFactory.dir(fileResolver.resolve(
                Project.DEFAULT_BUILD_DIR_NAME)));
    }

    @Override
    public Directory getProjectDirectory() {
        return projectDir;
    }

    @Override
    public DirectoryProperty getBuildDirectory() {
        return buildDir;
    }

    @Override
    public ConfigurableFileCollection newInputFileCollection(Task consumer) {
        return new CachingTaskInputFileCollection(fileResolver, patternSetFactory, taskDependencyFactory, propertyHost);
    }

    @Override
    public FileCollection newCalculatedInputFileCollection(Task consumer, MinimalFileSet calculatedFiles, FileCollection... inputs) {
        return new CalculatedTaskInputFileCollection(consumer.getPath(), calculatedFiles, inputs);
    }

    @Override
    public Provider<RegularFile> file(Provider<File> provider) {
        return new MappingProvider<>(RegularFile.class, Providers.internal(provider), new Transformer<RegularFile, File>() {
            @Override
            public RegularFile transform(File file) {
                return fileFactory.file(fileResolver.resolve(file));
            }
        });
    }

    @Override
    public Provider<Directory> dir(Provider<File> provider) {
        return new MappingProvider<>(Directory.class, Providers.internal(provider), new Transformer<Directory, File>() {
            @Override
            public Directory transform(File file) {
                return fileFactory.dir(fileResolver.resolve(file));
            }
        });
    }

    @Override
    public FileCollection files(Object... paths) {
        return fileCollectionFactory.resolving(paths);
    }

    /**
     * A temporary home. Should be on the public API somewhere
     */
    public void setBuildDirectory(Object value) {
        buildDir.set(fileResolver.resolve(value));
    }
}

