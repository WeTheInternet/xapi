/**
 *
 */
package xapi.model.api;

import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.IntTo;
import xapi.fu.itr.SizedIterable;
import xapi.model.service.ModelService;
import xapi.source.lex.CharIterator;

import java.lang.reflect.Array;
import java.util.*;

import static xapi.collect.X_Collect.newList;

/**
 * A ModelQuery is a bean describing a query for a given model.
 * <p>
 * It contains a set of query parameters that describe filters and sorting
 * that is requested for a given field.
 * <p>
 * In the future, subclasses of queries can be generated from indexing annotations
 * on the model fields.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 */
public class ModelQuery<M extends Model> {

    public enum QueryParameterType {
        EQUALS, GREATER_THAN, LESS_THAN, CONTAINS
    }

    public enum SortOrder {
        ASCENDING, DESCENDING
    }

    public static final class SortOption {
        public SortOption(final String propertyName, final SortOrder order) {
            this.propertyName = propertyName;
            this.order = order;
        }

        /**
         * @return -> propertyName
         */
        public String getPropertyName() {
            return propertyName;
        }

        /**
         * @return -> order
         */
        public SortOrder getOrder() {
            return order;
        }

        private final String propertyName;
        private final SortOrder order;

        @Override
        public String toString() {
            return "{" + propertyName + "=" + order + "}";
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final SortOption that = (SortOption) o;
            return Objects.equals(getPropertyName(), that.getPropertyName()) && getOrder() == that.getOrder();
        }

        @Override
        public int hashCode() {
            return Objects.hash(getPropertyName(), getOrder());
        }
    }

    public static final class QueryParameter {
        private QueryParameterType filterType;
        private Object filterValue;
        private String parameterName;

        /**
         * @param parameterName
         * @param filterType
         * @param filterValue
         */
        public QueryParameter(final String parameterName, final QueryParameterType filterType, final Object filterValue) {
            setParameterName(parameterName);
            setFilterType(filterType);
            setFilterValue(filterValue);
        }

        /**
         * @return -> filterType
         */
        public QueryParameterType getFilterType() {
            return filterType;
        }

        /**
         * @param filterType -> set filterType
         * @return
         */
        public QueryParameter setFilterType(final QueryParameterType filterType) {
            this.filterType = filterType;
            return this;
        }

        /**
         * @return -> filterValue
         */
        public Object getFilterValue() {
            return filterValue;
        }

        /**
         * @param filterValue -> set filterValue
         * @return
         */
        public QueryParameter setFilterValue(final Object filterValue) {
            this.filterValue = filterValue;
            return this;
        }

        /**
         * @return -> parameterName
         */
        public String getParameterName() {
            return parameterName;
        }

        /**
         * @param parameterName -> set parameterName
         * @return
         */
        public QueryParameter setParameterName(final String parameterName) {
            this.parameterName = parameterName;
            return this;
        }

        @Override
        public String toString() {
            return "{" + parameterName + "." + filterType + "=" + filterValue + "}";
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final QueryParameter that = (QueryParameter) o;
            return getFilterType() == that.getFilterType() && Objects.equals(getFilterValue(), that.getFilterValue()) && Objects.equals(getParameterName(), that.getParameterName());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getFilterType(), getFilterValue(), getParameterName());
        }
    }

    private ModelKey ancestor;

    private final IntTo<QueryParameter> parameters;
    private final IntTo<SortOption> sortOptions;

    private int pageSize;

    private int limit;

    private String cursor;

    private String namespace;

    @SuppressWarnings("unchecked")
    public ModelQuery() {
        parameters = newList(QueryParameter.class);
        sortOptions = newList(SortOption.class);
        pageSize = 50;
        limit = 500;
        namespace = "";
    }

    public ModelQuery<M> addSort(final String propertyName, final SortOrder order) {
        sortOptions.add(new SortOption(propertyName, order));
        return this;
    }

    public ModelQuery<M> addSortAscending(final String propertyName) {
        return addSort(propertyName, SortOrder.ASCENDING);
    }

    public ModelQuery<M> addSortDescending(final String propertyName) {
        return addSort(propertyName, SortOrder.DESCENDING);
    }

    public ModelQuery<M> addFilter(final String parameterName, final QueryParameterType filterType, final Object filterValue) {
        parameters.add(new QueryParameter(parameterName, filterType, filterValue));
        return this;
    }

    public ModelQuery<M> addEqualsFilter(final String parameterName, final Object filterValue) {
        return addFilter(parameterName, QueryParameterType.EQUALS, filterValue);
    }

    public ModelQuery<M> addGreaterThanFilter(final String parameterName, final Object filterValue) {
        return addFilter(parameterName, QueryParameterType.GREATER_THAN, filterValue);
    }

    public ModelQuery<M> addLessThanFilter(final String parameterName, final Object filterValue) {
        return addFilter(parameterName, QueryParameterType.LESS_THAN, filterValue);
    }

    public ModelQuery<M> addContainsFilter(final String parameterName, final Object filterValue) {
        return addFilter(parameterName, QueryParameterType.CONTAINS, filterValue);
    }

    public SizedIterable<QueryParameter> getParameters() {
        return parameters.forEachItem();
    }

    public Iterable<SortOption> getSortOptions() {
        return sortOptions.forEach();
    }

    /**
     * @return -> pageSize
     */
    public int getPageSize() {
        return pageSize;
    }

    public ModelQuery<M> setPageSize(final int pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    /**
     * @return -> cursor
     */
    public String getCursor() {
        return cursor;
    }

    /**
     * @param cursor -> set cursor
     * @return
     */
    public ModelQuery<M> setCursor(final String cursor) {
        this.cursor = cursor;
        return this;
    }

    /**
     * @return -> namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * @param namespace -> set namespace
     * @return
     */
    public ModelQuery<M> setNamespace(final String namespace) {
        assert namespace != null : "Namespace cannot be null!";
        this.namespace = namespace;
        return this;
    }

    /**
     * @return -> limit
     */
    public int getLimit() {
        return limit;
    }

    /**
     * @param limit -> set limit
     * @return
     */
    public ModelQuery<M> setLimit(final int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * @return -> ancestor
     */
    public ModelKey getAncestor() {
        return ancestor;
    }

    /**
     * @param ancestor -> set ancestor
     * @return
     */
    public ModelQuery<M> setAncestor(final ModelKey ancestor) {
        this.ancestor = ancestor;
        return this;
    }

    /**
     * @return
     */
    @SuppressWarnings("rawtypes")
    public String serialize(final ModelService service, final PrimitiveSerializer primitives) {
        final StringBuilder b = new StringBuilder();
        if (ancestor == null) {
            b.append(primitives.serializeBoolean(false));
        } else {
            b.append(primitives.serializeBoolean(true));
            b.append(primitives.serializeString(service.keyToString(ancestor)));
        }
        b.append(primitives.serializeInt(pageSize));
        b.append(primitives.serializeInt(limit));
        b.append(primitives.serializeString(cursor));
        b.append(primitives.serializeString(namespace));
        b.append(primitives.serializeInt(parameters.size()));
        for (final QueryParameter param : parameters.forEach()) {
            b.append(primitives.serializeString(param.getParameterName()));
            b.append(primitives.serializeInt(param.getFilterType().ordinal()));
            final Object value = param.getFilterValue();
            if (value == null) {
                b.append(primitives.serializeInt(-1));
            } else {
                if (value.getClass().isArray()) {
                    final int length = Array.getLength(value);
                    b.append(primitives.serializeInt(0));
                    b.append(primitives.serializeInt(typeOf(value.getClass().getComponentType())));
                    b.append(primitives.serializeInt(Array.getLength(value)));
                    for (int i = 0; i < length; i++) {
                        writeFilterValue(b, primitives, Array.get(value, i));
                    }
                } else if (value instanceof Collection) {
                    final Collection all = (Collection) value;
                    b.append(primitives.serializeInt(1));
                    b.append(primitives.serializeInt(all.size()));
                    for (final Object item : all) {
                        writeFilterValue(b, primitives, item);
                    }
                } else {
                    b.append(primitives.serializeInt(2));
                    writeFilterValue(b, primitives, value);
                }
            }
        }
        b.append(primitives.serializeInt(sortOptions.size()));
        for (final SortOption sort : sortOptions.forEach()) {
            b.append(primitives.serializeString(sort.getPropertyName()));
            b.append(primitives.serializeInt(sort.getOrder().ordinal()));

        }
        return b.toString();
    }

    private static final int
            TYPE_String = 0,
            TYPE_Date = 1,
            TYPE_int = 2,
            TYPE_Integer = 3,
            TYPE_long = 4,
            TYPE_Long = 5,
            TYPE_float = 6,
            TYPE_Float = 7,
            TYPE_double = 8,
            TYPE_Double = 9,
            TYPE_boolean = 10,
            TYPE_Boolean = 11,
            TYPE_byte = 12,
            TYPE_Byte = 13,
            TYPE_char = 14,
            TYPE_Character = 15,
            TYPE_short = 16,
            TYPE_Short = 17;

    private static final ClassTo<Integer> componentsTypes = X_Collect.newClassMap(Integer.class);

    static {
        componentsTypes.put(String.class, TYPE_String);
        componentsTypes.put(Date.class, TYPE_Date);
        componentsTypes.put(int.class, TYPE_int);
        componentsTypes.put(Integer.class, TYPE_Integer);
        componentsTypes.put(long.class, TYPE_long);
        componentsTypes.put(Long.class, TYPE_Long);
        componentsTypes.put(float.class, TYPE_float);
        componentsTypes.put(Float.class, TYPE_Float);
        componentsTypes.put(double.class, TYPE_double);
        componentsTypes.put(Double.class, TYPE_Double);
        componentsTypes.put(boolean.class, TYPE_boolean);
        componentsTypes.put(Boolean.class, TYPE_Boolean);
        componentsTypes.put(byte.class, TYPE_byte);
        componentsTypes.put(Byte.class, TYPE_Byte);
        componentsTypes.put(char.class, TYPE_char);
        componentsTypes.put(Character.class, TYPE_Character);
        componentsTypes.put(short.class, TYPE_short);
        componentsTypes.put(Short.class, TYPE_Short);
    }

    private int typeOf(final Class<?> componentType) {
        return componentsTypes.get(componentType);
    }

    private void writeFilterValue(final StringBuilder b, final PrimitiveSerializer primitives, final Object value) {
        b.append(primitives.serializeInt(typeOf(value.getClass())));
        if (value instanceof String) {
            b.append(primitives.serializeString((String) value));
        } else if (value instanceof CharSequence) {
            b.append(primitives.serializeString(value.toString()));
        } else if (value instanceof Long) {
            b.append(primitives.serializeLong((Long) value));
        } else if (value instanceof Integer) {
            b.append(primitives.serializeInt((Integer) value));
        } else if (value instanceof Float) {
            b.append(primitives.serializeFloat((Float) value));
        } else if (value instanceof Double) {
            b.append(primitives.serializeDouble((Double) value));
        } else if (value instanceof Short) {
            b.append(primitives.serializeShort((Short) value));
        } else if (value instanceof Character) {
            b.append(primitives.serializeChar((Character) value));
        } else if (value instanceof Byte) {
            b.append(primitives.serializeByte((Byte) value));
        } else if (value instanceof Boolean) {
            b.append(primitives.serializeBoolean((Boolean) value));
        } else if (value instanceof Date) {
            b.append(primitives.serializeLong(((Date) value).getTime()));
        } else {
            throw new UnsupportedOperationException("Unable to filter on objects of type " + value.getClass() + "; bad filter value: " + value);
        }
    }

    public static <M extends Model> ModelQuery<M> deserialize(final ModelService service, final PrimitiveSerializer primitives,
                                                              final CharIterator queryString) {
        final ModelQuery query = new ModelQuery<>();
        final boolean hasAncestor = primitives.deserializeBoolean(queryString);
        if (hasAncestor) {
            query.setAncestor(service.keyFromString(primitives.deserializeString(queryString)));
        }
        query.setPageSize(primitives.deserializeInt(queryString));
        query.setLimit(primitives.deserializeInt(queryString));
        query.setCursor(primitives.deserializeString(queryString));
        query.setNamespace(primitives.deserializeString(queryString));
        int params = primitives.deserializeInt(queryString);
        while (params-- > 0) {
            final String propertyName = primitives.deserializeString(queryString);
            final QueryParameterType filterType = QueryParameterType.values()[primitives.deserializeInt(queryString)];
            final Object filterValue = deserializeFilterValue(primitives, queryString);
            query.addFilter(propertyName, filterType, filterValue);
        }
        int sorts = primitives.deserializeInt(queryString);
        while (sorts-- > 0) {
            final String propertyName = primitives.deserializeString(queryString);
            final SortOrder sortOrder = SortOrder.values()[primitives.deserializeInt(queryString)];
            query.addSort(propertyName, sortOrder);
        }
        return query;
    }

    private static Object deserializeFilterValue(final PrimitiveSerializer primitives, final CharIterator queryString) {
        final int type = primitives.deserializeInt(queryString);
        int componentType = 0;
        switch (type) {
            case -1:
                return null;
            case 0:
                componentType = primitives.deserializeInt(queryString);
            case 1:
                final List filter = new ArrayList();
                int size = primitives.deserializeInt(queryString);
                while (size-- > 0) {
                    filter.add(readFilterValue(primitives, queryString));
                }
                if (type == 0) {
                    // Must convert to an array of the correct type!
                    return toArray(componentType, filter);
                }
                return filter;
            case 2:
                return readFilterValue(primitives, queryString);
            default:
                throw new IllegalStateException("Failure to deserialize ModelQuery; leftover text: " + queryString);
        }
    }

    /**
     * @param componentType
     * @param filter
     * @return
     */
    private static Object toArray(final int componentType, final List filter) {
        int size = filter.size();
        switch (componentType) {
            case TYPE_boolean:
                final boolean[] booleans = new boolean[size];
                while (size-- > 0) {
                    booleans[size] = (Boolean) filter.get(size);
                }
                return booleans;
            case TYPE_byte:
                final byte[] bytes = new byte[size];
                while (size-- > 0) {
                    bytes[size] = (Byte) filter.get(size);
                }
                return bytes;
            case TYPE_char:
                final char[] chars = new char[size];
                while (size-- > 0) {
                    chars[size] = (Character) filter.get(size);
                }
                return chars;
            case TYPE_short:
                final short[] shorts = new short[size];
                while (size-- > 0) {
                    shorts[size] = (Short) filter.get(size);
                }
                return shorts;
            case TYPE_int:
                final int[] ints = new int[size];
                while (size-- > 0) {
                    ints[size] = (Integer) filter.get(size);
                }
                return ints;
            case TYPE_long:
                final long[] longs = new long[size];
                while (size-- > 0) {
                    longs[size] = (Long) filter.get(size);
                }
                return longs;
            case TYPE_float:
                final float[] floats = new float[size];
                while (size-- > 0) {
                    floats[size] = (Float) filter.get(size);
                }
                return floats;
            case TYPE_double:
                final double[] doubles = new double[size];
                while (size-- > 0) {
                    doubles[size] = (Double) filter.get(size);
                }
                return doubles;
            default:
                return filter.toArray(newObjectArray(componentType, size));
        }
    }

    private static Object[] newObjectArray(final int componentType, final int size) {
        switch (componentType) {
            case TYPE_Boolean:
                return new Boolean[size];
            case TYPE_Byte:
                return new Byte[size];
            case TYPE_Character:
                return new Character[size];
            case TYPE_Short:
                return new Short[size];
            case TYPE_Integer:
                return new Integer[size];
            case TYPE_Long:
                return new Long[size];
            case TYPE_Float:
                return new Float[size];
            case TYPE_Double:
                return new Double[size];
            case TYPE_String:
                return new String[size];
            case TYPE_Date:
                return new Date[size];
            default:
                throw new UnsupportedOperationException("Unable to create array for type " + componentType);
        }
    }

    /**
     * @param primitives
     * @param queryString
     * @return
     */
    private static Object readFilterValue(final PrimitiveSerializer primitives, final CharIterator queryString) {
        return null;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ModelQuery<?> that = (ModelQuery<?>) o;
        return getPageSize() == that.getPageSize() && getLimit() == that.getLimit() &&
                Objects.equals(getAncestor(), that.getAncestor()) &&
                Objects.equals(parameters, that.parameters) &&
                Objects.equals(sortOptions, that.sortOptions) &&
                Objects.equals(getNamespace(), that.getNamespace());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAncestor(), getPageSize(), getLimit(), getNamespace());
    }

    @Override
    public String toString() {
        return "ModelQuery{" +
                "ancestor=" + ancestor +
                ", namespace='" + namespace + '\'' +
                ", cursor='" + cursor + '\'' +
                ", parameters=" + parameters.join(":") +
                ", sortOptions=" + sortOptions.join(":") +
                ", pageSize=" + pageSize +
                ", limit=" + limit +
                '}';
    }
}
