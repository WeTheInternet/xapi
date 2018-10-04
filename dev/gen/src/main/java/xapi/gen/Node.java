package xapi.gen;

/**
 *
 * TODO transform this into a full-featured, dynamic object graph-style collection.
 *
 * Also, create a "sparse array" using linked list node w/ indexes, so when adding new nodes,
 * you can just look forward until you go too far, then insert yourself in between.
 * Add in an optimization for adjacent nodes that are within a "step size" to merge into a "list block",
 * so searches can skip ahead quickly through a dense block of indices.
 * Using a dense array for these blocks can reduce time spent retrieving / setting within a used range of memory.
 * If the list grows beyond some threshold of block sizes, then a lookup ListBlock[] of indices can be used.
 * When looking for a node, first perform binary search of available block indices, then ask that block for the item.
 * If that block is also a large, sparse collection, repeat the procedure until you encounter a dense array block,
 * which you can directly read/write.
 *
 * To use this efficiently for String keys of enumerable characters (say, ascii / java name / alphanum),
 * treat each letter as a digit of a very large number in your sparse array.
 *
 * Whenever there are a run of characters that are either unique, store a block with size of n ^ m,
 * where n is the size of digits in your allowed charset, and m is the number of unique characters in a row.
 * All dense lists are arrays of size n (or a standard sparse array if you use a unicode charset),
 * And all blocks will be measured in the number of digits they contain.
 * When you insert a node inside of a multi-char block, rather than split the block into multiple nodes,
 * it may make sense to have it internally store a sparse list, unless a particular prefix "becomes hot",
 * and it would be efficient to split a larger run into a prefix and suffix.
 *
 * For example, java packages and members: com.foo.feature, com.foo.impl, com.bar.thing1, com.bar.thing2.
 * The prefix com. is always the same, so that would be a node of four digits, and then the next node is either foo. or bar.
 *
 * ...
 *
 *
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 1/2/16.
 */
public class Node <Self extends Node> {

  @SuppressWarnings("unchecked")
  public Self self() {
    return (Self)this;
  }
}
