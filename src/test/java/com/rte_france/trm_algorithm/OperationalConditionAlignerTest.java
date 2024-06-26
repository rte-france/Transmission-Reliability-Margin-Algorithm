/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import com.powsybl.balances_adjustment.balance_computation.BalanceComputationParameters;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracFactory;
import com.rte_france.trm_algorithm.operational_conditions_aligners.ExchangeAligner;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class OperationalConditionAlignerTest {

    public static final double EPSILON = 1e-3;

    @Test
    void testHvdcAndPstAlignment() {
        Network referenceNetwork = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWithHvdc.xiidm");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWithHvdc.xiidm");
        Crac crac = CracFactory.findDefault().create("crac");
        String hvdcId = "BBE2AA11 FFR3AA11 1";
        referenceNetwork.getHvdcLine(hvdcId).setActivePowerSetpoint(100);
        assertEquals(100, referenceNetwork.getHvdcLine(hvdcId).getActivePowerSetpoint());
        String pstId = "BBE2AA11 BBE3AA11 1";
        referenceNetwork.getTwoWindingsTransformer(pstId).getPhaseTapChanger().setTapPosition(-5);
        assertEquals(-5, referenceNetwork.getTwoWindingsTransformer(pstId).getPhaseTapChanger().getTapPosition());
        ZonalDataImpl<Scalable> marketZonalScalable = new ZonalDataImpl<>(Collections.emptyMap());
        TrmResults.Builder builder = TrmResults.getBuilder();
        new OperationalConditionAligner(new BalanceComputationParameters(), LoadFlow.find(), LocalComputationManager.getDefault()).align(referenceNetwork, marketBasedNetwork, crac, marketZonalScalable, builder);
        assertEquals(100, marketBasedNetwork.getHvdcLine(hvdcId).getActivePowerSetpoint());
        assertEquals(-5, marketBasedNetwork.getTwoWindingsTransformer(pstId).getPhaseTapChanger().getTapPosition());

        builder.addUncertainties(Collections.emptyMap());
        TrmResults trmResults = builder.build();
        Map<String, Boolean> cracAlignmentResults = trmResults.getCracAlignmentResults();
        assertTrue(cracAlignmentResults.isEmpty());
        Map<String, Boolean> pstAlignmentResults = trmResults.getPstAlignmentResults();
        assertTrue(pstAlignmentResults.get("BBE2AA11 BBE3AA11 1"));
        assertTrue(pstAlignmentResults.get("FFR2AA11 FFR4AA11 1"));
        ExchangeAligner.Result exchangeAlignerResult = trmResults.getExchangeAlignerResult();
        assertEquals(ExchangeAligner.Status.ALREADY_ALIGNED, exchangeAlignerResult.getStatus());
        assertNull(exchangeAlignerResult.getBalanceComputationResult());
        Map<Country, Double> referenceNetPositions = exchangeAlignerResult.getReferenceNetPositions();
        assertEquals(-2000, referenceNetPositions.get(Country.DE), EPSILON);
        assertEquals(-500, referenceNetPositions.get(Country.NL), EPSILON);
        assertEquals(-0.996, referenceNetPositions.get(Country.FR), EPSILON);
        assertEquals(2500.996, referenceNetPositions.get(Country.BE), EPSILON);
        Map<Country, Double> initialMarketBasedNetPositions = exchangeAlignerResult.getInitialMarketBasedNetPositions();
        assertEquals(-2000, initialMarketBasedNetPositions.get(Country.DE), EPSILON);
        assertEquals(-500, initialMarketBasedNetPositions.get(Country.NL), EPSILON);
        assertEquals(-0.996, initialMarketBasedNetPositions.get(Country.FR), EPSILON);
        assertEquals(2500.996, initialMarketBasedNetPositions.get(Country.BE), EPSILON);
        assertNull(exchangeAlignerResult.getNewMarketBasedNetPositions());
    }
}
