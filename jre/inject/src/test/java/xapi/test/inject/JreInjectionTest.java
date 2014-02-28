package xapi.test.inject;

import org.junit.Test;

import xapi.test.AbstractInjectionTest;
import xapi.annotation.inject.InstanceDefault;
import xapi.annotation.inject.SingletonDefault;
import xapi.inject.X_Inject;
import static org.junit.Assert.assertNotNull;

@SingletonDefault(implFor=JreInjectionTest.class)
public class JreInjectionTest extends AbstractInjectionTest{


  private interface ImportTestSingleton {
    void test();
  }
  private interface ImportTestInstance {
    void test();
  }

  @SingletonDefault(implFor=ImportSingleton.class)
  public static class ImportSingleton implements ImportTestSingleton {
    @Override
    public void test() {

    }
  }
  @InstanceDefault(implFor=ImportTestInstance.class)
  public static class ImportInstance implements ImportTestInstance {
    @Override
    public void test() {

    }
  }

//  @Test 
//  public void arg() throws Throwable {
//    String jar = "/home/james/.m2/repository/net/wetheinter/xapi-core-api/0.5-SNAPSHOT/xapi-core-api-0.5-SNAPSHOT.jar";
//    JarFile J = new JarFile(jar);
//    Enumeration<JarEntry> entries = J.entries();
//    MultithreadedStringTrie<JarEntry> trie = new MultithreadedStringTrie<>();
//    HashSet<JarEntry> jars = new LinkedHashSet<>();
//    int cnt=0;
//    while (entries.hasMoreElements()) {
//      JarEntry next = entries.nextElement();
//      if (next.isDirectory()){
//        System.out.println(next);
////        continue;
//      }
//      trie.put(next.getName(), next);
//      jars.add(next);
//      cnt++;
//    }
//    for (JarEntry e : trie.findPrefixed("")) {
//      jars.remove(e);
//      cnt--;
//    }
//    System.err.println(cnt);
//    for (JarEntry left : jars) {
//      System.err.println(left);
//    }
//    System.err.flush();
//    J.close();
//  }

  
	@Test
	public void testSingletonInjection(){
		ImportSingleton service = X_Inject.singleton(ImportSingleton.class);
		assertNotNull("Injector did not provide a singleton service", service);
		service.test();
	}
	@Test
	public void testInstanceInjection(){
		ImportInstance instance = X_Inject.instance(ImportInstance.class);
		assertNotNull("Injector did not provide an instance object", instance);
		instance.test();
	}

}
