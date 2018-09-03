package xapi.dev.source;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/16/17.
 */
@RunWith(Parameterized.class)
public class TypeBuilderTest {

    @Parameter
    public TestCase testCase;

    @Target(ElementType.TYPE_PARAMETER)
    @interface TypeId {
        int value() default 0;
    }

    static class Super <
        @TypeId(-1) SuperType,
        @TypeId(-2) SubType extends SuperType
    > {
        public <
            @TypeId(1) MethodVar extends SuperType,
            @TypeId(2) MethodSub extends SubType,
            @TypeId(3) Intersection extends List & Serializable
        > MethodVar doStuff(
            Class<? super SuperType> superTypeVar,
            Class<? extends SuperType> extendsTypeVar,
            Class<? super MethodVar> superMethodVar,
            Class<? extends MethodVar> extendsMethodVar,
            Class<SuperType> isTypeVar,
            Class<MethodVar> isMethodVar
        ) {
            return null;
        }

        SuperType getSuper() { return null; }
        SubType getSub() { return null; }
    }

    static class Concrete<
        @TypeId(-3) C,
        @TypeId(-4) D extends ArrayList<C>>
        extends Super <List<C>, D> {
        @Override
        public <
            @TypeId(4) MethodVar extends List<C>,
            @TypeId(5) MethodSub extends D,
            @TypeId(6) Intersection extends List & Serializable> MethodVar doStuff(
            Class<? super List<C>> superTypeVar,
            Class<? extends List<C>> extendsTypeVar,
            Class<? super MethodVar> superMethodVar,
            Class<? extends MethodVar> extendsMethodVar,
            Class<List<C>> isTypeVar,
            Class<MethodVar> isMethodVar
        ) {
            return null;
        }
    }

    public static class TestCase {
        final Super<?, ?> superCase;

        public TestCase(Super<?, ?> superCase) {
            this.superCase = superCase;
        }
    }

    @Parameters
    public static
        // terrible... just terrible... but, we will, in fact, pass this type into a test case
    <@TypeId(7) A extends Number, @TypeId(8) B extends A, @TypeId(9) C extends ArrayList<A>>
    Collection<TestCase> getCases() {
        Super<AbstractList<? super Integer>, ArrayList<? super Number>> case1 = new Super<>();
        Super<AbstractList<? super Integer>, ArrayList<Number>> case2 = new Super<>();
        Super<AbstractList<? extends Integer>, ArrayList<Integer>> case3 = new Super<>();
        Super<AbstractList<? extends Number>, ArrayList<Integer>> case4 = new Super<>();
        Concrete<? extends Number, ArrayList<? extends Number>> case5 = new Concrete();
        Concrete<? super Integer, ArrayList<Number>> case6 = new Concrete<>();
        // why the hell would someone ever do this?

        return Arrays.asList(
            new TestCase(case1),
            new TestCase(case2),
            new TestCase(case3),
            new TestCase(case4),
            new TestCase(case5),
            new TestCase(case6),
            new TestCase(
                new Super<AbstractList<? extends Number>, ArrayList<? extends Number>>()
            ),
            new TestCase(
                new Super<List<? super Integer>, ArrayList<? super Number>>()
            ),
            new TestCase(
                new Super<A, A>()
            ),
            new TestCase(
                new Super<A, B>()
            ),
            new TestCase(
                new Super<List<A>, C>()
            ),
            new TestCase(
                new Concrete<A, ArrayList<A>>()
            ),
            new TestCase(
                new Concrete<A, C>()
            )

        );
    }

    @Test
    public void testJavaSemantics() throws NoSuchMethodException {
//        Super<AbstractList<? extends Number>, ArrayList<? extends Number>> wildcard0 = new Super<>();
//
//        Concrete<? super Integer, ArrayList<Number>> case4 = new Concrete<>();
//
//        final Method method = wildcard0.getClass().getMethod("doStuff",
//            Class.class, Class.class,
//            Class.class, Class.class,
//            Class.class, Class.class
//        );
//        final Type genericReturn = method.getGenericReturnType();
//        final Type[] paramTypes = method.getGenericParameterTypes();
//        final TypeVariable<Method>[] typeParams = method.getTypeParameters();
//        assertEquals("", 6, paramTypes.length);
//        TypeVariable<Method> g = (TypeVariable<Method>) genericReturn;
//        Method m = g.getGenericDeclaration();
//        // test equality methods on both; not relevant in JRE, but emulated equals is still important;
//        // if an end user puts us in a map, that map will depend on standard object semantics
//        assertEquals(m, method);
//        assertEquals(method, m);
//        assertEquals(method.hashCode(), m.hashCode());
//
//        final TypeVariable<Method>[] params = g.getGenericDeclaration().getTypeParameters();
//        final TypeVariable<Method> variable = params[0];
//        final Type[] bounds = variable.getBounds();
//        final Type[] annotatedBounds = variable.getBounds();
//        final String name = variable.getName();
//        final String toString = variable.toString();
//        assertTrue(genericReturn instanceof TypeVariable);

    }
}
