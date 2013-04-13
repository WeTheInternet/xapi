package xapi.source.api;

public interface HasProtection extends HasModifier{

  boolean isPublic();
  boolean isPrivate();
  boolean isProtected();
  boolean isPackageProtected();

}
