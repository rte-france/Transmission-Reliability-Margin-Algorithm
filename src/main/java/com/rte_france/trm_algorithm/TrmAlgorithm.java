/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import com.powsybl.balances_adjustment.balance_computation.BalanceComputationParameters;
import com.powsybl.computation.ComputationManager;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class TrmAlgorithm {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrmAlgorithm.class);

    private final OperationalConditionAligner operationalConditionAligner;
    private final ZonalSensitivityComputer zonalSensitivityComputer;
    private final FlowExtractor flowExtractor;

    public TrmAlgorithm(LoadFlowParameters loadFlowParameters, BalanceComputationParameters balanceComputationParameters, LoadFlow.Runner loadFlowRunner, ComputationManager computationManager) {
        this.operationalConditionAligner = new OperationalConditionAligner(balanceComputationParameters, loadFlowRunner, computationManager);
        this.flowExtractor = new FlowExtractor(loadFlowParameters);
        this.zonalSensitivityComputer = new ZonalSensitivityComputer(loadFlowParameters);
    }

    private void checkReferenceElementNotEmpty(Set<Branch> referenceNetworkElements) {
        if (referenceNetworkElements.isEmpty()) {
            throw new TrmException("Reference critical network elements are empty");
        }
    }

    private static void checkReferenceElementAreIncludedInMarketBasedElements(Set<Branch> referenceNetworkElements, Set<Branch> marketBasedNetworkElements) {
        Set<String> referenceNetworkElementIds = getNetworkElementIds(referenceNetworkElements);
        Set<String> marketBasedNetworkElementIds = getNetworkElementIds(marketBasedNetworkElements);
        if (!marketBasedNetworkElementIds.containsAll(referenceNetworkElementIds)) {
            Set<String> extraReferenceNetworkElements = computeSetDifference(referenceNetworkElementIds, marketBasedNetworkElementIds);
            throw new TrmException(String.format("Market-based critical network elements doesn't contain the following elements: %s.", extraReferenceNetworkElements));
        }
        if (!referenceNetworkElementIds.containsAll(marketBasedNetworkElementIds)) {
            Set<String> extraMarketBasedNetworkElements = computeSetDifference(marketBasedNetworkElementIds, referenceNetworkElementIds);
            LOGGER.warn(String.format("Reference critical network elements doesn't contain the following elements: %s.", extraMarketBasedNetworkElements));
        }
    }

    private static Set<String> getNetworkElementIds(Set<Branch> referenceNetworkElements) {
        return referenceNetworkElements.stream().map(Identifiable::getId).collect(Collectors.toSet());
    }

    private static Set<String> computeSetDifference(Set<String> networkElementsId1, Set<String> networkElementIds2) {
        Set<String> extraNetworkElementIds = new HashSet<>(networkElementsId1);
        extraNetworkElementIds.removeAll(networkElementIds2);
        return extraNetworkElementIds;
    }

    private static double computeUncertainty(Branch branch, Double marketBasedFlow, double referenceFlow, double refereneZonalPtdf) {
        double uncertainty = (marketBasedFlow - referenceFlow) / refereneZonalPtdf;
        LOGGER.info("Uncertainty of branch id:'{}', name '{}' = {} with market-based flow: {}, reference flow: {}, reference zonal Ptdf: {}", branch.getId(), branch.getNameOrId(), uncertainty, marketBasedFlow, referenceFlow, refereneZonalPtdf);
        return uncertainty;
    }

    public Map<String, Double> computeUncertainties(Network referenceNetwork, Network marketBasedNetwork, ZonalData<SensitivityVariableSet> referenceZonalGlsks, Crac crac, ZonalData<Scalable> marketZonalScalable) {
        Set<Branch> referenceNetworkElements = CneSelector.getNetworkElements(referenceNetwork);
        Set<Branch> marketBasedNetworkElements = CneSelector.getNetworkElements(marketBasedNetwork);

        checkReferenceElementNotEmpty(referenceNetworkElements);
        checkReferenceElementAreIncludedInMarketBasedElements(referenceNetworkElements, marketBasedNetworkElements);

        operationalConditionAligner.align(referenceNetwork, marketBasedNetwork, crac, marketZonalScalable);
        Map<String, Double> marketBasedFlows = flowExtractor.extract(marketBasedNetwork, marketBasedNetworkElements);
        Map<String, ZonalPtdfAndFlow> referencePdtfAndFlow = zonalSensitivityComputer.run(referenceNetwork, referenceNetworkElements, referenceZonalGlsks);
        return referencePdtfAndFlow.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> computeUncertainty(referenceNetwork.getBranch(entry.getKey()), marketBasedFlows.get(entry.getKey()), entry.getValue().getFlow(), entry.getValue().getZonalPtdf())
        ));
    }
}
