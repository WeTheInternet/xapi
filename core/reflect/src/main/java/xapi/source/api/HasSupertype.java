package xapi.source.api;

/**
 * Represents objects that have super-types (classes, and enum instances would have these).
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 10/7/18 @ 1:35 AM.
 */
public interface HasSupertype {

    IsParameterizedType getSupertype();

}
