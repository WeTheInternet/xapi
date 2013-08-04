package xapi.dev.source;

import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import xapi.source.write.Template;

public class TemplateTest {

  @Test
  public void testSimpleTemplate() {
    Template generator = new Template("(<>[])$.toArray()", "<>", "$");
    String result = generator.apply("String", "this.value");
    Assert.assertEquals(result, "(String[])this.value.toArray()");
  }

  @Test
  public void testComplexTemplate() {
    Template generator = new Template(
        "Hello $2!\n" +
        "$1.$3($4, $5);\n" +
        "$4.$1($2, $3, $1, $5);"
        , "$1", "$2", "$3", "$4", "$5");
    String result = generator.apply(
        "hello", "world", "three", "four", "5");
    
    Assert.assertEquals(result, 
        "Hello world!\n" +
        "hello.three(four, 5);\n" +
        "four.hello(world, three, hello, 5);"
    );
  }
  
  @Test
  public void testRandomTemplates() {
    if (Boolean.getBoolean("xapi.benchmark"))
      for (int i = 0; i<14;i++){
        testRandomTemplates((int)Math.pow(2, i));
      }
    else
      testRandomTemplates(1000);
    
  }
  private void testRandomTemplates(final int iters) {
    int loops = 50;
    int numItems = 32;
    final String[] templates = new String[loops];
    final Object[] manualCases = new Object[loops];
    final Object[] templateCases = new Object[loops];
    
    // Prepare the cases before running them, so we get accurate timing.
    while (loops --> 0) {
      final int loop = loops;
      numItems = 8 +(numItems%23) % 28;
      final String[] replacers = new String[numItems];
      final String[] values = new String[numItems];
      while (numItems --> 0) {
        char c = Character.forDigit(numItems, 36);
        values[numItems] = "_"+c+"_";
        replacers[numItems] = String.valueOf(c);
      }
      templates[loop] = randomChars(16 + (int)(Math.random()*128));
      
      templateCases[loop] = new Runnable() {
        @Override
        public void run() {
          int iterations = iters;
          Template t = new Template(templates[loop], replacers);
          String templateResult=null;
          while (iterations --> 0) {
            templateResult = t.apply(values);
          }
          templateCases[loop] = templateResult;
        }
      };
      manualCases[loop] = new Runnable() {
        @Override
        public void run() {
          int iterations = iters;
          // Our control case of performing runtime string operations
          // is naive enough that we can actually use regex patterns
          // (which does speed up it's performance to only 1.5-3x slower).
          // The Template class does not require regex syntax compatibility.
          Pattern[] patterns = new Pattern[values.length];
          for (int i = values.length; i --> 0; ) {
            patterns[i] = Pattern.compile(replacers[i]);
          }
          while (iterations --> 0) {
            String manualResult = templates[loop];
            for (int i = 0; i < values.length; i++) {
              manualResult = patterns[i].matcher(manualResult).replaceAll(values[i]);
            }
            manualCases[loop] = manualResult;
          }
          
        }
      };
    }
    
    // Run the cases.
    System.gc();
    long start = System.nanoTime();
    for (Object o : manualCases)
      ((Runnable)o).run();
    float manualTime = (System.nanoTime() - start);

    start = System.nanoTime();
    for (Object o : templateCases)
      ((Runnable)o).run();
    float templateTime = (System.nanoTime() - start);
    
    
    System.out.println("Template is "+(float)(manualTime/templateTime)+ " faster than runtime " +
    		"string operations for " +iters+" iterations.");
    
    for (int i = templateCases.length; i --> 0 ;) {
      Assert.assertEquals(
          "Original: "+templates[i]+"\n"+
          "Manual: "+manualCases[i]+"\n"+
          "Template: "+templateCases[i]+"\n",
          (String)manualCases[i], (String)templateCases[i]);
    }
  }

  private String randomChars(int numChars) {
    StringBuilder b = new StringBuilder();
    for (int i = 0, m = (int)(Math.random()*numChars); i < m; i++ ) {
      b.append(Character.forDigit((int)(Math.random()*36), 36));
    }
    return b.toString();
  }
  
}
