package xapi.ui.api.component;

import xapi.fu.Mutable;
import xapi.fu.Out1;
import xapi.fu.lazy.ResettableLazy;
import xapi.ui.api.ElementBuilder;
import xapi.ui.api.ElementInjector;

/**
 * A place to collect helper methods for conditional components.
 *
 * The primary purpose here is to reduce duplicated code in generated output,
 * not to create some meaningful API for downstream consumers
 * (though that may be added later, it likely belongs to it's own named type that we would extend here)
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 9/9/18 @ 3:17 AM.
 */
public interface ConditionalComponentMixin <El, ElBuilder extends ElementBuilder<El>> {

    ElementInjector<? super El> newInjector(El el);

    default void renderConditional(final El into, final ElBuilder was, final ResettableLazy<ElBuilder> rootIf, Out1<El> findSibling) {
        rootIf.reset();
        final ElBuilder is = rootIf.out1();
        if (was != is) {
            final ElementInjector<? super El> inj = newInjector(into);
            if (was == null) {
                // first time selecting a winner, or "recovering" from null (no else), so do an attach.
                // look for adjacent siblings who are already attached...
                El sibling = findSibling.out1();
                if (sibling == null || inj.getParent(sibling) == null) {
                    // if we don't have an attached sibling, we can just do an append
                    inj.appendChild(is.getElement());
                } else {
                    // our sibling is already in place, so insert before that sibling;
                    // this is normal if you are toggling conditional state without an else block.
                    inj.insertBefore(is.getElement(), sibling);
                }
            } else if (is == null) {
                // current state is nothing-shown; just remove current element.
                inj.removeChild(was.getElement());
            } else {
                // changing winners, swap elements
                final El old = was.getElement();
                inj.replaceChild(is.getElement(), old);
            }
        }
    }

    default void appendConditional(ElBuilder b, ResettableLazy<ElBuilder> elIf) {

        final Mutable<ElBuilder> was = new Mutable<>();
        elIf.onSet(el->was.useThenSet(previous->{
            if (previous != null) {
                // We resolve builders before elements are resolved,
                // so the fact this asynchronicity is required means
                // we are actually assembling everything as html first,
                // and then attaching extra behavior right after the element is created.
                // This is, in practice, the absolute soonest after a custom element is made
                // that you can actually read and modify the element (we use RunSoon for created callbacks).
                b.onCreated(e->
                    newInjector(e)
                        .removeChild(previous.getElement())
                );
            }
            // el is never null in onSet (Lazy will not, by default, resolve on null)
            b.append(el);

        }, el));

        elIf.onReset(()->
            was.useIfNotNull(e->
                b.onCreated(el ->
                    was.useThenSet(current->{
                        if (current == e) {
                            was.in(null);
                            newInjector(el)
                                .removeChild(current.getElement());
                        }
                    }, null)
                )
            )
        );
    }
}
