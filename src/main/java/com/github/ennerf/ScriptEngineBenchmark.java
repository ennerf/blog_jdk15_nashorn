package com.github.ennerf;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * == Zulu 11
 * Benchmark                           Mode  Cnt   Score   Error   Units
 * ScriptEngineBenchmark.evalNative   thrpt   10  31.294 ± 0.201  ops/us
 * ScriptEngineBenchmark.evalDynamic  thrpt   10  31.341 ± 0.120  ops/us
 * ScriptEngineBenchmark.evalJShell   thrpt   10  31.483 ± 0.308  ops/us
 * ScriptEngineBenchmark.evalNashorn  thrpt   10  16.436 ± 0.517  ops/us
 * ScriptEngineBenchmark.evalGraalJs  thrpt   10   2.303 ± 0.101  ops/us
 * <p>
 * == Zulu 11 + JVMCI + upgrade path
 * Benchmark                           Mode  Cnt   Score   Error   Units
 * ScriptEngineBenchmark.evalNative   thrpt   10  30.336 ± 0.184  ops/us
 * ScriptEngineBenchmark.evalDynamic  thrpt   10  30.554 ± 0.188  ops/us
 * ScriptEngineBenchmark.evalJShell   thrpt   10  30.673 ± 0.129  ops/us
 * ScriptEngineBenchmark.evalNashorn  thrpt   10  17.515 ± 0.771  ops/us
 * ScriptEngineBenchmark.evalGraalJs  thrpt   10  16.080 ± 0.685  ops/us
 * <p>
 * == Graal 11 CE
 * Benchmark                           Mode  Cnt   Score   Error   Units
 * ScriptEngineBenchmark.evalNative   thrpt   10  31.378 ± 0.272  ops/us
 * ScriptEngineBenchmark.evalDynamic  thrpt   10  31.501 ± 0.313  ops/us
 * ScriptEngineBenchmark.evalJShell   thrpt   10  31.409 ± 0.308  ops/us
 * ScriptEngineBenchmark.evalNashorn  thrpt   10   3.178 ± 0.060  ops/us
 * ScriptEngineBenchmark.evalGraalJs  thrpt   10  16.080 ± 0.889  ops/us
 * <p>
 * == Graal 11 EE
 * Benchmark                           Mode  Cnt   Score   Error   Units
 * ScriptEngineBenchmark.evalNative   thrpt   10  31.325 ± 0.231  ops/us
 * ScriptEngineBenchmark.evalDynamic  thrpt   10  30.775 ± 0.666  ops/us
 * ScriptEngineBenchmark.evalJShell   thrpt   10  30.458 ± 0.844  ops/us
 * ScriptEngineBenchmark.evalNashorn  thrpt   10   3.138 ± 0.088  ops/us
 * ScriptEngineBenchmark.evalGraalJs  thrpt   10  19.055 ± 0.874  ops/us
 *
 * @author Florian Enner
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 200, timeUnit = TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class ScriptEngineBenchmark {

    private static final boolean USE_GRAAL_COMPILER = true;

    public static void main(String[] args) throws RunnerException {

        // Enable GraalVM compiler for Graal.js compilation. The files
        // in the $compiler directory are downloaded by Maven.
        String[] jvmArgs = new String[0];
        if (USE_GRAAL_COMPILER) {
            String compilerDir = "target/compiler";
            jvmArgs = new String[]{
                    "-XX:+UnlockExperimentalVMOptions",
                    "-XX:+EnableJVMCI",
                    "--module-path=" + compilerDir,
                    "--upgrade-module-path=" + compilerDir + "/compiler.jar;" + compilerDir + "/compiler-management.jar"
            };
        }

        // Run benchmarks
        Options options = new OptionsBuilder()
                .jvmArgsAppend(jvmArgs)
                .include(".*" + ScriptEngineBenchmark.class.getSimpleName() + ".*")
                .verbosity(VerboseMode.NORMAL)
                .build();
        new Runner(options).run();

    }

    static final String equation = "state.value - prevState.value";
    StateFunction nativeJava = (prevState, state) -> state.value - prevState.value;
    StateFunction dynamicJava = ScriptEngines.evalJava(equation);
    StateFunction jshell = ScriptEngines.evalJShell(equation);
    StateFunction nashorn = ScriptEngines.evalNashorn(equation);
    StateFunction graal = ScriptEngines.evalGraalJs(equation);

    StateVariable prevState = new StateVariable();
    StateVariable state = new StateVariable();
    Random rnd = new Random(0);

    @Setup(Level.Trial)
    public void resetRandom() {
        rnd.setSeed(0);
    }

    @Setup(Level.Invocation)
    public void changeState() {
        prevState.value = state.value;
        state.value += rnd.nextDouble();
    }

    @Benchmark
    public double evalNative() {
        return nativeJava.computeValue(prevState, state);
    }

    @Benchmark
    public double evalDynamic() {
        return dynamicJava.computeValue(prevState, state);
    }

    @Benchmark
    public double evalJShell() {
        return dynamicJava.computeValue(prevState, state);
    }

    @Benchmark
    public double evalNashorn() {
        return nashorn.computeValue(prevState, state);
    }

    @Benchmark
    public double evalGraalJs() {
        return graal.computeValue(prevState, state);
    }

}
