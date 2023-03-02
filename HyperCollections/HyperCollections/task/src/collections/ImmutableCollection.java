package collections;

import java.util.ArrayList;
import java.util.List;

public final class ImmutableCollection<T> {

    private List<T> list;

    private ImmutableCollection(List<T> items) {
        this.list = items;
    }

    public static <T> ImmutableCollection<T> of(T... arr) {
        List<T> list = new ArrayList<T>();


        for (T t : arr) {
            list.add(t);
        }

        if (list.contains(null)) throw new NullPointerException();

        return new ImmutableCollection(list);

    }

    public static <T> ImmutableCollection<T> of() {
        List<T> list = new ArrayList<T>();

        return new ImmutableCollection(list);
    }

    public boolean contains(T element){
        return list.contains(element);
    }

    public int size() {
        return list.size();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }


}
