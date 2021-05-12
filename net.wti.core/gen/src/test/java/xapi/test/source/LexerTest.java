package xapi.test.source;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import xapi.source.lex.CharIterator;
import xapi.source.lex.Lexer;
import xapi.source.lex.LexerForWords;
import xapi.source.lex.LexerStack;
import xapi.source.lex.StringCharIterator;

public class LexerTest {

  private class TestLexerForWords extends LexerForWords {
    private ArrayList<String> words = new ArrayList<>();

    @Override
    protected Lexer onWord(String word, CharIterator str) {
      words.add(word);
      return super.onWord(word, str);
    }
  }

  private class TestLexerForHello extends LexerStack {
    boolean sawHello;
    @Override
    protected Lexer onWord(LexerStack parent, String word, CharIterator str) {
      if ("Hello".equals(word)) {
        sawHello = true;
      }
      return super.onWord(parent, word, str);
    }
  }

  private class TestLexerForWorld extends LexerStack {
    boolean sawWorld;
    @Override
    protected Lexer onWord(LexerStack parent, String word, CharIterator str) {
      if ("World".equals(word)) {
        sawWorld = true;
      }
      return super.onWord(parent, word, str);
    }
  }

  @Test
  public void testLexerForWords() {
    TestLexerForWords lex = new TestLexerForWords();
    lex.consume(new StringCharIterator("Hello World"));
    Assert.assertEquals(lex.words+" != Hello World", 2, lex.words.size());
    Assert.assertEquals("Hello", lex.words.get(0));
    Assert.assertEquals("World", lex.words.get(1));
  }

  @Test
  public void testLexerStack() {
    TestLexerForHello hello = new TestLexerForHello();
    TestLexerForWorld world = new TestLexerForWorld();
    new LexerStack()
        .addLexer(hello)
        .addLexer(world)
        .consume(new StringCharIterator("Hello World"));
    Assert.assertTrue(hello.sawHello);
    Assert.assertTrue(world.sawWorld);
  }

}
