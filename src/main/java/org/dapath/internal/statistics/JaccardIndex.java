package org.dapath.internal.statistics;

import java.util.ArrayList;

public class JaccardIndex {

    public static <E> double calculateJaccardIndex(ArrayList<E> list1, ArrayList<E> list2) {

        int common = 0;
        for (E er : list1) {
            if (list2.contains(er)) {
                common++;
            }
        }
        return ((double) common) / (list1.size() + list2.size() - common);
    }
}
