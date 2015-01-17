package de.johni0702.replaystudio.api.packet;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * A list for PacketData allowing efficient modification.
 */
public class PacketList implements List<PacketData> {

    protected int size;
    protected PacketData first;
    protected PacketData last;

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean contains(Object o) {
        PacketData e = first;
        while (e != null) {
            if (e.equals(o)) {
                return true;
            }
            e = e.next;
        }
        return false;
    }

    @Override
    public PacketListIterator iterator() {
        return listIterator();
    }

    @Override
    public Object[] toArray() {
        Object[] array = new Object[size];
        int i = 0;
        PacketData e = first;
        while (e != null) {
            array[i] = e;
            e = e.next;
        }
        return array;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length < size) {
            a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
        }
        PacketData e = first;
        for (int i = 0; i < a.length; i++) {
            a[i] = (T) e;
            e = e == null ? null : e.next;
        }
        return a;
    }

    @Override
    public boolean add(PacketData packetData) {
        iterator().skipTo(packetData.getTime());
        return true;
    }

    @Override
    public boolean remove(Object o) {
        PacketListIterator iter = iterator();
        while (iter.hasNext()) {
            if (iter.next().equals(o)) {
                iter.remove();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends PacketData> c) {
        boolean changed = false;
        for (PacketData data : c) {
            changed |= add(data);
        }
        return changed;
    }

    @Override
    public boolean addAll(int index, Collection<? extends PacketData> c) {
        return addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        PacketListIterator iter = iterator();
        while (iter.hasNext()) {
            if (c.contains(iter.next())) {
                iter.remove();
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean changed = false;
        PacketListIterator iter = iterator();
        while (iter.hasNext()) {
            if (!c.contains(iter.next())) {
                iter.remove();
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public void sort(Comparator<? super PacketData> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        size = 0;
        first = null;
        last = null;
    }

    @Override
    public PacketData get(int index) {
        return listIterator(index).next();
    }

    @Override
    public PacketData set(int index, PacketData element) {
        PacketListIterator iter = listIterator(index);
        PacketData previous = iter.next();
        iter.set(element);
        return previous;
    }

    @Override
    public void add(int index, PacketData element) {
        listIterator(index).add(element);
    }

    @Override
    public PacketData remove(int index) {
        PacketListIterator iter = listIterator(index);
        PacketData previous = iter.next();
        iter.remove();
        return previous;
    }

    @Override
    public int indexOf(Object o) {
        PacketListIterator iter = iterator();
        int i = 0;
        while (iter.hasNext()) {
            if (iter.next().equals(o)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        PacketListIterator iter = listIterator(size);
        int i = size;
        while (iter.hasPrevious()) {
            i--;
            if (iter.previous().equals(o)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public PacketListIterator listIterator() {
        return new PacketListIterator(this);
    }

    @Override
    public PacketListIterator listIterator(int index) {
        return new PacketListIterator(this, index);
    }

    @Override
    public List<PacketData> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

}
