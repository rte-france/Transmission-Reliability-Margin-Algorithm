/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.powsybl.iidm.network.Network;
import com.rte_france.trm_algorithm.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class PstAlignerTest {
    @Test
    void testOnePstAlignment() {
        Network referenceNetwork = TestUtils.importNetwork("operational_conditions_aligners/pst/NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/pst/NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct");
        String pstId = "BLOAD 11 BLOAD 12 2";
        referenceNetwork.getTwoWindingsTransformer(pstId).getPhaseTapChanger().setTapPosition(3);
        Map<String, Boolean> results = PstAligner.align(referenceNetwork, marketBasedNetwork);
        assertEquals(3, marketBasedNetwork.getTwoWindingsTransformer(pstId).getPhaseTapChanger().getTapPosition());
        assertEquals(1, results.size());
        assertTrue(results.get(pstId));
    }

    @Test
    void testTwoPstAlignment() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        String pstId1 = "BBE2AA1  BBE3AA1  1";
        String pstId2 = "FFR2AA1  FFR4AA1  1";
        referenceNetwork.getTwoWindingsTransformer(pstId1).getPhaseTapChanger().setTapPosition(5);
        referenceNetwork.getTwoWindingsTransformer(pstId2).getPhaseTapChanger().setTapPosition(-3);
        Map<String, Boolean> results = PstAligner.align(referenceNetwork, marketBasedNetwork);
        assertEquals(5, marketBasedNetwork.getTwoWindingsTransformer(pstId1).getPhaseTapChanger().getTapPosition());
        assertEquals(-3, marketBasedNetwork.getTwoWindingsTransformer(pstId2).getPhaseTapChanger().getTapPosition());
        assertEquals(2, results.size());
        assertTrue(results.get(pstId1));
        assertTrue(results.get(pstId2));
    }

    @Test
    void testTwoDifferentNetworksDoesNotAlign() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/pst/NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct");
        Map<String, Boolean> results = PstAligner.align(referenceNetwork, marketBasedNetwork);
        assertEquals(2, results.size());
        assertFalse(results.get("BBE2AA1  BBE3AA1  1"));
        assertFalse(results.get("FFR2AA1  FFR4AA1  1"));
    }
}
