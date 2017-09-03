
//bin/cat &>/dev/null <<'EOF'
/* This mess is a trick to start a shell script inside a valid java comment.
EOF

 mkdir -p /tmp/xapi # grab a safe place to work
 yes | cp -u $0 /tmp/xapi # copy this file there

 # suppress annoying log messages
 pushd () {
  command pushd "$@" > /dev/null
 }

 popd () {
  command popd "$@" > /dev/null
 }
 pushd /tmp/xapi # go to tmp dir

#rotate logs
rm -f logOut.1 logErr.1
mv logOut logOut.1 &>/dev/null
mv logErr logErr.1 &>/dev/null
touch logOut logErr

#compile self;
javac `basename $0`

# let's stream both files to the executing shell
tail -f logOut &
tail -f logErr &

# run the java command, allowing it to use System.out to execute commands in executed shell
java Xapi $@ | sh 1>out 2>err

# once the program terminates, kill everything we've started
(jobs -p | xargs kill -9) &>/dev/null

# go back to wherever we came from
popd

exit

*/

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class Xapi {

    // grab references to the real, system out/err streams, so we can replace them
    private static final PrintStream run = System.out;
    private static final PrintStream log = System.err;
    private static final String XAPI_VERSION = "0.6-SNAPSHOT";
    private static final Path out = Paths.get("out");
    private static final Path err = Paths.get("err");

    interface Print<T> { void out(T t); }

  public static void main(String ... args) throws Throwable{

    // std out runs shell commands, per the unix shell hack at the top of this file
    // our log, which is streamed back to executing cli (yes, it's dirty, but it works...)

    try {

        // We'll try to use mvn to download xapi jars (for now, no fallback... in the future, hardcode uberjar url)
        run.println("which mvn");
        String mvn = read(out);

        log.print("Using maven: " + mvn);

        // Replace the real stdOut and stdErr with printstreams that log to files
        PrintStream o = new PrintStream("logOut");
        System.setOut(log);
        o = new PrintStream("logErr");
        System.setErr(o);

        // extract arguments we want to pass along to the demo
        List<String> progArg = new ArrayList<>();
        Map<String, String> sysArg = new LinkedHashMap<>();
        for (String arg : args) {
            if (arg.startsWith("-D")) {
                // system prop
                int ind = arg.indexOf('=');
                String key = arg.substring(2, ind);
                sysArg.put(key, arg.substring(ind+1));
            } else {
                // plain arguments
                progArg.add(arg);
            }
        }

        // Generate a pom for us to tell mvn what to do;
        String nl = "\\n";
        // for ease on the eyes, we'll use printf
        run.print("printf \"");
        // to create nicely formatted output that is readable, we'll make indent/outdent
        String[] depth = {""};
        Print<String> output = s->run.print(depth[0] + s + nl);
        Print<String> indent = s->{
            run.print(depth[0] + s + nl);
            depth[0] = depth[0] + "  ";
        };
        Print<String> outdent = s->{
            assert !depth[0].isEmpty() : "Called outdent too many times";
            run.print(
                // in case assertions disabled, silently ignore
                (depth[0] = depth[0].isEmpty() ? "" : depth[0].substring(2))
                    + s + nl);
        };

        indent.out("<project>");
            output.out("<modelVersion>4.0.0</modelVersion>");
            output.out("<groupId>net.wti</groupId>");
            output.out("<artifactId>demo</artifactId>");
            output.out("<version>0.1</version>");
        indent.out("<dependencies>");
        indent.out("<dependency>");
            output.out("<groupId>net.wetheinter</groupId>");
            output.out("<artifactId>xapi-demo</artifactId>");
            output.out("<version>" + XAPI_VERSION + "</version>");
        outdent.out("</dependency>");
        outdent.out("</dependencies>");
        indent.out("<build>");
        indent.out("<plugins>");
        indent.out("<plugin>");
            output.out("<groupId>org.codehaus.mojo</groupId>");
            output.out("<artifactId>exec-maven-plugin</artifactId>");
            output.out("<version>1.2.1</version>");
        indent.out("<configuration>");
            output.out("<mainClass>xapi.demo.jre.DemoApp</mainClass>");

        if (!sysArg.isEmpty()) {
            indent.out("<systemProperties>");
            sysArg.entrySet().forEach(e->{
               output.out("<key>" + e.getKey() + "</key>");
               output.out("<value>" + e.getValue() + "</value>");
            });
            indent.out("<systemProperty>");
            outdent.out("</systemProperty>");
            outdent.out("</systemProperties>");
        }
        if (!progArg.isEmpty()) {
            indent.out("<arguments>");
            progArg.forEach(arg->
                output.out("<argument>" + arg + "</argument>")
            );
            outdent.out("</arguments>");
        }

        outdent.out("</configuration>");
        outdent.out("</plugin>");
        outdent.out("</plugins>");
        outdent.out("</build>");
        outdent.out("</project>");

        run.println("\" > pom.xml");

        String res = read(Paths.get("pom.xml"));
        log.println("maven pom:\n" + res);

        // Now, execute a maven command that will download and run the xapi demo app;
        // we'll use run to append to logOut so our message appears in the server log (output to log.print() is ephemeral)
        run.println("echo \"Executing XApi\" >> logOut");

        // This will cause the sh command we are being piped to to block, keeping this process alive.
        run.println(mvn.trim() + " exec:java -q 1>>logOut 2>>logErr");

        run.println("echo \"Finished Executing XApi\" >> logOut");

      } catch (Throwable t){
        t.printStackTrace(log);
        throw t;
      } finally {
        run.println("exit");
      }
    }

  private static String read(Path source) throws Exception {
    byte[] bytes = Files.readAllBytes(source);
    return new String(bytes);
  }
}

