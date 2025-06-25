/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.rte_france.trm_algorithm.TestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class ItalyNorthExchangeAlignerTest {
    public static final double EPSILON = 1e-1;
    public static final double EPSILON_BIG = 5;

    @Test
    void testComputeItalianNetPosition() {
        Network network = TestUtils.importNetwork("TestCase12Nodes/NETWORK_TEST_IN.uct");
        ItalyNorthExchangeAligner italyNorthExchangeAligner = new ItalyNorthExchangeAligner(network);

        assertEquals(-1948.4, italyNorthExchangeAligner.getCountryBalance(Country.IT), EPSILON);
        assertEquals(-1950.0, italyNorthExchangeAligner.getCountryBalance(Country.IT), EPSILON_BIG);
    }

    @Test
    void testComputeAllNetPositions() {
        Network network = TestUtils.importNetwork("TestCase12Nodes/NETWORK_TEST_IN.uct");
        ItalyNorthExchangeAligner italyNorthExchangeAligner = new ItalyNorthExchangeAligner(network);

        assertEquals(401.0, italyNorthExchangeAligner.getCountryBalance(Country.AT), EPSILON);
        assertEquals(323.2, italyNorthExchangeAligner.getCountryBalance(Country.CH), EPSILON); //350
        assertEquals(501.7, italyNorthExchangeAligner.getCountryBalance(Country.DE), EPSILON);
        assertEquals(618.8, italyNorthExchangeAligner.getCountryBalance(Country.FR), EPSILON); //600
        assertEquals(-1948.4, italyNorthExchangeAligner.getCountryBalance(Country.IT), EPSILON);
        assertEquals(103.3, italyNorthExchangeAligner.getCountryBalance(Country.SI), EPSILON);
    }

    @Test
    void testShiftNetworkToFixedItalianImport() {
    }

    @Test
    void testShiftLoopNetworkWithEpsilon() {
    }

    @Test
    void testShiftNetworkToOtherNetwork() {
    }

    @Test
    void testReadShiftingFactors() {
    }

    @Test
    void testShiftNetworkWithShiftingFactors() {
    }
}
