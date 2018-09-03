package xapi.server.api;

/**
 * A Trie structure for mapping routes to handlers, such as my/url/*path/...
 *
 * Created by James X. Nelson (james @wetheinter.net) on 9/15/17.
 */
public class RouteTrie<T> {

    protected class TrieNode {
        private TrieNode wildcard;
    }

    protected class LetterNode extends TrieNode {

    }

    protected class PathNode extends TrieNode {

    }

}
