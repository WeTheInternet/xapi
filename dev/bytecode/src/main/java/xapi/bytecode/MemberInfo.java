package xapi.bytecode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import xapi.bytecode.annotation.Annotation;
import xapi.bytecode.annotation.AnnotationsAttribute;
import xapi.bytecode.attributes.AttributeInfo;

public abstract class MemberInfo implements Annotated {

  private static WeakHashMap<MemberInfo, Map<String, Annotation>> cache = 
      new WeakHashMap<MemberInfo, Map<String, Annotation>>();
  
  ArrayList<AttributeInfo> attribute; // may be null
  

  /**
   * Returns all the attributes.  The returned <code>List</code> object
   * is shared with this object.  If you add a new attribute to the list,
   * the attribute is also added to the method represented by this
   * object.  If you remove an attribute from the list, it is also removed
   * from the method.
   *
   * @return a list of <code>AttributeInfo</code> objects.
   * @see AttributeInfo
   */
  public List<AttributeInfo> getAttributes() {
      if (attribute == null)
          attribute = new ArrayList<AttributeInfo>();

      return attribute;
  }

  /**
   * Returns the attribute with the specified name. If it is not found, this
   * method returns null.
   *
   * @param name attribute name
   * @return an <code>AttributeInfo</code> object or null.
   * @see #getAttributes()
   */
  public AttributeInfo getAttribute(String name) {
      return AttributeInfo.lookup(attribute, name);
  }
  
  @Override
  public Annotation[] getAnnotations() {
    if (!cache.containsKey(this)) {
      loadAnnotations();
    }
    return cache.get(this).values().toArray(new Annotation[0]);
  }

  public Annotation getAnnotation(String annoName) {
    Map<String, Annotation> map = cache.get(this);
    if (map == null) {
      map = loadAnnotations();
    } 
    return map.get(annoName);
  }
  
  public boolean hasAnnotation(String annoName) {
    return cache.containsKey(this) && cache.get(this).containsKey(annoName);
  }

  private Map<String, Annotation> loadAnnotations() {
    Map<String, Annotation> annos = new LinkedHashMap<String, Annotation>();
    cache.put(this, annos);
    AttributeInfo vis = getAttribute(AnnotationsAttribute.visibleTag);
    if (vis != null) {
      for (Annotation anno : ((AnnotationsAttribute)vis).getAnnotations()) {
        annos.put(anno.getTypeName(), anno);
      }
    }
    AttributeInfo invis = getAttribute(AnnotationsAttribute.invisibleTag);
    if (invis != null) {
      for (Annotation anno : ((AnnotationsAttribute)invis).getAnnotations()) {
        annos.put(anno.getTypeName(), anno);
      }
    }
    return annos;
  }
  
  public abstract String getSignature();
  
  @Override
  public boolean equals(Object obj) {
    return obj instanceof MemberInfo && getSignature().equals(((MemberInfo)obj).getSignature());
  }
  
  @Override
  public int hashCode() {
    return getSignature().hashCode();
  }

}
