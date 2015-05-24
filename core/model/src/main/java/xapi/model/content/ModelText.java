package xapi.model.content;

import xapi.model.api.Model;

public interface ModelText extends Model{

  String getText();
  ModelText setText(String text);

  double getTime();
  ModelText setTime(double time);

}
