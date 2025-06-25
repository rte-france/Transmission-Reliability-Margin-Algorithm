/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners.exchange_and_net_position;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.rte_france.trm_algorithm.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class ExchangeAndNetPositionTest {
    private static final double EPSILON = 1e-3;

    @Test
    void testSimpleNetwork() {
        Network network = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");
        LoadFlow.run(network);
        ExchangeAndNetPosition exchangeAndNetPosition = new ExchangeAndNetPosition(network);
        assertEquals(Set.of(Country.BE, Country.FR), exchangeAndNetPosition.getCountries());
        assertEquals(100.093, exchangeAndNetPosition.getNetPosition(Country.FR), EPSILON);
        assertEquals(-100.093, exchangeAndNetPosition.getNetPosition(Country.BE), EPSILON);
        assertEquals(100.093, exchangeAndNetPosition.getExchange(Country.FR, Country.BE), EPSILON);
        assertEquals(0, exchangeAndNetPosition.getExchangeToExterior(Country.FR), EPSILON);
        assertEquals(0., exchangeAndNetPosition.getExchangeToExterior(Country.BE), EPSILON);
    }

    @Test
    void testWithDanglingLine() {
        Network network = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_UNBOUNDED_XNODE.uct");
        LoadFlow.run(network);
        ExchangeAndNetPosition exchangeAndNetPosition = new ExchangeAndNetPosition(network);
        assertEquals(Set.of(Country.BE, Country.FR), exchangeAndNetPosition.getCountries());
        assertEquals(100.156, exchangeAndNetPosition.getNetPosition(Country.FR), EPSILON);
        assertEquals(-0.156, exchangeAndNetPosition.getNetPosition(Country.BE), EPSILON);
        assertEquals(100.156, exchangeAndNetPosition.getExchange(Country.FR, Country.BE), EPSILON);
        assertEquals(0, exchangeAndNetPosition.getExchangeToExterior(Country.FR), EPSILON);
        assertEquals(100., exchangeAndNetPosition.getExchangeToExterior(Country.BE), EPSILON);
    }

    @Test
    void testItalyNorthNetwork() {
        Network network = TestUtils.importNetwork("TestCase12Nodes/NETWORK_TEST_IN.uct");
        LoadFlow.run(network);
        ExchangeAndNetPosition exchangeAndNetPosition = new ExchangeAndNetPosition(network);
        assertEquals(Set.of(Country.AT, Country.CH, Country.DE, Country.FR, Country.IT, Country.SI), exchangeAndNetPosition.getCountries());
        assertEquals(401.098, exchangeAndNetPosition.getNetPosition(Country.AT), EPSILON);
        assertEquals(323.276, exchangeAndNetPosition.getNetPosition(Country.CH), EPSILON); //350
        assertEquals(501.792, exchangeAndNetPosition.getNetPosition(Country.DE), EPSILON);
        assertEquals(618.850, exchangeAndNetPosition.getNetPosition(Country.FR), EPSILON); //600
        assertEquals(-1948.416, exchangeAndNetPosition.getNetPosition(Country.IT), EPSILON);
        assertEquals(103.399, exchangeAndNetPosition.getNetPosition(Country.SI), EPSILON);
    }
}
