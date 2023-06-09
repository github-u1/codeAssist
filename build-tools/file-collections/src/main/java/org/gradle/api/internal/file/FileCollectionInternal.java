package org.gradle.api.internal.file;

import org.gradle.api.file.FileCollection;
import org.gradle.api.specs.Spec;
import org.gradle.internal.logging.text.TreeFormatter;
import org.gradle.api.internal.tasks.TaskDependencyContainer;

import java.io.File;
import java.util.function.Supplier;

public interface FileCollectionInternal extends FileCollection, TaskDependencyContainer {
    @Override
    FileCollectionInternal filter(Spec<? super File> filterSpec);

    @Override
    FileTreeInternal getAsFileTree();

    /**
     * Returns a copy of this collection, with the given collection replaced with the value returned by the given supplier.
     *
     * This is used to deal with the case where a mutable collection may be added to itself. This is intended to become an error at some point.
     */
    FileCollectionInternal replace(FileCollectionInternal original, Supplier<FileCollectionInternal> supplier);

    /**
     * Visits the structure of this collection, that is, zero or more atomic sources of files.
     *
     * <p>The implementation should call the most specific methods on {@link FileCollectionStructureVisitor} that it is able to.</p>
     */
    void visitStructure(FileCollectionStructureVisitor visitor);

    /**
     * Appends diagnostic information about the contents of this collection to the given formatter.
     */
    TreeFormatter describeContents(TreeFormatter formatter);

    /**
     * Some representation of a source of files.
     */
    interface Source {
    }

    /**
     * An opaque source of files.
     */
    Source OTHER = new Source() {
    };
}