package net.wti.lang.parser.ast.exception;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/4/16.
 */
public class NotFoundException extends RuntimeException {

    private final String name;

    public NotFoundException(String name) {
        super("Did not find node named " + name);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
