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
public enum RelationDirection {
    INCOMING(1), OUTGOING(2);

    private int direction;

    private RelationDirection(int direction) {
        this.direction = direction;
    }

    public int getDirection() {
        return direction;
    }
}
