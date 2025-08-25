package xapi.model.api;

/// HasModel:
///
/// A handy interface for when you want to declare Model superinterfaces which have default methods that want to do
/// something like "create a child key from the owning Model", but allow other subtypes which do not implement Model.
///
/// You are encouraged, but not required, to override {@link #asModel()} with a better no-cast "return this;"
///
/// Created by James X. Nelson (James@WeTheInter.net) on 09/04/2025 @ 02:04
public interface HasModel <T extends Model> {

    @SuppressWarnings("unchecked")
    default T asModel() {
        return (T) this;
    }
}
