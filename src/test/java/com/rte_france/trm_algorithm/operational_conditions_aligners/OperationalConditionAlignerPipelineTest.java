/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.HvdcCreationParameters;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.SwePreprocessorParameters;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.json.JsonSwePreprocessorImporter;
import com.powsybl.balances_adjustment.balance_computation.BalanceComputationParameters;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.openrao.data.crac.api.Crac;
import com.powsybl.openrao.data.crac.api.CracFactory;
import com.rte_france.trm_algorithm.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class OperationalConditionAlignerPipelineTest {
    public static final double EPSILON = 1e-3;

    private static Network getReferenceNetworkWithHvdc() {
        Network referenceNetwork = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWithHvdc.xiidm");
        referenceNetwork.getHvdcLine("BBE2AA11 FFR3AA11 1").setActivePowerSetpoint(100);
        referenceNetwork.getTwoWindingsTransformer("BBE2AA11 BBE3AA11 1").getPhaseTapChanger().setTapPosition(-5);
        return referenceNetwork;
    }

    private static void assertPstAlignerResult(PstAligner.Result pstAlignmentResults, String pstId1, String pstId2) {
        assertTrue(pstAlignmentResults.getPhaseTapChangerResults().get(pstId1));
        assertTrue(pstAlignmentResults.getPhaseTapChangerResults().get(pstId2));
        assertTrue(pstAlignmentResults.getRatioTapChangerResults().isEmpty());
    }

    private static void assertExchangeAlignerResult(ExchangeAlignerResult exchangeAlignerResult, Map<Country, Double> expectedNetPositions, Map<Country, Map<Country, Double>> expectedExchanges) {
        assertEquals(ExchangeAligner.Status.ALREADY_ALIGNED, exchangeAlignerResult.getStatus());
        TestUtils.assertNetPositions(expectedNetPositions, exchangeAlignerResult.getReferenceExchangeAndNetPosition());
        TestUtils.assertNetPositions(expectedNetPositions, exchangeAlignerResult.getInitialMarketBasedExchangeAndNetPosition());
        TestUtils.assertExchanges(expectedExchanges, exchangeAlignerResult.getReferenceExchangeAndNetPosition());
        TestUtils.assertExchanges(expectedExchanges, exchangeAlignerResult.getInitialMarketBasedExchangeAndNetPosition());
        assertEquals(0, exchangeAlignerResult.getInitialMaxAbsoluteExchangeDifference(), EPSILON);
        TestUtils.assertNetPositions(expectedNetPositions, exchangeAlignerResult.getTargetNetPositions());
        assertNull(exchangeAlignerResult.getBalanceComputationResult());
        assertTrue(exchangeAlignerResult.getNewMarketBasedExchangeAndNetPosition().getCountries().isEmpty());
        assertEquals(Double.NaN, exchangeAlignerResult.getNewMaxAbsoluteExchangeDifference());
    }

    @Test
    void testAlignmentChain() {
        Network referenceNetwork = getReferenceNetworkWithHvdc();
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWithHvdc.xiidm");
        Crac crac = CracFactory.findDefault().create("crac");

        CracAligner cracAligner = new CracAligner(crac);
        HvdcAligner hvdcAligner = new HvdcAligner();
        PstAligner pstAligner = new PstAligner();
        DanglingLineAligner danglingLineAligner = new DanglingLineAligner();
        ExchangeAligner exchangeAligner = new ExchangeAligner(new BalanceComputationParameters(), LoadFlow.find(), LocalComputationManager.getDefault(), new ZonalDataImpl<>(Collections.emptyMap()));
        OperationalConditionAligner operationalConditionAligner = new OperationalConditionAlignerPipeline(cracAligner, hvdcAligner, pstAligner, danglingLineAligner, exchangeAligner);
        operationalConditionAligner.align(referenceNetwork, marketBasedNetwork);

        assertEquals(100, marketBasedNetwork.getHvdcLine("BBE2AA11 FFR3AA11 1").getActivePowerSetpoint());
        assertEquals(-5, marketBasedNetwork.getTwoWindingsTransformer("BBE2AA11 BBE3AA11 1").getPhaseTapChanger().getTapPosition());
        assertTrue(cracAligner.getResult().isEmpty());
        assertPstAlignerResult(pstAligner.getResult(), "BBE2AA11 BBE3AA11 1", "FFR2AA11 FFR4AA11 1");
        assertTrue(danglingLineAligner.getResult().isEmpty());
        assertExchangeAlignerResult(exchangeAligner.getResult(),
            Map.of(Country.BE, 2501., Country.DE, -2000., Country.FR, -1., Country.NL, -500.),
            Map.of(Country.FR, Map.of(Country.DE, 1053.), Country.NL, Map.of(Country.DE, 947.), Country.BE, Map.of(Country.FR, 1053.9, Country.NL, 1447.)));
    }

    @Test
    void testAlignmentPartialChain() {
        Network referenceNetwork = getReferenceNetworkWithHvdc();
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWithHvdc.xiidm");
        Crac crac = CracFactory.findDefault().create("crac");

        CracAligner cracAligner = new CracAligner(crac);
        DanglingLineAligner danglingLineAligner = new DanglingLineAligner();
        OperationalConditionAligner operationalConditionAligner = new OperationalConditionAlignerPipeline(cracAligner, danglingLineAligner);
        operationalConditionAligner.align(referenceNetwork, marketBasedNetwork);

        assertEquals(0, marketBasedNetwork.getHvdcLine("BBE2AA11 FFR3AA11 1").getActivePowerSetpoint());
        assertEquals(0, marketBasedNetwork.getTwoWindingsTransformer("BBE2AA11 BBE3AA11 1").getPhaseTapChanger().getTapPosition());
        assertTrue(cracAligner.getResult().isEmpty());
        assertTrue(danglingLineAligner.getResult().isEmpty());
    }

    @Test
    void testAlignmentChainWithHvdcModeling() {
        Network referenceNetwork = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16Nodes.xiidm");
        referenceNetwork.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().setTapPosition(-5);
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16Nodes.xiidm");
        SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("hvdc/SwePreprocessorParameters.json"));
        Set<HvdcCreationParameters> creationParametersSet = params.getHvdcCreationParametersSet();

        PstAligner pstAligner = new PstAligner();
        ExchangeAligner exchangeAligner = new ExchangeAligner(new BalanceComputationParameters(), LoadFlow.find(), LocalComputationManager.getDefault(), new ZonalDataImpl<>(Collections.emptyMap()));
        HvdcAcModelingEnvironment hvdcAcModelingEnvironment = new HvdcAcModelingEnvironment(creationParametersSet, exchangeAligner);
        OperationalConditionAligner operationalConditionAligner = new OperationalConditionAlignerPipeline(pstAligner, hvdcAcModelingEnvironment);
        operationalConditionAligner.align(referenceNetwork, marketBasedNetwork);

        assertEquals(-5, marketBasedNetwork.getTwoWindingsTransformer("BBE2AA1  BBE3AA1  1").getPhaseTapChanger().getTapPosition());
        assertPstAlignerResult(pstAligner.getResult(), "BBE2AA1  BBE3AA1  1", "FFR2AA1  FFR4AA1  1");
        assertExchangeAlignerResult(exchangeAligner.getResult(),
            Map.of(Country.BE, 2027.266, Country.DE, -1005.173, Country.FR, -522.093, Country.NL, -500.),
            Map.of(Country.FR, Map.of(Country.DE, 431.153), Country.NL, Map.of(Country.DE, 574.02), Country.BE, Map.of(Country.FR, 953.246, Country.NL, 1074.02)));
    }
}
