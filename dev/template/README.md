# XApi Templating Library

This library is a small collection of source writers for use in generating java source.

It supports direct, declarative generation of source code using the SourceBuilder class, as well as template-based codegen, which enables one or more generator classes to be reflectively called during code generation.  

An example template:

//@repackaged(wetheinter.net.generated)//  
package wetheinter.net.template;

//@imports()//

import java.util.Date;

//@classDefinition(public class Template)//  
abstract class Template{

//@generateWith(wetheinter.net.dev.template.TestTemplate)//

  public static void main(String[] args){  
    new Template().injected(args);  
  }

//injected() //  
abstract void injected(String ... args);

//@skipline(2)//  
stuff to delete! */

}


Templates are marked up with processing instructions, which are denoted by single-line comments ending with //  
Templates are designed to, but not required to, be valid java syntax.

Templates are, by default, denoted with the extension .x, but can be changed by setting the system property "template.suffix".  Be sure to include the . in your suffix.

Processing instructions beginning with @ correspond to an enumerated set of special instructions.

@repackaged tells the generator to replace the package statement.  
@emulated tells the generator to add the emulated package to the existing one (for generating gwt super-source).  
@imports tells the generator where the import block is, in case you want to add imports after the fact.  
@classDefinition tells the generator where the main class statement is, to allow programmatic manipulation of its structure.  
@skipline tells the generator to skip a given number of lines, to enable replacing existing code with generated code.  
@generateWith tells the generator to load an instance of the given generator class.

Any processing instruction that does not begin with @ corresponds to a method in the last generator loaded with @generateWith.

In this example, //injected()// will call wetheinter.net.dev.template.TestTemplate#injected, and will supply a buffer to generate code into (as well as the BufferedReader in case the generator wants to skip lines manually). 

The templating system also accepts arbitrary payload objects in case you need to pass extra metadata to your generators.  This is primarily useful for specifying a manifest file with properties that affect what code is generated.

Usage of the main method, TemplateToJava as follows:

-template  
* Location of template to generate from classpath resources, using / as a delimiter.  

-payload  
* Fully qualified classname of payload used in the SourceBuilder object.  
  This class must be static with a zero-arg public constructor.  

-extra    
* Extra string data you wish to make available to your context's payload object (like a manifest file location).  

-output    
* Location to output generated files.  Default location is ${launch_directory}/src/main/java.  

-logLevel 
* The level of logging detail: ERROR, WARN, INFO, TRACE, DEBUG, SPAM, or ALL


The programmatic codegen utilities are a set of string buffers designed to ease java codegen with a fluent interface, and the ability to declare inner classes or methods as their own code block, so that source does not have to be generated linearly.  This is to make it easier to start generating a method, adding to it as you go, perhaps storing a reference to the method in your generator, so you can add to it based on future invocations of generator methods.

This library is beta, and will not be receiving updates, as it has been rewritten and updated into a much more powerful codegen library.

If you like it and want more features, email james@wetheinter.net, and your requests can be added to the full release.
