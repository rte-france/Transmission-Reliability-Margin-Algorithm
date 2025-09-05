/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastian Huaraca {@literal <sebastian.huaracalapa at rte-france.com>}
 */
public class UcteMapperTest {
    @Test
    void testMultiLines() {
        // Given
        Network networkReference = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes_NewId.uct");
        Network networkMarketBased = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        // When
        IdentifiableMapping mappingResults = UcteMapper.mapNetworks(networkReference, networkMarketBased);
        //Then
        assertEquals("BBE1AA1  BBE2AA1  1", mappingResults.idInMarketBased("BBE1AA12 BBE2AA11 1"));
        assertEquals("DDE1AA1  DDE2AA1  1", mappingResults.idInMarketBased("DDE1AA1  DDE2AA1  1"));
        assertEquals("NNL2AA1  BBE3AA1  1", mappingResults.idInReference("NNL2AA1  BBE3AA1  1"));
        assertEquals("BBE2AA11 BBE3AA1  1", mappingResults.idInReference("BBE2AA1  BBE3AA1  1"));
    }

    @Test
    void testSameOrderCode() {
        //Given
        Network networkReference = TestUtils.importNetwork("TestCase12Nodes/20170322_1844_SN3_FR2.uct");
        Network networkMarketBased = TestUtils.importNetwork("TestCase12Nodes/20170322_1844_SN3_FR2_NewId.uct");
        //When
        IdentifiableMapping mappingResults = UcteMapper.mapNetworks(networkReference, networkMarketBased);
        //Then
        assertEquals("FFNHV211 FFNHV311 1", mappingResults.idInReference("FFNHV211 FFNHV311 1"));
        assertThrows(IdMappingNotFoundException.class, () -> {
            assertEquals("FFNHV111 FFNHV211 1", mappingResults.idInReference("FFNHV111 FFNHV211 1"));
        });
        assertEquals("FFNHV211 FFNHV311 1", mappingResults.idInMarketBased("FFNHV211 FFNHV311 1"));
    }

    @Test
    void testLineSwitchSubstationPosition() {
        //Given
        Network networkReference = TestUtils.importNetwork("TestCase12Nodes/20170322_1844_SN3_FR2.uct");
        Network networkMarketBased = TestUtils.importNetwork("TestCase12Nodes/20170322_1844_SN3_FR2_NewPosition.uct");
        //When
        IdentifiableMapping mappingResults = UcteMapper.mapNetworks(networkReference, networkMarketBased);
        //Then
        assertEquals("FFNHV111 FFNHV211 1", mappingResults.idInReference("FFNHV211 FFNHV111 1"));
        assertEquals("FFNHV211 FFNHV111 1", mappingResults.idInMarketBased("FFNHV111 FFNHV211 1"));
    }

    @Test
    void testRemoveLines() {
        //Given
        Network networkReference = TestUtils.importNetwork("TestCase12Nodes/20170322_1844_SN3_FR2.uct");
        Network networkMarketBased = TestUtils.importNetwork("TestCase12Nodes/20170322_1844_SN3_FR2_NewPosition.uct");
        networkReference.getLine("FFNHV111 FFNHV211 2").remove();
        networkMarketBased.getLine("FFNHV311 FFNHV211 1").remove();
        //When
        IdentifiableMapping mappingResults = UcteMapper.mapNetworks(networkReference, networkMarketBased);
        //Then
        networkMarketBased.getLine("FFNHV111 FFNHV211 2");
        assertThrows(IdMappingNotFoundException.class, () -> {
            assertEquals("FFNHV111 FFNHV211 2", mappingResults.idInMarketBased("FFNHV111 FFNHV211 2"));
        });
        networkReference.getLine("FFNHV311 FFNHV211 1");
        assertThrows(IdMappingNotFoundException.class, () -> {
            assertEquals("FFNHV311 FFNHV211 1", mappingResults.idInReference("FFNHV311 FFNHV211 1"));
        });
    }

    @Test
    void testDuplicateValues() {
        //Given
        Network networkReference = TestUtils.importNetwork("TestCase12Nodes/20170322_1844_SN3_FR2_Repetitive.uct");
        Network networkMarketBased = TestUtils.importNetwork("TestCase12Nodes/20170322_1844_SN3_FR2_NewDuplicate.uct");
        //When
        IdentifiableMapping mappingResults = UcteMapper.mapNetworks(networkReference, networkMarketBased);
        //Then
        assertEquals("FFNHV211 FFNHV311 1", mappingResults.idInReference("FFNHV311 FFNHV211 1"));
        assertEquals("FFNHV211 FFNLOA31 L", mappingResults.idInReference("FFNHV211 FFNLOA31 L"));
        assertEquals("FFNHV111 FFNHV211 1", mappingResults.idInReference("FFNHV211 FFNHV111 1"));
        assertEquals("FFNGEN71 FFNHV111 1", mappingResults.idInReference("FFNGEN71 FFNHV111 1"));
        assertEquals("FFNHV311 FFNHV211 1", mappingResults.idInMarketBased("FFNHV211 FFNHV311 1"));
        assertEquals("FFNHV211 FFNLOA31 L", mappingResults.idInMarketBased("FFNHV211 FFNLOA31 L"));
        assertEquals("FFNGEN71 FFNHV111 1", mappingResults.idInMarketBased("FFNGEN71 FFNHV111 1"));
    }

    @Test
    void testChosenCountries() {
        //Given
        Network networkReference = TestUtils.importNetwork("TestCase12Nodes/NETWORK_TEST_IN_REFERENCE.uct");
        Network networkMarketBased = TestUtils.importNetwork("TestCase12Nodes/NETWORK_TEST_IN.uct");
        //When
        IdentifiableMapping mappingResults =  UcteMapper.mapNetworks(networkReference, networkMarketBased);
        IdentifiableMapping mappingResultsChosenCountrys = UcteMapper.mapNetworks(networkReference, networkMarketBased, Country.IT, Country.FR, Country.SI, Country.CH, Country.AT);
        //Then
        assertEquals("DGENE111 DLOAD111 1", mappingResults.idInReference("DGENE111 DLOAD111 1"));
        assertThrows(IdMappingNotFoundException.class, () -> {
            assertEquals("DGENE111 DLOAD111 1", mappingResultsChosenCountrys.idInReference("DGENE111 DLOAD111 1"));
        });
    }

    @Test
    void testApplyMappingAliases() {
        // Given
        Network networkReference = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes_NewId.uct");
        Network networkMarketBased = TestUtils.importNetwork("TestCase12Nodes/TestCase12Nodes.uct");
        // When
        UcteMapper.mapNetworksAndAddAliases(networkReference, networkMarketBased);
        //Then
        assertEquals(networkReference.getIdentifiable("BBE1AA1  BBE2AA1  1"), networkReference.getIdentifiable("BBE1AA12 BBE2AA11 1"));
        assertEquals(networkMarketBased.getIdentifiable("BBE1AA1  BBE2AA1  1"), networkMarketBased.getIdentifiable("BBE1AA12 BBE2AA11 1"));
    }
}
