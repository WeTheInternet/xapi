package xapi.model.content;

public interface HasVotes {

  ModelRating[] getUpvotes();
  ModelContent setUpvotes(ModelRating[] upvotes);

  ModelRating[] getDownvotes();
  ModelContent setDownvotes(ModelRating[] upvotes);


}
