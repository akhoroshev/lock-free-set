package ifmo;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class ConcurrentCollectionSingleThreadTest {
    private Collection<Integer> getInstance() {
        return new ConcurrentCollection<>();
    }


    @Test
    public void testAdd() {
        Collection<Integer> list = getInstance();

        Assert.assertTrue(list.add(-12));
        Assert.assertTrue(list.add(-2));
        Assert.assertTrue(list.add(4));
        Assert.assertTrue(list.add(12));

        list.blockFurtherAdd();

        Assert.assertEquals(Arrays.asList(12, 4, -2, -12), list.content());
    }

    @Test
    public void testEmpty() {
        Collection<Integer> list = getInstance();
        list.blockFurtherAdd();

        Assert.assertFalse(list.add(-12));
        Assert.assertFalse(list.add(-2));
        Assert.assertFalse(list.add(4));
        Assert.assertFalse(list.add(12));

        Assert.assertEquals(Collections.emptyList(), list.content());
    }
}