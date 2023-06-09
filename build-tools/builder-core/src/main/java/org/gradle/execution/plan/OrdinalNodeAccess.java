package org.gradle.execution.plan;

import javax.annotation.Nullable;
import java.util.List;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static com.google.common.collect.Streams.concat;

/**
 * A factory for creating and accessing ordinal nodes
 */
public class OrdinalNodeAccess {
    private final OrdinalGroupFactory ordinalGroups;
    private final IdentityHashMap<OrdinalGroup, OrdinalNode> destroyerLocationNodes = new IdentityHashMap<>();
    private final IdentityHashMap<OrdinalGroup, OrdinalNode> producerLocationNodes = new IdentityHashMap<>();

    public OrdinalNodeAccess(OrdinalGroupFactory ordinalGroups) {
        this.ordinalGroups = ordinalGroups;
    }

    OrdinalNode getOrCreateDestroyableLocationNode(OrdinalGroup ordinal) {
        return destroyerLocationNodes.computeIfAbsent(ordinal, this::createDestroyerLocationNode);
    }

    OrdinalNode getOrCreateOutputLocationNode(OrdinalGroup ordinal) {
        return producerLocationNodes.computeIfAbsent(ordinal, this::createProducerLocationNode);
    }

    List<OrdinalGroup> getAllGroups() {
        return ordinalGroups.getAllGroups();
    }

    Stream<OrdinalNode> getAllNodes() {
        return concat(destroyerLocationNodes.values().stream(), producerLocationNodes.values().stream());
    }

    /**
     * Create relationships between the ordinal nodes such that destroyer ordinals cannot complete until all preceding producer
     * ordinals have completed (and vice versa).  This ensures that an ordinal does not complete early simply because the nodes in
     * the ordinal group it represents have no explicit dependencies.
     */
    void createInterNodeRelationships() {
        createInterNodeRelationshipsFor(destroyerLocationNodes);
        createInterNodeRelationshipsFor(producerLocationNodes);
    }

    private void createInterNodeRelationshipsFor(Map<OrdinalGroup, OrdinalNode> nodes) {
        nodes.forEach((ordinal, node) -> {
            for (int i = 0; i < ordinal.getOrdinal(); i++) {
                Node precedingNode = nodes.get(group(i));
                if (precedingNode != null) {
                    node.addDependencySuccessor(precedingNode);
                }
            }
        });
    }

    private OrdinalNode createDestroyerLocationNode(OrdinalGroup ordinal) {
        return createOrdinalNode(OrdinalNode.Type.DESTROYER, ordinal);
    }

    private OrdinalNode createProducerLocationNode(OrdinalGroup ordinal) {
        return createOrdinalNode(OrdinalNode.Type.PRODUCER, ordinal);
    }

    private OrdinalNode createOrdinalNode(OrdinalNode.Type type, OrdinalGroup ordinal) {
        OrdinalNode ordinalNode = new OrdinalNode(type, ordinal);
        ordinalNode.require();
        return ordinalNode;
    }

    public OrdinalGroup group(int ordinal) {
        return ordinalGroups.group(ordinal);
    }

    @Nullable
    public Node getPrecedingProducerLocationNode(OrdinalGroup ordinal) {
        if (ordinal.getOrdinal() == 0) {
            return null;
        } else {
            return getOrCreateOutputLocationNode(group(ordinal.getOrdinal() - 1));
        }
    }

    @Nullable
    public Node getPrecedingDestroyerLocationNode(OrdinalGroup ordinal) {
        if (ordinal.getOrdinal() == 0) {
            return null;
        } else {
            return getOrCreateDestroyableLocationNode(group(ordinal.getOrdinal() - 1));
        }
    }

    public void reset() {
        ordinalGroups.reset();
        destroyerLocationNodes.clear();
        producerLocationNodes.clear();
    }
}