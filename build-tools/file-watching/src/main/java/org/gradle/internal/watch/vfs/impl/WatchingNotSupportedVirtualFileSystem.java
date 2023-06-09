package org.gradle.internal.watch.vfs.impl;

import org.gradle.internal.operations.BuildOperationContext;
import org.gradle.internal.operations.BuildOperationDescriptor;
import org.gradle.internal.operations.BuildOperationRunner;
import org.gradle.internal.operations.CallableBuildOperation;
import org.gradle.internal.snapshot.SnapshotHierarchy;
import org.gradle.internal.vfs.VirtualFileSystem;
import org.gradle.internal.vfs.impl.AbstractVirtualFileSystem;
import org.gradle.internal.vfs.impl.VfsRootReference;
import org.gradle.internal.watch.registry.WatchMode;
import org.gradle.internal.watch.vfs.BuildFinishedFileSystemWatchingBuildOperationType;
import org.gradle.internal.watch.vfs.BuildLifecycleAwareVirtualFileSystem;
import org.gradle.internal.watch.vfs.BuildStartedFileSystemWatchingBuildOperationType;
import org.gradle.internal.watch.vfs.FileSystemWatchingInformation;
import org.gradle.internal.watch.vfs.VfsLogging;
import org.gradle.internal.watch.vfs.WatchLogging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * A {@link VirtualFileSystem} which is not able to register any watches.
 */
public class WatchingNotSupportedVirtualFileSystem extends AbstractVirtualFileSystem implements BuildLifecycleAwareVirtualFileSystem, FileSystemWatchingInformation {

    private static final Logger LOGGER = LoggerFactory.getLogger(WatchingNotSupportedVirtualFileSystem.class);

    public WatchingNotSupportedVirtualFileSystem(VfsRootReference rootReference) {
        super(rootReference);
    }

    @Override
    protected SnapshotHierarchy updateNotifyingListeners(AbstractVirtualFileSystem.UpdateFunction updateFunction) {
        return updateFunction.update(SnapshotHierarchy.NodeDiffListener.NOOP);
    }

    @Override
    public boolean afterBuildStarted(
            WatchMode watchMode,
            VfsLogging vfsLogging,
            WatchLogging watchLogging,
            BuildOperationRunner buildOperationRunner
    ) {
        if (watchMode == WatchMode.ENABLED) {
            LOGGER.warn("Watching the file system is not supported.");
        }
        rootReference.update(vfsRoot -> buildOperationRunner.call(new CallableBuildOperation<SnapshotHierarchy>() {
            @Override
            public SnapshotHierarchy call(BuildOperationContext context) {
                context.setResult(BuildStartedFileSystemWatchingBuildOperationType.Result.WATCHING_DISABLED);
                return vfsRoot.empty();
            }

            @Override
            public BuildOperationDescriptor.Builder description() {
                return BuildOperationDescriptor.displayName(BuildStartedFileSystemWatchingBuildOperationType.DISPLAY_NAME)
                        .details(BuildStartedFileSystemWatchingBuildOperationType.Details.INSTANCE);
            }
        }));
        return false;
    }

    @Override
    public void registerWatchableHierarchy(File rootDirectoryForWatching) {
    }

    @Override
    public void beforeBuildFinished(
            WatchMode watchMode,
            VfsLogging vfsLogging,
            WatchLogging watchLogging,
            BuildOperationRunner buildOperationRunner,
            int maximumNumberOfWatchedHierarchies
    ) {
        rootReference.update(vfsRoot -> buildOperationRunner.call(new CallableBuildOperation<SnapshotHierarchy>() {
            @Override
            public SnapshotHierarchy call(BuildOperationContext context) {
                context.setResult(BuildFinishedFileSystemWatchingBuildOperationType.Result.WATCHING_DISABLED);
                return vfsRoot.empty();
            }

            @Override
            public BuildOperationDescriptor.Builder description() {
                return BuildOperationDescriptor.displayName(BuildFinishedFileSystemWatchingBuildOperationType.DISPLAY_NAME)
                        .details(BuildFinishedFileSystemWatchingBuildOperationType.Details.INSTANCE);
            }
        }));
    }

    @Override
    public boolean isWatchingAnyLocations() {
        return false;
    }
}
