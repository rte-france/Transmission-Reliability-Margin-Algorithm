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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Sebastian Huaraca {@literal <sebastian.huaracalapa at rte-france.com>}
 */

public class UcteMappingTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(UcteMapping.class);
    @Test
    void testMapIdenticalLine() {
        Network networkReference = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        Network networkMarketBased = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        Line line = networkMarketBased.getLine("BBE1AA1  BBE2AA1  1");
        MappingResults mappingResults = UcteMapping.mapNetworks(networkReference, networkMarketBased, line.getId());
        String lineId = mappingResults.lineFromReferenceNetwork();
        assertEquals("BBE1AA1  BBE2AA1  1", lineId);
    }

    @Test
    void testMapExistingLine() {
        Network networkReference = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes_NewId.uct");
        Network networkMarketBased = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        Line line = networkMarketBased.getLine("BBE1AA1  BBE2AA1  1");
        MappingResults mappingResults = UcteMapping.mapNetworks(networkReference, networkMarketBased, line.getId());
        String lineId = mappingResults.lineFromReferenceNetwork();
        assertEquals("BBE1AA12 BBE2AA11 1", lineId);
        System.out.println();
    }

    @Test
    void testMultiLines() {
        Network networkReference = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes_NewId.uct");
        Network networkMarketBased = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        List<Line> lines;
        lines = networkMarketBased.getLineStream().toList();
        Map<String,String> groupedCodLines = new HashMap<>();
        for (Line line : lines) {
            MappingResults mappingResults = UcteMapping.mapNetworks(networkReference, networkMarketBased, line.getId());
            String lineId = mappingResults.lineFromReferenceNetwork();
             assert mappingResults.mappingFound()==true;
             groupedCodLines.put(lineId,line.getId());
        }
    }

    @Test
    void testMultiLinesList() {
        Network networkReference = TestUtils.importNetwork("TestCase12Nodes/20170322_1844_SN3_FR2.uct");
        Network networkMarketBased = TestUtils.importNetwork("TestCase12Nodes/20170322_1844_SN3_FR2_NewId.uct");
        List<String> linesId = new ArrayList<>();
        networkMarketBased.getLines().forEach(line -> {
            linesId.add(line.getId());
        });
        List<MappingResults> mappingResults = UcteMapping.mapNetworks(networkReference,networkMarketBased,linesId);
        Map<String,String> groupedCodLines = new HashMap<>();
        mappingResults.forEach(n -> {
            groupedCodLines.put(n.lineFromMarketBasedNetwork(),n.lineFromReferenceNetwork());
        });
    }

    @Test
    void testLineElementName() {
        Network networkReference = TestUtils.importNetwork("TestCase12Nodes/20170322_1844_SN3_FR2.uct");
        Network networkMarketBased = TestUtils.importNetwork("TestCase12Nodes/20170322_1844_SN3_FR2.uct");
        List<String> linesId = new ArrayList<>();
        networkMarketBased.getLines().forEach(line -> {
            linesId.add(line.getId());
        });
        String elementName = networkReference.getLine("FFNHV111 FFNHV211 1").getProperty("elementName");
        System.out.println(elementName);
    }
}
