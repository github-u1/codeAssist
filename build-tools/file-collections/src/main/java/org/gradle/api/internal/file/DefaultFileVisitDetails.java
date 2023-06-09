package org.gradle.api.internal.file;

import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.RelativePath;
import org.gradle.internal.file.Chmod;
import org.gradle.internal.file.Stat;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultFileVisitDetails extends DefaultFileTreeElement implements FileVisitDetails {
    private final AtomicBoolean stop;

    public DefaultFileVisitDetails(File file, RelativePath relativePath, AtomicBoolean stop, Chmod chmod, Stat stat) {
        super(file, relativePath, chmod, stat);
        this.stop = stop;
    }

    public DefaultFileVisitDetails(File file, Chmod chmod, Stat stat) {
        this(file, new RelativePath(!file.isDirectory(), file.getName()), new AtomicBoolean(), chmod, stat);
    }

    @Override
    public void stopVisiting() {
        stop.set(true);
    }
}