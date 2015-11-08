
//bin/cat &>/dev/null <<'EOF'
/* This mess is a trick to start a shell script inside a valid java comment.
EOF

 mkdir -p /tmp/xapi # grab a safe place to work
 cp $0 /tmp/xapi # copy this file there
 cd /tmp/xapi # go to tmp dir

#rotate logs
rm -f logOut.1 logErr.1
mv logOut logOut.1 &>/dev/null
mv logErr logErr.1 &>/dev/null
touch logOut logErr

#compile self
javac `basename $0`

tail -f logOut &
tail -f logErr &

java Xapi $@ | sh 1>out 2>err

(jobs -p | xargs kill -9) &>/dev/null

exit

*/

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

class Xapi {

  public static void main(String ... args) throws Throwable{
    PrintStream run = System.out; // runs shell commands
    PrintStream log = System.err; // our log
    try {

        System.out.println("which mvn");
        String out = new String(Files.readAllBytes(Paths.get("out")));
        System.err.print("read from stdId: " + out);

        // Replace the real stdOut with a print stream that logs to a file
        PrintStream o = new PrintStream("logOut");
        System.setOut(o);

        //
        o = new PrintStream("logErr");
        System.setErr(o);

        System.err.println("to err");
        System.out.println("to out");
      } finally {
        run.println("exit");
      }
    }

}

