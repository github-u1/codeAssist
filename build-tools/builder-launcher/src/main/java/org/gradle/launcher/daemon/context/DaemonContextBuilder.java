package org.gradle.launcher.daemon.context;

import com.google.common.collect.Lists;
import org.gradle.internal.Factory;
import org.gradle.internal.jvm.Jvm;
import org.gradle.internal.nativeintegration.ProcessEnvironment;
import org.gradle.launcher.daemon.configuration.DaemonParameters;

import java.io.File;
import java.util.List;
import java.util.Locale;

import static org.gradle.util.internal.GFileUtils.canonicalize;

/**
 * Builds a daemon context, reflecting the current environment.
 * <p>
 * The builder itself has properties for different context values, that allow you to override
 * what would be set based on the environment. This is primarily to aid in testing.
 */
public class DaemonContextBuilder implements Factory<DaemonContext> {

    private String uid;
    private File javaHome;
    private File daemonRegistryDir;
    private Long pid;
    private Integer idleTimeout;
    private Locale locale = Locale.getDefault();
    private List<String> daemonOpts = Lists.newArrayList();
    private DaemonParameters.Priority priority;

    public DaemonContextBuilder(ProcessEnvironment environment) {
        javaHome = canonicalize(Jvm.current().getJavaHome());
        pid = environment.maybeGetPid();
    }

    public File getJavaHome() {
        return javaHome;
    }

    public void setJavaHome(File javaHome) {
        this.javaHome = javaHome;
    }

    public File getDaemonRegistryDir() {
        return this.daemonRegistryDir;
    }

    public void setDaemonRegistryDir(File daemonRegistryDir) {
        this.daemonRegistryDir = daemonRegistryDir;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public Long getPid() {
        return pid;
    }
    
    public void setPid(Long pid) {
        this.pid = pid;
    }

    public Integer getIdleTimeout() {
        return idleTimeout;
    }
    
    public void setIdleTimeout(Integer idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public List<String> getDaemonOpts() {
        return daemonOpts;
    }

    public void setDaemonOpts(List<String> daemonOpts) {
        this.daemonOpts = daemonOpts;
    }

    public void setPriority(DaemonParameters.Priority priority) {
        this.priority = priority;
    }

    public void useDaemonParameters(DaemonParameters daemonParameters) {
        setJavaHome(daemonParameters.getEffectiveJvm().getJavaHome());
        setDaemonOpts(daemonParameters.getEffectiveJvmArgs());
        setPriority(daemonParameters.getPriority());
    }

    /**
     * Creates a new daemon context, based on the current state of this builder.
     */
    @Override
    public DaemonContext create() {
        if (daemonRegistryDir == null) {
            throw new IllegalStateException("Registry dir must be specified.");
        }
        return new DefaultDaemonContext(uid, javaHome, daemonRegistryDir, pid, idleTimeout, daemonOpts, priority);
    }
}
