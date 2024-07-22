/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.TieLine;
import com.powsybl.loadflow.LoadFlow;
import com.rte_france.trm_algorithm.TestUtils;
import com.rte_france.trm_algorithm.TrmException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class DanglingLineAlignerTest {

    private static final double EPSILON = 1e-3;

    @Test
    void testAlignDanglingLine() {
        Network referenceNetwork = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_UNBOUNDED_XNODE.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_UNBOUNDED_XNODE.uct");
        String danglingLineId = "BLOAD 11 X     11 1";
        DanglingLine danglingLine = marketBasedNetwork.getDanglingLine(danglingLineId);
        danglingLine.setP0(-50);
        assertEquals(-50, danglingLine.getP0());

        DanglingLineAligner danglingLineAligner = new DanglingLineAligner();
        danglingLineAligner.align(referenceNetwork, marketBasedNetwork);
        Map<String, DanglingLineAligner.Status> results = danglingLineAligner.getResult();
        assertEquals(1, results.size());
        assertEquals(DanglingLineAligner.Status.ALIGNED, results.get(danglingLineId));
        assertEquals(100, danglingLine.getP0());
    }

    @Test
    void testAlignDanglingLineNotFound() {
        Network referenceNetwork = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_UNBOUNDED_XNODE.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");
        String danglingLineId = "BLOAD 11 X     11 1";

        DanglingLineAligner danglingLineAligner = new DanglingLineAligner();
        danglingLineAligner.align(referenceNetwork, marketBasedNetwork);
        Map<String, DanglingLineAligner.Status> results = danglingLineAligner.getResult();
        assertEquals(1, results.size());
        assertEquals(DanglingLineAligner.Status.NOT_FOUND_IN_MARKET_BASED_NETWORK, results.get(danglingLineId));
    }

    @Test
    void testAlignReferenceDanglingLineWithMarketBasedTieLineSide1() {
        Network referenceNetwork = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_UNBOUNDED_XNODE.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_BOUNDED_XNODE.uct");
        LoadFlow.run(marketBasedNetwork);
        LoadFlow.run(referenceNetwork);

        String danglingLineId = "BLOAD 11 X     11 1";
        DanglingLine referenceDanglingLine = referenceNetwork.getDanglingLine(danglingLineId);
        DanglingLine marketBasedDanglingLine = marketBasedNetwork.getDanglingLine(danglingLineId);
        assertEquals(100, referenceDanglingLine.getP0(), EPSILON);
        assertEquals(100.062, referenceDanglingLine.getTerminal().getP(), EPSILON);
        assertEquals(0, marketBasedDanglingLine.getP0(), EPSILON);
        assertEquals(120.180, marketBasedDanglingLine.getTerminal().getP(), EPSILON);

        String tieLineId = "BLOAD 11 X     11 1 + X     11 DLOAD 11 1";
        TieLine marketBasedTieLine = marketBasedNetwork.getTieLine(tieLineId);
        DanglingLine marketBasedDanglingLine1 = marketBasedTieLine.getDanglingLine1();
        DanglingLine marketBasedDanglingLine2 = marketBasedTieLine.getDanglingLine2();
        assertEquals(120.180, marketBasedTieLine.getTerminal1().getP(), EPSILON);
        assertEquals(-120, marketBasedTieLine.getTerminal2().getP(), EPSILON);
        assertEquals(0, marketBasedDanglingLine1.getP0(), EPSILON);
        assertEquals(120.180, marketBasedDanglingLine1.getTerminal().getP(), EPSILON);
        assertEquals(0, marketBasedDanglingLine2.getP0(), EPSILON);
        assertEquals(-120, marketBasedDanglingLine2.getTerminal().getP(), EPSILON);

        DanglingLineAligner danglingLineAligner = new DanglingLineAligner();
        danglingLineAligner.align(referenceNetwork, marketBasedNetwork);
        Map<String, DanglingLineAligner.Status> results = danglingLineAligner.getResult();
        assertEquals(1, results.size());
        assertEquals(DanglingLineAligner.Status.DANGLING_LINE_MERGED_IN_MARKET_BASED_NETWORK, results.get(danglingLineId));
        LoadFlow.run(marketBasedNetwork);

        assertEquals(100, marketBasedDanglingLine.getP0(), EPSILON);
        assertEquals(100.062, marketBasedDanglingLine.getTerminal().getP(), EPSILON);
        assertEquals(100, marketBasedTieLine.getDanglingLine1().getP0(), EPSILON);
        assertEquals(100.062, marketBasedTieLine.getTerminal1().getP(), EPSILON);
        assertTrue(marketBasedDanglingLine.getTerminal().isConnected());
        assertTrue(marketBasedTieLine.getTerminal1().isConnected());
        assertFalse(marketBasedDanglingLine.isPaired());
        assertEquals(100, marketBasedDanglingLine1.getP0(), EPSILON);
        assertEquals(100.062, marketBasedDanglingLine1.getTerminal().getP(), EPSILON);
        assertEquals(0., marketBasedDanglingLine2.getP0(), EPSILON);
        Terminal marketBasedDanglingLine2Terminal = marketBasedDanglingLine2.getTerminal();
        PowsyblException exception = assertThrows(PowsyblException.class, marketBasedDanglingLine2Terminal::getP);
        assertEquals("Cannot access p of removed equipment X     11 DLOAD 11 1", exception.getMessage());
    }

    @Test
    void testAlignReferenceDanglingLineWithMarketBasedTieLineSide2() {
        Network referenceNetwork = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_BOUNDED_XNODE.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_BOUNDED_XNODE.uct");
        String danglingLine1Id = "BLOAD 11 X     11 1";
        String danglingLine2Id = "X     11 DLOAD 11 1";
        String tieLineId = "BLOAD 11 X     11 1 + X     11 DLOAD 11 1";
        referenceNetwork.getTieLine(tieLineId).remove();
        referenceNetwork.getDanglingLine(danglingLine1Id).remove();
        referenceNetwork.getDanglingLine(danglingLine2Id).setP0(-50);
        assertEquals(0, marketBasedNetwork.getDanglingLine(danglingLine2Id).getP0(), EPSILON);

        DanglingLineAligner danglingLineAligner = new DanglingLineAligner();
        danglingLineAligner.align(referenceNetwork, marketBasedNetwork);
        Map<String, DanglingLineAligner.Status> results = danglingLineAligner.getResult();
        assertEquals(1, results.size());
        assertEquals(DanglingLineAligner.Status.DANGLING_LINE_MERGED_IN_MARKET_BASED_NETWORK, results.get(danglingLine2Id));
        assertEquals(-50, marketBasedNetwork.getDanglingLine(danglingLine2Id).getP0(), EPSILON);
    }

    @Test
    void testAlignReferenceTieLineWithMarketBasedDanglingLine() {
        Network referenceNetwork = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_BOUNDED_XNODE.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_UNBOUNDED_XNODE.uct");

        DanglingLineAligner danglingLineAligner = new DanglingLineAligner();
        TrmException trmException = assertThrows(TrmException.class, () -> danglingLineAligner.align(referenceNetwork, marketBasedNetwork));
        assertEquals("Reference dangling line \"BLOAD 11 X     11 1\" (\"XNODE\") has been paired \"BLOAD 11 X     11 1 + X     11 DLOAD 11 1\" (\"BLOAD 11 X     11 1 + X     11 DLOAD 11 1\") but market-based dangling line \"BLOAD 11 X     11 1\" (\"XNODE\") has not been paired", trmException.getMessage());
    }

    @Test
    void testAlignReferenceTieLineWithMarketBasedTieLine() {
        Network referenceNetwork = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_BOUNDED_XNODE.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_BOUNDED_XNODE.uct");

        DanglingLineAligner danglingLineAligner = new DanglingLineAligner();
        danglingLineAligner.align(referenceNetwork, marketBasedNetwork);
        Map<String, DanglingLineAligner.Status> results = danglingLineAligner.getResult();
        assertEquals(2, results.size());
        assertEquals(DanglingLineAligner.Status.PAIRED_DANGLING_LINE_IN_BOTH_NETWORKS, results.get("BLOAD 11 X     11 1"));
        assertEquals(DanglingLineAligner.Status.PAIRED_DANGLING_LINE_IN_BOTH_NETWORKS, results.get("X     11 DLOAD 11 1"));
    }

}
