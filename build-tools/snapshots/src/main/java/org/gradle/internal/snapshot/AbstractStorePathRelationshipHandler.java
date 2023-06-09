package org.gradle.internal.snapshot;

public abstract class AbstractStorePathRelationshipHandler<T> implements ChildMap.Entry.PathRelationshipHandler<ChildMap<T>, T> {

    private final CaseSensitivity caseSensitivity;
    private final ChildMap.StoreHandler<T> handler;

    public AbstractStorePathRelationshipHandler(CaseSensitivity caseSensitivity, ChildMap.StoreHandler<T> handler) {
        this.caseSensitivity = caseSensitivity;
        this.handler = handler;
    }

    public abstract ChildMap<T> withReplacedChild(T newChild);

    public abstract ChildMap<T> withReplacedChild(String newChildPath, T newChild);

    public abstract ChildMap<T> withNewChild(String newChildPath, T newChild);

    @Override
    public ChildMap<T> handleAsDescendantOfChild(VfsRelativePath targetPath, String childPath, T child) {
        T newChild = handler.handleAsDescendantOfChild(targetPath.pathFromChild(childPath), child);
        return withReplacedChild(newChild);
    }

    @Override
    public ChildMap<T> handleAsAncestorOfChild(VfsRelativePath targetPath, String childPath, T child) {
        T newChild = handler.handleAsAncestorOfChild(childPath, child);
        return withReplacedChild(targetPath.getAsString(), newChild);
    }

    @Override
    public ChildMap<T> handleExactMatchWithChild(VfsRelativePath targetPath, String childPath, T child) {
        T newChild = handler.mergeWithExisting(child);
        return withReplacedChild(newChild);
    }

    @Override
    public ChildMap<T> handleSiblingOfChild(VfsRelativePath targetPath, String childPath, T child, int commonPrefixLength) {
        String commonPrefix = childPath.substring(0, commonPrefixLength);
        String newChildPath = childPath.substring(commonPrefixLength + 1);
        ChildMap.Entry<T> newChild = new ChildMap.Entry<>(newChildPath, child);
        String siblingPath = targetPath.pathFromChild(commonPrefix).getAsString();
        ChildMap.Entry<T> sibling = new ChildMap.Entry<>(siblingPath, handler.createChild());
        ChildMap<T> newChildren = ChildMapFactory.childMap(caseSensitivity, newChild, sibling);
        return withReplacedChild(commonPrefix, handler.createNodeFromChildren(newChildren));
    }

    @Override
    public ChildMap<T> handleUnrelatedToAnyChild(VfsRelativePath targetPath) {
        String path = targetPath.getAsString();
        T newNode = handler.createChild();
        return withNewChild(path, newNode);
    }
}