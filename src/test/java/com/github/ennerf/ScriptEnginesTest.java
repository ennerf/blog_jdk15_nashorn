package com.github.ennerf;

import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Florian Enner
 */
class ScriptEnginesTest {

    @Test
    void evalNashorn() {
        testEngine(ScriptEngines::evalNashorn);
    }

    @Test
    void evalGraalJs() {
        testEngine(ScriptEngines::evalGraalJs);
    }

    @Test
    void evalJava() {
        testEngine(ScriptEngines::evalJava);
    }

    @Test
    void evalJShell() {
        testEngine(ScriptEngines::evalJShell);
    }

    private void testEngine(Function<String, StateFunction> engine) {
        StateVariable prevState = new StateVariable();
        StateVariable state = new StateVariable();

        prevState.value = 31;
        state.value = 42;

        StateFunction func = engine.apply("state.value - prevState.value");
        assertEquals(state.value - prevState.value, func.computeValue(prevState, state));

    }

}