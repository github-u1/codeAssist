package org.gradle.internal.concurrent;

import java.util.concurrent.ScheduledExecutorService;

public interface ManagedScheduledExecutor extends ManagedExecutor, ScheduledExecutorService {
}