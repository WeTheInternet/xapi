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
  private PrimitiveSerializer primitives;
  private final Model model;
  private final ModelManifest manifest;
  private boolean clientToServer;

  public ModelDeserializationContext(final Model model, final ModelService service, final ModelManifest manifest) {
    this.model = model;
    this.service = service;
    this.manifest = manifest;
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

  public ModelDeserializationContext createChildContext(final Class<? extends Model> propertyType) {
    return new ModelDeserializationContext(getService().create(propertyType), getService(), getManifest());
  }

}
