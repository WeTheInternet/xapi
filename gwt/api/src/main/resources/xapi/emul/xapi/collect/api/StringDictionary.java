package xapi.collect.api;

import xapi.gwt.collect.JsStringDictionary;

import com.google.gwt.core.client.SingleJsoImpl;

@SingleJsoImpl(JsStringDictionary.class)
public interface StringDictionary <V> extends Dictionary<String,V> {

}
