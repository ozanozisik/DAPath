/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dapath.internal.dapath;

import org.dapath.internal.pathway.Entry;

/**
 *
 * @author Ozan Ozisik
 */
public class InputOutputPair {
    private Entry output;
    private Entry input;

    public InputOutputPair(Entry input, Entry output) {
        super();
        this.output = output;
        this.input = input;
    }

    public Entry getOutput() {
        return output;
    }

    public Entry getInput() {
        return input;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((input == null) ? 0 : input.hashCode());
        result = prime * result + ((output == null) ? 0 : output.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        InputOutputPair other = (InputOutputPair) obj;
        if (input == null) {
            if (other.input != null) {
                return false;
            }
        } else if (!input.equals(other.input)) {
            return false;
        }
        if (output == null) {
            if (other.output != null) {
                return false;
            }
        } else if (!output.equals(other.output)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "" + input.toString() + " " + output.toString();
    }
}
