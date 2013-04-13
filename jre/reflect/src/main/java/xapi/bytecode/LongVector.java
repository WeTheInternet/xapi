package xapi.bytecode;

final class LongVector {
    static final int ASIZE = 128;
    static final int ABITS = 7;  // ASIZE = 2^ABITS
    static final int VSIZE = 8;
    private ConstInfo[][] objects;
    private int elements;

    public LongVector() {
        objects = new ConstInfo[VSIZE][];
        elements = 0;
    }

    public LongVector(int initialSize) {
        int vsize = ((initialSize >> ABITS) & ~(VSIZE - 1)) + VSIZE;
        objects = new ConstInfo[vsize][];
        elements = 0;
    }

    public int size() { return elements; }

    public int capacity() { return objects.length * ASIZE; }

    public ConstInfo elementAt(int i) {
        if (i < 0 || elements <= i)
            return null;

        return objects[i >> ABITS][i & (ASIZE - 1)];
    }

    public void addElement(ConstInfo value) {
        int nth = elements >> ABITS;
        int offset = elements & (ASIZE - 1);
        int len = objects.length;
        if (nth >= len) {
            ConstInfo[][] newObj = new ConstInfo[len + VSIZE][];
            System.arraycopy(objects, 0, newObj, 0, len);
            objects = newObj;
        }

        if (objects[nth] == null)
            objects[nth] = new ConstInfo[ASIZE];

        objects[nth][offset] = value;
        elements++;
    }
}
