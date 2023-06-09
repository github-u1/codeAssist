package org.gradle.internal.normalization.java;

import com.google.common.hash.Hasher;
import org.gradle.internal.normalization.java.impl.ApiMemberSelector;
import org.gradle.internal.normalization.java.impl.ApiMemberWriter;
import org.gradle.internal.normalization.java.impl.MethodStubbingApiMemberAdapter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Extracts an "API class" from an original "runtime class".
 */
public class ApiClassExtractor {

    private static final Pattern LOCAL_CLASS_PATTERN = Pattern.compile(".+\\$[0-9]+(?:[\\p{Alnum}_$]+)?$");

    private final Set<String> exportedPackages;
    private final boolean apiIncludesPackagePrivateMembers;
    private final ApiMemberWriterFactory apiMemberWriterFactory;

    public ApiClassExtractor(Set<String> exportedPackages) {
        this(exportedPackages, classWriter -> new ApiMemberWriter(new MethodStubbingApiMemberAdapter(classWriter)));
    }

    public ApiClassExtractor(Set<String> exportedPackages, ApiMemberWriterFactory apiMemberWriterFactory) {
        this.exportedPackages = exportedPackages.isEmpty() ? null : exportedPackages;
        this.apiIncludesPackagePrivateMembers = exportedPackages.isEmpty();
        this.apiMemberWriterFactory = apiMemberWriterFactory;
    }

    private boolean shouldExtractApiClassFrom(ClassReader originalClassReader) {
        if (!ApiMemberSelector.isCandidateApiMember(originalClassReader.getAccess(), apiIncludesPackagePrivateMembers)) {
            return false;
        }
        String originalClassName = originalClassReader.getClassName();
        if (isLocalClass(originalClassName)) {
            return false;
        }
        return exportedPackages == null
               || exportedPackages.contains(packageNameOf(originalClassName));
    }

    /**
     * Extracts an API class from a given original class.
     *
     * @param originalClassReader the reader containing the original class
     * @return bytecode of the API class extracted from the original class. Returns {@link Optional#empty()} when class should not be included, due to some reason that is not known until visited.
     *
     * <p>Checks whether the class's package is in the list of packages
     * explicitly exported by the library (if any), and whether the class should be
     * included in the public API based on its visibility. If the list of exported
     * packages is empty (e.g. the library has not declared an explicit {@code api {...}}
     * specification, then package-private classes are included in the public API. If the
     * list of exported packages is non-empty (i.e. the library has declared an
     * {@code api {...}} specification, then package-private classes are excluded.</p>
     */
    public Optional<byte[]> extractApiClassFrom(ClassReader originalClassReader) {
        if (!shouldExtractApiClassFrom(originalClassReader)) {
            return Optional.empty();
        }
        ClassWriter apiClassWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ApiMemberSelector visitor = new ApiMemberSelector(originalClassReader.getClassName(), apiMemberWriterFactory.makeApiMemberWriter(apiClassWriter), apiIncludesPackagePrivateMembers);
        originalClassReader.accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        if (visitor.isPrivateInnerClass()) {
            return Optional.empty();
        }
        return Optional.of(apiClassWriter.toByteArray());
    }

    public void appendConfigurationToHasher(Hasher hasher) {
        hasher.putString(getClass().getName(), StandardCharsets.UTF_8);
        if (exportedPackages != null) {
            exportedPackages.forEach(charSequence -> hasher.putString(charSequence, StandardCharsets.UTF_8));
        }
    }

    private static String packageNameOf(String internalClassName) {
        int packageSeparatorIndex = internalClassName.lastIndexOf('/');
        return packageSeparatorIndex > 0
                ? internalClassName.substring(0, packageSeparatorIndex).replace('/', '.')
                : "";
    }

    // See JLS3 "Binary Compatibility" (13.1)
    private static boolean isLocalClass(String className) {
        return LOCAL_CLASS_PATTERN.matcher(className).matches();
    }
}