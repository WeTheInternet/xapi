package xapi.lang.oracle;

import xapi.fu.Lazy;
import xapi.fu.X_Fu;
import xapi.lang.api.AstLayerType;
import xapi.source.X_Source;

import java.io.Serializable;

/**
 * Represents all known information about a given typename.
 *
 * Takes an arbitrary Extra object to wrap,
 * so implementors can inject their own state into an oracle.
 *
 * This makes it easier to index other data on top of Ast graph.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 5/1/17.
 */
public class AstInfo<Extra> implements Comparable<AstInfo<Extra>>, Serializable {

    // We are maintaining a pointer to our source oracle,
    // so you can easily get access to a given node's source scope.
    // Ideally you only have one oracle at a time, but it is not
    // insane to consider having more than one mutually exclusive
    // oracle-dependent service running at the same time
    private final AstOracle<Extra> oracle;

    // These three fields comprise the primary key of AstInfo, w.r.t. equals()/hashCode()
    private final AstInfo<Extra> enclosingType;
    private final String packageName;
    private final String simpleName;

    // Lazy computation... might save some cpu cycles,
    // and avoid any race conditions that might occur should
    // subclasses do weird things, like creating enclosing nodes in constructor
    // to pass to super (ick).
    private final Lazy<String> qualifiedName;
    private final Lazy<String> binaryName;
    private final Lazy<String> enclosedName;
    private final Lazy<String> enclosedBinaryName;

    // The only mutable fields we will mark volatile to keep code friendly
    // w.r.t. parallelism
    private volatile Extra extra;
    private volatile AstLayerType layer;


    public AstInfo(AstOracle<Extra> oracle, AstInfo<Extra> enclosingType, Extra extra, String pkg, String simpleName) {
        this.oracle = oracle;
        this.extra = extra;
        this.packageName = pkg;
        this.simpleName = simpleName;
        this.enclosingType = enclosingType;

        enclosedName = Lazy.deferred1(()->{
            if (enclosingType == null) {
                return simpleName;
            }
            return enclosingType.getEnclosedName() + "." + simpleName;
        });

        qualifiedName = Lazy.deferred1(()->{
            if (enclosingType == null) {
                if (packageName == null) {
                    return simpleName;
                }
                return X_Source.qualifiedName(packageName, simpleName);
            }
            if (packageName == null) {
                return enclosingType.getQualifiedName() + "." + simpleName;
            }
            return X_Source.qualifiedName(packageName, enclosingType.getEnclosedName() + "." + simpleName);
        });

        enclosedBinaryName = Lazy.deferred1(()->{
            if (enclosingType == null) {
                return simpleName;
            }
            return enclosingType.getEnclosedName() + "$" + simpleName;
        });

        binaryName = Lazy.deferred1(()->{

            if (enclosingType == null) {
                if (packageName == null) {
                    return simpleName;
                }
                return X_Source.qualifiedName(packageName, simpleName);
            }
            if (packageName == null) {
                return enclosingType.getBinaryName() + "$" + simpleName;
            }
            return X_Source.qualifiedName(packageName,
                enclosingType.getEnclosedBinaryName() + "$" + simpleName);

        });
    }

    public AstInfo(AstOracle<Extra> oracle, Extra extra, String pkg, String simpleName) {
        this(oracle, null, extra, pkg, simpleName);
    }

    // TODO Include members, type parameters and other general purpose type info


    public AstInfo<Extra> getEnclosingType() {
        return enclosingType;
    }

    public String getPackageName() {
        if (packageName != null) {
            return packageName;
        }
        if (enclosingType != null) {
            return enclosingType.getPackageName();
        }
        return null;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String getEnclosedName() {
        return enclosedName.out1();
    }

    public String getEnclosedBinaryName() {
        return enclosedBinaryName.out1();
    }

    public String getQualifiedName() {
        return qualifiedName.out1();
    }

    public String getBinaryName() {
        return binaryName.out1();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof AstInfo))
            return false;

        final AstInfo astInfo = (AstInfo) o;

        if (X_Fu.notEqual(enclosingType, astInfo.enclosingType))
            return false;
        if (X_Fu.notEqual(getPackageName(), astInfo.getPackageName())) {
            return false;
        }
        if (X_Fu.notEqual(simpleName, astInfo.getSimpleName())) {
            return false;
        }
        return X_Fu.equal(extra, astInfo.extra);
    }

    @Override
    public final int hashCode() {
        int result = enclosingType != null ? enclosingType.hashCode() : 0;
        result = 31 * result + (packageName != null ? packageName.hashCode() : 0);
        result = 31 * result + (simpleName != null ? simpleName.hashCode() : 0);
        return result;
    }

    @Override
    public final int compareTo(AstInfo<Extra> o) {
        // always sort by packages first...
        String myPkg = getPackageName();
        String yourPkg = getPackageName();
        if (myPkg == null) {
            if (yourPkg != null) {
                // default package will sort to the front
                return -1;
            }
        } else if (yourPkg == null) {
            return 1;
        } else {
            int current = myPkg.compareTo(yourPkg);
            if (current != 0) {
                return current;
            }
        }
        // packages are the same.  Now check enclosing types

        // rather than null check and compare individual strings,
        // we actual prefer the alphabetical sorting of "compute enclosed name",
        // so we'll just use that... (annoying to compare com.pkg.OuterType.Alpha to com.pkg.Beta otherwise)

        final int current = getEnclosedName().compareTo(o.getEnclosedName());
        if (current != 0) {
            return current;
        }
        final Extra myExtra = getExtra();
        if (myExtra instanceof Comparable) {
            return ((Comparable)myExtra).compareTo(o.getExtra());
        }
        return 0;
    }

    public Extra getExtra() {
        return extra;
    }

    public void setExtra(Extra extra) {
        this.extra = extra;
    }

    public AstOracle<Extra> getOracle() {
        return oracle;
    }

    public AstLayerType getLayer() {
        return layer;
    }

    public void setLayer(AstLayerType layer) {
        if (this.layer == layer) {
            return;
        }
        if (this.layer != null) {
            oracle.removeFromLayer(this.layer, this);
        }
        this.layer = layer;
        if (layer != null) {
            oracle.addToLayer(layer, this);
        }
    }
}
