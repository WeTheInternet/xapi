package xapi.util.api;

public interface MergesValues <K1, K2, V> {

  V merge(K1 key1, K2 key2);

}
