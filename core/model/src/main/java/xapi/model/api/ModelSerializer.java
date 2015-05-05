/**
 *
 */
package xapi.model.api;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public interface ModelSerializer <M extends Model> {

  String modelToString(M model);

  M modelFromString(String model);
}
