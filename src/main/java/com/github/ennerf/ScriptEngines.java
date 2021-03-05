package com.github.ennerf;

import jdk.jshell.JShell;
import jdk.jshell.execution.LocalExecutionControlProvider;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import net.openhft.compiler.CompilerUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Florian Enner
 */
public class ScriptEngines {

    /**
     * Compiles JavaScript syntax into Bytecode using Nashorn
     */
    @SuppressWarnings("removal") // removed in Java 15
    public static StateFunction evalNashorn(String equation) {
        try {
            ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine("--no-deprecation-warning");
            engine.eval("function computeValue(prevState, state) {\n" +
                    "    return " + equation + ";" +
                    "};");
            return ((Invocable) engine).getInterface(StateFunction.class);
        } catch (Throwable e) {
            // Return an empty op in case it runs on an incompatible Java version
            System.err.println("Failed to evaluate Nashorn Engine: " + e.getMessage());
            return (prevState, state) -> 0;
        }
    }

    /**
     * Compiles JavaScript syntax into Bytecode using Graal.js
     */
    public static StateFunction evalGraalJs(String equation) {
        Context jsContext = Context.newBuilder()
                .allowExperimentalOptions(true)
                .allowHostAccess(HostAccess.ALL)
                .build();
        Value value = jsContext.eval("js", "(prevState, state) => " + equation);
        return value.as(StateFunction.class);
    }

    /**
     * Creates a new Java class that implements the StateFunction interface
     * and compiles it at runtime. Each implementation needs a new name.
     * <p>
     * Note: openhft implementation currently only works with Java 8 and 11
     * (https://github.com/OpenHFT/Java-Runtime-Compiler/issues/48)
     */
    public static StateFunction evalJava(String equation) {
        int count = counter.incrementAndGet();
        String className = "com.github.ennerf.generated.StateFunction$" + count;
        String javaCode = "\n" +
                "package com.github.ennerf.generated;\n" +
                "\n" +
                "import com.github.ennerf.StateVariable;\n" +
                "import com.github.ennerf.StateFunction;\n" +
                "\n" +
                "public class StateFunction$" + count + " implements StateFunction {\n" +
                "    public double computeValue(StateVariable prevState, StateVariable state) {\n" +
                "        return " + equation + ";" +
                "    }\n" +
                "}";
        try {
            Class generatedClass = CompilerUtils.CACHED_COMPILER.loadFromJava(className, javaCode);
            return (StateFunction) generatedClass.newInstance();
        } catch (Throwable e) {
            // Return an empty op in case it runs on an incompatible Java version
            System.err.println("Failed to compile Java API: " + e.getMessage());
            return (prevState, state) -> 0;
        }
    }

    private static AtomicInteger counter = new AtomicInteger();

    /**
     * Creates a lambda at runtime using the JShell API
     */
    public static StateFunction evalJShell(String equation) {
        // Setup a JShell that executes in the current runtime
        JShell jShell = JShell.builder()
                .executionEngine(new LocalExecutionControlProvider(), null)
                .build();

        // The JShell API only returns String based values, so it's not possible
        // to return a lambda. However, since we can modify classes/values in the
        // current ClassLoader, we can exchange lambdas through global variables.
        synchronized (JShellShared.class) {
            try {
                jShell.eval(JShellShared.class.getCanonicalName() + ".function = (prevState, state) -> " + equation + ";");
                return JShellShared.function;
            } finally {
                JShellShared.function = null;
            }
        }
    }

    public static class JShellShared {
        public static StateFunction function = null;
    }

}
