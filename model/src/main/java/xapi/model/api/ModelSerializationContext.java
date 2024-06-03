/**
 *
 */
package xapi.model.api;

import xapi.dev.source.CharBuffer;
import xapi.model.service.ModelService;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class ModelSerializationContext {

  private ModelService service;
  private CharBuffer buffer;
  private PrimitiveSerializer primitives;
  private final ModelManifest manifest;
  private boolean clientToServer;

  public ModelSerializationContext(final CharBuffer buffer, final ModelService service, final ModelManifest manifest) {
    this.service = service;
    this.buffer = buffer;
    primitives = service.primitiveSerializer();
    this.manifest = manifest;
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
   * @return -> buffer
   */
  public CharBuffer getBuffer() {
    return buffer;
  }

  /**
   * @param buffer -> set buffer
   */
  public void setBuffer(final CharBuffer buffer) {
    this.buffer = buffer;
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

  public String[] getPropertyNames(final Model model) {
    if (manifest != null && manifest.getModelType().isInstance(model)) {
      return manifest.getPropertyNames();
    }
    return model.getPropertyNames();
  }
}
