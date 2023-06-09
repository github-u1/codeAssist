package org.gradle.internal.graph;

import org.gradle.api.Action;
import org.gradle.internal.logging.text.StyledTextOutput;

public class GraphRenderer {
    private final StyledTextOutput output;
    private StringBuilder prefix = new StringBuilder();
    private boolean seenRootChildren;
    private boolean lastChild = true;

    public GraphRenderer(StyledTextOutput output) {
        this.output = output;
    }

    /**
     * Visits a node in the graph.
     */
    public void visit(Action<? super StyledTextOutput> node, boolean lastChild) {
        if (seenRootChildren) {
            output.withStyle(StyledTextOutput.Style.Info).text(prefix + (lastChild ? "\\--- " : "+--- "));
        }
        this.lastChild = lastChild;
        node.execute(output);
        output.println();
    }

    /**
     * Starts visiting the children of the most recently visited node.
     */
    public void startChildren() {
        if (seenRootChildren) {
            prefix.append(lastChild ? "     " : "|    ");
        }
        seenRootChildren = true;
    }

    /**
     * Completes visiting the children of the node which most recently started visiting children.
     */
    public void completeChildren() {
        if (prefix.length() == 0) {
            seenRootChildren = false;
        } else {
            prefix.setLength(prefix.length() - 5);
        }
    }

    public StyledTextOutput getOutput() {
        return output;
    }
}