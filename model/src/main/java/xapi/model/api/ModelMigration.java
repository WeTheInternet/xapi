/**
 *
 */
package xapi.model.api;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public interface ModelMigration <Context> {

  Model migrate(Model m, Context c);

}
