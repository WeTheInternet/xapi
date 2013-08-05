# XApi - Extremely Extensible Cross Platform API #

The core of the XApi library is a lightweight dependency injection system designed to bind java interfaces to implementation classes, either in singleton or instance scope.

The primary targets are web server and client applications built using [Google Web Toolkit](http://code.google.com/p/google-web-toolkit), though work is also being done on a multi-platform java client framework called [PlayN](http://code.google.com/p/playn).

Using this core tool, every other module is built up as a standalone service, designed to expose all functionality through interfaces which can be easily overridden.

Most of the implementation modules are not yet ready for release, but the core library has proven itself useful enough times to go public and put artifacts on maven central.

## Usage ##

### Step Zero: Get the code ###

(maven users)

    <dependency>
     <groupId>wetheinter.net</groupId>
     <artifactId>xapi-jre</artifactId>
     <version>0.4</version> 
    </dependency>
    
    <!--for your gwt modules:-->
    <dependency>
     <groupId>wetheinter.net</groupId>
     <artifactId>xapi-gwt</artifactId>
     <version>0.4</version> 
    </dependency>

(anyone else)  
git clone git@github.com:WeTheInternet/XApi.git  
Run .quickBuild.sh or mvn install -T 2C

### Step One: Create a service api ###

    interface Service {
      void doSomething();
    }

### Step Two: Create some implementations, with annotations ###

    //you can pick one basic class as a default for all platforms 
    @SingletonDefault(implFor=Service.class)
    class BasicService implements Service {
      public void doSomething() {
        //do nothing
      }
    }
    
    //then, create service overrides in any runtime platform you want
    @GwtPlatform // Limits this service to GWT; easy to define your own platform overrides
    @SingletonOverride(implFor=Service.class)
    class GwtService implements Service {
      public void doSomething() {
        GWT.create(OtherThing.class);
      }
    }

### Step Three: Use X_Inject class to get singletons or instances ###

    Service service = X_Inject.singleton(Service.class); // Only creates one
    Instance service = X_Inject.instance(Instance.class); // Roughly equivalent to GWT.create()

That's it.  
You now have your very first cross platform service interface.

There is a lot more to this project than just this injection service,  
but it is the underpinning of all cross-platform support in XApi.


## Modules ##

This framework is highly modular, and is built to make every feature as standalone as possible.

At the [core of the project](/core), there is are a number of abstract modules defining the API for one or more services.

Each core module will usually have [a gwt module](/gwt) and [a jre module](/jre) which inherits the core module,
and then fills in only the platform-specific implementations needed to fulfil a given service interface. 

There is also [a dev tools module](/dev) which contains a number of very useful java development tools.

Finally, [the maven module](/maven) has a plugin which is used to apply the tools in /dev to our build process.

Some tools worth noting are [the bytecode reader](/dev/bytecode) (derived from javassist),   
[the codegen api](/dev/source) (a fluent tool for generate java source code),  
[GWT magic method injection](/gwt/gwt-method-inject) and [Reflection API for GWT](/gwt/gwt-reflect),  
[the injection api](/core/inject), with [GWT implementation](/gwt/inject) and [JRE implementation](/jre/inject),  
[the streaming unix shell](/dev/shell), which supports piping commands and logs in and out of sh,  
and [the multithreaded classpath scanner](/dev/scanner).


## Roadmap

### Partial Support ###

* Collections Module
 * JSNI for GWT, collections for jvm
 * Friendlier api than java.util
 * All collections are injectable, for easy mocking and debugging. 

* Concurrency Module
 * Timer for GWT, Thread for java
 * Basic concurrency service already in production
 * Advanced concurrency service aware of GUI threading issues in the works

* Data Model Module
 * Basic support for generating client and server models from annotated interfaces
 * Model interfaces can be annotated to restrict certain fields flowing to/from the server
 * Built in platform-agnostic persistence and caching is in the works
 * Autogenerated, cross-platform editor guis for data models is also a work in progress.

* IO Module
 * Cross platform url fetching
 * Basic get and put already supported

* PlayN Bindings
 * Easily inject different types for different platforms
 * Leverages more generic services to create a rich user experience
 * This module was the original prototype of XApi, and is in a private repo until cleaned up for release.

* GWT-Appengine Emulation
 * Emulated datastore Entity and Key on the client
 * Can decode and generate keys from any Appengine instance
 * Work in progress to implement that asynchronous datastore api by piping commands to the server
 * NSFW (in a private repo; message if interested)

* [collIDE](http://collide.googlecode.com), as [forked by We The Internet](https://github.com/WeTheInternet/collide)
 * A real-time collaborative web IDE allowing multiple users to edit shared source files in the browser.
 * Built, open sourced and abandoned by Google.
 * Our fork is updated to include a GWT super dev mode recompiler, allowing the app to edit, recompile and hotswap its own code.
 * Comes in heavy-weight IDE format, and light-weight floating-toolbar format.

### Future Support ###

* Runtime Agnostic GUI
 * JavaFX and GWT share very similar declarative markup (FXML and UI Binder)
 * Use code generators to translate a single markup file and View interface into GUI classes and resource files for every client runtime.
 * Each runtime implements the same View interface, allowing a single presenter / controller to work on every platform.

* GWT Multi-Threading
 * Use web workers or shared workers to achieve true concurrency in javascript
 * "Threads" would exist as compiled gwt modules that can de/serialize objects from one GWT runtime environment to another
 * API will allow cross-platform concurrency in GWT and pure jres using a shared, common service.

* Data Binding
 * Why explicitly wire up your data type to a form, when you can just use a generator?
 * The goal is to be able to generate a view for a model interface using one annotation and zero lines of boilerplate code.
 * Automatic integration with persistence and caching layer 

* Advanced Concurrency
 * Use method annotations to describe how a concurrent process should fan out (map / fork) and fan in (reduce / join).
 * Annotated source classes are run through a generator to implement common concurrency patterns without manually re-writing code.
 * Control complex processes with clear, concise code.

## Unify Your Codebase ##

Many web applications suffer higher development costs from the separation of client and server than perhaps any other hurdle to web development (except maybe the thankfully deceased IE6).  After having worked in a number of enterprise environments on cloud computing web application, it became clear that a light weight server which can offload most of the work to the client was the way to go.  Keep the state on the client, and let the server just respond to requests as quickly as it mechanically can.

When there's a code-sharing barrier, and your client can't touch stuff on the server, and you don't want to pollute your server classloader with client dependencies, you run into the inevitable "I want to use this code over there, but I can't" routine.  And that gets old fast.

So, that is why we model all our services through an interface that operates only on other interfaces.  That way, if your business logic delves into sql, concurrency or server-only code, it doesn't block the client from implementing those same interfaces that just route through http requests instead of translating copies of objects back and forth on the server.

Any functionality your server is exposing will be used in one way or another by the client.  You can try to keep them separate if you want, but your *server* is just exposing *services*, so why try to fight it?  Make your services asynchronous; the client must be async, and the server *should* be async as well.  If you have multiple threads available and can shard one big task into ten small async tasks, your server will appear to go ten times faster to the client.  Obviously not all services need to be async, but any time any client or thread or actor in your application is sending off work to some other processor, do your clock cycles a favor and avoid busy-wait like the plague!

Best of all, supporting multiple runtime platforms when everything is an injected implementation of an interface is that your code is very, very portable.  If your business logic doesn't touch the concrete datastore or cache service it depends on, you can write (or generate!) a second implementation in another environment, and reuse as much code as there is overlap in the runtimes.

The XApi is still in its developmental infancy, but it is being built with limitless future extensibility in mind.  Runtimes with full support include java apps, android, all major web browsers via gwt, appengine, vert.x (in collIDE), and standard servlet containers.  The core depends on nothing, and every module being completed is designed to be as self-contained as possible; each major service is built as a maven module to avoid leaking dependencies, and to maximize the ability for compilers and pre-processors to optimize and prune as much as possible.


