package xapi.collect.api;

import xapi.util.X_Runtime;

public final class CollectionOptions {

  private final boolean identityKeys;
  private final boolean concurrent;
  private final boolean forbidsDuplicate;
  private final boolean insertionOrdered;
  private final boolean keyOrdered;
  private final boolean mutable;
  private final boolean sparse;
  private final CollectionOptions multiplex;

  private CollectionOptions(
      boolean concurrent, boolean forbidsDuplicate,
      boolean insertionOrdered, boolean keyOrdered,
      boolean mutable, boolean sparse,
      boolean identityKeys, CollectionOptions multiplex
  ) {
    this.concurrent = concurrent;
    this.forbidsDuplicate = forbidsDuplicate;
    this.insertionOrdered = insertionOrdered;
    this.mutable = mutable;
    this.sparse = sparse;
    this.keyOrdered = keyOrdered;
    this.identityKeys = identityKeys;
    this.multiplex = multiplex;
  }

  public boolean concurrent() {
    return concurrent;
  }

  public boolean forbidsDuplicate() {
    return forbidsDuplicate;
  }

  public boolean identityKeys() {
    return identityKeys;
  }

  public boolean insertionOrdered() {
    return insertionOrdered;
  }

  public boolean keyOrdered() {
    return keyOrdered;
  }

  public boolean mutable() {
    return mutable;
  }

  public CollectionOptions multiplex() {
    return multiplex;
  }
  public static final class Builder {

    private boolean concurrent;
    private boolean insertionOrdered;
    private boolean forbidsDuplicate;
    private boolean mutable;
    private boolean sparse;
    private boolean keyOrdered;
    private boolean identityKeys;
    private CollectionOptions multiplex;

    public Builder() {
      concurrent = X_Runtime.isMultithreaded();
      mutable = true; // default to mutable.  Most collections should start mutable,
      // and only become immutable once they are fully initialized.
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

    public Builder keyOrdered(boolean keyOrdered) {
      this.keyOrdered = keyOrdered;
      return this;
    }

    public Builder mutable(boolean mutable) {
      this.mutable = mutable;
      return this;
    }

    public Builder identityKeys(boolean identityKeys) {
      this.identityKeys = identityKeys;
      return this;
    }

    public CollectionOptions build() {
      return new CollectionOptions(
        concurrent, forbidsDuplicate, insertionOrdered, keyOrdered, mutable, sparse, identityKeys, multiplex);
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

  public static Builder asKeyOrdered() {
    return new Builder().keyOrdered(true);
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
      builder.keyOrdered = false;
      builder.insertionOrdered = true;
    }
    if (opts.keyOrdered()) {
      builder.keyOrdered = true;
      builder.insertionOrdered = false;
    }
    // TODO: consider allowing keyOrdered + insertionOrdered to both be true,
    // if opts.objectIdentity() is specified as well, to produce a keyOrdered multimap,
    // with insertion-ordered keys.
    if (opts.concurrent()) {
      builder.concurrent = true;
    }
    if (opts.forbidsDuplicate()) {
      builder.forbidsDuplicate = true;
    }
    if (opts.mutable()) {
      builder.mutable = true;
    }
    if (opts.sparse()) {
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
