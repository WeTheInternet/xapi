# XApi Web Component Support

This module is designed to simplify the creation of web component custom elements both manually, via [WebComponentBuilder](src/main/java/xapi/components/impl/WebComponentBuilder.java), and automatically, via [WebComponentFactory](src/main/java/xapi/components/api/WebComponentFactory.java).

### Before you start coding

If you have not already done so, you should [brush up on custom element registration](http://www.html5rocks.com/en/tutorials/webcomponents/customelements/), and pick out a polyfill like [x-tags](http://www.x-tags.org/download) or [webcomponents.js](https://github.com/WebComponents/webcomponentsjs).  Webcomponents.js is used by [Polymer](https://www.polymer-project.org/1.0/) from Google, but will need some additional polyfills, like [dom-token-list](https://github.com/pennyfx/dom-token-list) if you want IE9 support.  X-Tags includes polyfills all the way back to IE9, however, it was created by Mozilla as the underpinning for their [Brick](https://mozbrick.github.io/blog/) component library, and Brick is now [moving away from X-tags in favor of webcomponents.js](https://mozbrick.github.io/blog/#on-x-tag).

To aid in registering polyfills at runtime, XApi includes a class, [WebComponentSupport](src/main/java/xapi/components/impl/WebComponentSupport.java) for injecting your polyfill of choice with a callback to be invoked after the given polyfill has loaded.  Due to it's wider support for legacy browsers, the default url used is `GWT.getHostPageBaseURL() + "x-tag-components.min.js"`, however it is up to you to ensure that this file exists (or to subclass WebComponentSupport to use a different polyfill of your choice).  You will not need to bother with this if you manually include the polyfill script in your host page.

### Manual Web Component Registration

Once you are familiar with custom element registration, you might be tempted to build them imperatively as you would in javascript.  This is what [WebComponentBuilder](src/main/java/xapi/components/impl/WebComponentBuilder.java) is for.  You can use this class to manually assemble your web component prototype just like you would in javascript.

    // Create a builder that will assemble callbacks and functions
    WebComponentBuilder builder = WebComponentBuilder.create(); 

    // Add a callback for when component is attached
    builder.attachedCallback(e-> e.setClassName("..."));
    // Or detached
    builder.detachedCallback(e-> e.setClassName("..."));
    // Or created
    builder.detachedCallback(e-> e.setClassName("..."));
    // Add a callback for when an attribute changes
    builder.attributeChangedCallback((name, oldVal, newVal) -> {} );

    // Add a property to the component
    builder.addProperty("prop", ()-> prop, (p) -> prop = p);
    // Add a function to the component
    builder.addValue("doStuff", JsFunctionSupport.wrapRunnable(()-> {...});

    // If you wish to extend an existing tag...
    // Grab the prototype you want to extend
    JavaScriptObject myProto = returnsHTMLElementPrototype();
    // Use that prototype object in your builder
    WebComponentBuilder builder = WebComponentBuilder.create(myProto);
    // And specify the tagname to extend
    builder.setExtends("textarea");
    // This component will look like <textarea is="my-tag" />
    // instead of <my-tag />
    
    // When you are done assembling your component, create a prototype wrapper
    JavaScriptObject proto = builder.build();
    // Then register that wrapper with the document
    Supplier<IsWebComponent> factory = WebComponentSupport.register("my-tag", proto);
    
    // Once you have registered your tag, you may instantiate it
    // ... With the factory (constructor) returned from document.register
    IsWebComponent newComponent = factory.get();
    newComponent.element().setAttribute("...","...");
    // ... Via Document.createElement
    Element e = getDocument().createElement("my-tag");
    // ... or Via innerHTML
    myElement.setInnerHTML("<my-tag some-attr='value' />");
    
Now! ...this imperative technique works, but it has a few drawbacks.

First and foremost, that's a lot of code.  
And a lot of boilerplate.  
It's hard to read.  
It's not typesafe.  
It's easy to make a typo.

This approach should only be considered if you intend to expose your web components to raw javascript/HTML code; I.E., you will not be creating and using the web components within your Gwt application.

A better approach is to declaratively compose web components from a `@JsType` interface.

### Automatic Web Component Registration

Why write out a bunch of error-prone imperative commands when you can just declaratively describe how you want your web component to behave?

This is what [WebComponentFactory](src/main/java/xapi/components/api/WebComponentFactory.java) is for.  This is a wrapper interface which takes a generic parameter of an IsWebComponent type.  When you instantiate this factory via GWT.create(), the code generator [WebComponentFactoryGenerator](src/main/java/xapi/dev/components/WebComponentFactoryGenerator.java) will write out all those nasty WebComponentBuilder commands for you!

A code example can make this much clearer:

    @JsType
    @WebComponent(tagName="my-component")
    public interface MyComponent extends IsWebComponent<Element> {
      
      WebComponentFactory<MyComponent> NEW_MY_COMPONENT =  
        GWT.create(MyComponent.class);
        
      default void doStuff() {
        element().setInnerHTML("I did stuff");
      }
    }
    
    MyComponent component = MyComponent.NEW_MY_COMPONENT.newComponent();
    component.doStuff();
    
    
In this very simple example, you see how simple it is to create a custom component that has a method attached.  Most importantly, this method is accessible from both Gwt code and native javascript, meaning that this component plays nicely with its neighbors.

It should also be noted that, because an interface is used, you can take advantage of multi-inheritance to create interfaces for commonly used paradigms, like adding labels, tooltips or validators to an element.

    @JsType
    public interface ComponentLabel <E extends Element> extends IsWebComponent<E> {
      
      @JsProperty
      LabelElement getLabel();
      
      @JsProperty
      void setLabel(LabelElement label);
      
      default LabelElement getOrMakeLabel() {
        // Allows other types to set a label before we call this
        LabelElement element = getLabel();
        if (element == null) {
          // Search for a <label/> inside the current HTML
          element = (LabelElement)element().querySelector("label");
          if (element == null) {
	        element = getDoc.createLabelElement();
	        // Append the label to the current component.
	        // You will likely want to insert it with more care
	        element().appendChild(element);
          }
          // Save the element for later
          setLabel(element);
        }
        return element;
      }
    }
    
    @JsType
    @WebComponent(tagName="my-labeled-component")
    public interface MyLabeledComponent extends ComponentLabel<Element>, OnWebComponentCreated<Element> {
        
      default void onCreated(Element e) {
        getOrMakeLabel().setInnerText("Hello World!");
      }
    }

So, now you see how easy it can be to add behaviors or features to multiple web components!  By using interfaces, you gain complete code reuse across your components by merely extending the interfaces you need.  Just be aware that because these are @JsType interfaces, you will not be able to use instanceof to figure out if a given element happens to have a certain feature or not.  It will be left to you to use a boolean or some bitwise int logic to mark what sorts of features your components use.

Next, lets have a look at how properties are handled.

    @JsType
    @WebComponent(tagName="my-component")
    public interface MyComponent extends IsWebComponent<Element> {
      
      WebComponentFactory<MyComponent> NEW_MY_COMPONENT =  
        GWT.create(MyComponent.class);
        
      @JsProperty
      int getValue();
      
      @JsProperty
      void setValue(int value);
      
    }
    
In this example, the component now has a property, `.value`.  This value will act just like a standard javascript object property.  You set or get it via componentInstance.value=123; which affords you nice interopability with javascript.  However, this does not play nice with HTML, as this javascript property does not map nicely to an attribute.  To tell the generator that you want to store the value in an attribute instead of a property, use `@WebComponentMethod(mapToAttribute=true)`.

#### Primitive attribute de/serialization

    @JsType
    @WebComponent(tagName="my-component")
    public interface MyComponent extends IsWebComponent<Element> {
      
      WebComponentFactory<MyComponent> NEW_MY_COMPONENT =  
        GWT.create(MyComponent.class);
        
      @JsProperty
      @WebComponentMethod(mapToAttribute=true)
      int getValue();
      
      @JsProperty
      @WebComponentMethod(mapToAttribute=true)
      void setValue(int value);
      
    }
     
Now your custom element will store .value in an attribute instead of a javascript property.  It will also define a javascript property getter/setter which defers to the attribute.  Thus, myComponent.value = 123 will actually call a function that performs myComponent.setAttribute("value", 123);

This gives you complete and standardized property definitions for Gwt, javascript and HTML.  And, even better, it allows you to use the attribute changed listener to detect when a set operation that changes the value of the property occurs.


    @JsType
    @WebComponent(tagName="my-component")
    public interface MyComponent extends IsWebComponent<Element>, OnWebComponentAttributeChanged {
      
      WebComponentFactory<MyComponent> NEW_MY_COMPONENT =  
        GWT.create(MyComponent.class);
        
      @JsProperty
      @WebComponentMethod(mapToAttribute=true)
      int getValue();
      
      @JsProperty
      @WebComponentMethod(mapToAttribute=true)
      void setValue(int value);
      
      default void onAttributeChanged(String name, String oldVal, String newVal) {
        switch(name) {
          case "value":
            // This will get called whether you set the value from the Gwt method,
            // the javascript property or javascript .setAttribute()
            int asInt = getValue();
            // do stuff with int
        }
      }
    } 
    
As you can see, this makes it very simple to expose multiple means to manipulate properties, and receive a notification when the value is changed.  You may have also noticed that the attribute changed event exposes String, while our data type is int, and that we used our getter method to coerce it back to int.

This is because HTML attributes are always stored as String, and our web component must generate type coercion code for us.

This also means that there are limitations to what sorts of data can be stored on our custom elements.

Simple primitives have simple coercion rules that behave as you would expect.  booleans will de/serialize as "true" or "false".  int types will be rounded and coerced into the expected java values.  long types are the only difficult ones.

As you may already know, long integers are emulated in javascript as a triplet of 52 bit floating points (the only number type in javascript).  A "native" long primitive is actually a javascript Object which looks like `{l:0, m:0, h:0}`, with l, m and h meaning low, medium and high.  Gwt uses a special class, LongLibBase to perform BigInteger style math on long to achieve 64 bit precision.

In our web components, this makes coercion a little more difficult; javascript won't be able to perform + or - on a javascript object.  For this reason, we have made a slight compromise; we modify the native long object to have a .valueOf() method which rounds the value into a 52 bit floating point.  So, if you do try to use the resulting object like a number, it will work correctly, though with a possible loss of precision when used on very large or small long numbers.  

This compromise gives you safe 52 bit long ints, and it allows you to pass these values around in javascript and send them safely back into Gwt, where Gwt will perform precise BigInteger style math.  It also allows you to supply a javascript floating point number to your component and have it turn it into an emulated long before passing it along to Gwt.

If you must have full 64 bits of precision, then you should expose operator methods to javascript that will use LongLibBase to perform expensive but precise operations.

Whew!

So, that covers primitives, but what about enums or java objects?

#### Enum and Object attribute de/serialization

Well, enums are pretty easy.  They pass into and from javascript as the String name of the enum.  This allows you to specify enum names in your HTML or javascript, and Gwt will convert back into the enum object whenever you use the getter.

This leaves one last problem: Java objects.  Gwt will obfuscate or prune the field names of your java object, such that you cannot simply pass a javascript object into Gwt that looks like your Java object and hope that it will work.

For this reason, we have settled on the use of magically named methods toString() and fromString().  If these are missing, then .valueOf() or .name() will be used, for automatic support of enum types.  If your java object defines these methods (either statically or at the instance level, with the caveat that instance-level .fromString() must also have a non-private zero-arg constructor), then the generated code can understand how to translate them to/from String attribute values.   

A common serialized format would be JSON.  For `@JsType` interfaces, this serialization can be done for you via native JSON.parse and JSON.stringify methods.  For plain java types, you will need to implement this serialization yourself.  There is also forthcoming support for XApi data [Model](https://github.com/WeTheInternet/xapi/tree/master/core/model).

To make this clearer, here is an example:

	@JsType @WebComponent(tagName=”my-object-picker”)
	interface MyObjectPicker extends IsWebComponent<Element> {
	  // This will look for .fromString() or .valueOf() methods to coerce the attribute String
	  @JsProperty @WebComponentMethod(mapToAttribute=true)
	  SomeObject getValue();
	  // This will use .toString() if .fromString() is present, or .name() if .valueOf() is present
	  @JsProperty @WebComponentMethod(mapToAttribute=true)
	  void setValue(SomeObject value);
	}
	class SomeObject {
	  int value;
	  int otherValue;
	  
	  String toString(){
	    // In this contrived example, we use " " as a delimiter
	    return value + " " + otherValue;
	  }
	  
	  static SomeObject fromString(String serialized) {
	    SomeObject inst = new SomeObject();
	    String[] bits = serialized.split(" ");
	    // Perform validation; throw exceptions
	    assert bits.length == 2;
	    inst.value = Integer.parseInt(bits[0]);
	    inst.otherValue = Integer.parseInt(bits[1]);
	    return inst;
	  }
	}

Note that there currently is not support for the use of a static toString() method.  Thus, if you do not want your serialization format to interfere with standard descriptive .toString(), then you should consider using name() and valueOf() instead.

If a more intuitive / automatic or developer friendly method of Object de/serialization is desired, please feel free to open an issue and describe what feature you would like to see.

#### More resources

You may wish to also check out the [presentation slides from GWT.create](https://docs.google.com/presentation/d/1Q1W02T6M8iFQ8W5YXgvHDkbUS0o2UO-JuHQWA4kuO4c/edit#slide=id.g585d80386_0299) where this feature was demo'd for the first time.

You may also wish to checkout and play with the flagship implementation, a [Gwt Compiler web component](https://github.com/WeTheInternet/gwtc).
 
For any other inquiries, you may reach the author at James -AT- WeTheInter -DOT- net.