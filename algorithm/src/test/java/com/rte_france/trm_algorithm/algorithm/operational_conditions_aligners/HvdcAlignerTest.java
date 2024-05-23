/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.algorithm.operational_conditions_aligners;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControlAdder;
import com.rte_france.trm_algorithm.algorithm.TestUtils;
import com.rte_france.trm_algorithm.algorithm.TrmException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class HvdcAlignerTest {
    @Test
    void testOneHvdcDcOnlyAlignment() {
        Network network = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWithHvdc.xiidm");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWithHvdc.xiidm");
        String hvdcId = "BBE2AA11 FFR3AA11 1";
        network.getHvdcLine(hvdcId).setActivePowerSetpoint(100);
        assertEquals(100, network.getHvdcLine(hvdcId).getActivePowerSetpoint());
        HvdcAligner.align(network, marketBasedNetwork);
        assertEquals(100, marketBasedNetwork.getHvdcLine(hvdcId).getActivePowerSetpoint());
    }

    @Test
    void testOneHvdcAcEmulation() {
        Network network = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWithHvdc.xiidm");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWithHvdc.xiidm");
        String hvdcId = "BBE2AA11 FFR3AA11 1";
        assertNull(network.getHvdcLine(hvdcId).getExtension(HvdcAngleDroopActivePowerControl.class));
        network.getHvdcLine(hvdcId).newExtension(HvdcAngleDroopActivePowerControlAdder.class)
                .withP0(200)
                .withDroop(0.3f)
                .withEnabled(true)
                .add();
        assertEquals(200, network.getHvdcLine(hvdcId).getExtension(HvdcAngleDroopActivePowerControl.class).getP0());
        HvdcAligner.align(network, marketBasedNetwork);
        assertEquals(200, marketBasedNetwork.getHvdcLine(hvdcId).getExtension(HvdcAngleDroopActivePowerControl.class).getP0());
    }

    @Test
    void testOneHvdcRemoveAcEmulation() {
        Network network = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWithHvdc.xiidm");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWithHvdc.xiidm");
        String hvdcId = "BBE2AA11 FFR3AA11 1";
        assertNull(marketBasedNetwork.getHvdcLine(hvdcId).getExtension(HvdcAngleDroopActivePowerControl.class));
        marketBasedNetwork.getHvdcLine(hvdcId).newExtension(HvdcAngleDroopActivePowerControlAdder.class)
            .withP0(200)
            .withDroop(0.3f)
            .withEnabled(true)
            .add();
        assertEquals(200, marketBasedNetwork.getHvdcLine(hvdcId).getExtension(HvdcAngleDroopActivePowerControl.class).getP0());
        HvdcAligner.align(network, marketBasedNetwork);
        assertNull(marketBasedNetwork.getHvdcLine(hvdcId).getExtension(HvdcAngleDroopActivePowerControl.class));
    }

    @Test
    void testTwoHvdcAlignment() {
        Network network = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWith2Hvdc.xiidm");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWithHvdc.xiidm");
        TrmException exception = assertThrows(TrmException.class, () -> HvdcAligner.align(network, marketBasedNetwork));
        assertEquals("HvdcLine with id BBE2BB11 FFR3AA11 1 not found", exception.getMessage());
    }
}
