package xapi.ui.layout;

/**
 * A LayoutGenerator that has slots which accept certain types of components.
 *
 * This is close to parity w/ V0 web components, where a <content select="tag-name" />
 * is used to give the slot control of what to pull into its contents.
 *
 * For our implementation, we will allow the use of tag names, or of fully qualified
 * type names, and we will compute assignability to decide where to slot elements.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 6/11/17.
 */
public interface HasTypedSlots <E> {

    // prefer a real type arg to ? extends wildcard
    // as this allows your callsite to retain generic type information
    // TODO: consider a non-system object to represent type info?
    <G extends E> void addSlotTyped(Class<G> type, E item);

}
