package xapi.model.api;

import java.io.IOException;

/**
 * A model provider is temporarily unable to serve operations, independent of whether
 * an individual model exists. For example, a transaction store may be closed or may
 * have lost its ownership lease.
 *
 * <p>Callers must not treat this failure as {@link ModelNotFoundException} or infer
 * that an in-flight write was rolled back. Reconcile uncertain writes by stable key
 * or operation ID before attempting a different write.
 */
public class ModelUnavailable extends IOException {

  private static final long serialVersionUID = 1L;

  public ModelUnavailable(final String message) {
    super(message);
  }

  public ModelUnavailable(final String message, final Throwable cause) {
    super(message, cause);
  }
}
