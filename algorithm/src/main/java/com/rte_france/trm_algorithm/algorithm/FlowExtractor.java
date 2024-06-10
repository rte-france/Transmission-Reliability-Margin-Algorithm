/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.rte_france.trm_algorithm.algorithm;

import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class FlowExtractor {
    private final LoadFlowParameters loadFlowParameters;

    public FlowExtractor(LoadFlowParameters loadFlowParameters) {
        this.loadFlowParameters = loadFlowParameters.copy().setDc(false);
    }

    public Map<String, Double> extract(Network network, Set<Line> lineSet) {
        LoadFlow.run(network, loadFlowParameters);
        return lineSet.stream().collect(Collectors.toMap(Identifiable::getId, line -> line.getTerminal1().getP()));
    }
}
