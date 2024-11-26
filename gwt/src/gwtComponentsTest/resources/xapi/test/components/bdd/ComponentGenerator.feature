Feature: ComponentGenerator.feature: Transpile xapi templates into web component factory generators.

  Note that the WebComponentFactoryGenerator is an old prototype project that we are hijacking with template support.
  As such, some shortcuts will be taken to get a complete working demo;
  the next iteration will be a production-grade ui-environment-agnostic abstraction layer
  which will wind up fulfilling the same functionality as this prototype,
  but have clear and deterministic extension points, unlike the hacked together mess we are about to test.

  Scenario: Perform extremely simple component generation
    Given compile the component:
      | package xapi.test.components.client;              |
      | import jsinterop.annotations.JsType;              |
      | import xapi.components.api.WebComponent;          |
      |                                                   |
      | @JsType                                           |
      | @WebComponent(tagName = "test-component")         |
      | public interface TestComponent                    |
      | extends IsWebComponent<elemental.dom.Element> { } |
    And save generated source of component "test-component" as "UseTheSource"
    Then confirm source "UseTheSource" matches:
      | package xapi.test.components.client;                                                                          |
      |                                                                                                               |
      |                                                                                                               |
      | import com.google.gwt.core.client.JavaScriptObject;                                                           |
      |                                                                                                               |
      | import java.util.function.Supplier;                                                                           |
      |                                                                                                               |
      | import xapi.components.api.HasElemente_d_Element_JsFunctionAccess;                                            |
      | import xapi.components.api.IsWebComponente_d_Element_JsFunctionAccess;                                        |
      | import xapi.components.api.JsoSupplier;                                                                       |
      | import xapi.components.api.WebComponentFactory;                                                               |
      | import xapi.components.impl.WebComponentBuilder;                                                              |
      | import xapi.components.impl.WebComponentSupport;                                                              |
      | import xapi.inject.X_Inject;                                                                                  |
      |                                                                                                               |
      | public final class TestComponent_WebComponentFactory implements WebComponentFactory<TestComponent> {          |
      |                                                                                                               |
      | private static native JavaScriptObject proto () /*-{                                                          |
      |   return Object.create(HTMLElement.prototype);                                                                  |
      | }-*/;                                                                                                         |
      |                                                                                                               |
      | private static WebComponentBuilder applyProperty_element (WebComponentBuilder builder) {                      |
      |   builder.addProperty(CONST_ELEMENT,                                                                            |
      |     new JsoSupplier(HasElemente_d_Element_JsFunctionAccess.get_element()),                                        |
      |     null,                                                                                                         |
      |     false, true);                                                                                                 |
      |   return builder;                                                                                               |
      | }                                                                                                             |
      |                                                                                                               |
      | private static WebComponentBuilder applyValue_element (WebComponentBuilder builder) {                         |
      | builder.addValue(CONST_ELEMENT, IsWebComponente_d_Element_JsFunctionAccess.element(null),false, true, false); |
      |   return builder;                                                                                               |
      | }                                                                                                             |
      |                                                                                                               |
      | private static Supplier<TestComponent> ctor;                                                                  |
      |                                                                                                               |
      | private static final String CONST_ELEMENT = "element";                                                        |
      |                                                                                                               |
      | public TestComponent newComponent () {                                                                        |
      |   return ctor.get();                                                                                            |
      | }                                                                                                             |
      |                                                                                                               |
      | public String querySelector () {                                                                              |
      |   return "test-component";                                                                                      |
      | }                                                                                                             |
      | static {                                                                                                      |
      |   WebComponentBuilder builder = WebComponentBuilder.create(proto());                                            |
      |   applyProperty_element(builder);                                                                               |
      |   applyValue_element(builder);                                                                                  |
      |   ctor = WebComponentSupport.register("test-component", builder.build());                                       |
      | }                                                                                                             |
      |                                                                                                               |
      | }                                                                                                           |


#  Scenario: Generate a component with a simple click handler
#    Given compile the component:
#      | package xapi.test.components.client;    |
#      |                                         |
#      | import xapi.ui.api.Ui;                  |
#      | import xapi.ui.api.UiElement;           |
#      | import xapi.util.api.RemovalHandler;    |
#      | import xapi.fu.In1;                     |
#      | import elemental.dom.Element;           |
#      | import elemental.events.Event;          |
#      |                                         |
#      | @WebComponent(tagName="test-component", |
#      | shadowDom=@ShadowDom(`                  |
#      | <box                                    |
#      | onClick=$this::onClick                  |
#      | text = "Hello $world"                   |
#      | />                                      |
#      | `))                                     |
#      | public interface TestComponent          |
#      | extends IsWebComponent<Element> {       |
#      | void onClick(Event handler);            |
#      | String getWorld();                      |
#      | TestComponent setWorld(String world);   |
#      | }                                       |
#    And save generated source of component "test-component" as "UseTheSource"
#    Then confirm source "UseTheSource" matches:
#      | package xapi.test.components.client;                                                                          |
#      |                                                                                                               |
#      |                                                                                                               |
#      | import com.google.gwt.core.client.JavaScriptObject;                                                           |
#      |                                                                                                               |
#      | import elemental.dom.Element;                                                                                 |
#      |                                                                                                               |
#      | import java.util.function.Supplier;                                                                           |
#      |                                                                                                               |
#      | import xapi.components.api.HasElemente_d_Element_JsFunctionAccess;                                            |
#      | import xapi.components.api.IsWebComponente_d_Element_JsFunctionAccess;                                        |
#      | import xapi.components.api.JsoConsumer;                                                                       |
#      | import xapi.components.api.JsoSupplier;                                                                       |
#      | import xapi.components.api.WebComponentFactory;                                                               |
#      | import xapi.components.impl.WebComponentBuilder;                                                              |
#      | import xapi.components.impl.WebComponentSupport;                                                              |
#      | import xapi.inject.X_Inject;                                                                                  |
#      | import xapi.test.components.client.TestComponent;                                                             |
#      | import xapi.ui.service.UiService;                                                                             |
#      |                                                                                                               |
#      | public final class TestComponent_WebComponentFactory implements WebComponentFactory<TestComponent> {          |
#      |                                                                                                               |
#      | private static native JavaScriptObject proto () /*-{                                                          |
#      | return Object.create(HTMLElement.prototype);                                                                  |
#      | }-*/;                                                                                                         |
#      |                                                                                                               |
#      | private static WebComponentBuilder applyProperty_element (WebComponentBuilder builder) {                      |
#      | builder.addProperty(CONST_ELEMENT,                                                                            |
#      | new JsoSupplier(HasElemente_d_Element_JsFunctionAccess.get_element()),                                        |
#      | null,                                                                                                         |
#      | false, true);                                                                                                 |
#      | return builder;                                                                                               |
#      | }                                                                                                             |
#      |                                                                                                               |
#      | private static WebComponentBuilder applyValue_element (WebComponentBuilder builder) {                         |
#      | builder.addValue(CONST_ELEMENT, IsWebComponente_d_Element_JsFunctionAccess.element(null),false, true, false); |
#      | return builder;                                                                                               |
#      | }                                                                                                             |
#      |                                                                                                               |
#      | private static WebComponentBuilder applyProperty_onClick (WebComponentBuilder builder) {                      |
#      | builder.addProperty(CONST_ONCLICK,                                                                            |
#      | null,                                                                                                         |
#      | new JsoConsumer(TestComponent_JsFunctionAccess.set_onClick()),                                                |
#      | false, true);                                                                                                 |
#      | return builder;                                                                                               |
#      | }                                                                                                             |
#      |                                                                                                               |
#      | private static WebComponentBuilder applyProperty_world (WebComponentBuilder builder) {                        |
#      | builder.addProperty(CONST_WORLD,                                                                              |
#      | new JsoSupplier(TestComponent_JsFunctionAccess.get_getWorld()),                                               |
#      | new JsoConsumer(TestComponent_JsFunctionAccess.set_setWorld()),                                               |
#      | false, true);                                                                                                 |
#      | return builder;                                                                                               |
#      | }                                                                                                             |
#      |                                                                                                               |
#      | private static Supplier<TestComponent> ctor;                                                                  |
#      |                                                                                                               |
#      | private static final String CONST_ELEMENT = "element";                                                        |
#      |                                                                                                               |
#      | private static final String CONST_ONCLICK = "onClick";                                                        |
#      |                                                                                                               |
#      | private static final String CONST_WORLD = "world";                                                            |
#      |                                                                                                               |
#      | public TestComponent newComponent () {                                                                        |
#      | return ctor.get();                                                                                            |
#      | }                                                                                                             |
#      |                                                                                                               |
#      | public String querySelector () {                                                                              |
#      | return "test-component";                                                                                      |
#      | }                                                                                                             |
#      | static {                                                                                                      |
#      | WebComponentBuilder builder = WebComponentBuilder.create(proto());                                            |
#      | applyProperty_element(builder);                                                                               |
#      | applyValue_element(builder);                                                                                  |
#      | applyProperty_onClick(builder);                                                                               |
#      | applyProperty_world(builder);                                                                                 |
#      | builder.addShadowRoot("<box\n  text = \"Hello $world\"\n  id = \"gen1\"/>"                                    |
#      | , (host, shadow) -> {                                                                                         |
#      | UiService $ui = UiService.getUiService();                                                                     |
#      | TestComponent $this = (TestComponent) $ui.getHost(shadow);                                                    |
#      | Element gen1 = shadow.querySelector("#gen1");                                                                 |
#      | gen1.addEventListener("click",                                                                                |
#      | $this::onClick                                                                                                |
#      | );                                                                                                            |
#      | return shadow;                                                                                                |
#      | });                                                                                                           |
#      | ctor = WebComponentSupport.register("test-component", builder.build());                                       |
#      | }                                                                                                             |
#      |                                                                                                               |
#      | }                                                                                                             |

  Scenario:  Create a TODO list app

    Given compile the component:
      | @Ui( javaPrefix="Test",                                                        |
      | value=`<define-tags>                                                                                                              |
      | <define-tag name="to-do"                                                                                                          |
      | dom=<div class="to-do">                                                                                                           |
      | {text} <!-- matches the <to-do text="" /> feature -->                                                                             |
      | </div>                                                                                                                            |
      | features: {                                                                                                                       |
      | text: String.class,                                                                                                               |
      | data: HasData.class,                                                                                                              |
      | onClick: ClickHandler.class,                                                                                                      |
      | isDone: Filter<This>.class,                                                                                                       |
      | }                                                                                                                                 |
      | <!-- Supply defaults for some feature -->                                                                                         |
      | isDone: ()->this.data.<Boolean>get("done"),                                                                                       |
      | onClick: e->this.data.set("done", this.data.<Boolean>get("done")),                                                                |
      | data: {done: false}                                                                                                               |
      |                                                                                                                                   |
      |                                                                                                                                   |
      |                                                                                                                                   |
      |                                                                                                                                   |
      | />                                                                                                                                |
      | <define-tag name="to-dos"                                                                                                         |
      | dom=<div class="to-dos" />                                                                                                        |
      | {text} <!-- matches the <to-do text="" /> feature -->                                                                             |
      | </div>                                                                                                                            |
      | example=<to-dos>                                                                                                                  |
      | <to-do id="polyuser-collaboration-room"                                                                                           |
      | text=\`Patent a multi-user collaboration room,                                                                                    |
      | with a 360 degree digital display around a desk.                                                                                  |
      | anyone with a synced keyboard / mouse / powerglove                                                                                |
      | can login, open windows and drag them around anywhere.                                                                            |
      | />                                                                                                                                |
      | <to-do id="createAwesomeToDoApp"                                                                                                  |
      | text=\`\`                                                                                                                         |
      | doneWhen=()->this.siblings().stream().allMatch( This::data, HasData::get, "done" )                                                |
      | data={ done : false }                                                                                                             |
      | methods={                                                                                                                         |
      | isDone=()->this.done                                                                                                              |
      | }                                                                                                                                 |
      | onClick=()->this.done = !this.done                                                                                                |
      | />                                                                                                                                |
      | </todos>                                                                                                                          |
      |                                                                                                                                   |
      |                                                                                                                                   |
      |                                                                                                                                   |
      | `) interface Component {}                                                                                                         |
      | // this line is purposely long so the editor will leave us lots of whitespace if we autoformat this file (alt+ctrl+L in intellij) |

  Scenario: Compile a ToDoApp entirely from templates...
    Given add xapi component named to-dos:
      | <define-tags                                                                       |
      | imports = [                                                                       |
      |     xapi.fu.Filter1,                                                              |
      |    "de.mocra.cy.shared.*"                                                         |
      | ]                                                                                  |
      | tags = [                                                                             |
      |    <define-tag                                                                |
      |     name="to-do"                                                                           |
      |     model={                                                                           |
      |       text:String.class,                                                                   |
      |       data:HasData.class,                                                                  |
      |       todos:ListLike.class.$generic(Todo.class),                                                                  |
      |       isDone:(Filter<Self>.class),                                                         |
      |       onClick:ClickHandler.class,                                                          |
      |     }                                                                                   |
      |     api = [                                                                                   |
      |       public default void addTodo(Todo todo) {                                                                                   |
      |         getModel().getTodos().add(todo);                                                                                   |
      |       }                                                                                   |
      |     ]                                                                                   |
      |     data = {done:false, text:""}                                                         |
      |     isDone = ()->data.<Boolean>get("done")                                               |
      |     onClick = e -> this.done = !this.done                                                |
      |                                                                                          |
      |     // matches the value of the text feature in <to-do text="What is rendered"/>         |
      |     ui= @ShadowDom <div class="to-do">{text}</div>                                                  |
      |   /define-tag>                                                                         |
      |   ,                                                                                    |
      |   <define-tag name="to-dos"                                                                                    |
      |   ui=<div class="to-dos" children = select("to-do") />                                |
      |                                                                                          |
      |   /define-tag>                                                                                    |
      |   ]                                                                                    |
      |                                                                                        |
#      |   example =                                                                    |
#      |                                                                                          |
#      |         <to-dos>                                                                   |
#      |           <to-do text = "Create awesome examples of <to-do/>s to finish" />                    |
#      |           <to-do id   = "polyuser-collaboration-room"                                          |
#      |                         text = `Patent a multi-user collaboration room,                                      |
#      |                         with a 360 degree digital display around a desk.                                     |
#      |                         anyone with a synced keyboard/mouse/powerglove                                       |
#      |                         can login, open windows, create and edit text (code),                                |
#      |                         drag ui elements around, and interact  them around anywhere.`                        |
#      |           /to-do>                                                                              |
#      |           <to-do id    = "createAwesomeToDoApp"                                                |
#      |               text     = "Finish ALLLLL the things!"                                               |
#      |               thisType = "ToDo" // Create an alias to the type of the currrent element, <to-do/>   |
#      |               isDone   = (siblings().allMatch(ToDo::isDone))                                       |
#      |               onClick  = e->{                                                                      |
#      |                   if (!$this.isDone()) {                                                               |
#      |                   xapi.log.X_Log.alert("Finish your other todos first!");                              |
#      |                   }                                                                                    |
#      |                   e.cancel(); // do not propagate to the "super" method in the definition of <to-do /> |
#      |               }                                                                                    |
#      |           /to-do>                                                                              |
#      |           <add-to-do />                                                                        |
#      |           <to-do text = "Make the add-to-do element work in a concise, declarative manner" />  |
#      |         </to-dos>                                                                            |
      | /define-tags>                                                                        |
#    And save generated gwt source file "xapi.ui.generated.BaseToDo" as "BaseToDoSource"
#    And save generated gwt source file "xapi.ui.generated.BaseToDos" as "BaseToDosSource"
#    And save generated gwt source file "xapi.ui.generated.ToDo" as "ToDoSource"
#    And save generated gwt source file "xapi.ui.generated.ToDos" as "ToDosSource"
#    And save generated gwt source file "xapi.ui.generated.ModelToDo" as "ModelToDoSource"
    Then confirm api source for "ToDos" matches:
    | package xapi.test.components;              |
    |                                            |
    | import static xapi.model.X_Model.create;   |
    |                                            |
    | import xapi.components.api;                |
    | import xapi.ui.api.component.IsComponent;  |
    |                                            |
    | public interface ToDosComponent {          |
    |                                            |

    | default String getModelType () {    |
    |   return "toDos";                          |
    | }                                          |
    |                                            |
    | default ModelToDos createModel () { |
    |   return create(ModelToDos.class);         |
    | }                                          |
    |                                            |
    |                                            |
    | default void addTodo(Todo todo) {          |
    |   getModel().getTodos().add(todo);         |
    | }                                          |
    |                                            |
    | }                                          |

    Scenario: Run classpath component generator
      Given run classpath component generator
      Then compile generated code
