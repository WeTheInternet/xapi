//@repackaged(xapi.generated)//
package xapi.template;

//@imports()//

import java.util.Date;

//@classDefinition(public class Success)//
abstract class Success{

//@generateWith(xapi.dev.template.TestTemplate)//

  public static void main(String[] args){
    new Success().injected(args);
  }

//injected() //
abstract void injected(String ... args);

//@skipline(2)//
stuff to not compile! */

}