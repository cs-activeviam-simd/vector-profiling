# Java 12 Vector API Profiling

This repository contains speed tests on the Java 12 Vector API, currently in development as part of OpenJDK Project Panama.

## Building

To run this project, you will need a specific development build of OpenJDK, containing the OpenJDK jdk.incubator.vector module. You will need to build it from source.

Detailed build instructions are available at http://hg.openjdk.java.net/panama/dev/ in `doc/building.md`.

You will need a regular Java 11 installation, a Mercurial client, a usual build environment, and on Windows, Cygwin and the Microsoft Visual C++ Build Tools 2017.

In a shell (on Windows, in a Cygwin shell), run:
```shell
hg clone http://hg.openjdk.java.net/panama/dev/
cd dev
hg checkout vectorIntrinsics  # the branch we need is vectorIntrinsics
bash configure --disable-warnings-as-errors
make images
```

If everything worked properly, the compiled JDK will be available at `build/*/images/jdk` (for example, the `java` tool is available at `build/*/images/jdk/bin/java`).

## Running

Install Maven.

Then, to compile, simply run in a shell:
```shell
export JAVA_HOME=/path/to/compiled/jdk  # tells maven to use our JDK
mvn clean install  # compiles the project
```

Then, in a shell, run:
```shell
$JAVA_HOME/bin/java -jar target/simd.jar
```

To automatically write the compiled assembly from the benchmark methods into `results/`, run:
```shell
$JAVA_HOME/bin/java -cp target/simd.jar fr.centralesupelec.simd.AsmLogger
```

Ignore any warnings regarding the use of incubator modules or illegal reflective accesses.
