package org.gradle.groovy.scripts;

import org.gradle.internal.DisplayName;
import org.gradle.internal.resource.TextResource;

public class DelegatingScriptSource implements ScriptSource {
    private final ScriptSource source;

    public DelegatingScriptSource(ScriptSource source) {
        this.source = source;
    }

    public ScriptSource getSource() {
        return source;
    }

    @Override
    public String getClassName() {
        return source.getClassName();
    }

    @Override
    public String getDisplayName() {
        return source.getDisplayName();
    }

    @Override
    public DisplayName getLongDisplayName() {
        return source.getLongDisplayName();
    }

    @Override
    public DisplayName getShortDisplayName() {
        return source.getShortDisplayName();
    }

    @Override
    public String getFileName() {
        return source.getFileName();
    }

    @Override
    public TextResource getResource() {
        return source.getResource();
    }
}
