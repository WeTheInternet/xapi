package xapi.io.api;

import xapi.fu.Out1;
import xapi.io.X_IO;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/7/17.
 */
public class StringURLConnection extends URLConnection {
    private final Out1<String> contents;

    public StringURLConnection(URL url, Out1<String> contents) {
        super(url);
        this.contents = contents;
    }

    @Override
    public void connect() throws IOException {}

    @Override
    public InputStream getInputStream() throws IOException {
        return X_IO.toStream(contents.out1(), Charset.defaultCharset().name());
    }

}
