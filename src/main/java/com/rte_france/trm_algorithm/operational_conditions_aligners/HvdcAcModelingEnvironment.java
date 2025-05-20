/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.farao_community.farao.gridcapa_swe_commons.hvdc.HvdcInformation;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.HvdcLinkProcessor;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.HvdcCreationParameters;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.SwePreprocessorParameters;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.json.JsonSwePreprocessorImporter;
import com.powsybl.iidm.network.*;
import com.rte_france.trm_algorithm.TrmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class HvdcAcModelingEnvironment implements OperationalConditionAligner {
    private static final Logger LOGGER = LoggerFactory.getLogger(HvdcAcModelingEnvironment.class);

    private final Set<HvdcCreationParameters> creationParametersSet;
    private final OperationalConditionAligner operationalConditionAligner;
    private List<HvdcInformation> hvdcReferenceInformationList;
    private List<HvdcInformation> hvdcMarketBasedInformationList;

    public HvdcAcModelingEnvironment(Set<HvdcCreationParameters> creationParametersSet, OperationalConditionAligner operationalConditionAligner) {
        creationParametersSet.stream()
            .filter(hvdcCreationParameters -> Objects.isNull(hvdcCreationParameters.getAngleDroopActivePowerControlParameters()))
            .forEach(hvdcCreationParameters -> {
                throw new TrmException(String.format("HVDC \"%s\" has a DC control mode.", hvdcCreationParameters.getId()));
            });
        this.creationParametersSet = creationParametersSet;
        this.operationalConditionAligner = operationalConditionAligner;
        this.hvdcReferenceInformationList = new ArrayList<>();
        this.hvdcMarketBasedInformationList = new ArrayList<>();
    }

    private void replaceEquivalentModelByHvdc(Network referenceNetwork, Network marketBasedNetwork) {
        LOGGER.info("Transform HVDC AC equivalent model to HVDC");
        HvdcLinkProcessor.replaceEquivalentModelByHvdc(referenceNetwork, creationParametersSet);
        HvdcLinkProcessor.replaceEquivalentModelByHvdc(marketBasedNetwork, creationParametersSet);
    }

    private void replaceHvdcByEquivalentModel(Network referenceNetwork, Network marketBasedNetwork) {
        LOGGER.info("Transform HVDC to their AC equivalent models");
        hvdcReferenceInformationList = getHvdcInformationFromNetwork(referenceNetwork);
        hvdcMarketBasedInformationList = getHvdcInformationFromNetwork(marketBasedNetwork);
        HvdcLinkProcessor.replaceHvdcByEquivalentModel(referenceNetwork, creationParametersSet, hvdcReferenceInformationList);
        HvdcLinkProcessor.replaceHvdcByEquivalentModel(marketBasedNetwork, creationParametersSet, hvdcMarketBasedInformationList);
    }

    @Override
    public void align(Network referenceNetwork, Network marketBasedNetwork) {
        replaceEquivalentModelByHvdc(referenceNetwork, marketBasedNetwork);
        operationalConditionAligner.align(referenceNetwork, marketBasedNetwork);
        replaceHvdcByEquivalentModel(referenceNetwork, marketBasedNetwork);
    }

    List<HvdcInformation> getHvdcInformationFromNetwork(Network network) {
        List<HvdcInformation> hvdcInformationList = new ArrayList<>();
        SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("/hvdc/SwePreprocessorParameters.json"));

        List<HvdcCreationParameters> sortedHvdcCreationParameters = params.getHvdcCreationParametersSet().stream()
                .sorted(Comparator.comparing(HvdcCreationParameters::getId)).toList();

        for (HvdcCreationParameters parameter : sortedHvdcCreationParameters) {
            HvdcInformation hvdcInformation = new HvdcInformation(parameter.getId());
            Optional<Line> line = Optional.ofNullable(network.getLine(parameter.getEquivalentAcLineId()));
            Optional<Generator> genSide1 = Optional.ofNullable(network.getGenerator(parameter.getEquivalentGeneratorId(TwoSides.ONE)));
            Optional<Generator> genSide2 = Optional.ofNullable(network.getGenerator(parameter.getEquivalentGeneratorId(TwoSides.TWO)));
            Optional<Load> loadSide1 = Optional.ofNullable(network.getLoad(parameter.getEquivalentLoadId(TwoSides.ONE).get(1)));
            Optional<Load> loadSide2 = Optional.ofNullable(network.getLoad(parameter.getEquivalentLoadId(TwoSides.TWO).get(1)));

            line.ifPresent(line1 -> {
                hvdcInformation.setAcLineTerminal1Connected(line1.getTerminal1().isConnected());
                hvdcInformation.setAcLineTerminal2Connected(line1.getTerminal2().isConnected());
            });

            genSide1.ifPresent(generator -> {
                hvdcInformation.setSide1GeneratorConnected(generator.getTerminal().isConnected());
                hvdcInformation.setSide1GeneratorTargetP(generator.getTargetP());
            });
            genSide2.ifPresent(generator -> {
                hvdcInformation.setSide2GeneratorConnected(generator.getTerminal().isConnected());
                hvdcInformation.setSide2GeneratorTargetP(generator.getTargetP());
            });

            loadSide1.ifPresentOrElse(
                    load -> {
                        hvdcInformation.setSide1LoadConnected(load.getTerminal().isConnected());
                        hvdcInformation.setSide1LoadP(load.getP0());
                    },
                    () -> Optional.ofNullable(network.getLoad(parameter.getEquivalentLoadId(TwoSides.ONE).get(2)))
                            .ifPresent(loadWithSecondOptionId -> {
                                hvdcInformation.setSide1LoadConnected(loadWithSecondOptionId.getTerminal().isConnected());
                                hvdcInformation.setSide1LoadP(loadWithSecondOptionId.getP0());
                            })
            );

            loadSide2.ifPresent(load -> {
                hvdcInformation.setSide2LoadConnected(load.getTerminal().isConnected());
                hvdcInformation.setSide2LoadP(load.getP0());
            });

            hvdcInformationList.add(hvdcInformation);
        }

        return hvdcInformationList;
    }
}
