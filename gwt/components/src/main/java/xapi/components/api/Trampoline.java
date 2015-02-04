package xapi.components.api;

/**
 * This annotation is used on the methods of a web component interface,
 * in order to re-route a given method to a *STATIC* method within the codebase.
 * 
 * @author James
 *
 */
public @interface Trampoline {

	boolean elementAsFirstParam() default true;
	
	Class<?> trampolineClass();
	
	String trampolineMethod();
}
