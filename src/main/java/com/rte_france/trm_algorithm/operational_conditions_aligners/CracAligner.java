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
import com.powsybl.openrao.data.cracapi.networkaction.NetworkAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

public final class CracAligner {

    private static final Logger LOGGER = LoggerFactory.getLogger(CracAligner.class);

    private CracAligner() { } // utility class

    public static void align(Network referenceNetwork, Network marketBasedNetwork, Crac crac) {
        crac.getNetworkActions().forEach(networkAction -> applyNetworkActions(referenceNetwork, marketBasedNetwork, networkAction));
    }

    private static void applyNetworkActions(Network referenceNetwork, Network marketBasedNetwork, NetworkAction networkAction) {
        if (!networkAction.hasImpactOnNetwork(referenceNetwork) && networkAction.hasImpactOnNetwork(marketBasedNetwork)) {
            networkAction.apply(marketBasedNetwork);
            LOGGER.info("Network Action {} has been applied to market based network.", networkAction);
        } else {
            LOGGER.debug("Network Action {} has NOT been applied to market based network.", networkAction);
        }
    }
}
