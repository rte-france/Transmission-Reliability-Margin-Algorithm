/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import com.powsybl.iidm.network.Identifiable;
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
        // Given
        Network networkReference = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes_NewId.uct");
        Network networkMarketBased = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        List<String> lineIds = networkMarketBased.getLineStream().map(Identifiable::getId).toList();
        // When
        List<MappingResults> mappingResults = UcteMapping.mapNetworks(networkReference, networkMarketBased, lineIds);
        // Then
        List<MappingResults> expectedMappingResults = List.of(
                new MappingResults("BBE1AA1  BBE2AA1  1", "BBE1AA12 BBE2AA11 1", true),
                new MappingResults("BBE1AA1  BBE3AA1  1", "BBE1AA12 BBE3AA1  1", true),
                new MappingResults("FFR1AA1  FFR2AA1  1", "FFR1AA1  FFR2AA1  1", true),
                new MappingResults("FFR1AA1  FFR3AA1  1", "FFR1AA1  FFR3AA1  1", true),
                new MappingResults("FFR2AA1  FFR3AA1  1", "FFR2AA1  FFR3AA1  1", true),
                new MappingResults("DDE1AA1  DDE2AA1  1", "DDE1AA1  DDE2AA1  1", true),
                new MappingResults("DDE1AA1  DDE3AA1  1", "DDE1AA1  DDE3AA1  1", true),
                new MappingResults("DDE2AA1  DDE3AA1  1", "DDE2AA1  DDE3AA1  1", true),
                new MappingResults("NNL1AA1  NNL2AA1  1", "NNL1AA1  NNL2AA1  1", true),
                new MappingResults("NNL1AA1  NNL3AA1  1", "NNL1AA1  NNL3AA1  1", true),
                new MappingResults("NNL2AA1  NNL3AA1  1", "NNL2AA1  NNL3AA1  1", true),
                new MappingResults("FFR2AA1  DDE3AA1  1", "FFR2AA1  DDE3AA1  1", true),
                new MappingResults("DDE2AA1  NNL3AA1  1", "DDE2AA1  NNL3AA1  1", true),
                new MappingResults("NNL2AA1  BBE3AA1  1", "NNL2AA1  BBE3AA1  1", true),
                new MappingResults("BBE2AA1  FFR3AA1  1", "BBE2AA11 FFR3AA1  1", true));
        assertEquals(expectedMappingResults, mappingResults);
    }

    @Test
    void testMultiLinesList() {
        //Given
        Network networkReference = TestUtils.importNetwork("TestCase12Nodes/20170322_1844_SN3_FR2.uct");
        Network networkMarketBased = TestUtils.importNetwork("TestCase12Nodes/20170322_1844_SN3_FR2_NewId.uct");
        List<String> lineIds = networkMarketBased.getLineStream().map(Identifiable::getId).toList();
        //When
        List<MappingResults> mappingResults = UcteMapping.mapNetworks(networkReference,networkMarketBased,lineIds);
        //Then
        List<MappingResults> expectedMappingResults = List.of(
                new MappingResults("FFNHV111 FFNHV211 1", "FFNHV111 FFNHV211 1", true),
                new MappingResults("FFNHV111 FFNHV211 2", "FFNHV111 FFNHV211 2", true),
                new MappingResults("FFNHV211 FFNHV311 1", "FFNHV211 FFNHV311 1", true));
        assertEquals(expectedMappingResults, mappingResults);
    }

    @Test
    void testLineSwitchPosition() {
        //Given
        Network networkReference = TestUtils.importNetwork("TestCase12Nodes/20170322_1844_SN3_FR2.uct");
        Network networkMarketBased = TestUtils.importNetwork("TestCase12Nodes/20170322_1844_SN3_FR2_NewPosition.uct");
        List<String> lineIds = networkMarketBased.getLineStream().map(Identifiable::getId).toList();
        //When
        List<MappingResults> mappingResults = UcteMapping.mapNetworks(networkReference,networkMarketBased,lineIds);
        //Then
        List<MappingResults> expectedMappingResults = List.of(
                new MappingResults("FFNHV211 FFNHV111 1", "FFNHV111 FFNHV211 1", true),
                new MappingResults("FFNHV111 FFNHV211 2", "FFNHV111 FFNHV211 2", true),
                new MappingResults("FFNHV311 FFNHV211 1", "FFNHV211 FFNHV311 1", true));
        assertEquals(expectedMappingResults, mappingResults);
    }

    @Test
    void testWeirdCases() {
        //Given
        Network networkReference = TestUtils.importNetwork("TestCase12Nodes/20170322_1844_SN3_FR2.uct");
        Network networkMarketBased = TestUtils.importNetwork("TestCase12Nodes/20170322_1844_SN3_FR2_NewPosition.uct");
        networkReference.getLine("FFNHV111 FFNHV211 2").remove();
        networkMarketBased.getLine("FFNHV311 FFNHV211 1").remove();
        //When
        List<String> lineIds = networkMarketBased.getLineStream().map(Identifiable::getId).toList();
        List<MappingResults> mappingResults = UcteMapping.mapNetworks(networkReference,networkMarketBased,lineIds);
        //Then
        List<MappingResults> expectedMappingResults = List.of(
                new MappingResults("FFNHV211 FFNHV111 1", "FFNHV111 FFNHV211 1", true),
                new MappingResults("FFNHV111 FFNHV211 2", "", false));
        assertEquals(expectedMappingResults, mappingResults);
    }
}
