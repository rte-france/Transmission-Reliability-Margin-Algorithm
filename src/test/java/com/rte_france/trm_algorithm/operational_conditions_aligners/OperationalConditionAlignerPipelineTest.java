/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.powsybl.balances_adjustment.balance_computation.BalanceComputationParameters;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracFactory;
import com.rte_france.trm_algorithm.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class OperationalConditionAlignerPipelineTest {
    public static final double EPSILON = 1e-3;

    private static Network getReferenceNetwork() {
        Network referenceNetwork = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWithHvdc.xiidm");
        referenceNetwork.getHvdcLine("BBE2AA11 FFR3AA11 1").setActivePowerSetpoint(100);
        referenceNetwork.getTwoWindingsTransformer("BBE2AA11 BBE3AA11 1").getPhaseTapChanger().setTapPosition(-5);
        return referenceNetwork;
    }

    private static void assertPstAlignerResult(PstAligner.Result pstAlignmentResults) {
        assertTrue(pstAlignmentResults.getPhaseTapChangerResults().get("BBE2AA11 BBE3AA11 1"));
        assertTrue(pstAlignmentResults.getPhaseTapChangerResults().get("FFR2AA11 FFR4AA11 1"));
        assertTrue(pstAlignmentResults.getRatioTapChangerResults().isEmpty());
    }

    private static void assertExchangeAlignerResult(ExchangeAlignerResult exchangeAlignerResult) {
        assertEquals(ExchangeAligner.Status.ALREADY_ALIGNED, exchangeAlignerResult.getStatus());
        TestUtils.assertNetPositions(Map.of(Country.BE, 2501., Country.DE, -2000., Country.FR, -1., Country.NL, -500.), exchangeAlignerResult.getReferenceNetPositions());
        TestUtils.assertNetPositions(Map.of(Country.BE, 2501., Country.DE, -2000., Country.FR, -1., Country.NL, -500.), exchangeAlignerResult.getInitialMarketBasedNetPositions());
        TestUtils.assertExchanges(Map.of(Country.FR, Map.of(Country.DE, 1053.), Country.NL, Map.of(Country.DE, 947.), Country.BE, Map.of(Country.FR, 1053.9, Country.NL, 1447.)), exchangeAlignerResult.getReferenceExchanges());
        TestUtils.assertExchanges(Map.of(Country.FR, Map.of(Country.DE, 1053.), Country.NL, Map.of(Country.DE, 947.), Country.BE, Map.of(Country.FR, 1053.9, Country.NL, 1447.)), exchangeAlignerResult.getInitialMarketBasedExchanges());
        assertEquals(0, exchangeAlignerResult.getInitialMaxAbsoluteExchangeDifference(), EPSILON);
        TestUtils.assertNetPositions(Map.of(Country.BE, 2501., Country.DE, -2000., Country.FR, -1., Country.NL, -500.), exchangeAlignerResult.getTargetNetPositions());
        assertNull(exchangeAlignerResult.getNewMarketBasedNetPositions());
        assertNull(exchangeAlignerResult.getNewMarketBasedExchanges());
        assertNull(exchangeAlignerResult.getNewMaxAbsoluteExchangeDifference());
        assertNull(exchangeAlignerResult.getBalanceComputationResult());
    }

    @Test
    void testAlignmentChain() {
        Network referenceNetwork = getReferenceNetwork();
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
        assertPstAlignerResult(pstAligner.getResult());
        assertTrue(danglingLineAligner.getResult().isEmpty());
        assertExchangeAlignerResult(exchangeAligner.getResult());
    }

    @Test
    void testAlignmentPartialChain() {
        Network referenceNetwork = getReferenceNetwork();
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
}
