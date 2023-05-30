package xapi.model.test;

import xapi.annotation.model.IsModel;
import xapi.annotation.model.SerializationStrategy;
import xapi.model.api.Model;

import java.util.EnumMap;

/**
 * TestModelEnumMap:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 12/05/2023 @ 6:04 p.m.
 */
@IsModel(modelType = "hasEnumMap")
public interface TestModelEnumMap extends Model {

    EnumMap<SerializationStrategy, Integer> getItems();
    void setItems(EnumMap<SerializationStrategy, Integer> items);
}
