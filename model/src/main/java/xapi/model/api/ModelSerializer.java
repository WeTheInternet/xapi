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

  CharBuffer modelToString(M model, ModelSerializationContext ctx);

  M modelFromString(CharIterator model, ModelDeserializationContext ctx);
}

@SuppressWarnings("rawtypes")
final class DoNothing implements ModelSerializer {

  @Override
  public CharBuffer modelToString(final Model model, final ModelSerializationContext ctx) {
    return new CharBuffer();
  }

  @Override
  public Model modelFromString(final CharIterator model, final ModelDeserializationContext ctx) {
    return null;
  }

}
