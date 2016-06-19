package xapi.dev.ui;

import xapi.dev.source.DomBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.fu.Lazy;
import xapi.fu.Out1;

import static xapi.fu.Immutable.immutable1;
import static xapi.inject.X_Inject.instance;

/**
 * Created by james on 6/17/16.
 */
public class ComponentBuffer extends PrintBuffer {

  private final Out1<GeneratedComponentMetadata> root;

  private Out1<SourceBuilder<?>> componentBinder = Lazy.deferred1(this::defaultSourceBuilder);

  private Out1<DomBuffer> domBuffer = Lazy.deferred1(this::defaultDomBuffer);

  public ComponentBuffer() {
    this(immutable1(instance(GeneratedComponentMetadata.class)), true);
  }

  public ComponentBuffer(GeneratedComponentMetadata metadata) {
    this(immutable1(metadata), true);
  }

  public ComponentBuffer(Out1<GeneratedComponentMetadata> metadata) {
    this(Lazy.deferred1(metadata), false);
  }

  public ComponentBuffer(Out1<GeneratedComponentMetadata> root, boolean immediate) {
     this.root = immediate ? immutable1(root.out1()) : Lazy.deferred1(root);
  }

  protected SourceBuilder<?> defaultSourceBuilder() {
    return root.out1().getSourceBuilder();
  }

  protected DomBuffer defaultDomBuffer() {
    return instance(DomBuffer.class);
  }

  private Out1<DomBuffer> dom = Lazy.deferred1(DomBuffer::new);

  public SourceBuilder getBinder() {
    return componentBinder.out1();
  }

  public DomBuffer getDom() {
    return domBuffer.out1();
  }


}
