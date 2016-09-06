package xapi.mvn.model;

import xapi.mvn.impl.MvnCacheImpl;
import xapi.mvn.service.MvnCache;

public interface MvnDependency extends MvnCoords<MvnDependency> {


  String getType();
  String getClassifier();
  MvnCoords<?> getParentCoords();

  MvnDependency setType(String extension);
  MvnDependency setClassifier(String classifier);
  MvnDependency setParentCoords(MvnCoords parent);

  default MvnModule getParent(MvnCache cache) {
    final MvnCoords<?> coords = getParentCoords();
    if (coords == null) {
      return null;
    }
    if (coords instanceof MvnModule) {
      return (MvnModule) coords;
    }
    return cache.getModule(coords);
  }

  default String toCoords(MvnCacheImpl cache) {
    return null;
  }

}
