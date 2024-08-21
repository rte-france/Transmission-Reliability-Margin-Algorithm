/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.farao_community.farao.gridcapa_swe_commons.hvdc.HvdcLinkProcessor;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.HvdcCreationParameters;
import com.powsybl.iidm.network.Network;
import com.rte_france.trm_algorithm.TrmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class HvdcAcModelingEnvironment implements OperationalConditionAligner {
    private static final Logger LOGGER = LoggerFactory.getLogger(HvdcAcModelingEnvironment.class);

    private final Set<HvdcCreationParameters> creationParametersSet;
    private final OperationalConditionAligner operationalConditionAligner;

    public HvdcAcModelingEnvironment(Set<HvdcCreationParameters> creationParametersSet, OperationalConditionAligner operationalConditionAligner) {
        creationParametersSet.stream()
            .filter(hvdcCreationParameters -> Objects.isNull(hvdcCreationParameters.getAngleDroopActivePowerControlParameters()))
            .forEach(hvdcCreationParameters -> {
                throw new TrmException(String.format("HVDC \"%s\" has a DC control mode.", hvdcCreationParameters.getId()));
            });
        this.creationParametersSet = creationParametersSet;
        this.operationalConditionAligner = operationalConditionAligner;
    }

    private void replaceEquivalentModelByHvdc(Network referenceNetwork, Network marketBasedNetwork) {
        LOGGER.info("Transform HVDC AC equivalent model to HVDC");
        HvdcLinkProcessor.replaceEquivalentModelByHvdc(referenceNetwork, creationParametersSet);
        HvdcLinkProcessor.replaceEquivalentModelByHvdc(marketBasedNetwork, creationParametersSet);
    }

    private void replaceHvdcByEquivalentModel(Network referenceNetwork, Network marketBasedNetwork) {
        LOGGER.info("Transform HVDC to their AC equivalent models");
        HvdcLinkProcessor.replaceHvdcByEquivalentModel(referenceNetwork, creationParametersSet);
        HvdcLinkProcessor.replaceHvdcByEquivalentModel(marketBasedNetwork, creationParametersSet);
    }

    @Override
    public void align(Network referenceNetwork, Network marketBasedNetwork) {
        replaceEquivalentModelByHvdc(referenceNetwork, marketBasedNetwork);
        operationalConditionAligner.align(referenceNetwork, marketBasedNetwork);
        replaceHvdcByEquivalentModel(referenceNetwork, marketBasedNetwork);
    }
}
