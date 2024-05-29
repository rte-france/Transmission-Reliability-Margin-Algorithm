/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.algorithm;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class ZonalPtdfAndFlow {
    private final double zonalPtdf;
    private final double flow;

    public ZonalPtdfAndFlow(double zonalPtdf, double flow) {
        this.zonalPtdf = zonalPtdf;
        this.flow = flow;
    }

    public double getZonalPtdf() {
        return zonalPtdf;
    }

    public double getFlow() {
        return flow;
    }
}
