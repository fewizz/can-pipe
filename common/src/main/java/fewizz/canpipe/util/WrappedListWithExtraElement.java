package fewizz.canpipe.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class WrappedListWithExtraElement<T, E> implements List<T> {

    private final List<T> originalList;
    public final E element;

    public WrappedListWithExtraElement(List<T> originalList, E element) {
        this.originalList = originalList;
        this.element = element;
    }

    @Override public boolean add(T arg) { return this.originalList.add(arg); }
    @Override public void add(int arg0, T arg1) { this.originalList.add(arg0, arg1); }
    @Override public boolean addAll(Collection<? extends T> c) { return this.originalList.addAll(c); }
    @Override public boolean addAll(int index, Collection<? extends T> c) { return this.originalList.addAll(index, c); }
    @Override public void clear() { this.originalList.clear(); }
    @Override public boolean contains(Object o) { return this.originalList.contains(o); }
    @Override public boolean containsAll(Collection<?> c) { return this.originalList.containsAll(c); }
    @Override public T get(int index) { return this.originalList.get(index); }
    @Override public int indexOf(Object o) { return this.originalList.indexOf(o); }
    @Override public boolean isEmpty() { return this.originalList.isEmpty(); }
    @Override public Iterator<T> iterator() { return this.originalList.iterator(); }
    @Override public int lastIndexOf(Object o) { return this.originalList.lastIndexOf(o); }
    @Override public ListIterator<T> listIterator() { return this.originalList.listIterator(); }
    @Override public ListIterator<T> listIterator(int index) { return this.originalList.listIterator(index); }
    @Override public boolean remove(Object o) { return this.originalList.remove(o); }
    @Override public T remove(int index) { return this.originalList.remove(index); }
    @Override public boolean removeAll(Collection<?> c) { return this.originalList.removeAll(c); }
    @Override public boolean retainAll(Collection<?> c) { return this.originalList.retainAll(c); }
    @Override public T set(int arg0, T arg1) { return this.originalList.set(arg0, arg1); }
    @Override public int size() { return this.originalList.size(); }
    @Override public List<T> subList(int fromIndex, int toIndex) { return this.originalList.subList(fromIndex, toIndex); }
    @Override public Object[] toArray() { return this.originalList.toArray(); }
    @Override public <T0> T0[] toArray(T0[] arg0) { return this.originalList.toArray(arg0); }

}
