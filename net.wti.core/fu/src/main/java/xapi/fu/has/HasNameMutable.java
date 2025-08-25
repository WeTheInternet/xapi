package xapi.fu.has;

/// HasNameMutable:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/03/2025 @ 21:03
public interface HasNameMutable extends HasName {
    @Override
    String getName();

    void setName(String name);
}
