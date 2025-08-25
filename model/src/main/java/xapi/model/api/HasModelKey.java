package xapi.model.api;

/// HasModelKey:
///
/// A handy interface for "objects that expose a ModelKey", which may or may not be backed by a real data model;
/// suitable for passing ids (ModelKeys) and values (arbitrary models) to drawing/game logic code.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 09/04/2025 @ 02:12
public interface HasModelKey {
    ModelKey getKey();
}
