/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.google.common.collect.ImmutableMap;
import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.rte_france.trm_algorithm.TestUtils;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Map;

import static com.powsybl.iidm.network.Country.*;
import static com.rte_france.trm_algorithm.operational_conditions_aligners.ExchangeAlignerStatus.*;
import static com.rte_france.trm_algorithm.operational_conditions_aligners.exchange_and_net_position.SplittingFactorsUtils.importSplittingFactorsFromNtcDocs;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class ItalyNorthExchangeAlignerTest {
    public static final double EPSILON = 1e-1;

    @Test
    void testSimpleShift() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase12Nodes/NETWORK_TEST_IN_REFERENCE.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase12Nodes/NETWORK_TEST_IN.uct");
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();

        Map<String, Double> reducedSplittingFactors = ImmutableMap.of(
                new CountryEICode(FR).getCode(), 0.4,
                new CountryEICode(AT).getCode(), 0.3,
                new CountryEICode(CH).getCode(), 0.1,
                new CountryEICode(SI).getCode(), 0.2
        );

        ItalyNorthExchangeAligner italyNorthExchangeAligner = new ItalyNorthExchangeAligner(loadFlowParameters, reducedSplittingFactors);
        italyNorthExchangeAligner.align(referenceNetwork, marketBasedNetwork);
        ItalyNorthExchangeAlignerResult italyNorthExchangeAlignerResult = italyNorthExchangeAligner.getResult();

        assertEquals(ALIGNED_WITH_SHIFT, italyNorthExchangeAlignerResult.getStatus());
        assertEquals(-2970.077, italyNorthExchangeAlignerResult.getReferenceExchangeAndNetPosition().getNetPosition(IT), EPSILON);
        assertEquals(-1948.416, italyNorthExchangeAlignerResult.getInitialMarketBasedExchangeAndNetPosition().getNetPosition(IT), EPSILON);
        assertEquals(-2970.077, italyNorthExchangeAlignerResult.getNewMarketBasedExchangeAndNetPosition().getNetPosition(IT), EPSILON);

        assertEquals(705.517, italyNorthExchangeAlignerResult.getNewMarketBasedExchangeAndNetPosition().getNetPosition(AT), EPSILON);
        assertEquals(425.356, italyNorthExchangeAlignerResult.getNewMarketBasedExchangeAndNetPosition().getNetPosition(CH), EPSILON);
        assertEquals(1029.614, italyNorthExchangeAlignerResult.getNewMarketBasedExchangeAndNetPosition().getNetPosition(FR), EPSILON);
        assertEquals(307.032, italyNorthExchangeAlignerResult.getNewMarketBasedExchangeAndNetPosition().getNetPosition(SI), EPSILON);
    }

    @Test
    void testAlreadyAligned() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase12Nodes/NETWORK_TEST_IN_REFERENCE.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase12Nodes/NETWORK_TEST_IN.uct");
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();

        Map<String, Double> reducedSplittingFactors = ImmutableMap.of(
                new CountryEICode(AT).getCode(), 0.3,
                new CountryEICode(CH).getCode(), 0.1,
                new CountryEICode(FR).getCode(), 0.4,
                new CountryEICode(SI).getCode(), 0.2
        );

        ItalyNorthExchangeAligner italyNorthExchangeAligner = new ItalyNorthExchangeAligner(loadFlowParameters, reducedSplittingFactors);
        italyNorthExchangeAligner.align(referenceNetwork, marketBasedNetwork);
        ItalyNorthExchangeAlignerResult italyNorthExchangeAlignerResult = italyNorthExchangeAligner.getResult();
        assertEquals(ALIGNED_WITH_SHIFT, italyNorthExchangeAlignerResult.getStatus());

        ItalyNorthExchangeAligner italyNorthExchangeAligner2 = new ItalyNorthExchangeAligner(loadFlowParameters, reducedSplittingFactors);
        italyNorthExchangeAligner2.align(referenceNetwork, marketBasedNetwork);
        ItalyNorthExchangeAlignerResult italyNorthExchangeAlignerResult2 = italyNorthExchangeAligner2.getResult();
        assertEquals(ALREADY_ALIGNED, italyNorthExchangeAlignerResult2.getStatus());
    }

    @Test
    void testShiftWithImportedSplittingFactors() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase12Nodes/NETWORK_TEST_IN_REFERENCE.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase12Nodes/NETWORK_TEST_IN.uct");
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();

        InputStream yearlyData = ItalyNorthExchangeAlignerTest.class.getResourceAsStream("../TestCase12Nodes/NTC_annual_CSE_simplified_without_special_lines.xml");
        InputStream dailyData = ItalyNorthExchangeAlignerTest.class.getResourceAsStream("../TestCase12Nodes/NTC_reductions_CSE.xml");

        Map<String, Double> reducedSplittingFactors = importSplittingFactorsFromNtcDocs(OffsetDateTime.parse("2021-02-25T16:30Z"), yearlyData, dailyData);

        // These are not the splitting factors but the reduced splitting factors
        assertEquals(0.046, reducedSplittingFactors.get(new CountryEICode(AT).getCode()), EPSILON);
        assertEquals(0.425, reducedSplittingFactors.get(new CountryEICode(CH).getCode()), EPSILON);
        assertEquals(0.457, reducedSplittingFactors.get(new CountryEICode(FR).getCode()), EPSILON);
        assertEquals(0.073, reducedSplittingFactors.get(new CountryEICode(SI).getCode()), EPSILON);

        ItalyNorthExchangeAligner italyNorthExchangeAligner = new ItalyNorthExchangeAligner(loadFlowParameters, reducedSplittingFactors);
        italyNorthExchangeAligner.align(referenceNetwork, marketBasedNetwork);
        ItalyNorthExchangeAlignerResult italyNorthExchangeAlignerResult = italyNorthExchangeAligner.getResult();

        assertEquals(-2970.077, italyNorthExchangeAlignerResult.getReferenceExchangeAndNetPosition().getNetPosition(IT), EPSILON);
        assertEquals(-1948.416, italyNorthExchangeAlignerResult.getInitialMarketBasedExchangeAndNetPosition().getNetPosition(IT), EPSILON);
        assertEquals(-2970.077, italyNorthExchangeAlignerResult.getNewMarketBasedExchangeAndNetPosition().getNetPosition(IT), EPSILON);

        assertEquals(446.902, italyNorthExchangeAlignerResult.getNewMarketBasedExchangeAndNetPosition().getNetPosition(AT), EPSILON);
        assertEquals(756.477, italyNorthExchangeAlignerResult.getNewMarketBasedExchangeAndNetPosition().getNetPosition(CH), EPSILON);
        assertEquals(1086.250, italyNorthExchangeAlignerResult.getNewMarketBasedExchangeAndNetPosition().getNetPosition(FR), EPSILON);
        assertEquals(177.731, italyNorthExchangeAlignerResult.getNewMarketBasedExchangeAndNetPosition().getNetPosition(SI), EPSILON);
    }
}
