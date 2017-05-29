package xapi.test.components.xapi.test.components;

import xapi.model.api.Model;

public interface ModelAsserter extends Model{

  abstract String getTemplate () ;

  abstract String setTemplate (String template) ;

  abstract String getTag () ;

  abstract String setTag (String tag) ;

}
