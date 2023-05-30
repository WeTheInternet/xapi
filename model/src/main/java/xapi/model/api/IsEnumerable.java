package xapi.model.api;

/**
 * IsEnumerable:
 * <p>
 * <p>A handy interface so you can de/serialize objects by an ordinal(),
 * <p>along w/ the classname of the serialized instance.
 * <p>
 * <p>We first serialize class name then ordinal, then we will use Enum.valueOf(theClass, theOrdinal)
 * <p>when we deserialize these instances. This allows many differently typed enums to populate
 * <p>a given model field.
 * <p>
 * <pre><code>
 *     &lt;T extends Enum & IsEnumerable> T getAnyEnum();
 *
 *     // ...
 *
 *     enum MyEnum implements IsEnumerable { A, B, C }
 *     enum SomeOther implements IsEnumerable { D, E }
 *
 *     interface MyModel extends Model {
 *         IsEnumerable getSomething();
 *         void setSomething(IsEnumerable item); // can send _anything_
 *     }
 * </code></pre>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 28/12/2022 @ 2:19 a.m.
 */
public interface IsEnumerable {

    int ordinal();

}
