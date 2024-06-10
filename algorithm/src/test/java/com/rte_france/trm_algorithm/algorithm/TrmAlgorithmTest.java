/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.algorithm;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracFactory;
import com.powsybl.sensitivity.SensitivityVariableSet;
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
    void testSameNetwork12Nodes() {
        Crac crac = CracFactory.findDefault().create("crac");
        Network network = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase12Nodes/glsk_proportional_12nodes.xml"));
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(network);
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        TrmAlgorithm trmAlgorithm = new TrmAlgorithm(loadFlowParameters);
        Map<String, Double> result = trmAlgorithm.computeUncertainties(network, marketBasedNetwork, zonalGlsks, crac);
        assertEquals(4, result.size());
        assertEquals(0.0, result.get("BBE2AA1  FFR3AA1  1"), EPSILON);
        assertEquals(0.0, result.get("DDE2AA1  NNL3AA1  1"), EPSILON);
        assertEquals(0.0, result.get("FFR2AA1  DDE3AA1  1"), EPSILON);
        assertEquals(0.0, result.get("NNL2AA1  BBE3AA1  1"), EPSILON);
    }

    @Test
    void testSameNetwork16Nodes() {
        Crac crac = CracFactory.findDefault().create("crac");
        Network network = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase16Nodes/glsk_proportional_16nodes.xml"));
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(network);
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        TrmAlgorithm trmAlgorithm = new TrmAlgorithm(loadFlowParameters);
        Map<String, Double> result = trmAlgorithm.computeUncertainties(network, marketBasedNetwork, zonalGlsks, crac);
        assertEquals(8, result.size());
        assertEquals(0.0, result.get("BBE1AA1  FFR5AA1  1"), EPSILON);
        assertEquals(0.0, result.get("BBE2AA1  FFR3AA1  1"), EPSILON);
        assertEquals(0.0, result.get("BBE4AA1  FFR5AA1  1"), EPSILON);
        assertEquals(0.0, result.get("DDE2AA1  NNL3AA1  1"), EPSILON);
        assertEquals(0.0, result.get("FFR2AA1  DDE3AA1  1"), EPSILON);
        assertEquals(0.0, result.get("FFR4AA1  DDE1AA1  1"), EPSILON);
        assertEquals(0.0, result.get("FFR4AA1  DDE4AA1  1"), EPSILON);
        assertEquals(0.0, result.get("NNL2AA1  BBE3AA1  1"), EPSILON);
    }

    @Test
    void testSameNetwork12NodesWithOtherGenerationPlan() {
        Crac crac = CracFactory.findDefault().create("crac");
        Network network = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        network.getLoad("NNL2AA1 _load").setP0(1500);
        network.getGenerator("DDE2AA1 _generator").setTargetP(2500);
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase12Nodes/glsk_proportional_12nodes.xml"));
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(network);
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        TrmAlgorithm trmAlgorithm = new TrmAlgorithm(loadFlowParameters);
        Map<String, Double> result = trmAlgorithm.computeUncertainties(network, marketBasedNetwork, zonalGlsks, crac);
        assertEquals(4, result.size());
        assertEquals(164.086, result.get("BBE2AA1  FFR3AA1  1"), EPSILON);
        assertEquals(-482.949, result.get("DDE2AA1  NNL3AA1  1"), EPSILON);
        assertEquals(169.909, result.get("FFR2AA1  DDE3AA1  1"), EPSILON);
        assertEquals(171.663, result.get("NNL2AA1  BBE3AA1  1"), EPSILON);
    }

    @Test
    void testSameNetwork16NodesWithOtherGenerationPlan() {
        Crac crac = CracFactory.findDefault().create("crac");
        Network network = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase16Nodes/glsk_proportional_16nodes.xml"));
        network.getLoad("NNL2AA1 _load").setP0(1500);
        network.getGenerator("DDE2AA1 _generator").setTargetP(2500);
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(network);
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        TrmAlgorithm trmAlgorithm = new TrmAlgorithm(loadFlowParameters);
        Map<String, Double> result = trmAlgorithm.computeUncertainties(network, marketBasedNetwork, zonalGlsks, crac);
        assertEquals(8, result.size());
        assertEquals(188.762, result.get("BBE1AA1  FFR5AA1  1"), EPSILON);
        assertEquals(214.796, result.get("BBE2AA1  FFR3AA1  1"), EPSILON);
        assertEquals(188.762, result.get("BBE4AA1  FFR5AA1  1"), EPSILON);
        assertEquals(-486.766, result.get("DDE2AA1  NNL3AA1  1"), EPSILON);
        assertEquals(271.978, result.get("FFR2AA1  DDE3AA1  1"), EPSILON);
        assertEquals(199.364, result.get("FFR4AA1  DDE1AA1  1"), EPSILON);
        assertEquals(118.164, result.get("FFR4AA1  DDE4AA1  1"), EPSILON);
        assertEquals(253.523, result.get("NNL2AA1  BBE3AA1  1"), EPSILON);
    }

    @Test
    void testSameNetwork12NodesWithDisconnectedLine() {
        Crac crac = CracFactory.findDefault().create("crac");
        Network network = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        network.getLine("BBE2AA1  FFR3AA1  1").disconnect();
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase12Nodes/glsk_proportional_12nodes.xml"));
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(network);
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        TrmAlgorithm trmAlgorithm = new TrmAlgorithm(loadFlowParameters);
        Map<String, Double> result = trmAlgorithm.computeUncertainties(network, marketBasedNetwork, zonalGlsks, crac);
        assertEquals(4, result.size());
        assertEquals(Double.NaN, result.get("BBE2AA1  FFR3AA1  1"), EPSILON);
        assertEquals(499.779, result.get("DDE2AA1  NNL3AA1  1"), EPSILON);
        assertEquals(499.779, result.get("FFR2AA1  DDE3AA1  1"), EPSILON);
        assertEquals(499.779, result.get("NNL2AA1  BBE3AA1  1"), EPSILON);
    }

    @Test
    void testSameNetwork16NodesWithDisconnectedLine() {
        Crac crac = CracFactory.findDefault().create("crac");
        Network network = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase16Nodes/glsk_proportional_16nodes.xml"));
        network.getLine("BBE1AA1  FFR5AA1  1").disconnect();
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(network);
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        TrmAlgorithm trmAlgorithm = new TrmAlgorithm(loadFlowParameters);
        Map<String, Double> result = trmAlgorithm.computeUncertainties(network, marketBasedNetwork, zonalGlsks, crac);
        assertEquals(8, result.size());
        assertEquals(Double.NaN, result.get("BBE1AA1  FFR5AA1  1"), EPSILON);
        assertEquals(-68.678, result.get("BBE2AA1  FFR3AA1  1"), EPSILON);
        assertEquals(-271.286, result.get("BBE4AA1  FFR5AA1  1"), EPSILON);
        assertEquals(16.027, result.get("DDE2AA1  NNL3AA1  1"), EPSILON);
        assertEquals(17.719, result.get("FFR2AA1  DDE3AA1  1"), EPSILON);
        assertEquals(12.822, result.get("FFR4AA1  DDE1AA1  1"), EPSILON);
        assertEquals(7.433, result.get("FFR4AA1  DDE4AA1  1"), EPSILON);
        assertEquals(16.353, result.get("NNL2AA1  BBE3AA1  1"), EPSILON);
    }

    @Test
    void testDifferentNetwork() {
        Crac crac = CracFactory.findDefault().create("crac");
        Network network = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/pst/NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct");
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        TrmAlgorithm trmAlgorithm = new TrmAlgorithm(loadFlowParameters);
        ZonalData<SensitivityVariableSet> zonalGlsks = null;
        TrmException exception = assertThrows(TrmException.class, () -> trmAlgorithm.computeUncertainties(network, marketBasedNetwork, zonalGlsks, crac));
        assertTrue(exception.getMessage().contains("Market-based network doesn't contain the following elements:"));
    }

    @Test
    void testSameNetwork16NodesWithAutoGlskAndHvdcDisconnected() {
        Crac crac = CracFactory.findDefault().create("crac");
        Network network = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWith2Hvdc.xiidm");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWith2Hvdc.xiidm");
        network.getHvdcLine("BBE2BB11 FFR3AA11 1").getConverterStation1().disconnect();
        network.getHvdcLine("BBE2BB11 FFR3AA11 1").getConverterStation2().disconnect();
        ZonalData<SensitivityVariableSet> zonalGlsks = TestUtils.getAutoGlsk(network);
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        TrmAlgorithm trmAlgorithm = new TrmAlgorithm(loadFlowParameters);
        Map<String, Double> result = trmAlgorithm.computeUncertainties(network, marketBasedNetwork, zonalGlsks, crac);
        assertEquals(7, result.size());
        assertEquals(-811.963, result.get("BBE1AA11 FFR5AA11 1"), EPSILON);
        assertEquals(-811.963, result.get("BBE4AA11 FFR5AA11 1"), EPSILON);
        assertEquals(265.578, result.get("FFR4AA11 DDE1AA11 1"), EPSILON);
        assertEquals(316.587, result.get("NNL2AA11 BBE3AA11 1"), EPSILON);
        assertEquals(365.962, result.get("FFR2AA11 DDE3AA11 1"), EPSILON);
        assertEquals(314.792, result.get("DDE2AA11 NNL3AA11 1"), EPSILON);
        assertEquals(154.453, result.get("FFR4AA11 DDE4AA11 1"), EPSILON);
    }

    @Test
    void testSameNetwork16NodesWithDisconnectedReconnectedLine() {
        Network network = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase16Nodes/glsk_proportional_16nodes.xml"));
        network.getLine("BBE1AA1  FFR5AA1  1").disconnect();
        marketBasedNetwork.getLine("BBE4AA1  FFR5AA1  1").disconnect(); // This line will be reconnected
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(network);
        Crac crac = TestUtils.getIdealTopologicalAlignerCrac(network);
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        TrmAlgorithm trmAlgorithm = new TrmAlgorithm(loadFlowParameters);
        Map<String, Double> result = trmAlgorithm.computeUncertainties(network, marketBasedNetwork, zonalGlsks, crac);
        assertEquals(8, result.size());
        assertEquals(Double.NaN, result.get("BBE1AA1  FFR5AA1  1"), EPSILON); // This line is still open
        assertEquals(0.0, result.get("BBE2AA1  FFR3AA1  1"), EPSILON);
        assertEquals(0.0, result.get("BBE4AA1  FFR5AA1  1"), EPSILON);
        assertEquals(0.0, result.get("DDE2AA1  NNL3AA1  1"), EPSILON);
        assertEquals(0.0, result.get("FFR2AA1  DDE3AA1  1"), EPSILON);
        assertEquals(0.0, result.get("FFR4AA1  DDE1AA1  1"), EPSILON);
        assertEquals(0.0, result.get("FFR4AA1  DDE4AA1  1"), EPSILON);
        assertEquals(0.0, result.get("NNL2AA1  BBE3AA1  1"), EPSILON);
    }
}
