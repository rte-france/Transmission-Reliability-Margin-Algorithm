/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.id_mapping;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.rte_france.trm_algorithm.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastian Huaraca {@literal <sebastian.huaracalapa at rte-france.com>}
 */

public class UcteMappingTest {
    @Test
    void testMultiLines() {
        // Given
        Network networkReference = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes_NewId.uct");
        Network networkMarketBased = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        // When
        IdentifiableMapp mappingResults = UcteMapping.mapNetworks(networkReference, networkMarketBased);
        assertEquals("BBE1AA1  BBE2AA1  1", mappingResults.idInMarketBased("BBE1AA12 BBE2AA11 1"));
        assertEquals("DDE1AA1  DDE2AA1  1", mappingResults.idInMarketBased("DDE1AA1  DDE2AA1  1"));
        assertEquals("NNL2AA1  BBE3AA1  1", mappingResults.idInReference("NNL2AA1  BBE3AA1  1"));
        assertEquals("BBE2AA11 BBE3AA1  1", mappingResults.idInReference("BBE2AA1  BBE3AA1  1"));
        assertThrows(IdMappingNotFoundException.class, () -> mappingResults.idInReference("NON EXISTING ID"));
    }

    @Test
    void testMultiLinesList() {
        //Given
        Network networkReference = TestUtils.importNetwork("TestCase12Nodes/20170322_1844_SN3_FR2.uct");
        Network networkMarketBased = TestUtils.importNetwork("TestCase12Nodes/20170322_1844_SN3_FR2_NewId.uct");
        //When
        IdentifiableMapp mappingResults = UcteMapping.mapNetworks(networkReference, networkMarketBased);
        //Then
        List<MappingResults> expectedMappingResults = List.of(
                new MappingResults("FFNHV111 FFNHV211 1", "FFNHV111 FFNHV211 1", true),
                new MappingResults("FFNHV111 FFNHV211 2", "FFNHV111 FFNHV211 2", true),
                new MappingResults("FFNHV211 FFNHV311 1", "FFNHV211 FFNHV311 1", true),
                new MappingResults("FFNGEN71 FFNHV111 1", "FFNGEN71 FFNHV111 1", true),
                new MappingResults("FFNHV211 FFNLOA31 L", "FFNHV211 FFNLOA31 L", true));
        //assertEquals(expectedMappingResults, mappingResults.mappingResults());
    }

    @Test
    void testLineSwitchPosition() {
        //Given
        Network networkReference = TestUtils.importNetwork("TestCase12Nodes/20170322_1844_SN3_FR2.uct");
        Network networkMarketBased = TestUtils.importNetwork("TestCase12Nodes/20170322_1844_SN3_FR2_NewPosition.uct");
        //When
        IdentifiableMapp mappingResults = UcteMapping.mapNetworks(networkReference, networkMarketBased);
        //Then
        List<MappingResults> expectedMappingResults = List.of(
                new MappingResults("FFNHV211 FFNHV111 1", "FFNHV111 FFNHV211 1", true),
                new MappingResults("FFNHV111 FFNHV211 2", "FFNHV111 FFNHV211 2", true),
                new MappingResults("FFNHV311 FFNHV211 1", "FFNHV211 FFNHV311 1", true),
                new MappingResults("FFNGEN71 FFNHV111 1", "FFNGEN71 FFNHV111 1", true),
                new MappingResults("FFNHV211 FFNLOA31 L", "FFNHV211 FFNLOA31 L", true));
        //assertEquals(expectedMappingResults, mappingResults.mappingResults());
    }

    @Test
    void testRemoveLines() {
        //Given
        Network networkReference = TestUtils.importNetwork("TestCase12Nodes/20170322_1844_SN3_FR2.uct");
        Network networkMarketBased = TestUtils.importNetwork("TestCase12Nodes/20170322_1844_SN3_FR2_NewPosition.uct");
        networkReference.getLine("FFNHV111 FFNHV211 2").remove();
        networkMarketBased.getLine("FFNHV311 FFNHV211 1").remove();
        //When
        IdentifiableMapp mappingResults = UcteMapping.mapNetworks(networkReference, networkMarketBased);
        //Then
        assertEquals("FFNHV211 FFNHV111 1", mappingResults.idInMarketBased("FFNHV111 FFNHV211 1"));
        assertEquals("FFNGEN71 FFNHV111 1", mappingResults.idInReference("FFNGEN71 FFNHV111 1"));
        assertThrows(IdMappingNotFoundException.class, () -> mappingResults.idInReference("FFNHV111 FFNHV211 2"));
    }

    @Test
    void testDuplicateValues() {
        //Given
        Network networkReference = TestUtils.importNetwork("TestCase12Nodes/20170322_1844_SN3_FR2_Repetitive.uct");
        Network networkMarketBased = TestUtils.importNetwork("TestCase12Nodes/20170322_1844_SN3_FR2_NewDuplicate.uct");
        //When
        IdentifiableMapp mappingResults = UcteMapping.mapNetworks(networkReference, networkMarketBased);
        //Then
        assertEquals("FFNHV311 FFNHV211 1", mappingResults.idInMarketBased("FFNHV211 FFNHV311 1"));
        assertEquals("FFNHV211 FFNLOA31 L", mappingResults.idInReference("FFNHV211 FFNLOA31 L"));
        assertThrows(IdMappingNotFoundException.class, () -> mappingResults.idInMarketBased("FFNHV111 FFNHV211 1"));
    }

    @Test
    void testRealNetwork() {
        //Given
        Network networkReference = Network.read("/home/huaracaseb/Bureau/Pruebas/Reference/2023_1/20230101_0330_SN7_UX0.uct");
        Network networkMarketBased = Network.read("/home/huaracaseb/Bureau/Pruebas/MarketBased/2023_01/20230101_0330_FO7_UX1.uct");
        //When
        IdentifiableMapp mappingResults = UcteMapping.mapNetworks(networkReference, networkMarketBased, Country.FR, Country.CH, Country.AT, Country.SI, Country.IT);
    }

    @Test
    void testNewMappingMap() {
        //Given
        Network networkReference = TestUtils.importNetwork("TestCase12Nodes/NETWORK_TEST_IN_REFERENCE.uct");
        Network networkMarketBased = TestUtils.importNetwork("TestCase12Nodes/NETWORK_TEST_IN_MARKET.uct");
        //When
        IdentifiableMapp mappingResults = UcteMapping.mapNetworks(networkReference, networkMarketBased, Country.IT, Country.FR, Country.SI, Country.CH, Country.AT);
    }
}
