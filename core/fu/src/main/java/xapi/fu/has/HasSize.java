package xapi.fu.has;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/4/16.
 */
public interface HasSize {

    int size();

/*
    default ChainBuilder<Integer> toEndFrom0() {
        return toEndFromN(0);
    }

    default ChainBuilder<Integer> toEndFromN(int n) {
        ChainBuilder<Integer> chain = Chain.startChain();
        for (int s = size(); n < s ; n++ ) {
            chain.add(n);
        }
        return chain;
    }

    default ChainBuilder<Integer> to0FromEnd() {
        ChainBuilder<Integer> chain = Chain.startChain();
        for (int i = size(); i --> 0; ) {
            chain.add(i);
        }
        return chain;
    }

    default ChainBuilder<Integer> to0FromN(int end) {
        ChainBuilder<Integer> chain = Chain.startChain();
        for (int i = Math.min(size(), end); i --> 0 ; ) {
            chain.add(i);
        }
        return chain;
    }

    default ChainBuilder<Integer> toNFrom0(int end) {
        ChainBuilder<Integer> chain = Chain.startChain();
        for (int i = 0, m = size(); i < m; i++ ) {
            chain.add(i);
        }
        return chain;
    }

    default ChainBuilder<Integer> toNFromEnd(int end) {
        ChainBuilder<Integer> chain = Chain.startChain();
        for (int i = size(); i --> end ; ) {
            chain.add(i);
        }
        return chain;
    }
*/

}
