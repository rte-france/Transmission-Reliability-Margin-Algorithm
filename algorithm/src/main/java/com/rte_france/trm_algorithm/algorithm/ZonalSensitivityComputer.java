/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.algorithm;

import com.powsybl.contingency.ContingencyContext;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.powsybl.sensitivity.SensitivityFunctionType.BRANCH_ACTIVE_POWER_1;
import static com.powsybl.sensitivity.SensitivityVariableType.INJECTION_ACTIVE_POWER;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class ZonalSensitivityComputer {
    private final SensitivityAnalysisParameters sensitivityAnalysisParameters;

    public ZonalSensitivityComputer(LoadFlowParameters loadFlowParameters) {
        this.sensitivityAnalysisParameters = new SensitivityAnalysisParameters()
            .setLoadFlowParameters(loadFlowParameters.copy().setDc(false));
    }

    public Map<String, ZonalPtdfAndFlow> run(Network network, Set<Branch> branches, ZonalData<SensitivityVariableSet> glsk) {
        Map<String, SensitivityVariableSet> dataPerZone = glsk.getDataPerZone();
        List<SensitivityVariableSet> variableSets = getSensitivityVariableSets(glsk, dataPerZone);
        List<SensitivityFactor> factors = getSensitivityFactors(branches, dataPerZone);
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.run(network, factors, Collections.emptyList(), variableSets, sensitivityAnalysisParameters);
        return extractZonalPtdfs(branches, sensitivityAnalysisResult, factors);
    }

    private static Map<String, ZonalPtdfAndFlow> extractZonalPtdfs(Set<Branch> branches, SensitivityAnalysisResult sensitivityAnalysisResult, List<SensitivityFactor> factors) {
        return branches.stream().map(Identifiable::getId).collect(Collectors.toMap(Function.identity(), branch -> computeZonalPtdf(sensitivityAnalysisResult, factors, branch)));
    }

    private static ZonalPtdfAndFlow computeZonalPtdf(SensitivityAnalysisResult sensitivityAnalysisResult, List<SensitivityFactor> factors, String branch) {
        List<Double> sensitivityValues = sensitivityAnalysisResult.getValues().stream()
                .filter(value -> factors.get(value.getFactorIndex()).getFunctionId().equals(branch))
                .map(SensitivityValue::getValue).toList();
        List<Double> flowValues = sensitivityAnalysisResult.getValues().stream()
            .filter(value -> factors.get(value.getFactorIndex()).getFunctionId().equals(branch))
            .map(SensitivityValue::getFunctionReference).distinct().toList();
        if (flowValues.size() != 1) {
            throw new TrmException("Flow value of branch '" + branch + "' is not unique");
        }
        return new ZonalPtdfAndFlow(Collections.max(sensitivityValues) - Collections.min(sensitivityValues), flowValues.get(0));
    }

    private static List<SensitivityFactor> getSensitivityFactors(Set<Branch> branches, Map<String, SensitivityVariableSet> dataPerZone) {
        List<SensitivityFactor> factors = new ArrayList<>();
        branches.forEach(branch -> dataPerZone.keySet().forEach(country -> factors.add(
            new SensitivityFactor(BRANCH_ACTIVE_POWER_1, branch.getId(), INJECTION_ACTIVE_POWER, country, true, ContingencyContext.none()))));
        return factors;
    }

    private static List<SensitivityVariableSet> getSensitivityVariableSets(ZonalData<SensitivityVariableSet> glsk, Map<String, SensitivityVariableSet> dataPerZone) {
        return dataPerZone.keySet().stream()
            .map(country -> new SensitivityVariableSet(country, new ArrayList<>(glsk.getData(country).getVariables()))).toList();
    }

}
