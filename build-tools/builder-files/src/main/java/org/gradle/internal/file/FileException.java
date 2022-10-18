package org.gradle.internal.file;


public class FileException extends RuntimeException {
    public FileException(Throwable cause) {
        super(cause);
    }

    public FileException(String message, Throwable cause) {
        super(message, cause);
    }
}