/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import com.powsybl.flow_decomposition.XnecProvider;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.rte_france.trm_algorithm.operational_conditions_aligners.OperationalConditionAligner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    public TrmAlgorithm(LoadFlowParameters loadFlowParameters, OperationalConditionAligner operationalConditionAligner) {
        this(loadFlowParameters, operationalConditionAligner, new ArrayList<>());
    }

    public TrmAlgorithm(LoadFlowParameters loadFlowParameters, OperationalConditionAligner operationalConditionAligner, List<String> countryRestrictionEiCode) {
        this.operationalConditionAligner = operationalConditionAligner;
        this.flowExtractor = new FlowExtractor(loadFlowParameters);
        this.zonalSensitivityComputer = new ZonalSensitivityComputer(loadFlowParameters, countryRestrictionEiCode);
    }

    private void checkReferenceElementNotEmpty(List<String> referenceNetworkElementIds) {
        if (referenceNetworkElementIds.isEmpty()) {
            throw new TrmException("Reference critical network elements are empty");
        }
    }

    private List<String> checkReferenceElementAreAvailableInMarketBasedNetwork(List<String> referenceNetworkElementIds, Network marketBasedNetwork) {
        List<String> missingBranches = referenceNetworkElementIds.stream()
            .filter(branchId -> Objects.isNull(marketBasedNetwork.getBranch(branchId)))
            .sorted()
            .toList();
        if (!missingBranches.isEmpty()) {
            LOGGER.error("Market-based network doesn't contain the following network elements: {}.", missingBranches);
        }
        return referenceNetworkElementIds.stream().filter(branchId -> !missingBranches.contains(branchId)).collect(Collectors.toList());
    }

    public TrmResults computeUncertainties(Network referenceNetwork, Network marketBasedNetwork, XnecProvider xnecProvider, ZonalData<SensitivityVariableSet> referenceZonalGlsks) {
        TrmResults.Builder builder = TrmResults.builder();

        LOGGER.info("Selecting Critical network elements");
        List<String> referenceNetworkElementIds = xnecProvider.getNetworkElements(referenceNetwork).stream().map(Identifiable::getId).sorted().toList();
        checkReferenceElementNotEmpty(referenceNetworkElementIds);
        referenceNetworkElementIds = checkReferenceElementAreAvailableInMarketBasedNetwork(referenceNetworkElementIds, marketBasedNetwork);

        operationalConditionAligner.align(referenceNetwork, marketBasedNetwork);
        Map<String, Double> marketBasedFlows = flowExtractor.extract(marketBasedNetwork, referenceNetworkElementIds);
        Map<String, ZonalPtdfAndFlow> referencePtdfAndFlow = zonalSensitivityComputer.run(referenceNetwork, referenceNetworkElementIds, referenceZonalGlsks);
        LOGGER.info("Computing uncertainties");
        Map<String, UncertaintyResult> uncertaintiesMap = referencePtdfAndFlow.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> {
                Branch<?> referenceBranch = referenceNetwork.getBranch(entry.getKey());
                double marketBasedFlow = marketBasedFlows.get(entry.getKey());
                double referenceFlow = entry.getValue().getFlow();
                double referenceZonalPtdf = entry.getValue().getZonalPtdf();
                return new UncertaintyResult(referenceBranch, marketBasedFlow, referenceFlow, referenceZonalPtdf);
            }
        ));

        builder.addUncertainties(uncertaintiesMap);
        return builder.build();
    }
}
