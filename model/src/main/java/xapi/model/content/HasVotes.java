package xapi.model.content;

public interface HasVotes {

  ModelRating[] getUpvotes();
  HasVotes setUpvotes(ModelRating[] upvotes);

  ModelRating[] getDownvotes();
  HasVotes setDownvotes(ModelRating[] upvotes);


}
