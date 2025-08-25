package xapi.model.impl;

import xapi.model.api.ModelDeserializationContext;

/// ModelDeserializerHints:
///
/// A place to stuff random "hints" to pass along through ModelService spi methods.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/04/2025 @ 23:22
public class ModelSerializationHints {

    private boolean isListItem;
    private boolean isKeyOnly;
    private ModelDeserializationContext parentContext;
    private boolean clientToServer;

    public boolean isListItem() {
        return isListItem;
    }

    public void setListItem(final boolean listItem) {
        isListItem = listItem;
    }

    public boolean isKeyOnly() {
        return isKeyOnly;
    }

    public void setKeyOnly(final boolean keyOnly) {
        isKeyOnly = keyOnly;
    }

    public ModelDeserializationContext getParentContext() {
        return parentContext;
    }

    public void setParentContext(final ModelDeserializationContext parentContext) {
        this.parentContext = parentContext;
    }

    public void setClientToServer(final boolean clientToServer) {
        this.clientToServer = clientToServer;
    }

    public boolean isClientToServer() {
        return clientToServer;
    }
}
