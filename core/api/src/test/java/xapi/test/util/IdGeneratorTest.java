package xapi.test.util;

import org.junit.Assert;
import org.junit.Test;
import xapi.fu.Lazy;
import xapi.util.api.HasId;
import xapi.util.api.IdGenerator;
import xapi.util.impl.IdGeneratorDefault;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/29/16.
 */
public class IdGeneratorTest {
    IdGenerator<RecursiveId> gen = new IdGeneratorDefault<>();
    class RecursiveId implements HasId {

        private Lazy<String> id = Lazy.deferred1(()->gen.generateId(this));

        @Override
        public String getId() {
            return id.out1();
        }
    }

    @Test
    public void testRecursion() {
        final RecursiveId ident = new RecursiveId();
        String id = ident.getId();
        Assert.assertEquals("0", id);
        Assert.assertEquals("0", ident.getId());
    }
}
