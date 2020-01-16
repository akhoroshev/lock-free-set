package ifmo;

import com.devexperts.dxlab.lincheck.LinChecker;
import com.devexperts.dxlab.lincheck.annotations.Operation;
import com.devexperts.dxlab.lincheck.annotations.Param;
import com.devexperts.dxlab.lincheck.paramgen.IntGen;
import com.devexperts.dxlab.lincheck.strategy.stress.StressCTest;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;


public class SetLinCheckTestCases {

    @Test
    public void runAddRemoveContainsTest() {
        for (int i = 0; i < 10; i++) {
            LinChecker.check(AddRemoveContainsTest.class);
        }
    }

    @Test
    public void runIteratorTest() {
        for (int i = 0; i < 10; i++) {
            LinChecker.check(IteratorTest.class);
        }
    }

    @Test
    public void runMixedTest() {
        for (int i = 0; i < 10; i++) {
            LinChecker.check(MixedTest.class);
        }
    }

    @StressCTest
    public static class AddRemoveContainsTest {
        private Set<Integer> set = new ConcurrentSet<>();

        @Operation
        public boolean add(@Param(gen = IntGen.class, conf = "-10:10") int key) {
            return set.add(key);
        }

        @Operation
        public boolean remove(@Param(gen = IntGen.class, conf = "-10:10") int key) {
            return set.remove(key);
        }

        @Operation
        public boolean contains(@Param(gen = IntGen.class, conf = "-10:10") int key) {
            return set.contains(key);
        }
    }

    @StressCTest
    public static class IteratorTest {
        private Set<Integer> set = new ConcurrentSet<>();

        @Operation
        public boolean add(@Param(gen = IntGen.class, conf = "-10:10") int key) {
            return set.add(key);
        }

        @Operation
        public boolean remove(@Param(gen = IntGen.class, conf = "-10:10") int key) {
            return set.remove(key);
        }

        @Operation
        public List<Integer> snapshot() {
            List<Integer> snap = new ArrayList<>();
            set.iterator().forEachRemaining(snap::add);
            return snap;
        }
    }

    @StressCTest
    public static class MixedTest {
        private Set<Integer> set = new ConcurrentSet<>();

        @Operation
        public boolean add(@Param(gen = IntGen.class, conf = "-10:10") int key) {
            return set.add(key);
        }

        @Operation
        public boolean remove(@Param(gen = IntGen.class, conf = "-10:10") int key) {
            return set.remove(key);
        }

        @Operation
        public boolean contains(@Param(gen = IntGen.class, conf = "-10:10") int key) {
            return set.contains(key);
        }

        @Operation
        public boolean isEmpty() {
            return set.isEmpty();
        }

        @Operation
        public List<Integer> snapshot() {
            List<Integer> snap = new ArrayList<>();
            set.iterator().forEachRemaining(snap::add);
            return snap;
        }
    }
}
