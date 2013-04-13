package xapi.gwt.reflect;

import java.security.ProtectionDomain;


public abstract class ClassMap <T> {

  MemberMap supers = MemberMap.newInstance();
  MemberMap ifaces = MemberMap.newInstance();
  

  public final Class<?>[] getSuperClasses() {
    return MemberMap.getMembers(supers);
  }
  
  public final Class<?>[] getInterfaces() {
    return MemberMap.getMembers(supers);
  }
  
  public abstract T newInstance();
  public ProtectionDomain getProtectionDomain() {
    return null;
  }
  
}
