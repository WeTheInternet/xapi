package xapi.model.content;

import xapi.model.api.Model;

public interface ModelRating extends HasAuthor, Model {

  double getRating();
  ModelRating setRating(double rating);

}
