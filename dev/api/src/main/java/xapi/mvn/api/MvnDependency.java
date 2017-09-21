package xapi.mvn.api;

public interface MvnDependency extends MvnCoords<MvnDependency> {

  MvnCoords<?> getParentCoords();
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

}
