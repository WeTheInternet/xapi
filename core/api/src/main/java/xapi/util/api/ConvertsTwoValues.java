package xapi.util.api;

/**
 * Created by james on 25/10/15.
 */
public interface ConvertsTwoValues <From1, From2, To> {

  To convert(From1 one, From2 two);

}
