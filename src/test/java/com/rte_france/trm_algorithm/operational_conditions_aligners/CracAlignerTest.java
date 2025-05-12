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
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.api.networkaction.ActionType;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.fbconstraint.parameters.FbConstraintCracCreationParameters;
import com.rte_france.trm_algorithm.TestUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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
            .newTerminalsConnectionAction().withNetworkElement("FGEN  11 BLOAD 11 1").withActionType(ActionType.OPEN).add()
            .newTerminalsConnectionAction().withNetworkElement("FGEN  11 BLOAD 12 1").withActionType(ActionType.OPEN).add()
            .add();

        referenceNetwork.getLine("FGEN  11 BLOAD 12 1").disconnect();
        CracAligner cracAligner = new CracAligner(crac);
        cracAligner.align(referenceNetwork, marketBasedNetwork);
        Map<String, Boolean> results = cracAligner.getResult();
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
            .newTerminalsConnectionAction().withNetworkElement("FGEN  11 BLOAD 12 1").withActionType(ActionType.OPEN).add().add();
        crac.newNetworkAction().withId("topo-action-2")
            .newTerminalsConnectionAction().withNetworkElement("FGEN  11 BLOAD 11 1").withActionType(ActionType.OPEN).add().add();

        crac.getNetworkAction("topo-action").apply(referenceNetwork);
        CracAligner cracAligner = new CracAligner(crac);
        cracAligner.align(referenceNetwork, marketBasedNetwork);
        Map<String, Boolean> results = cracAligner.getResult();
        assertTrue(marketBasedNetwork.getLine("FGEN  11 BLOAD 11 1").getTerminal1().isConnected());
        assertTrue(marketBasedNetwork.getLine("FGEN  11 BLOAD 11 1").getTerminal2().isConnected());
        assertFalse(marketBasedNetwork.getLine("FGEN  11 BLOAD 12 1").getTerminal1().isConnected());
        assertFalse(marketBasedNetwork.getLine("FGEN  11 BLOAD 12 1").getTerminal2().isConnected());
        assertEquals(2, results.size());
        assertTrue(results.get("topo-action"));
        assertFalse(results.get("topo-action-2"));
    }

    @Test
    void testTwoNetworkWithCracFile() throws IOException {
        Network referenceNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12NodesHvdc.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12NodesHvdc.uct");
        String cracFilePath = "../TestCase12Nodes/cbcora_ep10us2case1.xml";
        CracCreationParameters parameters = new CracCreationParameters();
        parameters.addExtension(FbConstraintCracCreationParameters.class, new FbConstraintCracCreationParameters());
        parameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(OffsetDateTime.of(2019, 1, 7, 23, 30, 0, 0, ZoneOffset.UTC));
        Crac crac = Crac.read(cracFilePath, Objects.requireNonNull(getClass().getResourceAsStream(cracFilePath)), referenceNetwork, parameters);
        crac.getNetworkAction("Open FR1 FR2").apply(referenceNetwork);

        CracAligner cracAligner = new CracAligner(crac);
        cracAligner.align(referenceNetwork, marketBasedNetwork);
        Map<String, Boolean> results = cracAligner.getResult();
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
        CracAligner cracAligner = new CracAligner(crac);
        cracAligner.align(referenceNetwork, marketBasedNetwork);
        Map<String, Boolean> results = cracAligner.getResult();
        assertTrue(results.isEmpty());
    }
}
