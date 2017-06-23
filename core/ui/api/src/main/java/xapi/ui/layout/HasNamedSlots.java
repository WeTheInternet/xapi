package xapi.ui.layout;

/**
 * A LayoutGenerator that has named slots, useful for parity of Web Components V1.
 *
 * These slots must appear in a <tag layout=<dom><slot name="thing" /></dom> /tag>
 *
 * Created by James X. Nelson (james @wetheinter.net) on 6/11/17.
 */
public interface HasNamedSlots <E> {

    void addSlotNamed(String name, E el);

}
