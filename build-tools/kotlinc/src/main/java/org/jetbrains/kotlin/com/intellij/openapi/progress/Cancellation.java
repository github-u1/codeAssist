package org.jetbrains.kotlin.com.intellij.openapi.progress;

import static kotlinx.coroutines.JobKt.ensureActive;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.jetbrains.kotlin.com.intellij.openapi.util.ThrowableComputable;

import java.util.concurrent.CancellationException;

import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.Job;

@ApiStatus.Internal
public final class Cancellation {

  private Cancellation() { }

//  @VisibleForTesting
//  public static @Nullable Job currentJob() {
//    return contextJob(ThreadContext.currentThreadContext());
//  }
//
//  public static @Nullable Job contextJob(@NotNull CoroutineContext context) {
//    return context.get(Job.Key);
//  }
//
//  public static boolean isCancelled() {
//    Job job = currentJob();
//    return job != null && job.isCancelled();
//  }
//
//  public static void checkCancelled() {
//    Job currentJob = currentJob();
//    if (currentJob != null) {
//      try {
//        ensureActive(currentJob);
//      }
//      catch (CancellationException e) {
//        throw new JobCanceledException(e);
//      }
//    }
//  }
//
//  /**
//   * Installs the given job as {@link Cancellation#currentJob() current}, runs {@code action}, and returns its result.
//   * If the given job becomes cancelled, then {@code ProgressManager#checkCanceled} will throw an instance
//   * of the special {@link ProcessCanceledException} subclass inside the given action,
//   * and this method will throw the cancellation exception wrapping PCE.
//   */
//  public static <T, E extends Throwable> T withCurrentJob(
//    @NotNull Job job,
//    @NotNull ThrowableComputable<T, E> action
//  ) throws E, CancellationException {
//    try (AccessToken ignored = ThreadContext.withThreadContext(job)) {
//      return action.compute();
//    }
//    catch (JobCanceledException e) {
//      // This exception is thrown only from `Cancellation.checkCancelled`.
//      // If it's caught, then the job must've been cancelled.
//      if (!job.isCancelled()) {
//        throw new IllegalStateException("JobCanceledException must be thrown by ProgressManager.checkCanceled()", e);
//      }
//      throw new CurrentJobCancellationException(e);
//    }
//  }
//
//  public static @Nullable Throwable getCause(@NotNull CancellationException ce) {
//    if (ce instanceof CurrentJobCancellationException) {
//      return ((CurrentJobCancellationException)ce).getOriginalCancellationException().getCause();
//    }
//    else {
//      return ce.getCause();
//    }
//  }

  /**
   * {@code true} if running in non-cancelable section started with {@link #computeInNonCancelableSection)} in this thread,
   * otherwise {@code false}
   */
  // do not supply initial value to conserve memory
  private static final ThreadLocal<Boolean> isInNonCancelableSection = new ThreadLocal<>();

  public static boolean isInNonCancelableSection() {
    return isInNonCancelableSection.get() != null;
  }

  public static <T, E extends Exception> T computeInNonCancelableSection(@NotNull ThrowableComputable<T, E> computable) throws E {
    try {
      if (isInNonCancelableSection()) {
        return computable.compute();
      }
      try {
        isInNonCancelableSection.set(Boolean.TRUE);
        return computable.compute();
      }
      finally {
        isInNonCancelableSection.remove();
      }
    }
    catch (ProcessCanceledException e) {
      throw new RuntimeException("PCE is not expected in non-cancellable section execution", e);
    }
  }
}