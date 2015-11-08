package xapi.model.test;

import xapi.collect.api.ClassTo;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.collect.api.StringTo;
import xapi.model.api.Model;

/**
 * Created by james on 26/10/15.
 */
public interface ModelWithEverything extends Model {

  boolean getBooleanPrimitive();
  ModelWithEverything setBooleanPrimitive(boolean primitive);

  Boolean getBoolean();
  ModelWithEverything setBoolean(Boolean primitive);

  ModelWithEverything[] getModelArray();
  ModelWithEverything setModelArray(ModelWithEverything[] modelArray);

  int getIntPrimitive();
  ModelWithEverything setIntPrimitive(int primitive);

  Integer getInteger();
  ModelWithEverything setInteger(Integer primitive);

  double getDoublePrimitive();
  ModelWithEverything setDoublePrimitive(double primitive);

  Double getDouble();
  ModelWithEverything setDouble(Double primitive);


  long getLongPrimitive();
  ModelWithEverything setLongPrimitive(long primitive);

  Long getLong();
  ModelWithEverything setLong(Long primitive);

  String getString();
  ModelWithEverything setString(String primitive);

  long[] getLongArray();
  ModelWithEverything setLongArray(long[] primitive);

  IntTo<String> getIntTo();
  ModelWithEverything setIntTo(IntTo<String> intTo);

  StringTo<Integer> getStringTo();
  ModelWithEverything setStringTo(StringTo<Integer> intTo);

  ClassTo<Double> getClassTo();
  ModelWithEverything setClassTo(ClassTo<Double> intTo);

  ObjectTo<String, Double> getObjectTo();
  ModelWithEverything setObjectTo(ObjectTo<String, Double> intTo);

  IntTo.Many<String> getIntToMany();
  ModelWithEverything setIntToMany(IntTo.Many<String> intTo);

  StringTo.Many<Integer> getStringToMany();
  ModelWithEverything setStringToMany(StringTo.Many<Integer> intTo);

  ClassTo.Many<Double> getClassToMany();
  ModelWithEverything setClassToMany(ClassTo.Many<Double> intTo);

  ObjectTo.Many<String, Double> getObjectToMany();
  ModelWithEverything setObjectToMany(ObjectTo.Many<String, Double> intTo);


}
