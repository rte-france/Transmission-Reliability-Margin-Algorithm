/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.algorithm;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class CneSelectorTest {
    @Test
    void testOneInterconnectionCneSelector() {
        Network network = TestUtils.importNetwork("cne_selection/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");

        Set<Branch> branchList = CneSelector.getNetworkElements(network);
        assertEquals(1, branchList.size());
        assertTrue(branchList.contains(network.getBranch("FGEN1 11 BLOAD 11 1")));
    }

    @Test
    void testHvdcInterconnectionCneSelector() {
        Network network = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16NodesWithHvdc.xiidm");

        Set<HvdcLine> hvdcLines = CneSelector.getHvdcNetworkElements(network);
        assertEquals(1, hvdcLines.size());
        assertTrue(hvdcLines.contains(network.getHvdcLine("BBE2AA11 FFR3AA11 1")));
    }
}
