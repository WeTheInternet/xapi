package xapi.source.api;

/**
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public interface ChainedSource <Self> {

  Self done();
  String toSource();

}