/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import com.powsybl.contingency.ContingencyContext;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    private final List<String> countryRestrictionEiCode; // if empty, no restriction

    public ZonalSensitivityComputer(LoadFlowParameters loadFlowParameters, List<String> countryRestrictionEiCode) {
        this.sensitivityAnalysisParameters = new SensitivityAnalysisParameters()
            .setLoadFlowParameters(loadFlowParameters.copy().setDc(false));
        this.countryRestrictionEiCode = countryRestrictionEiCode;
    }

    private static Map<String, ZonalPtdfAndFlow> extractZonalPtdfs(List<String> branchIds, SensitivityAnalysisResult sensitivityAnalysisResult, List<SensitivityFactor> factors) {
        return branchIds.stream().collect(Collectors.toMap(Function.identity(), branchId -> computeZonalPtdf(sensitivityAnalysisResult, factors, branchId)));
    }

    private static ZonalPtdfAndFlow computeZonalPtdf(SensitivityAnalysisResult sensitivityAnalysisResult, List<SensitivityFactor> factors, String branchId) {
        List<Double> sensitivityValues = sensitivityAnalysisResult.getValues().stream()
            .filter(value -> factors.get(value.getFactorIndex()).getFunctionId().equals(branchId))
            .map(SensitivityValue::getValue).toList();
        List<Double> flowValues = sensitivityAnalysisResult.getValues().stream()
            .filter(value -> factors.get(value.getFactorIndex()).getFunctionId().equals(branchId))
            .map(SensitivityValue::getFunctionReference).distinct().toList();
        if (flowValues.isEmpty()) {
            throw new TrmException("No sensitivity flow found for branch '" + branchId + "'");
        }
        if (flowValues.size() > 1) {
            throw new TrmException("Flow value of branch '" + branchId + "' is not unique");
        }
        return new ZonalPtdfAndFlow(Collections.max(sensitivityValues) - Collections.min(sensitivityValues), flowValues.get(0));
    }

    private static List<SensitivityFactor> getSensitivityFactors(List<String> branchIds, Map<String, SensitivityVariableSet> dataPerZone) {
        List<SensitivityFactor> factors = new ArrayList<>();
        branchIds.forEach(branchId -> dataPerZone.keySet().forEach(country -> factors.add(
            new SensitivityFactor(BRANCH_ACTIVE_POWER_1, branchId, INJECTION_ACTIVE_POWER, country, true, ContingencyContext.none()))));
        return factors;
    }

    private static List<SensitivityVariableSet> getSensitivityVariableSets(ZonalData<SensitivityVariableSet> glsk, Map<String, SensitivityVariableSet> dataPerZone) {
        return dataPerZone.keySet().stream()
            .map(country -> new SensitivityVariableSet(country, new ArrayList<>(glsk.getData(country).getVariables()))).toList();
    }

    public Map<String, ZonalPtdfAndFlow> run(Network network, List<String> branchIds, ZonalData<SensitivityVariableSet> glsk) {
        Map<String, SensitivityVariableSet> dataPerZone = glsk.getDataPerZone();
        List<SensitivityVariableSet> variableSets = getSensitivityVariableSets(glsk, dataPerZone);
        List<SensitivityFactor> factors = getSensitivityFactors(branchIds, dataPerZone);
        if (!countryRestrictionEiCode.isEmpty()) {
            factors = factors.stream().filter(factor -> countryRestrictionEiCode.contains(factor.getVariableId())).toList();
        }
        SensitivityAnalysisResult sensitivityAnalysisResult = SensitivityAnalysis.run(network, factors, Collections.emptyList(), variableSets, sensitivityAnalysisParameters);
        return extractZonalPtdfs(branchIds, sensitivityAnalysisResult, factors);
    }
}
