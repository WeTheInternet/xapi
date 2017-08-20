package xapi.ui.api.component;

import org.junit.Test;
import xapi.fu.Out1;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/20/17.
 */
public class ComponentBuilderTest {

    class StringComponent extends AbstractComponent<CharSequence, String, StringComponent> {

        public StringComponent(String element) {
            super(element);
        }

        public StringComponent(
            ComponentOptions<CharSequence, String, StringComponent> opts,
            ComponentConstructor<CharSequence, String, StringComponent> constructor
        ) {
            super(opts, constructor);
        }

        public StringComponent(Out1<String> element) {
            super(element);
        }
    }

    class StringComponentOptions extends ComponentOptions<CharSequence, String, StringComponent> {

    }

    class StringComponentBuilder extends AbstractComponentBuilder<CharSequence, String, StringComponentOptions, StringComponent> {

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

        final StringComponent component = builder.build();

    }

}
