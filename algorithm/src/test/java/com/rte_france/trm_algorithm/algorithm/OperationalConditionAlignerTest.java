/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.algorithm;

import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class OperationalConditionAlignerTest {
    @Test
    void testHvdcAndPstAlignment() {
        Network network = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWithHvdc.xiidm");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWithHvdc.xiidm");
        Crac crac = CracFactory.findDefault().create("crac");
        String hvdcId = "BBE2AA11 FFR3AA11 1";
        network.getHvdcLine(hvdcId).setActivePowerSetpoint(100);
        assertEquals(100, network.getHvdcLine(hvdcId).getActivePowerSetpoint());
        String pstId = "BBE2AA11 BBE3AA11 1";
        network.getTwoWindingsTransformer(pstId).getPhaseTapChanger().setTapPosition(-5);
        assertEquals(-5, network.getTwoWindingsTransformer(pstId).getPhaseTapChanger().getTapPosition());
        OperationalConditionAligner.align(network, marketBasedNetwork, crac);
        assertEquals(100, marketBasedNetwork.getHvdcLine(hvdcId).getActivePowerSetpoint());
        assertEquals(-5, marketBasedNetwork.getTwoWindingsTransformer(pstId).getPhaseTapChanger().getTapPosition());
    }
}
