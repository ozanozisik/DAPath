/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dapath.internal.pathway;

/**
 *
 * @author Ozan Ozisik
 */
public enum RelationType {
    ACTIVATION(1, 1.0), INHIBITION(2, -1.0), EXPRESSION(3, 1.0), REPRESSION(4, -1.0),
    INDIRECT_EFFECT(5, 0), STATE_CHANGE(6, 0), BINDING_ASSOCIATION(7, 0),
    DISSOCIATION(8, 0), MISSING_INTERACTION(9, 0), PHOSPHORYLATION(10, 0),
    DEPHOSPHORYLATION(11, 0), GLYCOSYLATION(12, 0), UBIQUITINATION(13, 0), METHYLATION(14, 0),
    COMPOUND(15, 0), HIDDEN_COMPOUND(16, 0), OUTNODE(17, 0);

    private int type;
    private double stimulation;

    private RelationType(int type, double stimulation) {
        this.type = type;
        this.stimulation = stimulation;
    }

    public int getType() {
        return type;
    }

    public double getStimulation() {
        return stimulation;
    }

}
