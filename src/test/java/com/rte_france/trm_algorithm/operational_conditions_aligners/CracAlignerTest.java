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
import com.powsybl.openrao.data.cracapi.CracFactory;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.openrao.data.craccreation.creator.api.CracCreators;
import com.powsybl.openrao.data.nativecracapi.NativeCrac;
import com.powsybl.openrao.data.nativecracioapi.NativeCracImporters;
import com.rte_france.trm_algorithm.TestUtils;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

class CracAlignerTest {

    @Test
    void testOneNetworkActionWithTwoTopologicalActions() {
        Network referenceNetwork = TestUtils.importNetwork("operational_conditions_aligners/pst/NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/pst/NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct");

        Crac crac = CracFactory.findDefault().create("crac");
        crac.newNetworkAction().withId("topo-action")
                .newTopologicalAction().withNetworkElement("FGEN  11 BLOAD 11 1").withActionType(ActionType.OPEN).add()
                .newTopologicalAction().withNetworkElement("FGEN  11 BLOAD 12 1").withActionType(ActionType.OPEN).add()
                .add();

        referenceNetwork.getLine("FGEN  11 BLOAD 12 1").disconnect();
        Map<String, Boolean> results = CracAligner.align(referenceNetwork, marketBasedNetwork, crac);
        assertTrue(marketBasedNetwork.getLine("FGEN  11 BLOAD 11 1").getTerminal1().isConnected());
        assertTrue(marketBasedNetwork.getLine("FGEN  11 BLOAD 11 1").getTerminal2().isConnected());
        assertTrue(marketBasedNetwork.getLine("FGEN  11 BLOAD 12 1").getTerminal1().isConnected());
        assertTrue(marketBasedNetwork.getLine("FGEN  11 BLOAD 12 1").getTerminal2().isConnected());
        assertEquals(1, results.size());
        assertFalse(results.get("topo-action"));
    }

    @Test
    void testTwoNetworkTopologicalActions() {
        Network referenceNetwork = TestUtils.importNetwork("operational_conditions_aligners/pst/NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/pst/NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct");

        Crac crac = CracFactory.findDefault().create("crac");
        crac.newNetworkAction().withId("topo-action")
                .newTopologicalAction().withNetworkElement("FGEN  11 BLOAD 12 1").withActionType(ActionType.OPEN).add().add();
        crac.newNetworkAction().withId("topo-action-2")
                .newTopologicalAction().withNetworkElement("FGEN  11 BLOAD 11 1").withActionType(ActionType.OPEN).add().add();

        crac.getNetworkAction("topo-action").apply(referenceNetwork);
        Map<String, Boolean> results = CracAligner.align(referenceNetwork, marketBasedNetwork, crac);
        assertTrue(marketBasedNetwork.getLine("FGEN  11 BLOAD 11 1").getTerminal1().isConnected());
        assertTrue(marketBasedNetwork.getLine("FGEN  11 BLOAD 11 1").getTerminal2().isConnected());
        assertFalse(marketBasedNetwork.getLine("FGEN  11 BLOAD 12 1").getTerminal1().isConnected());
        assertFalse(marketBasedNetwork.getLine("FGEN  11 BLOAD 12 1").getTerminal2().isConnected());
        assertEquals(2, results.size());
        assertTrue(results.get("topo-action"));
        assertFalse(results.get("topo-action-2"));
    }

    @Test
    void testTwoNetworkWithCracFile() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12NodesHvdc.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12NodesHvdc.uct");
        String cracFilePath = "../TestCase12Nodes/cbcora_ep10us2case1.xml";
        NativeCrac nativeCrac = NativeCracImporters.importData(cracFilePath, Objects.requireNonNull(getClass().getResourceAsStream(cracFilePath)));
        Crac referenceCrac = CracCreators.createCrac(nativeCrac, referenceNetwork, OffsetDateTime.of(2019, 1, 7, 23, 30, 0, 0, ZoneOffset.UTC)).getCrac();
        referenceCrac.getNetworkAction("Open FR1 FR2").apply(referenceNetwork);

        Map<String, Boolean> results = CracAligner.align(referenceNetwork, marketBasedNetwork, referenceCrac);
        assertFalse(marketBasedNetwork.getLine("FFR1AA1  FFR2AA1  1").getTerminal1().isConnected());
        assertFalse(marketBasedNetwork.getLine("FFR1AA1  FFR2AA1  1").getTerminal2().isConnected());
        assertEquals(1, results.size());
        assertTrue(results.get("Open FR1 FR2"));
    }

    @Test
    void testEmptyCrac() {
        Crac crac = CracFactory.findDefault().create("crac");
        Network referenceNetwork = TestUtils.importNetwork("operational_conditions_aligners/pst/NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/pst/NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct");
        Map<String, Boolean> results = CracAligner.align(referenceNetwork, marketBasedNetwork, crac);
        assertTrue(results.isEmpty());
    }
}
