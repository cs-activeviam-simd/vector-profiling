package fr.centralesupelec.simd;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class AsmLogger {

    private static class Box<T> {
        private T data;

        T get() {
            return data;
        }

        void set(T data) {
            this.data = data;
        }
    }

    private static long blackhole = 42;

    public static void main(String[] args) {
        if(args.length == 0) {
            try {
                Files.createDirectories(Paths.get("results"));
            } catch (IOException e) {
                System.err.println("Error: Could not create results/ directory");
            }

            StringBuilder prefixSb = new StringBuilder();
            prefixSb.append("\ton JVM: ").append(System.getProperty("java.vm.name")).append('\n');
            prefixSb.append("\ton OS: ").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version")).append(" ").append(System.getProperty("os.arch")).append('\n');
            prefixSb.append("\twith vector size: ").append(VectorProfiling.VectorState.bitSize).append(" bits\n");
            prefixSb.append("-----\n");
            String prefix = prefixSb.toString();

            String java;
            if (System.getProperty("os.name").startsWith("Win")) {
                java = System.getProperties().getProperty("java.home") + "\\bin\\java.exe";
            } else {
                java = System.getProperties().getProperty("java.home") + "/bin/java";
            }

            List<String> jvmArgs = new ArrayList<>();
            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            for(String arg : runtimeMXBean.getInputArguments()) {
                if(!arg.startsWith("-agentlib")) {
                    jvmArgs.add(arg);
                }
            }
            jvmArgs.add("-classpath");
            jvmArgs.add(runtimeMXBean.getClassPath());

            Fork fork = VectorProfiling.class.getAnnotation(Fork.class);
            if(fork != null) {
                jvmArgs.addAll(Arrays.asList(fork.jvmArgsPrepend()));
            }

            Method[] methods = VectorProfiling.class.getMethods();
            for(Method method : methods) {
                if(method.getAnnotation(Benchmark.class) == null) {
                    continue;
                }
                List<String> methodArgs = new ArrayList<>();
                Fork methodFork = method.getAnnotation(Fork.class);
                if(methodFork != null) {
                    methodArgs.addAll(Arrays.asList(methodFork.jvmArgsAppend()));
                }

                List<String> forkArgs = new ArrayList<>();
                forkArgs.add(java);
                forkArgs.addAll(jvmArgs);
                forkArgs.addAll(methodArgs);
                forkArgs.add("-XX:+UnlockDiagnosticVMOptions");
                forkArgs.add("-XX:PrintAssemblyOptions=intel");
                forkArgs.add("-XX:CompileCommand=print,*VectorProfiling." + method.getName());
                forkArgs.add("-XX:CompileCommand=dontinline,*VectorProfiling." + method.getName());
                forkArgs.add(AsmLogger.class.getCanonicalName());
                forkArgs.add(method.getName());
                ProcessBuilder builder = new ProcessBuilder().command(forkArgs);
                Process process;
                try {
                    process = builder.start();
                } catch (IOException e) {
                    System.err.println("Error: Could not start JVM:");
                    e.printStackTrace();
                    return;
                }
                StringBuilder sb = new StringBuilder();
                Box<Exception> ex = new Box<>();
                CountDownLatch readLock = new CountDownLatch(1);
                Thread readThread = new Thread(() -> {
                    try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while((line = reader.readLine()) != null) {
                            sb.append(line).append('\n');
                        }
                    } catch(IOException e) {
                        if(Thread.interrupted()) {
                            return;
                        }
                        ex.set(e);
                    } finally {
                        readLock.countDown();
                    }
                });
                readThread.start();
                int exitCode;
                try {
                    exitCode = process.waitFor();
                } catch (InterruptedException e) {
                    readThread.interrupt();
                    return;
                }
                try {
                    readLock.await();
                } catch (InterruptedException e) {
                    return;
                }
                if(exitCode < 10) {
                    System.err.println("Error: Benchmark " + method.getName() + " exited with unsuccessful exit code: " + exitCode);
                    stderr(process, method);
                    continue;
                }
                if(ex.get() != null) {
                    System.err.println("Error: Unable to read benchmark " + method.getName() + " stdout:");
                    ex.get().printStackTrace();
                    continue;
                }
                int start = sb.lastIndexOf("Compiled method (c2)");
                if(start == -1) {
                    System.err.println("Error: No c2 compiled method for benchmark " + method.getName() + ":");
                    stderr(process, method);
                    System.err.println(sb.toString());
                    continue;
                }
                start = sb.indexOf("[Entry Point]", start);
                if(start == -1) {
                    System.err.println("Error: No entry point for method for benchmark " + method.getName() + ":");
                    stderr(process, method);
                    System.err.println(sb.toString());
                    continue;
                }
                int end = sb.indexOf("\nImmutableOopMap", start);
                if(end == -1) {
                    end = sb.length();
                }

                try(BufferedWriter writer = Files.newBufferedWriter(Paths.get("results", method.getName() + ".log"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    writer.write("Running benchmark " + method.getName() + ":\n");
                    writer.write(prefix);
                    writer.write(sb.substring(start, end));
                } catch(IOException e) {
                    System.err.println("Error: Unable to write assembly for benchmark " + method.getName() + ":");
                    e.printStackTrace();
                }

                System.err.println(method.getName() + ":");
                stderr(process, method);
            }
        } else {
            VectorProfiling profiling = new VectorProfiling();
            VectorProfiling.VectorState state = new VectorProfiling.VectorState();
            state.ARRAY_LENGTH = 8192;
            state.doSetup();

            Method method;
            try {
                method = VectorProfiling.class.getMethod(args[0], VectorProfiling.VectorState.class);
            } catch (NoSuchMethodException e) {
                System.err.println("Error: Could not find benchmark: " + args[0] + ":");
                e.printStackTrace();
                System.exit(1);
                return;
            }

            try {
                for (int i = 0; i < 100000; i++) {
                    // reflective method invocation, but perfomance does not matter wrt asm generation
                    consume(method.invoke(profiling, state));
                }
            } catch (Exception e) {
                System.err.println("Error: Benchmark error:");
                e.printStackTrace();
                System.exit(2);
            }

            // use blackhole in jvm exit code
            System.exit(10 + (int)(blackhole % 13));
        }
    }

    private static void consume(Object o) {
        if(o instanceof Integer) {
            blackhole ^= (Integer) o;
        } else if(o instanceof Long) {
            blackhole ^= (Long) o;
        } else if(o instanceof Boolean) {
            blackhole ^= ((Boolean) o) ? 1 : 0;
        } else if(o instanceof int[]) {
            int[] a = (int[]) o;
            for (int e : a) {
                blackhole ^= e;
            }
        } else {
            throw new IllegalArgumentException("Unexpected becnhmark return type: " + o.getClass().getCanonicalName());
        }
    }

    private static void stderr(Process process, Method method) {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while((line = reader.readLine()) != null) {
                System.err.println(line);
            }
        } catch(IOException e) {
            System.err.println("Error: Unable to read benchmark " + method.getName() + " stderr:");
            e.printStackTrace();
        }
    }
}
