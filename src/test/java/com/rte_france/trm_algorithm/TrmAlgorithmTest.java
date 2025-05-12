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
import com.powsybl.flow_decomposition.XnecProvider;
import com.powsybl.flow_decomposition.xnec_provider.XnecProvider5percPtdf;
import com.powsybl.flow_decomposition.xnec_provider.XnecProviderByIds;
import com.powsybl.flow_decomposition.xnec_provider.XnecProviderInterconnection;
import com.powsybl.flow_decomposition.xnec_provider.XnecProviderUnion;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.powsybl.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.powsybl.openrao.data.crac.api.parameters.CracCreationParameters;
import com.powsybl.openrao.data.crac.io.fbconstraint.parameters.FbConstraintCracCreationParameters;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.rte_france.trm_algorithm.operational_conditions_aligners.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class TrmAlgorithmTest {
    private static final double EPSILON = 1e-3;

    TrmAlgorithm setUp(Crac crac, ZonalData<Scalable> marketZonalScalable) {
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        BalanceComputationParameters balanceComputationParameters = new BalanceComputationParameters();
        LoadFlow.Runner loadFlowRunner = LoadFlow.find();
        ComputationManager computationManager = LocalComputationManager.getDefault();

        CracAligner cracAligner = new CracAligner(crac);
        HvdcAligner hvdcAligner = new HvdcAligner();
        PstAligner pstAligner = new PstAligner();
        DanglingLineAligner danglingLineAligner = new DanglingLineAligner();
        ExchangeAligner exchangeAligner = new ExchangeAligner(balanceComputationParameters, loadFlowRunner, computationManager, marketZonalScalable);
        OperationalConditionAligner operationalConditionAligner = new OperationalConditionAlignerPipeline(cracAligner, hvdcAligner, pstAligner, danglingLineAligner, exchangeAligner);
        return new TrmAlgorithm(loadFlowParameters, operationalConditionAligner);
    }

    @Test
    void testSameNetwork12NodesAutoGlsk() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        ZonalData<SensitivityVariableSet> zonalGlsks = TrmUtils.getAutoGlsk(referenceNetwork);
        ZonalData<Scalable> localMarketZonalScalable = TrmUtils.getAutoScalable(marketBasedNetwork);
        XnecProvider xnecProvider = new XnecProviderInterconnection();
        TrmAlgorithm trmAlgorithm = setUp(CracFactory.findDefault().create("crac"), localMarketZonalScalable);
        TrmResults trmResults = trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, xnecProvider, zonalGlsks);
        Map<String, UncertaintyResult> result = trmResults.getUncertaintiesMap();
        assertEquals(4, result.size());
        assertEquals(0.0, result.get("BBE2AA1  FFR3AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("DDE2AA1  NNL3AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("FFR2AA1  DDE3AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("NNL2AA1  BBE3AA1  1").getUncertainty(), EPSILON);
    }

    @Test
    void testSameNetwork12NodesAutoGlskAndZonalPtdfSelection() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        ZonalData<SensitivityVariableSet> zonalGlsks = TrmUtils.getAutoGlsk(referenceNetwork);
        ZonalData<Scalable> localMarketZonalScalable = TrmUtils.getAutoScalable(marketBasedNetwork);
        XnecProvider xnecProvider = new XnecProvider5percPtdf();
        TrmAlgorithm trmAlgorithm = setUp(CracFactory.findDefault().create("crac"), localMarketZonalScalable);
        TrmResults trmResults = trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, xnecProvider, zonalGlsks);
        Map<String, UncertaintyResult> result = trmResults.getUncertaintiesMap();
        assertEquals(16, result.size());
        assertEquals(0.0, result.get("BBE1AA1  BBE2AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("BBE1AA1  BBE3AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("BBE2AA1  BBE3AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("BBE2AA1  FFR3AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("DDE1AA1  DDE2AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("DDE1AA1  DDE3AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("DDE2AA1  DDE3AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("DDE2AA1  NNL3AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("FFR1AA1  FFR2AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("FFR1AA1  FFR3AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("FFR2AA1  DDE3AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("FFR2AA1  FFR3AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("NNL1AA1  NNL2AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("NNL1AA1  NNL3AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("NNL2AA1  BBE3AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("NNL2AA1  NNL3AA1  1").getUncertainty(), EPSILON);
    }

    @Test
    void testSameNetwork12Nodes() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase12Nodes/glsk_proportional_12nodes.xml"));
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(referenceNetwork);
        ZonalData<Scalable> localMarketZonalScalable = ucteGlskDocument.getZonalScalable(marketBasedNetwork);
        XnecProvider xnecProvider = new XnecProviderInterconnection();
        TrmAlgorithm trmAlgorithm = setUp(CracFactory.findDefault().create("crac"), localMarketZonalScalable);
        TrmResults trmResults = trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, xnecProvider, zonalGlsks);
        Map<String, UncertaintyResult> result = trmResults.getUncertaintiesMap();
        assertEquals(4, result.size());
        assertEquals(0.0, result.get("BBE2AA1  FFR3AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("DDE2AA1  NNL3AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("FFR2AA1  DDE3AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("NNL2AA1  BBE3AA1  1").getUncertainty(), EPSILON);
    }

    @Test
    void testSameNetwork16Nodes() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase16Nodes/glsk_proportional_16nodes.xml"));
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(referenceNetwork);
        ZonalData<Scalable> localMarketZonalScalable = ucteGlskDocument.getZonalScalable(marketBasedNetwork);
        XnecProvider xnecProvider = new XnecProviderInterconnection();
        TrmAlgorithm trmAlgorithm = setUp(CracFactory.findDefault().create("crac"), localMarketZonalScalable);
        TrmResults trmResults = trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, xnecProvider, zonalGlsks);
        Map<String, UncertaintyResult> result = trmResults.getUncertaintiesMap();
        assertEquals(8, result.size());
        assertEquals(0.0, result.get("BBE1AA1  FFR5AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("BBE2AA1  FFR3AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("BBE4AA1  FFR5AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("DDE2AA1  NNL3AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("FFR2AA1  DDE3AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("FFR4AA1  DDE1AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("FFR4AA1  DDE4AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("NNL2AA1  BBE3AA1  1").getUncertainty(), EPSILON);
    }

    @Test
    void testSameNetwork12NodesWithOtherGenerationPlan() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        referenceNetwork.getLoad("NNL2AA1 _load").setP0(1500);
        referenceNetwork.getGenerator("DDE2AA1 _generator").setTargetP(2500);
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase12Nodes/glsk_proportional_12nodes.xml"));
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(referenceNetwork);
        ZonalData<Scalable> localMarketZonalScalable = ucteGlskDocument.getZonalScalable(marketBasedNetwork);
        XnecProvider xnecProvider = new XnecProviderInterconnection();
        TrmAlgorithm trmAlgorithm = setUp(CracFactory.findDefault().create("crac"), localMarketZonalScalable);
        TrmResults trmResults = trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, xnecProvider, zonalGlsks);
        Map<String, UncertaintyResult> result = trmResults.getUncertaintiesMap();
        assertEquals(4, result.size());
        assertEquals(15.051, result.get("BBE2AA1  FFR3AA1  1").getUncertainty(), EPSILON);
        assertEquals(14.745, result.get("DDE2AA1  NNL3AA1  1").getUncertainty(), EPSILON);
        assertEquals(15.585, result.get("FFR2AA1  DDE3AA1  1").getUncertainty(), EPSILON);
        assertEquals(15.746, result.get("NNL2AA1  BBE3AA1  1").getUncertainty(), EPSILON);
    }

    @Test
    void testSameNetwork16NodesWithOtherGenerationPlan() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase16Nodes/glsk_proportional_16nodes.xml"));
        referenceNetwork.getLoad("NNL2AA1 _load").setP0(1500);
        referenceNetwork.getGenerator("DDE2AA1 _generator").setTargetP(2500);
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(referenceNetwork);
        ZonalData<Scalable> localMarketZonalScalable = ucteGlskDocument.getZonalScalable(marketBasedNetwork);
        XnecProvider xnecProvider = new XnecProviderInterconnection();
        TrmAlgorithm trmAlgorithm = setUp(CracFactory.findDefault().create("crac"), localMarketZonalScalable);
        TrmResults trmResults = trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, xnecProvider, zonalGlsks);
        Map<String, UncertaintyResult> result = trmResults.getUncertaintiesMap();
        assertEquals(8, result.size());
        assertEquals(7.294, result.get("BBE1AA1  FFR5AA1  1").getUncertainty(), EPSILON);
        assertEquals(8.300, result.get("BBE2AA1  FFR3AA1  1").getUncertainty(), EPSILON);
        assertEquals(7.294, result.get("BBE4AA1  FFR5AA1  1").getUncertainty(), EPSILON);
        assertEquals(9.536, result.get("DDE2AA1  NNL3AA1  1").getUncertainty(), EPSILON);
        assertEquals(157.116, result.get("FFR2AA1  DDE3AA1  1").getUncertainty(), EPSILON);
        assertEquals(15.829, result.get("FFR4AA1  DDE1AA1  1").getUncertainty(), EPSILON);
        assertEquals(-207.855, result.get("FFR4AA1  DDE4AA1  1").getUncertainty(), EPSILON);
        assertEquals(9.797, result.get("NNL2AA1  BBE3AA1  1").getUncertainty(), EPSILON);
    }

    @Test
    void testSameNetwork12NodesWithDisconnectedLine() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        referenceNetwork.getLine("BBE2AA1  FFR3AA1  1").disconnect();
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase12Nodes/glsk_proportional_12nodes.xml"));
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(referenceNetwork);
        ZonalData<Scalable> localMarketZonalScalable = ucteGlskDocument.getZonalScalable(marketBasedNetwork);
        XnecProviderByIds xnecProviderByIds = XnecProviderByIds.builder()
            .addNetworkElementsOnBasecase(Set.of("BBE2AA1  FFR3AA1  1"))
            .build();
        XnecProvider xnecProvider = new XnecProviderUnion(List.of(xnecProviderByIds, new XnecProviderInterconnection()));
        TrmAlgorithm trmAlgorithm = setUp(CracFactory.findDefault().create("crac"), localMarketZonalScalable);
        TrmResults trmResults = trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, xnecProvider, zonalGlsks);
        Map<String, UncertaintyResult> result = trmResults.getUncertaintiesMap();
        assertEquals(4, result.size());
        assertEquals(Double.NaN, result.get("BBE2AA1  FFR3AA1  1").getUncertainty(), EPSILON);
        assertEquals(499.779, result.get("DDE2AA1  NNL3AA1  1").getUncertainty(), EPSILON);
        assertEquals(499.779, result.get("FFR2AA1  DDE3AA1  1").getUncertainty(), EPSILON);
        assertEquals(499.779, result.get("NNL2AA1  BBE3AA1  1").getUncertainty(), EPSILON);
    }

    @Test
    void testSameNetwork16NodesWithDisconnectedLine() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase16Nodes/glsk_proportional_16nodes.xml"));
        referenceNetwork.getLine("BBE1AA1  FFR5AA1  1").disconnect();
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(referenceNetwork);
        ZonalData<Scalable> localMarketZonalScalable = ucteGlskDocument.getZonalScalable(marketBasedNetwork);
        XnecProviderByIds xnecProviderByIds = XnecProviderByIds.builder()
            .addNetworkElementsOnBasecase(Set.of("BBE1AA1  FFR5AA1  1"))
            .build();
        XnecProvider xnecProvider = new XnecProviderUnion(List.of(xnecProviderByIds, new XnecProviderInterconnection()));
        TrmAlgorithm trmAlgorithm = setUp(CracFactory.findDefault().create("crac"), localMarketZonalScalable);
        TrmResults trmResults = trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, xnecProvider, zonalGlsks);
        Map<String, UncertaintyResult> result = trmResults.getUncertaintiesMap();
        assertEquals(8, result.size());
        assertEquals(Double.NaN, result.get("BBE1AA1  FFR5AA1  1").getUncertainty(), EPSILON);
        assertEquals(-68.678, result.get("BBE2AA1  FFR3AA1  1").getUncertainty(), EPSILON);
        assertEquals(-271.286, result.get("BBE4AA1  FFR5AA1  1").getUncertainty(), EPSILON);
        assertEquals(16.027, result.get("DDE2AA1  NNL3AA1  1").getUncertainty(), EPSILON);
        assertEquals(17.719, result.get("FFR2AA1  DDE3AA1  1").getUncertainty(), EPSILON);
        assertEquals(12.822, result.get("FFR4AA1  DDE1AA1  1").getUncertainty(), EPSILON);
        assertEquals(7.433, result.get("FFR4AA1  DDE4AA1  1").getUncertainty(), EPSILON);
        assertEquals(16.353, result.get("NNL2AA1  BBE3AA1  1").getUncertainty(), EPSILON);
    }

    @Test
    void testDifferentNetwork() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/pst/NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct");
        XnecProvider xnecProvider = new XnecProviderInterconnection();
        TrmAlgorithm trmAlgorithm = setUp(CracFactory.findDefault().create("crac"), new ZonalDataImpl<>(Collections.emptyMap()));
        TrmException exception = assertThrows(TrmException.class, () -> trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, xnecProvider, null));
        assertEquals("Market-based network doesn't contain the following network elements: [BBE1AA1  FFR5AA1  1, BBE2AA1  FFR3AA1  1, BBE4AA1  FFR5AA1  1, DDE2AA1  NNL3AA1  1, FFR2AA1  DDE3AA1  1, FFR4AA1  DDE1AA1  1, FFR4AA1  DDE4AA1  1, NNL2AA1  BBE3AA1  1].", exception.getMessage());
    }

    @Test
    void testSameNetwork16NodesWithHvdcSelected() {
        Network referenceNetwork = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWith2Hvdc.xiidm");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWith2Hvdc.xiidm");
        ZonalData<SensitivityVariableSet> zonalGlsks = TrmUtils.getAutoGlsk(referenceNetwork);
        ZonalData<Scalable> localMarketZonalScalable = TrmUtils.getAutoScalable(marketBasedNetwork);
        XnecProvider xnecProvider = XnecProviderByIds.builder().addNetworkElementsOnBasecase(Set.of("BBE2BB11 FFR3AA11 1")).build();
        TrmAlgorithm trmAlgorithm = setUp(CracFactory.findDefault().create("crac"), localMarketZonalScalable);
        TrmException exception = assertThrows(TrmException.class, () -> trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, xnecProvider, zonalGlsks));
        assertEquals("Reference critical network elements are empty", exception.getMessage());
    }

//    @Test
//    void testSameNetwork16NodesWithAutoGlskAndHvdcDisconnected() {
//        Network referenceNetwork = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWith2Hvdc.xiidm");
//        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWith2Hvdc.xiidm");
//        referenceNetwork.getHvdcLine("BBE2BB11 FFR3AA11 1").getConverterStation1().disconnect();
//        referenceNetwork.getHvdcLine("BBE2BB11 FFR3AA11 1").getConverterStation2().disconnect();
//        ZonalData<SensitivityVariableSet> zonalGlsks = TrmUtils.getAutoGlsk(referenceNetwork);
//        ZonalData<Scalable> localMarketZonalScalable = TrmUtils.getAutoScalable(marketBasedNetwork);
//        XnecProvider xnecProvider = new XnecProviderInterconnection();
//        TrmAlgorithm trmAlgorithm = setUp(CracFactory.findDefault().create("crac"), localMarketZonalScalable);
//        TrmResults trmResults = trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, xnecProvider, zonalGlsks);
//        Map<String, UncertaintyResult> result = trmResults.getUncertaintiesMap();
//        assertEquals(7, result.size());
//        assertEquals(-817.306, result.get("BBE1AA11 FFR5AA11 1").getUncertainty(), EPSILON);
//        assertEquals(-817.306, result.get("BBE4AA11 FFR5AA11 1").getUncertainty(), EPSILON);
//        assertEquals(267.703, result.get("FFR4AA11 DDE1AA11 1").getUncertainty(), EPSILON);
//        assertEquals(318.717, result.get("NNL2AA11 BBE3AA11 1").getUncertainty(), EPSILON);
//        assertEquals(367.983, result.get("FFR2AA11 DDE3AA11 1").getUncertainty(), EPSILON);
//        assertEquals(316.909, result.get("DDE2AA11 NNL3AA11 1").getUncertainty(), EPSILON);
//        assertEquals(155.689, result.get("FFR4AA11 DDE4AA11 1").getUncertainty(), EPSILON);
//    }

    @Test
    void testSameNetwork16NodesWithDisconnectedReconnectedLine() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase16Nodes/glsk_proportional_16nodes.xml"));
        referenceNetwork.getLine("BBE1AA1  FFR5AA1  1").disconnect();
        marketBasedNetwork.getLine("BBE4AA1  FFR5AA1  1").disconnect(); // This line will be reconnected
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(referenceNetwork);
        Crac localCrac = TestUtils.getIdealTopologicalAlignerCrac(referenceNetwork);
        ZonalData<Scalable> localMarketZonalScalable = ucteGlskDocument.getZonalScalable(marketBasedNetwork);
        XnecProviderByIds xnecProviderByIds = XnecProviderByIds.builder()
            .addNetworkElementsOnBasecase(Set.of("BBE1AA1  FFR5AA1  1"))
            .build();
        XnecProvider xnecProvider = new XnecProviderUnion(List.of(xnecProviderByIds, new XnecProviderInterconnection()));
        TrmAlgorithm trmAlgorithm = setUp(localCrac, localMarketZonalScalable);
        TrmResults trmResults = trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, xnecProvider, zonalGlsks);
        Map<String, UncertaintyResult> result = trmResults.getUncertaintiesMap();
        assertEquals(8, result.size());
        assertEquals(Double.NaN, result.get("BBE1AA1  FFR5AA1  1").getUncertainty(), EPSILON); // This line is still open
        assertEquals(0.0, result.get("BBE2AA1  FFR3AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("BBE4AA1  FFR5AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("DDE2AA1  NNL3AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("FFR2AA1  DDE3AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("FFR4AA1  DDE1AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("FFR4AA1  DDE4AA1  1").getUncertainty(), EPSILON);
        assertEquals(0.0, result.get("NNL2AA1  BBE3AA1  1").getUncertainty(), EPSILON);
    }

    @Test
    void testTwoNetworkWithCracFile() throws IOException {
        Network referenceNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12NodesHvdc.uct");
        referenceNetwork.getLine("NNL2AA1  BBE3AA1  1").disconnect();
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12NodesHvdc.uct");
        ZonalData<SensitivityVariableSet> zonalGlsks = TrmUtils.getAutoGlsk(referenceNetwork);
        String cracFilePath = "TestCase12Nodes/cbcora_ep10us2case1.xml";
        CracCreationParameters parameters = new CracCreationParameters();
        parameters.addExtension(FbConstraintCracCreationParameters.class, new FbConstraintCracCreationParameters());
        parameters.getExtension(FbConstraintCracCreationParameters.class).setTimestamp(OffsetDateTime.of(2019, 1, 7, 23, 30, 0, 0, ZoneOffset.UTC));
        Crac localCrac = Crac.read(cracFilePath, Objects.requireNonNull(getClass().getResourceAsStream(cracFilePath)), referenceNetwork, parameters);
        localCrac.getNetworkAction("Open FR1 FR2").apply(referenceNetwork);
        ZonalData<Scalable> localMarketZonalScalable = TrmUtils.getAutoScalable(marketBasedNetwork);
        XnecProviderByIds xnecProviderByIds = XnecProviderByIds.builder()
            .addNetworkElementsOnBasecase(Set.of("NNL2AA1  BBE3AA1  1"))
            .build();
        XnecProvider xnecProvider = new XnecProviderUnion(List.of(xnecProviderByIds, new XnecProviderInterconnection()));
        TrmAlgorithm trmAlgorithm = setUp(localCrac, localMarketZonalScalable);
        TrmResults trmResults = trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, xnecProvider, zonalGlsks);
        Map<String, UncertaintyResult> result = trmResults.getUncertaintiesMap();
        assertEquals(4, result.size());
        assertEquals(-1383.344, result.get("BBE2AA1  FFR3AA1  1").getUncertainty(), EPSILON);
        assertEquals(-1383.344, result.get("FFR2AA1  DDE3AA1  1").getUncertainty(), EPSILON);
        assertEquals(Double.NaN, result.get("NNL2AA1  BBE3AA1  1").getUncertainty(), EPSILON);
        assertEquals(-1383.344, result.get("DDE2AA1  NNL3AA1  1").getUncertainty(), EPSILON);
    }

    @Test
    void testNetworkWithoutInterconnection() {
        Network referenceNetwork = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");
        referenceNetwork.getLine("FGEN1 11 BLOAD 11 1").remove();
        Network marketBasedNetwork = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");
        ZonalData<SensitivityVariableSet> zonalGlsks = TrmUtils.getAutoGlsk(referenceNetwork);
        ZonalData<Scalable> localMarketZonalScalable = TrmUtils.getAutoScalable(marketBasedNetwork);
        XnecProvider xnecProvider = new XnecProviderInterconnection();
        TrmAlgorithm trmAlgorithm = setUp(CracFactory.findDefault().create("crac"), localMarketZonalScalable);
        TrmException trmException = assertThrows(TrmException.class, () -> trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, xnecProvider, zonalGlsks));
        assertEquals("Reference critical network elements are empty", trmException.getMessage());
    }

    @Test
    void testReferenceNetworkSubpartOfMarketBasedNetwork() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        referenceNetwork.getLine("FFR2AA1  DDE3AA1  1").remove();
        referenceNetwork.getLine("BBE2AA1  FFR3AA1  1").remove();
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        ZonalData<SensitivityVariableSet> zonalGlsks = TrmUtils.getAutoGlsk(referenceNetwork);
        ZonalData<Scalable> localMarketZonalScalable = TrmUtils.getAutoScalable(marketBasedNetwork);
        XnecProvider xnecProvider = new XnecProviderInterconnection();
        TrmAlgorithm trmAlgorithm = setUp(CracFactory.findDefault().create("crac"), localMarketZonalScalable);
        TrmResults trmResults = trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, xnecProvider, zonalGlsks);
        Map<String, UncertaintyResult> result = trmResults.getUncertaintiesMap();
        assertEquals(2, result.size());
        assertEquals(1260.669, result.get("NNL2AA1  BBE3AA1  1").getUncertainty(), EPSILON);
        assertEquals(1260.669, result.get("DDE2AA1  NNL3AA1  1").getUncertainty(), EPSILON);
    }

    @Test
    void testUcteMapping() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        referenceNetwork.getLine("FFR2AA1  DDE3AA1  1").remove();
        referenceNetwork.getLine("BBE2AA1  FFR3AA1  1").remove();
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes_NewId.uct");
        ZonalData<SensitivityVariableSet> zonalGlsks = TrmUtils.getAutoGlsk(referenceNetwork);
        ZonalData<Scalable> localMarketZonalScalable = TrmUtils.getAutoScalable(marketBasedNetwork);
        XnecProvider xnecProvider = new XnecProviderInterconnection();
        TrmAlgorithm trmAlgorithm = setUp(CracFactory.findDefault().create("crac"), localMarketZonalScalable);
        TrmResults trmResults = trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, xnecProvider, zonalGlsks);
        Map<String, UncertaintyResult> result = trmResults.getUncertaintiesMap();
        assertEquals(2, result.size());
        assertEquals(1260.669, result.get("NNL2AA1  BBE3AA1  1").getUncertainty(), EPSILON);
        assertEquals(1260.669, result.get("DDE2AA1  NNL3AA1  1").getUncertainty(), EPSILON);
    }
}
