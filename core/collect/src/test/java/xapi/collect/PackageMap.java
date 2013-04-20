package xapi.collect;


/**
 * An optimized mapping structure for java packages; we are avoiding java.util
 * collections, but still need a string mapping structure for source packages.
 * Using fully qualified classnames as keys, this allows us to store a single
 * in-memory repository of complete class structure. Since we know all keys will
 * be in.package.form, we can create a relatively fast, light weight trie
 * structure, capable of producing iterators that can descend subpackages.
 * Adding support to map to methods and fields as well is simply a matter of
 * joining the fully qualified class name to the field or method name; like
 * com.foo.MyClass.@fieldName or com.foo.MyClass.#methodSig(Ljava/lang/Object;)
 * This will prevent collisions, and will be implemented in subclasses
 * elsewhere.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 */
public class PackageMap<T> {

//  @KeepClass(arrayDepth=1, debugData="DEBUG", newInstance=NewInstanceStrategy.NONE)
  public class PackageNode {
    private PackageMap<T>.PackageNode[] subnodes;
    private final PackageMap<T>.PackageNode parent;
    private String fragment;
    private T value;


    protected PackageNode(PackageNode parent) {
      this.parent = parent;
    }
    private PackageNode(String fragment) {
      this.parent = this;
      this.fragment = fragment;
    }

    @Override
    public String toString() {
      if (parent == root)
        return fragment;
      else
        return parent.toString()+"."+fragment;
    }
  }

  @SuppressWarnings("unchecked")
  private PackageMap<T>.PackageNode[] newArr(int len) {
    return new PackageMap.PackageNode[len];
//    return X_Reflect.newArray(PackageNode.class, len);
  }

  protected final PackageMap<T>.PackageNode root = this.new PackageNode("");

  public void add(String pkg, T item) {
    if (pkg == null) throw new NullPointerException();
    String[] keys = pkg.split("[.]");
    doAdd(0, keys, root, item);
  }

  @SuppressWarnings("unchecked")
  private void doAdd(int pos, String[] keys, PackageMap<T>.PackageNode node, T item) {
    if (pos < keys.length) {
      PackageMap<T>.PackageNode into;
      int insert;
      // only block if multiple threads are on the same node.
      synchro: //we don't want to recurse inside a synchronized block...
      synchronized (node)
        {
        if (node.subnodes == null) {
          // once we hit null, we know the rest of the keys will be null too
          node.subnodes = newArr(1);
          insert = 0;
        } else {
          String key = keys[pos];
          if (key.length() == 0) {
            into = node;
            break synchro; //eat empty .. or ./ keys
          }
          insert = node.subnodes.length;

          // this package has nodes, so we need to do a get-or-create
          for (int i = 0; i < node.subnodes.length; i++) {
            PackageMap<T>.PackageNode subnode = node.subnodes[i];
            if (subnode == null) {
              insert = i;
              break;
            }
            if (subnode.fragment.equals(key)) {
              // key matches, so we want to recurse,
              //but we want to release our lock on node first.
              into = subnode;
              break synchro; //so we just break out of the synchro block
            }
          }
          // no matches made, we need to extend our array and create the
          //rest of the package chain w/out checks, as we know its empty
          PackageMap<T>.PackageNode[] newnodes = new PackageMap.PackageNode[insert * 2];
          System.arraycopy(node.subnodes, 0, newnodes, 0, insert);
          node.subnodes = newnodes;
        }
        //we didn't break, so we know the rest of the package chain is empty
        PackageMap<T>.PackageNode newnode = this.new PackageNode(node);
        newnode.fragment = keys[pos];
        assert node.subnodes[insert] == null : "PackageMap key collision on "+node;
        node.subnodes[insert] = newnode;
        node = newnode;
        //now recurse through all the keys we have left
        for(;++pos<keys.length;) {
          newnode = new PackageNode(node);
          newnode.fragment = keys[pos];
          assert node.subnodes == null : "Package not empty @ "+keys[pos]+" in "+node;
          node.subnodes = newArr(1);
          node.subnodes[0] = newnode;
          node = newnode;
        }
        assert node.value == null : "Value not empty @ "+node;
        node.value = item;
        return;

      } //end synchro

      //if we didn't return, we need to recurse.
      doAdd(pos + 1, keys, into, item);

    } else {
      // no new nodes; set the item, we're done.
      node.value = item;
    }

  }

  public T get(String pkg) {
    assert pkg != null : "Do not send nulls to PackageMap!";
    PackageMap<T>.PackageNode node = root;
    for (String key : pkg.split("[.]")) {
      loop: {
        for (PackageMap<T>.PackageNode subnode : node.subnodes) {
          if (subnode.fragment.equals(key)) {
            node = subnode;
            break loop;
          }
        }
        return null;//not found
      }//end loop
    }
    return node.value;
  }

}
