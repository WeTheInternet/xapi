package xapi.test.collect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import xapi.collect.api.Fifo;
import xapi.collect.impl.SimpleFifo;

public class FifoTest {

  @Test public void testAdd() {
    Fifo<String> fifo = new SimpleFifo<String>();
    ArrayList<String> list = new ArrayList<String>();
    doAdd(fifo, list, "one");
    doAdd(fifo, list, "two");
    doAdd(fifo, list, "three");
    Assert.assertEquals(fifo.take(), list.get(0));
    Assert.assertEquals(fifo.take(), list.get(1));
    Assert.assertEquals(fifo.take(), list.get(2));
    Assert.assertTrue(fifo.isEmpty());
    for (String item : fifo.forEach()) {
      assert false : "Fifo not empty: "+item+" was leftover";
    }
  }

  @Test public void testRemove() {
    Fifo<String> fifo = new SimpleFifo<String>();
    ArrayList<String> list = new ArrayList<String>();
    fifo.give("four");
    doAdd(fifo, list, "one");
    doAdd(fifo, list, "two");
    doAdd(fifo, list, "three");
    doAdd(fifo, list, "four");
    doRemove(fifo, list, "three");
    doRemove(fifo, list, "four");
    int pos = 0;
    for (String item : fifo.forEach()) {
      String other = list.get(pos);
      Assert.assertEquals(item, other);
      pos++;
    }
    Assert.assertEquals(fifo.take(), list.get(0));
    Assert.assertEquals(fifo.take(), list.get(1));
    Assert.assertTrue(fifo.isEmpty());
    for (String item : fifo.forEach()) {
      assert false : "Fifo not empty: "+item+" was leftover";
    }
  }

  @SuppressWarnings("unused")
  private void dump(Fifo<String> fifo) {
    for (String item : fifo.forEach())
      System.out.println(item);
  }

  @Test public void testIterate() {
    Fifo<String> fifo = new SimpleFifo<String>();
    ArrayList<String> list = new ArrayList<String>();
    doAdd(fifo, list, "one");
    doAdd(fifo, list, "two");
    doAdd(fifo, list, "three");
    doAdd(fifo, list, "four");
    doRemove(fifo, list, "three");
    doAdd(fifo, list, "five");
    int pos = 0;
    for (String item : fifo.forEach()) {
      String other = list.get(pos);
      Assert.assertEquals(item, other);
      pos++;
    }
    Assert.assertEquals(fifo.size(), list.size());
  }
  @Test public void testIterateRemove() {
    Fifo<String> fifo = new SimpleFifo<String>();
    LinkedList<String> list = new LinkedList<String>();
    doAdd(fifo, list, "one");
    doAdd(fifo, list, "two");
    doAdd(fifo, list, "three");
    doAdd(fifo, list, "four");
    doAdd(fifo, list, "five");
    list.remove("four");
    list.remove("five");
    Iterator<String> iter = fifo.iterator();
    while(iter.hasNext()) {
      String next = iter.next();
      if ("four".equals(next))
        iter.remove();
      if ("five".equals(next))
        iter.remove();
    }
    int pos = 0;
    for (String item : fifo.forEach()) {
      String other = list.get(pos);
      Assert.assertEquals(item, other);
      pos++;
    }
    Assert.assertEquals(fifo.size(), list.size());
  }
  @Test public void testIterateRemoveAll() {
    Fifo<String> fifo = new SimpleFifo<String>();
    fifo.give("one").give("two").give("three").give("four");
    Iterator<String> iter = fifo.iterator();
    while(iter.hasNext()) {
      iter.next();
      iter.remove();
    }
    Assert.assertEquals(0, fifo.size());
    if(fifo.iterator().hasNext()) {
      throw new RuntimeException("Fifo not empty");
    }
    if(fifo.take()!=null) {
      throw new RuntimeException("Fifo not empty");
    }
  }

  private <X> void doAdd(Fifo<X> fifo, List<X> list, X value) {
    fifo.give(value);
    list.add(value);
  }
  private <X> void doRemove(Fifo<X> fifo, List<X> list, X value) {
    fifo.remove(value);
    list.remove(value);
  }

}
