package org.gradle.api.internal.changedetection.state;

import com.google.common.hash.HashCode;
import org.gradle.cache.GlobalCacheLocations;
import org.gradle.internal.fingerprint.hashing.FileSystemLocationSnapshotHasher;
import org.gradle.internal.fingerprint.hashing.RegularFileSnapshotContext;
import org.gradle.internal.fingerprint.hashing.RegularFileSnapshotContextHasher;
import org.gradle.internal.snapshot.FileSystemLocationSnapshot;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * A {@link ResourceSnapshotterCacheService} that delegates to the global service for immutable files
 * and uses the local service for all other files. This ensures optimal cache utilization.
 */
public class SplitResourceSnapshotterCacheService implements ResourceSnapshotterCacheService {
    private final ResourceSnapshotterCacheService globalCache;
    private final ResourceSnapshotterCacheService localCache;
    private final GlobalCacheLocations globalCacheLocations;

    public SplitResourceSnapshotterCacheService(ResourceSnapshotterCacheService globalCache, ResourceSnapshotterCacheService localCache, GlobalCacheLocations globalCacheLocations) {
        this.globalCache = globalCache;
        this.localCache = localCache;
        this.globalCacheLocations = globalCacheLocations;
    }

    @Nullable
    @Override
    public HashCode hashFile(FileSystemLocationSnapshot snapshot, FileSystemLocationSnapshotHasher hasher, HashCode configurationHash) throws IOException {
        if (globalCacheLocations.isInsideGlobalCache(snapshot.getAbsolutePath())) {
            return globalCache.hashFile(snapshot, hasher, configurationHash);
        } else {
            return localCache.hashFile(snapshot, hasher, configurationHash);
        }
    }

    @Override
    public HashCode hashFile(RegularFileSnapshotContext fileSnapshotContext, RegularFileSnapshotContextHasher hasher, HashCode configurationHash) throws IOException {
        if (globalCacheLocations.isInsideGlobalCache(fileSnapshotContext.getSnapshot().getAbsolutePath())) {
            return globalCache.hashFile(fileSnapshotContext, hasher, configurationHash);
        } else {
            return localCache.hashFile(fileSnapshotContext, hasher, configurationHash);
        }
    }
}
