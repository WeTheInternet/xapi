package xapi.annotation.cost;

/**
 * The cost associated with executing a given expression.
 *
 * TODO: wire up `@Cost("<cost insert=log(n) search=T*log(n) etc= ... />")` annotations.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 10/05/19 @ 1:21 AM.
 */
public @interface Cost {

    /**
     * Either: A string which is a single expression, describing the cost of a method,
     * or a xapi `<cost/>` element describing a set of "named costs",
     * using named attribute children, such as `attName=cost(expr(n, T))`.
     *
     * The actual names are relevant to whatever tool is processing this Cost annotation.
     *
     * For our default implementation, we will treat all unqualified identity tokens (`n`, `T`, `String`, etc)
     * with attempted lookups and ast-expression replacements from the generator context,
     * as well as the type signature of the class or method or parameter that this annotation is applied to.
     *
     * The result should be a cacheable/serializable representation of a cost,
     * which can be analyzed by caller methods to compute their own results.
     *
     * An example:
     * ```
     * @Cost("<cost time=log(x.length) space=1 />")
     * int binarySearch(int[] x, int find) {
     *     return doBinarySearch(x);
     * }
     *
     * @Cost(`<cost
     *  n=x.length
     *  m=findAll.length
     *  c=hasEachElement(x, findAll, binarySearch(x, findAll[m]))
     *
     *  time=m*c
     *  // resolves to~ timeBest=timeWorst=timeAverage=m*log(n)
     *
     *  space=c
     *  // resolves to~ spaceBest=spaceWorst=spaceAverage=1
     *
     * /cost>`)
     * boolean containsAll(int[] x, int[] findAll) {
     *     return hasEachElement(x, findAll, this::binarySearch);
     * }
     *
     * // hasEachElement does the real work.
     * @Cost(`<cost
     *  n=x.length
     *  m=findAll.length
     *
     *  time=m*matcher(x, findAll[m]) // matcher here matches this method's parameter.  findAll[m] means "any one of findAll"
     *  // when a method reference is sent, we can figure out that containsAll will be sending binarySearch.
     *  // thus, we can resolve containsAll with m*log(n), through:
     *  // m * matcher = binarySearch(x, 1) = m*log(x.length) = m*log(n)
     *
     *  space=1 // for this example, we are doing a naive search that does not dedup findAll candidates.
     *
     * /cost>`)
     * private <T> boolean hasEachElement(T[] x, T[] findAll, In2Out1<T[], T, boolean> matcher) {
     *     for (T f : findAll) {
     *         if (!matcher.io(x, f)) {
     *             return false;
     *         }
     *
     *         // if we say, used an efficient set to de-dup findAll, we might want to report additional space and time.
     *         // this could be represented by adding `In1Out1<T, boolean> filter` parameter, and a higher resolved cost elsewhere.
     *         // if this filter was only applied based on a "magic constant" threshold of findAll.length before it pays to dedup,
     *         // then we could have `@Cost(<cost m=findAll.length space=m<42?log(m):1 ... />)`,
     *         // where 42 would actually be something named int constant, like FILTER_THRESHOLD=42, hopefully set by benchmark testing
     *         //
     *         // anyway, the time complexity would also then be conditional: time=m < 42 ? log(n) * m : log(n) * $homogenity(findAll) + m * log(m)
     *         // where $homogenity is a system expression which can try to determine a 0-1 percent likelihood of any element in findAll being a duplicate.
     *         // when normalizing performance reports, try to present both real benchmarked values, as well as big-O expression syntax.
     *     }
     *     return true;
     *
     * }
     *
     *
     * ```
     *
     * Rather than expect a human to get and keep these correct / accurate, these value annotations should
     * likely be driven by machine generation / static analysis of method body to generate uptodate costs.
     *
     * Not sayin' a human can't get it right the first time,
     * but for 100 humans to keep every single expression always correct 100% of the time...
     * would be very wishful thinking, indeed.
     *
     * @return a single java expression, or a single xapi element, with each attribute acting as a named cost type.
     */
    String value();

}
