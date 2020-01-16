package ifmo;

import com.devexperts.dxlab.lincheck.LinChecker;
import com.devexperts.dxlab.lincheck.annotations.Operation;
import com.devexperts.dxlab.lincheck.annotations.Param;
import com.devexperts.dxlab.lincheck.paramgen.IntGen;
import com.devexperts.dxlab.lincheck.strategy.stress.StressCTest;
import org.junit.Test;

import java.util.List;

@StressCTest
public class ConcurrentCollectionLinCheckTest {
    private Collection<Integer> list = new ConcurrentCollection<>();

    @Operation
    public boolean add(@Param(gen = IntGen.class, conf = "-15:15") int key) {
        return list.add(key);
    }

    @Operation
    public List<Integer> content() {
        try {
            return list.content();
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    @Operation
    public void blockFurtherAdd() {
        list.blockFurtherAdd();
    }

    @Test
    public void runTest() {
        LinChecker.check(ConcurrentCollectionLinCheckTest.class);
    }

}
