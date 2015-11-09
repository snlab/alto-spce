/*
 * Copyright © 2015 Copyright (c) 2015 SNLAB and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.alto.spce.impl.algorithm.helper;

import java.util.HashMap;

public class DisjointSet<K> {

    public static class DisjointSetNode {
        private DisjointSetNode root;
        private int level;

        public DisjointSetNode() {
            root = null;
            level = -1;
        }

        public DisjointSetNode getRoot() {
            //First pass to find the root
            DisjointSetNode p = this;
            while (p.root != null) {
                p = p.root;
            }
            DisjointSetNode root = p;

            //Second pass to compress the tree
            p = this;
            for (DisjointSetNode q = p; p.root != root; p = q) {
                p.root = root;
            }
            return root;
        }

        public static void merge(DisjointSetNode x, DisjointSetNode y) {
            if ((x == null) || (y == null))
                return;

            DisjointSetNode _x = x.getRoot();
            DisjointSetNode _y = y.getRoot();

            if (_x == _y)
                return;

            if (_x.level == _y.level) {
                --_x.level;
                _y.root = _x;
            } else if (_x.level < _y.level) {
                _x.root = _y;
            } else {
                _y.root = _x;
            }
        }
    }

    private HashMap<K, DisjointSetNode> roots = new HashMap<>();

    protected DisjointSetNode getSet(K x) {
        if (!roots.containsKey(x)) {
            roots.put(x, new DisjointSetNode());
        }
        return roots.get(x);
    }

    public boolean disjointed(K x, K y) {
        DisjointSetNode x_set = getSet(x);
        DisjointSetNode y_set = getSet(y);

        return (x_set.getRoot() != y_set.getRoot());
    }

    public void merge(K x, K y) {
        DisjointSetNode x_set = getSet(x);
        DisjointSetNode y_set = getSet(y);

        if (x_set.getRoot() == y_set.getRoot())
            return;

        DisjointSetNode.merge(x_set, y_set);
    }
}


