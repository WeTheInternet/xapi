package xapi.ui.layout;

/**
 * Used for components that define a single "grab-all" slot for child components.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 6/11/17.
 */
public interface HasDefaultSlot <E> {

    void addSlotDefault(E el);

}
