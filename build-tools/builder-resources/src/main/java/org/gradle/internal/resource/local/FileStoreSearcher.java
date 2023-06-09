package org.gradle.internal.resource.local;

import java.util.Set;

public interface FileStoreSearcher<S> {

    Set<? extends LocallyAvailableResource> search(S key);

}