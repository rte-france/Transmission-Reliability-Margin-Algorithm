/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.rte_france.trm_algorithm.algorithm.operational_conditions_aligners;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracFactory;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.rte_france.trm_algorithm.algorithm.TestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

class CracAlignerTest {

    @Test
    void testOneNetworkActionWithTwoTopologicalActions() {
        Network network = TestUtils.importNetwork("operational_conditions_aligners/pst/NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/pst/NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct");

        Crac crac = CracFactory.findDefault().create("crac");
        crac.newNetworkAction().withId("topo-action")
                .newTopologicalAction().withNetworkElement("FGEN  11 BLOAD 11 1").withActionType(ActionType.OPEN).add()
                .newTopologicalAction().withNetworkElement("FGEN  11 BLOAD 12 1").withActionType(ActionType.OPEN).add()
                .add();

        network.getLine("FGEN  11 BLOAD 12 1").disconnect();
        CracAligner.align(network, marketBasedNetwork, crac);
        assertTrue(marketBasedNetwork.getLine("FGEN  11 BLOAD 11 1").getTerminal1().isConnected());
        assertTrue(marketBasedNetwork.getLine("FGEN  11 BLOAD 11 1").getTerminal2().isConnected());
        assertTrue(marketBasedNetwork.getLine("FGEN  11 BLOAD 12 1").getTerminal1().isConnected());
        assertTrue(marketBasedNetwork.getLine("FGEN  11 BLOAD 12 1").getTerminal2().isConnected());
    }

    @Test
    void testTwoNetworkTopologicalActions() {
        Network network = TestUtils.importNetwork("operational_conditions_aligners/pst/NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/pst/NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct");

        Crac crac = CracFactory.findDefault().create("crac");
        crac.newNetworkAction().withId("topo-action")
                .newTopologicalAction().withNetworkElement("FGEN  11 BLOAD 12 1").withActionType(ActionType.OPEN).add().add();
        crac.newNetworkAction().withId("topo-action-2")
                .newTopologicalAction().withNetworkElement("FGEN  11 BLOAD 11 1").withActionType(ActionType.OPEN).add().add();

        crac.getNetworkAction("topo-action").apply(network);
        CracAligner.align(network, marketBasedNetwork, crac);
        assertTrue(marketBasedNetwork.getLine("FGEN  11 BLOAD 11 1").getTerminal1().isConnected());
        assertTrue(marketBasedNetwork.getLine("FGEN  11 BLOAD 11 1").getTerminal2().isConnected());
        assertFalse(marketBasedNetwork.getLine("FGEN  11 BLOAD 12 1").getTerminal1().isConnected());
        assertFalse(marketBasedNetwork.getLine("FGEN  11 BLOAD 12 1").getTerminal2().isConnected());
    }
}
