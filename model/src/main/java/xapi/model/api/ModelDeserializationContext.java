/**
 *
 */
package xapi.model.api;

import xapi.model.service.ModelService;

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
  private boolean keyOnly;
  private boolean subModel;

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

  public boolean isKeyOnly() {
    return keyOnly;
  }

  /**
   * @param clientToServer -> set clientToServer
   */
  public void setClientToServer(final boolean clientToServer) {
    this.clientToServer = clientToServer;
  }

  public void setKeyOnly(final boolean keyOnly) {
    this.keyOnly = keyOnly;
  }

  public ModelDeserializationContext createChildContext(final Class<? extends Model> propertyType) {
    final ModelService svc = getService();
    final ModelDeserializationContext ctx = new ModelDeserializationContext(svc.create(propertyType), svc, getManifest());
    ctx.subModel = true;
    return ctx;
  }

  public boolean isSubModel() {
    return subModel;
  }
}
