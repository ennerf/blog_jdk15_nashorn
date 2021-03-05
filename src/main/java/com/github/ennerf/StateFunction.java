package com.github.ennerf;

/**
 * Function that computes a desired value based on the state of
 * the current and previous timesteps
 *
 * @author Florian Enner
 */
public interface StateFunction {

    public double computeValue(StateVariable prevState, StateVariable state);

}
