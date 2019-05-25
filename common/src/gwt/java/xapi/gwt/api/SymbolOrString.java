package xapi.gwt.api;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import jsinterop.base.Any;
import jsinterop.base.Js;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 12/3/17.
 */
@JsType(isNative = true, name = "?", namespace = JsPackage.GLOBAL)
public interface SymbolOrString {

    @JsOverlay
    static SymbolOrString fromString(String s) {
        return Js.cast(s);
    }

    @JsOverlay
    static SymbolOrString fromSymbol(Symbol s) {
        return Js.cast(s);
    }

    @JsOverlay
    default String asString() {
        return Js.cast(this);
    }

    @JsOverlay
    default Symbol asSymbol() {
        return Js.cast(this);
    }

    @JsOverlay
    default boolean isString() {
        return Js.isTripleEqual(Js.typeof(this), "string");
    }

    @JsOverlay
    default boolean isSymbol() {
        return Js.isTripleEqual(Js.typeof(this), "symbol");
    }

    @JsOverlay
    default boolean isDefinedOn(Object on) {
        return
            Js.isTruthy(on) &&
            Jso.of(on)
            .hasOwnProperty(this);
    }

    @JsOverlay
    default Any getFrom(Object on) {
        return Jso.getProperty(on, this);
    }

}
/*

class c{
  get [Symbol.toStringTag](){return "Hi";}
  valueOf(){return 2}
}
console.log(
(new c() + 1) +
(new c() + "1")
); // prints 321

console.log(
(new c() + "1") +
(new c() + 1)
); // prints 213

console.log(
(new c() + "1")); // prints 21

console.log(
("1" + new c() )); // prints 12

console.log(
(`${new c()}` + "1")); // prints Hi1

console.log(
("1" + new c().toString())); // prints Hi1

class d {
  toString(){return "Hi";}
  valueOf(){return 2}
  [Symbol.toPrimitive](h) {
    switch(h){
        case 'number': return 42;
        case 'string': return "Bye";
        default: return "Yo";
    }
  }
}

console.log(
(+new d() + 1)
); // prints 43

console.log(
(new d() + 1)
); // prints Yo1

console.log(
(1 + new d())
); // prints 1Yo

console.log(
(1 + +new d())
); // prints 43


console.log(
(`${new d()}` + 1)
); // prints Bye1

console.log(
(new d() + "1")
); // prints Yo1

console.log(
("1"+ new d() )
); // prints 1Yo

console.log(
("1"+ new d().toString() )
); // prints 1Hi

console.log(
(new c()+ new d() )
); // prints 2Yo

class b{
  valueOf(){return "Hello";}
}
console.log(
(new b() + 1)
); // prints Hello1

console.log(
("1" + new b() )); // prints 1Hello

console.log(
(`${new b()}` + "1")); // prints Hi1

console.log(
("1" + new b().toString())); // prints Hi1

*/
