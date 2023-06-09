package com.tyron.builder.gradle.internal.scope

import com.tyron.builder.api.artifact.Artifact
import com.tyron.builder.api.artifact.ArtifactKind
import org.gradle.api.file.Directory
import org.gradle.api.file.FileSystemLocation

/**
 * List of internal [Artifact.Multiple] [Artifact]
 */
sealed class InternalMultipleArtifactType<T: FileSystemLocation>(
    kind: ArtifactKind<T>,
    category: Category = Category.INTERMEDIATES
) : Artifact.Multiple<T>(kind, category), Artifact.Appendable {

    // The final dex files (if the dex splitter does not run)
    // that will get packaged in the APK or bundle.
    object DEX: InternalMultipleArtifactType<Directory>(DIRECTORY)

    // External libraries' dex files only.
    object EXTERNAL_LIBS_DEX: InternalMultipleArtifactType<Directory>(DIRECTORY)

    // Partial R.txt files generated by AAPT2 at compile time.
    object PARTIAL_R_FILES: InternalMultipleArtifactType<Directory>(DIRECTORY)

    // --- Namespaced android res ---
    // Compiled resources (directory of .flat files) for the local library
    object RES_COMPILED_FLAT_FILES: InternalMultipleArtifactType<Directory>(DIRECTORY)
}