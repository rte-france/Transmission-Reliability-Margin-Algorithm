/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Sebastian Huaraca {@literal <sebastian.huaracalapa at rte-france.com>}
 */

public class UcteMappingTest {
    @Test
    void testMapIdenticalLine() {
        Network networkReference = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        Network networkMarketBased = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        Line line = networkMarketBased.getLine("BBE1AA1  BBE2AA1  1");
        MappingResults mappingResults = UcteMapping.mapNetworks(networkReference,networkMarketBased, line.getId());
        String lineId = mappingResults.lineFromReferenceNetwork();
        assertEquals("BBE1AA1  BBE2AA1  1",lineId);
    }

    @Test
    void testMapExistingLine() {
        Network networkReference = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes_NewId.uct");
        Network networkMarketBased = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        Line line = networkMarketBased.getLine("BBE1AA1  BBE2AA1  1");
        MappingResults mappingResults = UcteMapping.mapNetworks(networkReference,networkMarketBased, line.getId());
        String lineId = mappingResults.lineFromReferenceNetwork();
        assertEquals("BBE1AA12 BBE2AA11 1",lineId);
    }
}
