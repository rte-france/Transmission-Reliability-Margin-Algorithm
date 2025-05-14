/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.networkaction.NetworkAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class CracAligner implements OperationalConditionAligner {
    private static final Logger LOGGER = LoggerFactory.getLogger(CracAligner.class);
    private final Crac crac;
    private Map<String, Boolean> cracAlignementResult = new HashMap<>();

    public CracAligner(Crac crac) {
        Objects.requireNonNull(crac);
        this.crac = crac;
    }

    private static boolean applyNetworkActions(Network referenceNetwork, Network marketBasedNetwork, NetworkAction networkAction) {
        if (!networkAction.hasImpactOnNetwork(referenceNetwork) && networkAction.hasImpactOnNetwork(marketBasedNetwork)) {
            networkAction.apply(marketBasedNetwork);
            LOGGER.info("Network Action '{}' has been applied to market based network.", networkAction);
            return true;
        } else {
            LOGGER.debug("Network Action '{}' has NOT been applied to market based network.", networkAction);
            return false;
        }
    }

    public Map<String, Boolean> getResult() {
        return cracAlignementResult;
    }

    @Override
    public void align(Network referenceNetwork, Network marketBasedNetwork) {
        if (crac.getNetworkActions().isEmpty()) {
            LOGGER.error("Crac does not have any network actions");
        }
        LOGGER.info("Aligning CRAC network actions");
        cracAlignementResult = crac.getNetworkActions().stream()
            .collect(Collectors.toMap(
                NetworkAction::getId,
                networkAction -> applyNetworkActions(referenceNetwork, marketBasedNetwork, networkAction)
            ));
    }
}
