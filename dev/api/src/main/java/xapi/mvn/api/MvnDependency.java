package xapi.mvn.api;

import xapi.model.X_Model;

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

  default String toCoords() {
    StringBuilder b = new StringBuilder();
    b.append(getGroupId()).append(":")
     .append(getArtifactId()).append(":");
    String classifier = getClassifier();
    String type = getPackaging();
    if (classifier != null) {
      if (type == null) {
        b.append("jar:");
      } else {
        b.append(type).append(":");
      }
      b.append(classifier).append(":");
    } else if (type != null) {
        b.append(type).append(":");
    }
    b.append(getVersion());
    return b.toString();
  }

  static MvnDependency fromCoords(String coords) {
    MvnDependency dep = X_Model.create(MvnDependency.class);
    String[] bits = coords.split(":");
    switch (bits.length) {
      case 5:
        dep.setClassifier(bits[3]);
      case 4:
        dep.setPackaging(bits[2]);
      case 3:
        dep.setArtifactId(bits[1]);
        dep.setGroupId(bits[0]);
        dep.setVersion(bits[bits.length-1]);
        return dep;
      default:
        throw new IllegalArgumentException("Illegal coords: " + coords);
    }
  }

}
