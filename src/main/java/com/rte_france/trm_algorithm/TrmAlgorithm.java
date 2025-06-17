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

import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    //
    private final IdentifiableMapper identifiableMapper;

    public TrmAlgorithm(LoadFlowParameters loadFlowParameters, OperationalConditionAligner operationalConditionAligner) {
        this(loadFlowParameters, operationalConditionAligner, new IdentityIdMapper());
    }

    public TrmAlgorithm(LoadFlowParameters loadFlowParameters, OperationalConditionAligner operationalConditionAligner, IdentifiableMapper identifiableMapper) {
        this.operationalConditionAligner = operationalConditionAligner;
        this.flowExtractor = new FlowExtractor(loadFlowParameters);
        this.zonalSensitivityComputer = new ZonalSensitivityComputer(loadFlowParameters);
        this.identifiableMapper = identifiableMapper;
    }

    private void checkReferenceElementNotEmpty(List<String> referenceNetworkElementIds) {
        if (referenceNetworkElementIds.isEmpty()) {
            throw new TrmException("Reference critical network elements are empty");
        }
    }

    private void checkReferenceElementAreAvailableInMarketBasedNetwork(List<String> referenceNetworkElementIds, Network marketBasedNetwork) {
        List<String> missingBranches = referenceNetworkElementIds.stream()
            .filter(branchIdInReference -> Objects.isNull(marketBasedNetwork.getBranch(identifiableMapper.getIdInMarket(branchIdInReference))))
            .sorted()
            .toList();
        if (!missingBranches.isEmpty()) {
            throw new TrmException(String.format("Market-based network doesn't contain the following network elements: %s.", missingBranches));
        }
    }

    public TrmResults computeUncertainties(Network referenceNetwork, Network marketBasedNetwork, XnecProvider xnecProvider, ZonalData<SensitivityVariableSet> referenceZonalGlsks) {
        TrmResults.Builder builder = TrmResults.builder();

        LOGGER.info("Selecting Critical network elements");
        // Filtrer pour n'utiliser que les branches présentes dans les deux fichiers
//        Set<String> idsMarket = marketBasedNetwork.getBranchStream()
//            .map(Identifiable::getId)
//            .collect(Collectors.toSet());
//        List<String> referenceNetworkElementIds = xnecProvider.getNetworkElements(referenceNetwork).stream().map(Identifiable::getId).sorted().filter(idsMarket::contains).toList();
        List<String> referenceNetworkElementIds = xnecProvider.getNetworkElements(referenceNetwork).stream().map(Identifiable::getId).sorted().toList();
        checkReferenceElementNotEmpty(referenceNetworkElementIds);
        checkReferenceElementAreAvailableInMarketBasedNetwork(referenceNetworkElementIds, marketBasedNetwork);

        operationalConditionAligner.align(referenceNetwork, marketBasedNetwork);
        List<String> marketBasedNetworkElementsIds = referenceNetworkElementIds.stream().map(id -> identifiableMapper.getIdInMarket(id)).toList();
        Map<String, Double> marketBasedFlows = flowExtractor.extract(marketBasedNetwork, marketBasedNetworkElementsIds);
        Map<String, ZonalPtdfAndFlow> referencePdtfAndFlow = zonalSensitivityComputer.run(referenceNetwork, referenceNetworkElementIds, referenceZonalGlsks);
        LOGGER.info("Computing uncertainties");
        Map<String, UncertaintyResult> uncertaintiesMap = referencePdtfAndFlow.entrySet().stream().collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> {
                String idInReference = entry.getKey();
                //
                String idInMarket = identifiableMapper.getIdInMarket(idInReference);
                Branch<?> referenceBranch = referenceNetwork.getBranch(idInReference);
                double marketBasedFlow = marketBasedFlows.get(idInMarket);
                double referenceFlow = entry.getValue().getFlow();
                double referenceZonalPtdf = entry.getValue().getZonalPtdf();
                return new UncertaintyResult(referenceBranch, marketBasedFlow, referenceFlow, referenceZonalPtdf);
            }
        ));

        builder.addUncertainties(uncertaintiesMap);
        return builder.build();
    }
}
