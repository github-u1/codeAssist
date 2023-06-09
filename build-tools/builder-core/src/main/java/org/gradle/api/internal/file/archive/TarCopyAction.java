package org.gradle.api.internal.file.archive;

import org.gradle.api.GradleException;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.internal.file.CopyActionProcessingStreamAction;
import org.gradle.api.internal.file.archive.compression.ArchiveOutputStreamFactory;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.internal.file.copy.CopyActionProcessingStream;
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal;
import org.gradle.api.tasks.WorkResult;
import org.gradle.api.tasks.WorkResults;
import org.gradle.internal.ErroringAction;
import org.gradle.internal.IoActions;

import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarOutputStream;
import org.apache.tools.zip.UnixStat;

import java.io.File;
import java.io.OutputStream;

public class TarCopyAction implements CopyAction {
    /**
     * An arbitrary timestamp chosen to provide constant file timestamps inside the tar archive.
     *
     * The value 0 is avoided to circumvent certain limitations of languages and applications that do not work well with the zero value.
     * (Like older Java implementations and libraries)
     *
     * The date is January 2, 1970.
     */
    public static final long CONSTANT_TIME_FOR_TAR_ENTRIES = 86400000;

    private final File tarFile;
    private final ArchiveOutputStreamFactory compressor;
    private final boolean preserveFileTimestamps;

    public TarCopyAction(File tarFile, ArchiveOutputStreamFactory compressor, boolean preserveFileTimestamps) {
        this.tarFile = tarFile;
        this.compressor = compressor;
        this.preserveFileTimestamps = preserveFileTimestamps;
    }

    @Override
    public WorkResult execute(final CopyActionProcessingStream stream) {

        final OutputStream outStr;
        try {
            outStr = compressor.createArchiveOutputStream(tarFile);
        } catch (Exception e) {
            throw new GradleException(String.format("Could not create TAR '%s'.", tarFile), e);
        }

        IoActions.withResource(outStr, new ErroringAction<OutputStream>() {
            @Override
            protected void doExecute(final OutputStream outStr) throws Exception {
                TarOutputStream tarOutStr;
                try {
                    tarOutStr = new TarOutputStream(outStr);
                } catch (Exception e) {
                    throw new GradleException(String.format("Could not create TAR '%s'.", tarFile), e);
                }
                tarOutStr.setLongFileMode(TarOutputStream.LONGFILE_GNU);
                tarOutStr.setBigNumberMode(TarOutputStream.BIGNUMBER_STAR);
                stream.process(new StreamAction(tarOutStr));
                tarOutStr.close();
            }
        });

        return WorkResults.didWork(true);
    }

    private class StreamAction implements CopyActionProcessingStreamAction {
        private final TarOutputStream tarOutStr;

        public StreamAction(TarOutputStream tarOutStr) {
            this.tarOutStr = tarOutStr;
        }

        @Override
        public void processFile(FileCopyDetailsInternal details) {
            if (details.isDirectory()) {
                visitDir(details);
            } else {
                visitFile(details);
            }
        }

        private void visitFile(FileCopyDetails fileDetails) {
            try {
                TarEntry archiveEntry = new TarEntry(fileDetails.getRelativePath().getPathString());
                archiveEntry.setModTime(getArchiveTimeFor(fileDetails));
                archiveEntry.setSize(fileDetails.getSize());
                archiveEntry.setMode(UnixStat.FILE_FLAG | fileDetails.getMode());
                tarOutStr.putNextEntry(archiveEntry);
                fileDetails.copyTo(tarOutStr);
                tarOutStr.closeEntry();
            } catch (Exception e) {
                throw new GradleException(String.format("Could not add %s to TAR '%s'.", fileDetails, tarFile), e);
            }
        }

        private void visitDir(FileCopyDetails dirDetails) {
            try {
                // Trailing slash on name indicates entry is a directory
                TarEntry archiveEntry = new TarEntry(dirDetails.getRelativePath().getPathString() + '/');
                archiveEntry.setModTime(getArchiveTimeFor(dirDetails));
                archiveEntry.setMode(UnixStat.DIR_FLAG | dirDetails.getMode());
                tarOutStr.putNextEntry(archiveEntry);
                tarOutStr.closeEntry();
            } catch (Exception e) {
                throw new GradleException(String.format("Could not add %s to TAR '%s'.", dirDetails, tarFile), e);
            }
        }
    }

    private long getArchiveTimeFor(FileCopyDetails details) {
        return preserveFileTimestamps ? details.getLastModified() : CONSTANT_TIME_FOR_TAR_ENTRIES;
    }
}
