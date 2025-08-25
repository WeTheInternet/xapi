package xapi.fu.has;

/// HasDescriptionMutable:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 08/03/2025 @ 21:03
public interface HasDescriptionMutable extends HasDescription {
    @Override
    String getDescription();

    void setDescription(String description);
}
