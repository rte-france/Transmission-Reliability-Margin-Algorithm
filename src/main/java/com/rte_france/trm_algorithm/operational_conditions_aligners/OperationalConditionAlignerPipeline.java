/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.powsybl.iidm.network.Network;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class OperationalConditionAlignerPipeline implements OperationalConditionAligner {
    private final OperationalConditionAligner[] operationalConditionAlignerList;

    public OperationalConditionAlignerPipeline(OperationalConditionAligner... operationalConditionAlignerList) {
        this.operationalConditionAlignerList = operationalConditionAlignerList;
    }

    @Override
    public void align(Network referenceNetwork, Network marketBasedNetwork) {
        for (OperationalConditionAligner operationalConditionAligner : operationalConditionAlignerList) {
            operationalConditionAligner.align(referenceNetwork, marketBasedNetwork);
        }
    }
}
