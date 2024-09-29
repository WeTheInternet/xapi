package xapi.except;

/**
 * A marker interface for exception to signal that they are fatal,
 * and should not be retried.
 *
 * All XApi error handlers will test for instanceof IsFatal,
 * and will not automatically retry such exceptions.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public interface IsFatal {

}
