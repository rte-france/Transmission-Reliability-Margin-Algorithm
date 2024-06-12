/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.algorithm;

import com.powsybl.balances_adjustment.balance_computation.BalanceComputationParameters;
import com.powsybl.computation.ComputationManager;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class TrmAlgorithm {
    private final OperationalConditionAligner operationalConditionAligner;
    private final ZonalSensitivityComputer zonalSensitivityComputer;
    private final FlowExtractor flowExtractor;

    public TrmAlgorithm(LoadFlowParameters loadFlowParameters, BalanceComputationParameters balanceComputationParameters, LoadFlow.Runner loadFlowRunner, ComputationManager computationManager) {
        this.operationalConditionAligner = new OperationalConditionAligner(balanceComputationParameters, loadFlowRunner, computationManager);
        this.flowExtractor = new FlowExtractor(loadFlowParameters);
        this.zonalSensitivityComputer = new ZonalSensitivityComputer(loadFlowParameters);
    }

    private static void checkIdenticalNetworkElements(Set<Line> networkElement1, Set<Line> networkElement2, String networkMsg) {
        Set<String> extraReferenceElement = networkElement1.stream().map(Identifiable::getId).collect(Collectors.toSet());
        extraReferenceElement.removeAll(networkElement2.stream().map(Identifiable::getId).collect(Collectors.toSet()));

        if (!extraReferenceElement.isEmpty()) {
            throw new TrmException(String.format("%s network doesn't contain the following elements: %s.", networkMsg, extraReferenceElement));
        }
    }

    public Map<String, Double> computeUncertainties(Network referenceNetwork, Network marketBasedNetwork, ZonalData<SensitivityVariableSet> referenceZonalGlsks, Crac crac, ZonalData<Scalable> marketZonalScalable) {
        Set<Line> referenceNetworkElements = CneSelector.getNetworkElements(referenceNetwork);
        Set<Line> marketBasedNetworkElements = CneSelector.getNetworkElements(marketBasedNetwork);

        checkIdenticalNetworkElements(referenceNetworkElements, marketBasedNetworkElements, "Market-based");
        checkIdenticalNetworkElements(marketBasedNetworkElements, referenceNetworkElements, "Real-time");

        operationalConditionAligner.align(referenceNetwork, marketBasedNetwork, crac, marketZonalScalable);
        Map<String, Double> marketBasedFlows = flowExtractor.extract(marketBasedNetwork, marketBasedNetworkElements);
        Map<String, ZonalPtdfAndFlow> referencePdtfAndFlow = zonalSensitivityComputer.run(referenceNetwork, referenceNetworkElements, referenceZonalGlsks);
        return referencePdtfAndFlow.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> (marketBasedFlows.get(entry.getKey()) - entry.getValue().getFlow()) / entry.getValue().getZonalPtdf()
        ));
    }
}
