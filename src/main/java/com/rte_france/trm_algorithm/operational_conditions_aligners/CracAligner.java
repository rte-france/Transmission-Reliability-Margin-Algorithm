/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.Identifiable;
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class CracAligner {
    private static final Logger LOGGER = LoggerFactory.getLogger(CracAligner.class);

    private CracAligner() {
        // utility class
    }

    public static Map<String, Boolean> align(Network referenceNetwork, Network marketBasedNetwork, Crac crac) {
        if (crac.getNetworkActions().isEmpty()) {
            LOGGER.error("Crac does not have any network actions");
        }
        return crac.getNetworkActions().stream()
            .collect(Collectors.toMap(
                Identifiable::getId,
                networkAction -> applyNetworkActions(referenceNetwork, marketBasedNetwork, networkAction)
            ));
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
}
