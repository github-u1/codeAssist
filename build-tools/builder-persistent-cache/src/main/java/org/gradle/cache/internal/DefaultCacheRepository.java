package org.gradle.cache.internal;


import static org.gradle.cache.internal.filelock.LockOptionsBuilder.mode;

import org.gradle.api.Action;
import org.gradle.cache.CacheBuilder;
import org.gradle.cache.CacheRepository;
import org.gradle.cache.CleanupAction;
import org.gradle.cache.FileLockManager;
import org.gradle.cache.LockOptions;
import org.gradle.cache.PersistentCache;

import java.io.File;
import java.util.Collections;
import java.util.Map;

public class DefaultCacheRepository implements CacheRepository {
    private final CacheScopeMapping cacheScopeMapping;
    private final CacheFactory factory;

    public DefaultCacheRepository(CacheScopeMapping cacheScopeMapping, CacheFactory factory) {
        this.cacheScopeMapping = cacheScopeMapping;
        this.factory = factory;
    }

    @Override
    public CacheBuilder cache(String key) {
        return new PersistentCacheBuilder(key);
    }

    @Override
    public CacheBuilder cache(File baseDir) {
        return new PersistentCacheBuilder(baseDir);
    }

    private class PersistentCacheBuilder implements CacheBuilder {
        final String key;
        final File baseDir;
        Map<String, ?> properties = Collections.emptyMap();
        Action<? super PersistentCache> initializer;
        CleanupAction cleanup;
        LockOptions lockOptions = mode(FileLockManager.LockMode.Shared);
        String displayName;
        VersionStrategy versionStrategy = VersionStrategy.CachePerVersion;
        LockTarget lockTarget = LockTarget.DefaultTarget;

        PersistentCacheBuilder(String key) {
            this.key = key;
            this.baseDir = null;
        }

        PersistentCacheBuilder(File baseDir) {
            this.key = null;
            this.baseDir = baseDir;
        }

        @Override
        public CacheBuilder withProperties(Map<String, ?> properties) {
            this.properties = properties;
            return this;
        }

        @Override
        public CacheBuilder withCrossVersionCache(LockTarget lockTarget) {
            this.versionStrategy = VersionStrategy.SharedCache;
            this.lockTarget = lockTarget;
            return this;
        }

        @Override
        public CacheBuilder withDisplayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        @Override
        public CacheBuilder withLockOptions(LockOptions lockOptions) {
            this.lockOptions = lockOptions;
            return this;
        }

        @Override
        public CacheBuilder withInitializer(Action<? super PersistentCache> initializer) {
            this.initializer = initializer;
            return this;
        }

        @Override
        public CacheBuilder withCleanup(CleanupAction cleanup) {
            this.cleanup = cleanup;
            return this;
        }

        @Override
        public PersistentCache open() {
            File cacheBaseDir;
            if (baseDir != null) {
                cacheBaseDir = baseDir;
            } else {
                cacheBaseDir = cacheScopeMapping.getBaseDirectory(null, key, versionStrategy);
            }
            return factory.open(cacheBaseDir, displayName, properties, lockTarget, lockOptions, initializer, cleanup);
        }
    }
}