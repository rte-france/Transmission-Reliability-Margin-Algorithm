/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import com.powsybl.iidm.network.Branch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class UncertaintyResult {
    private static final Logger LOGGER = LoggerFactory.getLogger(UncertaintyResult.class);
    private final double marketBasedFlow;
    private final double referenceFlow;
    private final double referenceZonalPtdf;
    private final double uncertainty;

    public UncertaintyResult(Branch branch, double marketBasedFlow, double referenceFlow, double referenceZonalPtdf) {
        this.marketBasedFlow = marketBasedFlow;
        this.referenceFlow = referenceFlow;
        this.referenceZonalPtdf = referenceZonalPtdf;
        this.uncertainty = (marketBasedFlow - referenceFlow) / referenceZonalPtdf;
        LOGGER.info("Uncertainty of branch id:'{}', name '{}' = {} with market-based flow: {}, reference flow: {}, reference zonal Ptdf: {}", branch.getId(), branch.getNameOrId(), uncertainty, marketBasedFlow, referenceFlow, referenceZonalPtdf);
    }

    public double getMarketBasedFlow() {
        return marketBasedFlow;
    }

    public double getReferenceZonalPtdf() {
        return referenceZonalPtdf;
    }

    public double getReferenceFlow() {
        return referenceFlow;
    }

    public double getUncertainty() {
        return uncertainty;
    }
}
