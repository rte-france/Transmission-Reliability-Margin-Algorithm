/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners.exchange_and_net_position;

import com.powsybl.glsk.commons.CountryEICode;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Map;

import static com.powsybl.iidm.network.Country.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class SplittingFactorsUtilsTest {
    private static final double DOUBLE_PRECISION = 0.001;

    @Test
    void testAdaptedNtcReductionsImport() {

        InputStream yearlyData = SplittingFactorsUtilsTest.class.getResourceAsStream("../../TestCase12Nodes/NTC_annual_CSE_simplified_without_special_lines.xml");
        InputStream dailyData = SplittingFactorsUtilsTest.class.getResourceAsStream("../../TestCase12Nodes/NTC_reductions_CSE.xml");

        Map<String, Double> splittingFactors = SplittingFactorsUtils.importSplittingFactorsFromAdaptedNtcDocs(OffsetDateTime.parse("2021-02-25T16:30Z"), yearlyData, dailyData);

        assertEquals(4, splittingFactors.size());
        assertEquals(0.456, splittingFactors.get(new CountryEICode(FR).getCode()), DOUBLE_PRECISION);
        assertEquals(0.425, splittingFactors.get(new CountryEICode(CH).getCode()), DOUBLE_PRECISION);
        assertEquals(0.045, splittingFactors.get(new CountryEICode(AT).getCode()), DOUBLE_PRECISION);
        assertEquals(0.073, splittingFactors.get(new CountryEICode(SI).getCode()), DOUBLE_PRECISION);
    }

    @Test
    void testNtcReductionsImport() {

        InputStream yearlyData = SplittingFactorsUtilsTest.class.getResourceAsStream("../../TestCase12Nodes/NTC_annual_CSE_simplified_without_special_lines_old_format.xml");
        InputStream dailyData = SplittingFactorsUtilsTest.class.getResourceAsStream("../../TestCase12Nodes/NTC_reductions_CSE_old_format.xml");

        Map<String, Double> splittingFactors = SplittingFactorsUtils.importSplittingFactorsFromNtcDocs(OffsetDateTime.parse("2021-02-25T16:30Z"), yearlyData, dailyData);

        assertEquals(4, splittingFactors.size());
        assertEquals(0.456, splittingFactors.get(new CountryEICode(FR).getCode()), DOUBLE_PRECISION);
        assertEquals(0.425, splittingFactors.get(new CountryEICode(CH).getCode()), DOUBLE_PRECISION);
        assertEquals(0.045, splittingFactors.get(new CountryEICode(AT).getCode()), DOUBLE_PRECISION);
        assertEquals(0.073, splittingFactors.get(new CountryEICode(SI).getCode()), DOUBLE_PRECISION);
    }

}
