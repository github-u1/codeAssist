package org.gradle.api.internal.file;

import org.gradle.internal.Factory;
import org.gradle.internal.logging.text.TreeFormatter;
import org.gradle.api.internal.tasks.TaskDependencyResolveContext;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class FilteredFileTree extends CompositeFileTree implements FileCollectionInternal.Source {
    private final FileTreeInternal tree;
    private final Supplier<? extends PatternSet> patternSupplier;

    public FilteredFileTree(FileTreeInternal tree, Factory<PatternSet> patternSetFactory, Supplier<? extends PatternSet> patternSupplier) {
        super(patternSetFactory);
        this.tree = tree;
        this.patternSupplier = patternSupplier;
    }

    @Override
    public String getDisplayName() {
        return tree.getDisplayName();
    }

    @Override
    protected void appendContents(TreeFormatter formatter) {
        formatter.node("backing tree");
        formatter.startChildren();
        tree.describeContents(formatter);
        formatter.endChildren();
    }

    public FileTreeInternal getTree() {
        return tree;
    }

    /**
     * The current set of patterns. Both the instance and the patterns it contains can change over time.
     */
    public PatternSet getPatterns() {
        return patternSupplier.get();
    }

    @Override
    protected void visitChildren(Consumer<FileCollectionInternal> visitor) {
        // For backwards compatibility, need to calculate the patterns on each query
        PatternFilterable patterns = getPatterns();
        tree.visitContentsAsFileTrees(child -> visitor.accept(child.matching(patterns)));
    }

    @Override
    public void visitDependencies(TaskDependencyResolveContext context) {
        tree.visitDependencies(context);
    }
}