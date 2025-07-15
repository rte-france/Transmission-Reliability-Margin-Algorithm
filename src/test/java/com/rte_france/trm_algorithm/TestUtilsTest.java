/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import com.powsybl.balances_adjustment.balance_computation.BalanceComputationResult;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.crac.api.Crac;
import com.rte_france.trm_algorithm.operational_conditions_aligners.ExchangeAlignerResult;
import org.junit.jupiter.api.Test;

import static com.rte_france.trm_algorithm.operational_conditions_aligners.ExchangeAlignerStatus.NOT_ALIGNED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class TestUtilsTest {
    @Test
    void testIdealTopologicalAlignerCrac() {
        Network network = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");

        Crac crac = TestUtils.getIdealTopologicalAlignerCrac(network);

        assertEquals(4, crac.getNetworkActions().size());
        assertEquals(1, crac.getNetworkAction("Topological action with branch:\"BLOAD 11 BGEN2 11 1\", actionType:CLOSE").getElementaryActions().size());
        assertEquals(1, crac.getNetworkAction("Topological action with branch:\"BLOAD 11 BGEN2 11 1\", actionType:OPEN").getElementaryActions().size());
        assertEquals(1, crac.getNetworkAction("Topological action with branch:\"FGEN1 11 BLOAD 11 1\", actionType:CLOSE").getElementaryActions().size());
        assertEquals(1, crac.getNetworkAction("Topological action with branch:\"FGEN1 11 BLOAD 11 1\", actionType:OPEN").getElementaryActions().size());
    }

    @Test
    void testBuildEmptyResults() {
        TrmResults trmResults = TestUtils.mockTrmResults().build();
        assertTrue(trmResults.getUncertaintiesMap().isEmpty());
    }

    @Test
    void testMockExchangeAlignerResult() {
        ExchangeAlignerResult exchangeAlignerResult = TestUtils.mockExchangeAlignerResult();
        assertTrue(exchangeAlignerResult.getReferenceExchangeAndNetPosition().getCountries().isEmpty());
        assertEquals(Double.NaN, exchangeAlignerResult.getReferenceExchangeAndNetPosition().getNetPosition(Country.FR));
        assertEquals(Double.NaN, exchangeAlignerResult.getReferenceExchangeAndNetPosition().getExchange(Country.FR, Country.BE));
        assertEquals(Double.NaN, exchangeAlignerResult.getReferenceExchangeAndNetPosition().getExchangeToExterior(Country.FR));
        assertTrue(exchangeAlignerResult.getInitialMarketBasedExchangeAndNetPosition().getCountries().isEmpty());
        assertTrue(exchangeAlignerResult.getTargetNetPositions().getCountries().isEmpty());
        assertEquals(Double.NaN, exchangeAlignerResult.getTargetNetPositions().getNetPosition(Country.FR));
        assertEquals(BalanceComputationResult.Status.FAILED, exchangeAlignerResult.getBalanceComputationResult().getStatus());
        assertTrue(exchangeAlignerResult.getNewMarketBasedExchangeAndNetPosition().getCountries().isEmpty());
        assertEquals(NOT_ALIGNED, exchangeAlignerResult.getStatus());
        assertEquals(Double.NaN, exchangeAlignerResult.getInitialMaxAbsoluteExchangeDifference());
        assertEquals(Double.NaN, exchangeAlignerResult.getNewMaxAbsoluteExchangeDifference());
    }
}
