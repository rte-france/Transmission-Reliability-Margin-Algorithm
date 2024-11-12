/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.rte_france.trm_algorithm.TestUtils;
import org.junit.jupiter.api.Test;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Sébastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class BranchConnectionStateAlignerTest {

    @Test
    void testAlignBranch() {
        Network referenceNetwork = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");

        String branchId = "FGEN1 11 BLOAD 11 1";

        Branch<?> branch = marketBasedNetwork.getBranch(branchId);
        assertEquals(TRUE, branch.getTerminal1().isConnected());
        assertEquals(TRUE, branch.getTerminal2().isConnected());
        branch.getTerminal1().disconnect();
        branch.getTerminal2().disconnect();
        assertEquals(FALSE, branch.getTerminal1().isConnected());
        assertEquals(FALSE, branch.getTerminal2().isConnected());

        BranchConnectionStateAligner branchConnectionStateAligner = new BranchConnectionStateAligner();
        branchConnectionStateAligner.align(referenceNetwork, marketBasedNetwork);

        assertEquals(TRUE, branch.getTerminal1().isConnected());
        assertEquals(TRUE, branch.getTerminal2().isConnected());
    }

    @Test
    void testAlignBranchMissing() {
        Network referenceNetwork = TestUtils.importNetwork("simple_networks/NETWORK_LOOP_FLOW_WITH_COUNTRIES.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");

        String branchId = "FGEN1 11 BLOAD 11 1";

        Branch<?> branch = marketBasedNetwork.getBranch(branchId);
        assertEquals(TRUE, branch.getTerminal1().isConnected());
        assertEquals(TRUE, branch.getTerminal2().isConnected());
        branch.getTerminal1().disconnect();
        branch.getTerminal2().disconnect();
        assertEquals(FALSE, branch.getTerminal1().isConnected());
        assertEquals(FALSE, branch.getTerminal2().isConnected());

        BranchConnectionStateAligner branchConnectionStateAligner = new BranchConnectionStateAligner();

        branchConnectionStateAligner.align(referenceNetwork, marketBasedNetwork);
    }

    @Test
    void testInvertedTerminals() {
        Network referenceNetwork = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");

        String lineId = "FGEN1 11 BLOAD 11 1";

        Line line = marketBasedNetwork.getLine(lineId);
        line.remove();

        Line referenceLine = referenceNetwork.getLine(lineId);

        marketBasedNetwork.newLine()
                .setId("FGEN1 11 BLOAD 11 1")
                .setR(referenceLine.getR())
                .setB1(referenceLine.getB1())
                .setB2(referenceLine.getB2())
                .setG1(referenceLine.getG1())
                .setG2(referenceLine.getG2())
                .setX(referenceLine.getX())
                .setVoltageLevel1("BLOAD 1")
                .setVoltageLevel2("FGEN1 1")
                .setBus1("BLOAD 11")
                .setBus2("FGEN1 11")
                .add();

        Line marketBasedNetworkLine = marketBasedNetwork.getLine(lineId);

        assertEquals(marketBasedNetworkLine.getTerminal1().getBusView().getBus().getId(), referenceLine.getTerminal2().getBusView().getBus().getId());
        assertEquals(marketBasedNetworkLine.getTerminal2().getBusView().getBus().getId(), referenceLine.getTerminal1().getBusView().getBus().getId());

        assertEquals(TRUE, referenceLine.getTerminal1().isConnected());
        assertEquals(TRUE, referenceLine.getTerminal2().isConnected());
        referenceLine.getTerminal1().disconnect();
        referenceLine.getTerminal2().disconnect();
        assertEquals(FALSE, referenceLine.getTerminal1().isConnected());
        assertEquals(FALSE, referenceLine.getTerminal2().isConnected());

        BranchConnectionStateAligner branchConnectionStateAligner = new BranchConnectionStateAligner();

        branchConnectionStateAligner.align(referenceNetwork, marketBasedNetwork);
    }
}
