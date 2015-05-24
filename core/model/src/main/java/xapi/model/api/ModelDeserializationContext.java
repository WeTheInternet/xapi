/**
 *
 */
package xapi.model.api;

import xapi.model.service.ModelService;
import xapi.source.api.CharIterator;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class ModelDeserializationContext {

  private ModelService service;
  private CharIterator chars;
  private PrimitiveSerializer primitives;
  private final Model model;
  private final ModelManifest manifest;
  private boolean clientToServer;

  public ModelDeserializationContext(final Model model, final CharIterator chars, final ModelService service, final ModelManifest manifest) {
    this.model = model;
    this.service = service;
    this.manifest = manifest;
    this.setChars(chars);
    this.primitives= service.primitiveSerializer();
  }

  /**
   * @return -> primitives
   */
  public PrimitiveSerializer getPrimitives() {
    return primitives;
  }
  /**
   * @param primitives -> set primitives
   */
  public void setPrimitives(final PrimitiveSerializer primitives) {
    this.primitives = primitives;
  }
  /**
   * @return -> service
   */
  public ModelService getService() {
    return service;
  }
  /**
   * @param service -> set service
   */
  public void setService(final ModelService service) {
    this.service = service;
  }

  /**
   * @return -> chars
   */
  public CharIterator getChars() {
    return chars;
  }

  /**
   * @param chars -> set chars
   */
  public void setChars(final CharIterator chars) {
    this.chars = chars;
  }

  /**
   * @return -> model
   */
  public Model getModel() {
    return model;
  }

  /**
   * @return -> manifest
   */
  public ModelManifest getManifest() {
    return manifest;
  }

  /**
   * @return -> clientToServer
   */
  public boolean isClientToServer() {
    return clientToServer;
  }

  /**
   * @param clientToServer -> set clientToServer
   */
  public void setClientToServer(final boolean clientToServer) {
    this.clientToServer = clientToServer;
  }

  public ModelDeserializationContext createChildContext(final Class<? extends Model> propertyType, final CharIterator src) {
    return new ModelDeserializationContext(getService().create(propertyType), src, getService(), getManifest());
  }

}
