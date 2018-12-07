package xapi.lang.api;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 5/1/17.
 */
public enum AstLayerType {
    /**
     * A generated type for a component.
     * <p>
     * This will be the default for all types generated in the
     * component's type hierarchy.
     */
    Component,
    /**
     * A known application Model type not belonging to a component.
     * <p>
     * This denotes some external Model class you control,
     * which can be added as a child to a generated ComponentModel (see below).
     * <p>
     * We differentiate between these types of Models,
     * as our generated types always bind to a component factory,
     * whilst external types must signal to the compiler via annotation:
     * `@IsModel(forComponent=MyComponent.class)`, that they should be bound to a given component.
     */
    Model,
    /**
     * A generated model for a component;
     * differentiating these from other models
     * helps us know when to lookup a component builder
     * which happens to accept a model type,
     * so component renderers can insert a potential node
     * which will use the model owned by calling code.
     * <p>
     * Storing only the model instead of an actual component
     * allows you to construct a shell of ui around an
     * arbitrary collection of data, rather than maintaining
     * references directly to live ui nodes.
     * <p>
     * Using a model-based storage will also help
     * maintain Serializability of components,
     * as you will be less likely to close over
     * any non-serializable type.
     */
    ComponentModel,
    /**
     * A generated builder for a given component.
     * <p>
     * Each component will expose any inputs it might have,
     * like a model or a style blob or some service you provide
     * via a builder class, to allow you to configure your
     * components once, and then just stamp them out however you like.
     */
    Builder,
    /**
     * A generated generator for a given component.
     * <p>
     * These will plugin to the AstOracle,
     * to be able to provide type information about
     * a given type name (fully qualified or otherwise).
     */
    Generator
}
