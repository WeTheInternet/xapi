package xapi.model.content;

import xapi.model.api.Model;

public interface ModelText extends Model{

  String getText();
  ModelContent setText(String text);

  double getTime();
  ModelContent setTime(double time);

}
