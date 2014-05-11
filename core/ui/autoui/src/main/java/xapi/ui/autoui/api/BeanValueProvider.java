package xapi.ui.autoui.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import xapi.collect.api.StringTo;
import xapi.collect.impl.ArrayIterable;
import xapi.source.write.MappedTemplate;
import xapi.util.api.ConvertsValue;

public class BeanValueProvider {

  private static Object getValue(String name, Object object, Map<String, ConvertsValue<Object, Object>> map) {
    if (!map.containsKey(name)){
      illegalArg(name);
    }
    return map.get(name).convert(object);
  }

  protected static Object illegalArg(String name) {
    throw new IllegalArgumentException("Value for field named "+name+" not found");
  }

  private class Converter {
    protected Object valueOf(String name, Object object) {
      return BeanValueProvider.this.valueOf(name, object);
    }
  }

  private class RebasedBeanValueProvider extends BeanValueProvider {
    private final String name;
    private final Converter converter;

    public RebasedBeanValueProvider(String name, Converter converter) {
      this.name = name;
      this.converter = converter;
    }

    @Override
    public Object getValue(String name, String context, Object object) {
//      assert context.equals(this.name);
      if ("$name".equals(name) || "this.name()".equals(name) || (this.name+".name()").equals(name))
        return this.name;
      if ("$value".equals(name) || "this".equals(name) || this.name.equals(name))
        return converter.valueOf(this.name, object);
      return converter.valueOf(this.name+"."+name, object);
    }
  }
  private class RebaseAllBeanValueProvider extends BeanValueProvider {

    private final Converter converter;

    private RebaseAllBeanValueProvider(Converter converter) {
      this.converter = converter;
    }

    @Override
    public Object getValue(String name, String context, Object object) {
      if ("$name".equals(name) || "this.name()".equals(name))
        return context;
      if ("$value".equals(name) || "this".equals(name))
        return object;
      if (context.length()>0) {
        context += ".";
      }
      return converter.valueOf(context+name, object);
    }

  }

  private Map<String, ConvertsValue<Object, Object>> map = new LinkedHashMap<String, ConvertsValue<Object, Object>>();
  private String[] childKeys;

  public void addProvider(String key, final String name, ConvertsValue<Object, Object> provider) {
    map.put(key, provider);
    ConvertsValue<Object, Object> namer = new ConvertsValue<Object, Object>() {
      @Override
      public Object convert(Object from) {
        return from instanceof HasName ? ((HasName)from).getName() : name;
      }
    };
    map.put(key+".name()", namer );
    if ("this".equals(key)) {
      map.put("$name", namer);
      map.put("$value", provider);
    }
  }

  protected Object valueOf(String name, Object object) {
    return getValue(name, object, map);
  }

  protected String[] getKeys(Map<String, ConvertsValue<Object, Object>> map) {
    String[] keys = map.keySet().toArray(new String[map.size()]);
    for (int i = keys.length; i-->0;) {
      keys[i] = "${"+keys[i]+"}";
    }
    return keys;
  }

  protected String[] childKeys() {
    if (childKeys == null) {
      List<String> keys = new ArrayList<String>();
      for (Entry<String, ConvertsValue<Object, Object>> entry : map.entrySet()) {
        String key = entry.getKey();
        if (key.indexOf('.') == -1 && !"this".equals(key)) {
          keys.add(key);
        }
      }
      childKeys = keys.toArray(new String[keys.size()]);
    }
    return childKeys;
  }

  public void setChildKeys(String[] keys) {
    this.childKeys = keys;
  }

  public Object getValue(String name, String context, Object object) {
    return valueOf(name, object);
  }

  public BeanValueProvider rebase(String methodName) {
    BeanValueProvider bean = new RebasedBeanValueProvider(methodName, new Converter());
    bean.childKeys = childKeys;
    bean.map = map;
    return bean;
  }

  public BeanValueProvider rebaseAll() {
    BeanValueProvider bean = new RebaseAllBeanValueProvider(new Converter());
    bean.childKeys = childKeys;
    bean.map = map;
    return bean;
  }

  public Iterable<String> getChildKeys() {
    return new ArrayIterable<String>(childKeys());
  }

  @SuppressWarnings({
      "rawtypes", "unchecked"
  })
  public void fillMap(String name, MappedTemplate template, StringTo map, Object val) {
    if ("".equals(name)) {
      for (String key : childKeys()) {
        String k = key.startsWith("$") ? key : "${"+key+"}";
        if (template.hasKey(k)) {
          map.put(k, getValue(key, "", val));
        }
        k = "${"+key+".name()}";
        if (template.hasKey(k)) {
          map.put(k, key);
        }
      }
    } else {
      val = getValue(name, "", val);
      map.put("$value", val);
      map.put("$name", name);
      map.put("${"+name+"}", val);
      map.put("${"+name+".name()}", name);
      // We should also have a means to do deep name resolving on rebased bean items...
    }
  }

}
