/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import com.powsybl.balances_adjustment.balance_computation.BalanceComputationParameters;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.powsybl.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracFactory;
import com.powsybl.openrao.data.craccreation.creator.api.CracCreators;
import com.powsybl.openrao.data.nativecracapi.NativeCrac;
import com.powsybl.openrao.data.nativecracioapi.NativeCracImporters;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class TrmAlgorithmTest {
    private static final double EPSILON = 1e-3;
    private TrmAlgorithm trmAlgorithm;
    private ZonalDataImpl<Scalable> marketZonalScalable;
    private Crac crac;

    @BeforeEach
    void setUp() {
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        BalanceComputationParameters balanceComputationParameters = new BalanceComputationParameters();
        LoadFlow.Runner loadFlowRunner = LoadFlow.find();
        ComputationManager computationManager = LocalComputationManager.getDefault();
        trmAlgorithm = new TrmAlgorithm(loadFlowParameters, balanceComputationParameters, loadFlowRunner, computationManager);

        crac = CracFactory.findDefault().create("crac");

        marketZonalScalable = new ZonalDataImpl<>(Collections.emptyMap());
    }

    @Test
    void testSameNetwork12NodesAutoGlsk() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        ZonalData<SensitivityVariableSet> zonalGlsks = TrmUtils.getAutoGlsk(referenceNetwork);
        ZonalData<Scalable> marketZonalScalable = TrmUtils.getAutoScalable(marketBasedNetwork);
        Map<String, Double> result = trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, zonalGlsks, crac, marketZonalScalable);
        assertEquals(4, result.size());
        assertEquals(0.0, result.get("BBE2AA1  FFR3AA1  1"), EPSILON);
        assertEquals(0.0, result.get("DDE2AA1  NNL3AA1  1"), EPSILON);
        assertEquals(0.0, result.get("FFR2AA1  DDE3AA1  1"), EPSILON);
        assertEquals(0.0, result.get("NNL2AA1  BBE3AA1  1"), EPSILON);
    }

    @Test
    void testSameNetwork12Nodes() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase12Nodes/glsk_proportional_12nodes.xml"));
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(referenceNetwork);
        ZonalData<Scalable> marketZonalScalable = ucteGlskDocument.getZonalScalable(marketBasedNetwork);
        Map<String, Double> result = trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, zonalGlsks, crac, marketZonalScalable);
        assertEquals(4, result.size());
        assertEquals(0.0, result.get("BBE2AA1  FFR3AA1  1"), EPSILON);
        assertEquals(0.0, result.get("DDE2AA1  NNL3AA1  1"), EPSILON);
        assertEquals(0.0, result.get("FFR2AA1  DDE3AA1  1"), EPSILON);
        assertEquals(0.0, result.get("NNL2AA1  BBE3AA1  1"), EPSILON);
    }

    @Test
    void testSameNetwork16Nodes() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase16Nodes/glsk_proportional_16nodes.xml"));
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(referenceNetwork);
        ZonalData<Scalable> marketZonalScalable = ucteGlskDocument.getZonalScalable(marketBasedNetwork);
        Map<String, Double> result = trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, zonalGlsks, crac, marketZonalScalable);
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
        Network referenceNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        referenceNetwork.getLoad("NNL2AA1 _load").setP0(1500);
        referenceNetwork.getGenerator("DDE2AA1 _generator").setTargetP(2500);
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase12Nodes/glsk_proportional_12nodes.xml"));
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(referenceNetwork);
        ZonalData<Scalable> marketZonalScalable = ucteGlskDocument.getZonalScalable(marketBasedNetwork);
        Map<String, Double> result = trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, zonalGlsks, crac, marketZonalScalable);
        assertEquals(4, result.size());
        assertEquals(15.051, result.get("BBE2AA1  FFR3AA1  1"), EPSILON);
        assertEquals(14.745, result.get("DDE2AA1  NNL3AA1  1"), EPSILON);
        assertEquals(15.585, result.get("FFR2AA1  DDE3AA1  1"), EPSILON);
        assertEquals(15.746, result.get("NNL2AA1  BBE3AA1  1"), EPSILON);
    }

    @Test
    void testSameNetwork16NodesWithOtherGenerationPlan() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase16Nodes/glsk_proportional_16nodes.xml"));
        referenceNetwork.getLoad("NNL2AA1 _load").setP0(1500);
        referenceNetwork.getGenerator("DDE2AA1 _generator").setTargetP(2500);
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(referenceNetwork);
        ZonalData<Scalable> marketZonalScalable = ucteGlskDocument.getZonalScalable(marketBasedNetwork);
        Map<String, Double> result = trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, zonalGlsks, crac, marketZonalScalable);
        assertEquals(8, result.size());
        assertEquals(7.294, result.get("BBE1AA1  FFR5AA1  1"), EPSILON);
        assertEquals(8.300, result.get("BBE2AA1  FFR3AA1  1"), EPSILON);
        assertEquals(7.294, result.get("BBE4AA1  FFR5AA1  1"), EPSILON);
        assertEquals(9.536, result.get("DDE2AA1  NNL3AA1  1"), EPSILON);
        assertEquals(157.116, result.get("FFR2AA1  DDE3AA1  1"), EPSILON);
        assertEquals(15.829, result.get("FFR4AA1  DDE1AA1  1"), EPSILON);
        assertEquals(-207.855, result.get("FFR4AA1  DDE4AA1  1"), EPSILON);
        assertEquals(9.797, result.get("NNL2AA1  BBE3AA1  1"), EPSILON);
    }

    @Test
    void testSameNetwork12NodesWithDisconnectedLine() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        referenceNetwork.getLine("BBE2AA1  FFR3AA1  1").disconnect();
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase12Nodes/glsk_proportional_12nodes.xml"));
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(referenceNetwork);
        ZonalData<Scalable> marketZonalScalable = ucteGlskDocument.getZonalScalable(marketBasedNetwork);
        Map<String, Double> result = trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, zonalGlsks, crac, marketZonalScalable);
        assertEquals(4, result.size());
        assertEquals(Double.NaN, result.get("BBE2AA1  FFR3AA1  1"), EPSILON);
        assertEquals(499.779, result.get("DDE2AA1  NNL3AA1  1"), EPSILON);
        assertEquals(499.779, result.get("FFR2AA1  DDE3AA1  1"), EPSILON);
        assertEquals(499.779, result.get("NNL2AA1  BBE3AA1  1"), EPSILON);
    }

    @Test
    void testSameNetwork16NodesWithDisconnectedLine() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase16Nodes/glsk_proportional_16nodes.xml"));
        referenceNetwork.getLine("BBE1AA1  FFR5AA1  1").disconnect();
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(referenceNetwork);
        ZonalData<Scalable> marketZonalScalable = ucteGlskDocument.getZonalScalable(marketBasedNetwork);
        Map<String, Double> result = trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, zonalGlsks, crac, marketZonalScalable);
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
        Network referenceNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/pst/NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct");
        ZonalData<SensitivityVariableSet> zonalGlsks = null;
        TrmException exception = assertThrows(TrmException.class, () -> trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, zonalGlsks, crac, marketZonalScalable));
        assertTrue(exception.getMessage().contains("Market-based critical network elements doesn't contain the following elements: ["));
    }

    @Test
    void testSameNetwork16NodesWithAutoGlskAndHvdcDisconnected() {
        Network referenceNetwork = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWith2Hvdc.xiidm");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWith2Hvdc.xiidm");
        referenceNetwork.getHvdcLine("BBE2BB11 FFR3AA11 1").getConverterStation1().disconnect();
        referenceNetwork.getHvdcLine("BBE2BB11 FFR3AA11 1").getConverterStation2().disconnect();
        ZonalData<SensitivityVariableSet> zonalGlsks = TrmUtils.getAutoGlsk(referenceNetwork);
        ZonalData<Scalable> marketZonalScalable = TrmUtils.getAutoScalable(marketBasedNetwork);
        Map<String, Double> result = trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, zonalGlsks, crac, marketZonalScalable);
        assertEquals(7, result.size());
        assertEquals(-817.366, result.get("BBE1AA11 FFR5AA11 1"), EPSILON);
        assertEquals(-817.366, result.get("BBE4AA11 FFR5AA11 1"), EPSILON);
        assertEquals(267.713, result.get("FFR4AA11 DDE1AA11 1"), EPSILON);
        assertEquals(318.732, result.get("NNL2AA11 BBE3AA11 1"), EPSILON);
        assertEquals(368.005, result.get("FFR2AA11 DDE3AA11 1"), EPSILON);
        assertEquals(316.924, result.get("DDE2AA11 NNL3AA11 1"), EPSILON);
        assertEquals(155.695, result.get("FFR4AA11 DDE4AA11 1"), EPSILON);
    }

    @Test
    void testSameNetwork16NodesWithDisconnectedReconnectedLine() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase16Nodes/glsk_proportional_16nodes.xml"));
        referenceNetwork.getLine("BBE1AA1  FFR5AA1  1").disconnect();
        marketBasedNetwork.getLine("BBE4AA1  FFR5AA1  1").disconnect(); // This line will be reconnected
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(referenceNetwork);
        Crac crac = TestUtils.getIdealTopologicalAlignerCrac(referenceNetwork);
        ZonalData<Scalable> marketZonalScalable = ucteGlskDocument.getZonalScalable(marketBasedNetwork);
        Map<String, Double> result = trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, zonalGlsks, crac, marketZonalScalable);
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

    @Test
    void testTwoNetworkWithCracFile() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12NodesHvdc.uct");
        referenceNetwork.getLine("NNL2AA1  BBE3AA1  1").disconnect();
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12NodesHvdc.uct");
        ZonalData<SensitivityVariableSet> zonalGlsks = TrmUtils.getAutoGlsk(referenceNetwork);
        String cracFilePath = "TestCase12Nodes/cbcora_ep10us2case1.xml";
        NativeCrac nativeCrac = NativeCracImporters.importData(cracFilePath, Objects.requireNonNull(getClass().getResourceAsStream(cracFilePath)));
        Crac crac = CracCreators.createCrac(nativeCrac, referenceNetwork, OffsetDateTime.of(2019, 1, 7, 23, 30, 0, 0, ZoneOffset.UTC)).getCrac();
        crac.getNetworkAction("Open FR1 FR2").apply(referenceNetwork);
        ZonalData<Scalable> marketZonalScalable = TrmUtils.getAutoScalable(marketBasedNetwork);
        Map<String, Double> result = trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, zonalGlsks, crac, marketZonalScalable);
        assertEquals(4, result.size());
        assertEquals(-1383.344, result.get("BBE2AA1  FFR3AA1  1"), EPSILON);
        assertEquals(-1383.344, result.get("FFR2AA1  DDE3AA1  1"), EPSILON);
        assertEquals(Double.NaN, result.get("NNL2AA1  BBE3AA1  1"), EPSILON);
        assertEquals(-1383.344, result.get("DDE2AA1  NNL3AA1  1"), EPSILON);
    }

    @Test
    void testNetworkWithoutInterconnection() {
        Network referenceNetwork = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");
        referenceNetwork.getLine("FGEN1 11 BLOAD 11 1").remove();
        Network marketBasedNetwork = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");
        ZonalData<SensitivityVariableSet> zonalGlsks = TrmUtils.getAutoGlsk(referenceNetwork);
        ZonalData<Scalable> marketZonalScalable = TrmUtils.getAutoScalable(marketBasedNetwork);
        TrmException trmException = assertThrows(TrmException.class, () -> trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, zonalGlsks, crac, marketZonalScalable));
        assertEquals("Reference critical network elements are empty", trmException.getMessage());
    }

    @Test
    void testReferenceNetworkSubpartOfMarketBasedNetwork() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        referenceNetwork.getLine("FFR2AA1  DDE3AA1  1").remove();
        referenceNetwork.getLine("BBE2AA1  FFR3AA1  1").remove();
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        ZonalData<SensitivityVariableSet> zonalGlsks = TrmUtils.getAutoGlsk(referenceNetwork);
        ZonalData<Scalable> marketZonalScalable = TrmUtils.getAutoScalable(marketBasedNetwork);
        Map<String, Double> result = trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, zonalGlsks, crac, marketZonalScalable);
        assertEquals(2, result.size());
        assertEquals(1260.669, result.get("NNL2AA1  BBE3AA1  1"), EPSILON);
        assertEquals(1260.669, result.get("DDE2AA1  NNL3AA1  1"), EPSILON);
    }
}
