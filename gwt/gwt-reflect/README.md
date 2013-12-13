# Gwt Reflection

Implements the java reflection API in GWT,  
with options for fly-weight, light-weight and heavy-weight (full) support baked in.

## Fly-Weight ##

The most optimal use case for reflection in GWT is to avoid creating any reflection Member objects at all.  
Rather than create a field, method or constructor, you may use the direct invocation methods provided in GwtReflect.  
This will cause the compiler to only generate a jsni accessor method (if needed), without pulling in any unused code.

Please note that, in order for fly-weight reflection to function, the class, string and class array literals to identifiy the member MUST resolve to constant values.  This means final fields, final local variables whose initializers resolve to final variables, or final methods whose return value is directly traceable to a final value.

    GwtReflect.invoke(List.class, "add", Object.class, myList, myObject);  // Works fine

    static final Class<?> LIST_CLASS = List.class;
    static final Class<?>[] OBJECT_CLASS = new Class<?>[]{Object.class};
    static String addMethod() { return "add"; }
    GwtReflect.invoke(LIST_CLASS, addMethod(), OBJECT_CLASS, myList, myObject); // also works

    GwtReflect.invoke(myList.getClass(), "add", Object.class, myList, myObject); // won't work; myList.getClass() cannot be resolved to a literal value

## Light-Weight ##

For anyone worried about the added code size of implementing reflection,  
the light-weight solution allows you to inject a single Constructor, Field or Method as desired.

    Object.class.getMethod("equals", Object.class); // works out of the box
    
    static final String COMPARE = "compare";
    static final Class<?>[] PARAMS = new Class<?>[]{Object.class, Object.class};
    static Class<?>[] comparator() { return Comparator.class; }
    comparator().getMethod(COMPARE, PARAMS); // Also works, provided all class and string literals can be traced down to constant values

The same rules that apply to GWT.create() for class literals have been bent to allow tracing down final values,
but in order for the selective insertion of reflection support to work, the compiler must be able to find your class and string literals.

## Heavy-Weight ##

Because you can't always control how code is calling into reflection apis,  
there is also a more heavy-weight solution, which enhances the class object with reflection data.

    GwtReflect.magicClass(Object.class);// GWT compiler enhances the class
    Class<Object> c = getFromWherever();
    c.getMethod("", ...); // All reflection methods on a class reference will work without resolving to a constant value.

This method is intended moreso for integrating libraries that use reflection,  
where you want to selectively enable all reflection data in code you do not control.  
It will create runtime objects to lookup reflection objects from JSNI dictionaries to achieve full reflection support.

Beware that this option can increase code size if used on a big classes, or many classes.

There is work being done to filter the amount of code the class enhancing process adds to your compile;  
in an effort to keep code size down, there is also an option to load Fields, Constructors and Methods without metadata,
such that they will be able to function correctly, but either elide or defer the download of additional data, like annotations.

This filtering is done using annotations, with reflection defaults set by annotating classes and packages,
and retaining or eliding specific members with constructor, field and method level annotations.

Finaly, there is an option to load all available reflection support (functionality and metadata) behind a code split.  
It is uses GWT.runAsync() to defer the code bloat out of your bootstrap load sequence;  
this feature will cause your compile size to swell, but it does allow you to examine all enhanced types in the gwt app.


## Usage ##

* Inherit com.google.gwt.reflect.GwtReflect
* Optionally enhance classes with GwtReflect.magicClass()
* Start using reflection

Note that this module depends upon net.wetheinter:gwt-method-inject and net.wetheinter:xapi-dev-source.

[gwt-method-inject](../gwt-method-inject) is used to implement the GWT.create-like feature of injecting arbitrary code for javascript compiles.  
[xapi-dev-source](../../dev/source) is a small codegen utility to ease the boilerplate and readability of code that generates code.

Also note that both gwt-method-inject and gwt-reflect must come before GWT dev on your classpath;  
pending community approval, this module will either be donated upstream to the GWT project,  
or we will build the GWT SDK with our modifications baked right in, and distribute it on maven central.

## Comments ##

Feel free to email, james [AT] wetheinter [DOT] net.
