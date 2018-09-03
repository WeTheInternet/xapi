package xapi.experimental.collect;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/30/16.
 */
public class Playground {

    private static final int scale = 2000;

    private static final BigDecimal TWO = BigDecimal.ONE.add(BigDecimal.ONE);
    private static BigDecimal root5 = sqrt(new BigDecimal("5"), scale);
    private static BigDecimal Phi = root5.add(BigDecimal.ONE).divide(TWO, scale, BigDecimal.ROUND_HALF_UP);
    private static BigDecimal neg_phi = BigDecimal.ONE.divide(Phi, scale, BigDecimal.ROUND_HALF_UP).negate();


    public static BigDecimal sqrt(BigDecimal A, final int SCALE) {
        BigDecimal x0 = BigDecimal.ZERO;
        BigDecimal x1 = new BigDecimal(Math.sqrt(A.doubleValue()));
        while (!x0.equals(x1)) {
            x0 = x1;
            x1 = A.divide(x0, SCALE, BigDecimal.ROUND_HALF_UP);
            x1 = x1.add(x0);
            x1 = x1.divide(TWO, SCALE, BigDecimal.ROUND_HALF_UP);
        }
        return x1;
    }

    public static BigInteger fib(int n) {
        final BigDecimal p1 = Phi.pow(n);
        final BigDecimal p2 = neg_phi.pow(n);
        final BigDecimal result = p1
            .subtract(p2)
            .divide(root5, scale, BigDecimal.ROUND_HALF_EVEN);
        return result.toBigInteger();
    }
    public static void main(String ... a) {
        final BigInteger num = fib(300);
        // 141693817714056513234709965875411919657707794958199867
        System.out.println(num.equals(new BigInteger("222232244629420445529739893461909967206666939096499764990979600")));
        System.out.println(num.toString(10));
        /*

Fib(n) =  	Phin − (−Phi)−n	 =  	Phin − ( −phi)n	√5	√5

        *
        * */
    }

}
