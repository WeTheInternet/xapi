package xapi.model.api;

/**
 * ModelDeserializationException:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 28/12/2022 @ 11:55 p.m.
 */
public class ModelDeserializationException extends RuntimeException {

    private final String uuid;
    private final String modelText;

    public ModelDeserializationException(final String expectedUuid, String modelText, final Throwable t) {
        super("Failed to deserialize model (version " + expectedUuid + "):\n" + modelText, t);
        this.uuid = expectedUuid;
        this.modelText = modelText;
    }

    public String getUuid() {
        return uuid;
    }

    public String getModelText() {
        return modelText;
    }
}
