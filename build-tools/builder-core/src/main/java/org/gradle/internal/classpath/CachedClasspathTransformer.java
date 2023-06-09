package org.gradle.internal.classpath;

import com.google.common.hash.Hasher;
import org.gradle.api.file.RelativePath;
import org.gradle.internal.Pair;
import org.gradle.internal.service.scopes.Scopes;
import org.gradle.internal.service.scopes.ServiceScope;

import org.objectweb.asm.ClassVisitor;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;

/**
 * Represents a transformer that takes a given ClassPath and transforms it to a ClassPath with cached jars
 */
@ServiceScope(Scopes.UserHome.class)
public interface CachedClasspathTransformer {
    enum StandardTransform {
        BuildLogic, None
    }

    /**
     * Transforms a classpath to a classpath with the given transformations applied.
     */
    ClassPath transform(ClassPath classPath, StandardTransform transform);

    /**
     * Transforms a classpath to a classpath with the given transformations applied.
     */
    ClassPath transform(ClassPath classPath, StandardTransform transform, Transform additional);

    /**
     * Transform a collection of urls to a new collection where the file urls are cached jars
     */
    Collection<URL> transform(Collection<URL> urls, StandardTransform transform);

    interface Transform {
        void applyConfigurationTo(Hasher hasher);

        Pair<RelativePath, ClassVisitor> apply(ClasspathEntryVisitor.Entry entry, ClassVisitor visitor) throws IOException;
    }
}
