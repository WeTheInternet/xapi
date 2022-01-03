/**
 *
 */
package xapi.model.api;

import xapi.dev.source.CharBuffer;
import xapi.source.lex.CharIterator;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public interface ModelSerializer <M extends Model> {

  @SuppressWarnings("rawtypes")
  ModelSerializer DO_NOTHING = new DoNothing();

  CharBuffer modelToString(Class<? extends Model> modelType, M model, ModelSerializationContext ctx, boolean keyOnly);

  void writeKey(M model, CharBuffer out, ModelSerializationContext ctx);

  M modelFromString(Class<? extends Model> modelType, CharIterator model, ModelDeserializationContext ctx, boolean keyOnly);
}

@SuppressWarnings("rawtypes")
final class DoNothing implements ModelSerializer {

  @Override
  public CharBuffer modelToString(Class modelType, final Model model, final ModelSerializationContext ctx, boolean keyOnly) {
    return new CharBuffer();
  }

  @Override
  public Model modelFromString(Class modelType, final CharIterator model, final ModelDeserializationContext ctx, boolean keyOnly) {
    return null;
  }

  @Override
  public void writeKey(final Model model, final CharBuffer out, final ModelSerializationContext ctx) {
  }
}
