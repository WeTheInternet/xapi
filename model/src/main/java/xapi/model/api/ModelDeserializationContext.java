/**
 *
 */
package xapi.model.api;

import xapi.model.impl.ModelSerializationHints;
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

  public ModelDeserializationContext createChildContext(final Class<? extends Model> propertyType, final String propName) {
    final ModelSerializationHints hints = new ModelSerializationHints();
    if (manifest != null) {
      hints.setKeyOnly(manifest.isKeyOnly(propName));
      hints.setClientToServer(manifest.isClientToServerEnabled(propName));
    }
    return createChildContext(propertyType, hints);
  }
  public ModelDeserializationContext createChildContext(final Class<? extends Model> propertyType, final ModelSerializationHints hints) {
    final ModelService svc = getService();
    final ModelDeserializationContext ctx;
    if (manifest == null) {
        ctx = new ModelDeserializationContext(svc.create(propertyType), svc, getManifest());
    } else {
        final ModelManifest childType = svc.findManifest(propertyType);
        ctx = new ModelDeserializationContext(svc.create(propertyType), svc, childType);
        ctx.setKeyOnly(hints.isKeyOnly());
        ctx.setClientToServer(hints.isClientToServer());
    }
    ctx.subModel = true;
    return ctx;
  }

  public boolean isSubModel() {
    return subModel;
  }

  public String[] getPropertyNames(final Model model) {
    if (manifest != null && manifest.getModelType().isInstance(model)) {
      return manifest.getPropertyNames();
    }
    return model.getPropertyNames();
  }

  public void setSubModel(final boolean subModel) {
    this.subModel = subModel;
  }
}
