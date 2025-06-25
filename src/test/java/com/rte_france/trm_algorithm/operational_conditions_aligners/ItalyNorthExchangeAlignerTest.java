/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.powsybl.iidm.network.Network;
import com.rte_france.trm_algorithm.TestUtils;
import org.junit.jupiter.api.Test;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class ItalyNorthExchangeAlignerTest {
    public static final double EPSILON = 1e-1;

    @Test
    void testComputeAllNetPositions() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase12Nodes/NETWORK_TEST_IN_REFERENCE.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase12Nodes/NETWORK_TEST_IN.uct");

        ItalyNorthExchangeAligner italyNorthExchangeAligner = new ItalyNorthExchangeAligner();
        italyNorthExchangeAligner.align(referenceNetwork, marketBasedNetwork);
    }

    @Test
    void testReadShiftingFactors() {
    }

    @Test
    void testShiftNetworkWithShiftingFactors() {
    }
}
