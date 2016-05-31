package xapi.collect.api;

import xapi.util.X_Runtime;

public final class CollectionOptions {

  private final boolean concurrent;
  private final boolean forbidsDuplicate;
  private final boolean insertionOrdered;
  private final boolean mutable;
  private final boolean sparse;

  private CollectionOptions(boolean concurrent, boolean forbidsDuplicate,
    boolean insertionOrdered, boolean mutable, boolean sparse) {
    this.concurrent = concurrent;
    this.forbidsDuplicate = forbidsDuplicate;
    this.insertionOrdered = insertionOrdered;
    this.mutable = mutable;
    this.sparse = sparse;
  }

  public boolean concurrent() {
    return concurrent;
  }

  public boolean forbidsDuplicate() {
    return forbidsDuplicate;
  }

  public boolean insertionOrdered() {
    return insertionOrdered;
  }

  public boolean mutable() {
    return mutable;
  }

  public static final class Builder {

    boolean concurrent;
    boolean insertionOrdered;
    boolean forbidsDuplicate;
    boolean mutable;
    boolean sparse;

    public Builder() {
      concurrent = X_Runtime.isMultithreaded();
    }

    public Builder concurrent(boolean concurrent) {
      this.concurrent = concurrent;
      return this;
    }

    public Builder sparse(boolean sparse) {
      this.sparse = sparse;
      return this;
    }

    public Builder forbidsDuplicate(boolean forbidsDuplicate) {
      this.forbidsDuplicate = forbidsDuplicate;
      return this;
    }

    public Builder insertionOrdered(boolean insertionOrdered) {
      this.insertionOrdered = insertionOrdered;
      return this;
    }

    public Builder mutable(boolean mutable) {
      this.mutable = mutable;
      return this;
    }

    public CollectionOptions build() {
      return new CollectionOptions(
        concurrent, forbidsDuplicate, insertionOrdered, mutable, sparse);
    }
  }

  public static Builder asConcurrent(boolean concurrent) {
    return new Builder().concurrent(concurrent);
  }

  public static Builder asImmutableList() {
    return new Builder().mutable(false).insertionOrdered(true);
  }

  public static Builder asInsertionOrdered() {
    return new Builder().insertionOrdered(true);
  }

  public static Builder asImmutableSet() {
    return new Builder().forbidsDuplicate(true);
  }

  public static Builder asMutableList() {
    return new Builder().insertionOrdered(true).mutable(true);
  }

  public static Builder asMutableSet() {
    return new Builder().forbidsDuplicate(true).mutable(true);
  }

  public static Builder asMutable(boolean mutable) {
    return new Builder().mutable(mutable);
  }

  public static Builder from(CollectionOptions opts) {
    final Builder builder = new Builder();
    if (opts.insertionOrdered()) {
      builder.insertionOrdered = true;
    }
    if (opts.concurrent) {
      builder.concurrent = true;
    }
    if (opts.forbidsDuplicate) {
      builder.forbidsDuplicate = true;
    }
    if (opts.mutable) {
      builder.mutable = true;
    }
    if (opts.sparse) {
      builder.sparse = true;
    }
    return builder;
  }

  public boolean dense() {
    return !sparse;
  }

  public boolean sparse() {
    return sparse;
  }
}
