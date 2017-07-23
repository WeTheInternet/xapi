package xapi.ui.api.component;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/8/17.
 */
public interface Slot <ParentEl, ChildEl> {

    String getSelector();

    ParentEl getElement();

    void append(ChildEl child);

    void applyName(ChildEl child);
}
