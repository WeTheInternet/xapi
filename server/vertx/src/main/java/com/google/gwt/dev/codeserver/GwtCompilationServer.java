package com.google.gwt.dev.codeserver;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import xapi.fu.In1Out1;
import xapi.gwtc.api.CompiledDirectory;
import xapi.log.X_Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/27/16.
 */
public class GwtCompilationServer {
    public void handleBuffer(
        NetSocket event,
        Buffer buffer,
        RecompileController controller,
        In1Out1<String, CompiledDirectory> moduleGetter
    )
    throws IOException {

        int len = buffer.length();
        String path = buffer.getString(0, len);
        Map<String, String> headers = new HashMap<>();
        for (String line : path.split("\n")) {
            int ind = line.indexOf(' ');
            if (ind > 0)
                headers.put(line.substring(0, ind), line.substring(ind + 1));
        }
        if (path.contains("sourcemaps")) {
            path = path.split("sourcemaps[/]")[1].split("\\s")[0];
        }
        // if requesting gwtSourceMap.json, we must resolve the compiled directory
        String module = path.split("/")[0];
        CompiledDirectory dir = moduleGetter.io(module);
        if (path.endsWith("gwtSourceMap.json")) {
            if (dir == null) {
                print(event, "<pre>"
                    + "Module " + module + "not yet compiled"
                    + "</pre>");
                return;
            }
            File extras = new File(dir.getSourceMapDir());
            if (!extras.exists()) {
                throw new RuntimeException("Can't find symbolMaps dir for " + module);
            }
            File[] sourceMapFiles = extras.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.matches(".*_sourceMap.*[.]json");
                }
            });
            if (sourceMapFiles == null) {
                sourceMapFiles = new File[0];
            }
            if (sourceMapFiles.length == 0) {
                print(event, "<pre>"
                    + "No sourcemaps found for " + module
                    + "</pre>");
                return;
            }
            File winner = sourceMapFiles[0];
            if (sourceMapFiles.length > 1) {
                // more than one permutation; we need to look up permutation map
                // we have to do browser-sniffing using User-Agent header; ick
                String userAgent = headers.get("User-Agent:").toLowerCase();
                String strongName;
                Map<String, String> permutations = dir.getUserAgentMap();
                // TODO lookup the user agent selection script used and run in ScriptEngine
                if (userAgent.contains("safari")) {
                    strongName = permutations.get("safari");
                } else if (userAgent.indexOf("gecko") > 0) {
                    strongName = permutations.get("gecko1_8");
                } else {
                    strongName = permutations.get("ie9");
                    if (strongName == null)
                        strongName = permutations.get("ie8");
                    if (strongName == null)
                        strongName = permutations.get("ie6");
                }
                // see if we've found the permutation we're looking for
                if (strongName != null) {
                    for (File candidate : sourceMapFiles) {
                        if (candidate.getName().startsWith(strongName)) {
                            winner = candidate;
                            break;
                        }
                    }
                }
            }
            X_Log.trace(getClass(), "Streaming source map file ",winner);
            stream(event, new FileInputStream(winner));
            // event.sendFile(winner.getCanonicalPath());
        } else {
            // assume non-sourcemap request wants a java / resource file.
            URL res = controller.getResource(path);
            if (res == null) {
                String resolved = path.replace(module+"/", "");
                if (resolved.startsWith("gen/")) {
                    resolved = resolved.replace("gen/", "");
                    // Check the generated source filed
                    if (dir == null) {
                        X_Log.warn(getClass(), "No directory found for ",module," this gwt module may not have finished compiling.");
                    } else {
                        File genDir = new File(dir.getGenDir());
                        if (genDir.exists()) {
                            X_Log.debug(getClass(), "Checking gen dir ",genDir," for ",resolved);
                            File genSrc = new File(genDir, resolved);
                            if (genSrc.exists()) {
                                X_Log.debug(getClass(), "Using ",genSrc," for ",path);
                                resolved = genSrc.getCanonicalPath();
                                res = genSrc.toURI().toURL();
                            } else {
                                X_Log.debug(getClass(), "No file exists @ ",genSrc," for ",path);
                            }
                        } else {
                            X_Log.warn(getClass(), "gen dir ",genDir," does not exist for ", path);
                        }
                    }
                } else {
                    res = controller.getResource(resolved);
                }
                X_Log.trace(getClass(), "Could not find ", path," checking ",resolved," resulted in ", res);
            }
            if (res == null) {
                String error = "Proxy only supports sourcemaps and webserver resources; you sent " + path;
                X_Log.error(getClass(), error);
                print(event, "<pre>" + error + "</pre>");
            } else {
                stream(event, res.openStream());
            }
        }
    }

    private void print(final NetSocket event, String out) throws IOException {
        stream(event, new ByteArrayInputStream(out.getBytes()));
    }

    private void stream(final NetSocket event, InputStream in) throws IOException {
        byte[] buff;
        Buffer b = Buffer.buffer();
        try {
            while (true) {
                buff = new byte[in.available() + 4096];
                int bytesRead = in.read(buff);
                if (bytesRead == -1) {
                    return;
                }
                if (buff.length == bytesRead)
                    b.appendBytes(buff);
                else
                    b.appendBytes(Arrays.copyOf(buff, bytesRead));
            }
        } finally {
            in.close();
            event.write(b);
            event.close();

        }
    }
}
