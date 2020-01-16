package ifmo;

import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;
import java.util.Random;
import java.util.TreeSet;

public class SetSingleThreadTest {
    private Set<Integer> getInstance() {
        return new ConcurrentSet<>();
    }

    @Test
    public void testAdd() {
        Set<Integer> set = getInstance();

        Assert.assertFalse(set.contains(0));
        Assert.assertTrue(set.add(0));
        Assert.assertTrue(set.contains(0));

        Assert.assertFalse(set.add(0));
        Assert.assertTrue(set.contains(0));

        Assert.assertTrue(set.add(-1));
        Assert.assertFalse(set.add(-1));
        Assert.assertTrue(set.contains(-1));
        Assert.assertTrue(set.contains(0));

        Assert.assertTrue(set.add(15));
        Assert.assertFalse(set.add(-1));
        Assert.assertFalse(set.add(0));

        Assert.assertTrue(set.contains(15));
        Assert.assertTrue(set.contains(-1));
        Assert.assertTrue(set.contains(0));
    }

    @Test
    public void testRemove() {
        Set<Integer> set = getInstance();

        Assert.assertTrue(set.add(0));
        Assert.assertTrue(set.contains(0));
        Assert.assertTrue(set.remove(0));
        Assert.assertFalse(set.contains(0));
        Assert.assertFalse(set.remove(0));

        Assert.assertTrue(set.add(0));
        Assert.assertTrue(set.contains(0));

        Assert.assertFalse(set.contains(1));
        Assert.assertTrue(set.add(1));
        Assert.assertTrue(set.contains(1));

        Assert.assertFalse(set.contains(-10));
        Assert.assertTrue(set.add(-10));
        Assert.assertTrue(set.contains(-10));

        Assert.assertTrue(set.remove(1));
        Assert.assertTrue(set.remove(0));
        Assert.assertTrue(set.remove(-10));

        Assert.assertFalse(set.contains(-10));
        Assert.assertFalse(set.contains(0));
        Assert.assertFalse(set.contains(1));
    }

    @Test
    public void testIterator() {
        Set<Integer> set = getInstance();

        Assert.assertTrue(set.add(15));
        Assert.assertTrue(set.add(-1));
        Assert.assertTrue(set.add(0));

        final Iterator<Integer> iterator = set.iterator();

        Assert.assertTrue(iterator.hasNext());

        Assert.assertEquals(new Integer(-1), iterator.next());
        Assert.assertEquals(new Integer(0), iterator.next());
        Assert.assertEquals(new Integer(15), iterator.next());

        Assert.assertFalse(iterator.hasNext());
    }


    @Test
    public void compareWithDefaultSet() {
        Set<Integer> mySet = getInstance();
        java.util.Set<Integer> originalSet = new TreeSet<>();

        Random rand = new Random(0);
        for (int count = 0; count < 1000; count++) {
            for (int i = -256; i < 256; i++) {
                switch (rand.nextInt() % 5) {
                    case 0:
                        Assert.assertEquals(originalSet.add(i), mySet.add(i));
                        break;
                    case 1:
                        Assert.assertEquals(originalSet.remove(i), mySet.remove(i));
                        break;
                    case 2:
                        Assert.assertEquals(originalSet.contains(i), mySet.contains(i));
                        break;
                    case 3:
                        final Iterator<Integer> originalIterator = originalSet.iterator();
                        final Iterator<Integer> myIterator = mySet.iterator();
                        Assert.assertEquals(originalIterator.hasNext(), myIterator.hasNext());
                        while (originalIterator.hasNext() && myIterator.hasNext()) {
                            Assert.assertEquals(originalIterator.next(), myIterator.next());
                        }
                        Assert.assertEquals(originalIterator.hasNext(), myIterator.hasNext());
                        break;
                    case 4:
                        Assert.assertEquals(originalSet.isEmpty(), mySet.isEmpty());
                        break;
                }
            }
        }
    }
}