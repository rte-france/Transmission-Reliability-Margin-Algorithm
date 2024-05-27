/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.algorithm;

import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class TrmAlgorithmTest {
    private static final double EPSILON = 1e-3;

    @Test
    void testSameNetwork() {
        Network network = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWithHvdc.xiidm");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWithHvdc.xiidm");
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        TrmAlgorithm trmAlgorithm = new TrmAlgorithm(loadFlowParameters);
        Map<String, Double> result = trmAlgorithm.run(network, marketBasedNetwork);
        assertEquals(7, result.size());
        assertEquals(0.0, result.get("BBE1AA11 FFR5AA11 1"), EPSILON);
        assertEquals(0.0, result.get("BBE4AA11 FFR5AA11 1"), EPSILON);
        assertEquals(0.0, result.get("FFR4AA11 DDE1AA11 1"), EPSILON);
        assertEquals(0.0, result.get("NNL2AA11 BBE3AA11 1"), EPSILON);
        assertEquals(0.0, result.get("FFR2AA11 DDE3AA11 1"), EPSILON);
        assertEquals(0.0, result.get("DDE2AA11 NNL3AA11 1"), EPSILON);
        assertEquals(0.0, result.get("FFR4AA11 DDE4AA11 1"), EPSILON);
    }

    @Test
    void testSameNetworkWithOtherGenerationPlan() {
        Network network = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWithHvdc.xiidm");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWithHvdc.xiidm");
        network.getLoad("NNL2AA11_load").setP0(1500);
        network.getGenerator("DDE2AA11_generator").setTargetP(2500);
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        TrmAlgorithm trmAlgorithm = new TrmAlgorithm(loadFlowParameters);
        Map<String, Double> result = trmAlgorithm.run(network, marketBasedNetwork);
        assertEquals(7, result.size());
        assertEquals(71.436, result.get("BBE1AA11 FFR5AA11 1"), EPSILON);
        assertEquals(71.436, result.get("BBE4AA11 FFR5AA11 1"), EPSILON);
        assertEquals(46.196, result.get("FFR4AA11 DDE1AA11 1"), EPSILON);
        assertEquals(142.872, result.get("NNL2AA11 BBE3AA11 1"), EPSILON);
        assertEquals(73.574, result.get("FFR2AA11 DDE3AA11 1"), EPSILON);
        assertEquals(-357.128, result.get("DDE2AA11 NNL3AA11 1"), EPSILON);
        assertEquals(23.102, result.get("FFR4AA11 DDE4AA11 1"), EPSILON);
    }

    @Test
    void testSameNetworkWithDisconnectedLine() {
        Network network = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWithHvdc.xiidm");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWithHvdc.xiidm");
        network.getLine("BBE1AA11 FFR5AA11 1").disconnect();
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        TrmAlgorithm trmAlgorithm = new TrmAlgorithm(loadFlowParameters);
        Map<String, Double> result = trmAlgorithm.run(network, marketBasedNetwork);
        assertEquals(7, result.size());
        assertEquals(Double.NaN, result.get("BBE1AA11 FFR5AA11 1"), EPSILON);
        assertEquals(-426.120, result.get("BBE4AA11 FFR5AA11 1"), EPSILON);
        assertEquals(25.829, result.get("FFR4AA11 DDE1AA11 1"), EPSILON);
        assertEquals(79.883, result.get("NNL2AA11 BBE3AA11 1"), EPSILON);
        assertEquals(41.137, result.get("FFR2AA11 DDE3AA11 1"), EPSILON);
        assertEquals(79.883, result.get("DDE2AA11 NNL3AA11 1"), EPSILON);
        assertEquals(12.917, result.get("FFR4AA11 DDE4AA11 1"), EPSILON);
    }

    @Test
    void testDifferentNetwork() {
        Network network = TestUtils.importNetwork("operational_conditions_aligners/pst/TestCase16Nodes_alignedPsts.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/pst/NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct");
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        TrmAlgorithm trmAlgorithm = new TrmAlgorithm(loadFlowParameters);
        TrmException exception = assertThrows(TrmException.class, () -> trmAlgorithm.run(network, marketBasedNetwork));
        assertTrue(exception.getMessage().contains("Market-based network doesn't contain the following elements:"));
    }
}
