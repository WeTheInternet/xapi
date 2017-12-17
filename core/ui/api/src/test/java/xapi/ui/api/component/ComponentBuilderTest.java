package xapi.ui.api.component;

import org.junit.Test;
import xapi.fu.Out1;

import static org.junit.Assert.assertNotNull;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/20/17.
 */
public class ComponentBuilderTest {

    class StringComponent extends AbstractComponent<String, StringComponent> {

        public StringComponent(String element) {
            super(element);
        }

        public StringComponent(
            ComponentOptions<String, StringComponent> opts,
            ComponentConstructor<String, StringComponent> constructor
        ) {
            super(opts, constructor);
        }

        public StringComponent(Out1<String> element) {
            super(element);
        }
    }

    class StringComponentOptions extends ComponentOptions<String, StringComponent> {

    }

    class StringComponentBuilder extends AbstractComponentBuilder<String, StringComponentOptions, StringComponent> {

        private CharSequence text;

        @Override
        public Class<StringComponent> componentType() {
            return StringComponent.class;
        }

        @Override
        public StringComponentOptions asOptions() {
            final StringComponentOptions opts = new StringComponentOptions();
            opts.withComponent(StringComponent::new);
            return opts;
        }

        public StringComponentBuilder withSequence(CharSequence text) {
            this.text = text;
            return this;
        }

        /**
         * Explicitly require string parameter instead of char sequence,
         * for more specific type inference in method references, etc.
         */
        public final StringComponentBuilder withString(String text) {
            return withSequence(text);
        }
    }

    @Test
    public void testSimpleComponentBuilder() {
        StringComponentBuilder builder = new StringComponentBuilder();
        final ComponentConstructor<String, StringComponent> ctor = new ComponentConstructor<>(opts->opts.getExisting().getElement());
        final ScopedComponentFactory<? extends ComponentOptions, StringComponent> factory = opts->new StringComponent(opts, ctor);
        ComponentBuilder.registerFactory(StringComponent.class, factory);
        final StringComponent component = builder.build();
        assertNotNull("Null component", component);
    }

}
