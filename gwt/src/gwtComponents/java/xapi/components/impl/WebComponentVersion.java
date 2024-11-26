package xapi.components.impl;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/26/17.
 */
public enum WebComponentVersion {
  V0("createdCallback", "attachedCallback", "detachedCallback"),
  V1("constructor", "connectedCallback", "disconnectedCallback");
  private final String created, attached, detached, attributeChanged, observedAttributes, adopted;

  WebComponentVersion(String created, String attached, String detached) {
    this.created = created;
    this.attached = attached;
    this.detached = detached;
    observedAttributes = "observedAttributes";
    attributeChanged = "attributeChangedCallback";
    adopted = "adoptedCallback";
  }

  public String getCreated() {
    return created;
  }

  public String getAttached() {
    return attached;
  }

  public String getDetached() {
    return detached;
  }

  public String getAttributeChanged() {
    return attributeChanged;
  }

  public String getObservedAttributes() {
    return observedAttributes;
  }

  public String getAdopted() {
    return adopted;
  }
}
