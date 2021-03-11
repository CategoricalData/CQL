package catdata.aql.fdm;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;

import catdata.Unit;

public class LazySet<E> implements Collection<E> {

  private Collection<E> s;

  private final Function<Unit, Collection<E>> c;

  private final int size;

  public LazySet(Function<Unit, Collection<E>> c, int size) {
    this.c = c;
    this.size = size;
  }

  @Override
  public int size() {
    return /* Util.anomaly(); */ size;
  }

  private void init() {
    if (s == null) {
      s = c.apply(Unit.unit);
    }
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public boolean contains(Object o) {
    init();
    return s.contains(o);
  }

  @Override
  public Iterator<E> iterator() {
    init();
    return s.iterator();
  }

  @Override
  public Object[] toArray() {
    init();
    return s.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    init();
    return s.toArray(a);
  }

  @Override
  public boolean add(E e) {
    init();
    return s.add(e);
  }

  @Override
  public boolean remove(Object o) {
    init();
    return s.remove(o);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    init();
    return s.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    init();
    return s.addAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    init();
    return s.retainAll(c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    init();
    return s.removeAll(c);
  }

  @Override
  public void clear() {
    init();
    s.clear();
  }

}
