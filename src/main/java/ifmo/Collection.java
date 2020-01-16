package ifmo;

import java.util.List;

/*
Collection
 */
public interface Collection<T> {

    /*
    Add value to collection
     */
    boolean add(T value);

    /*
    Blocking further additions to the collection
     */
    void blockFurtherAdd();

    /*
    Check the collection is blocked on add operations
     */
    boolean isBlocked();

    /*
    Returns the contents of the collection if it was previously blocked or throws `IllegalStateException`
     */
    List<T> content();
}
