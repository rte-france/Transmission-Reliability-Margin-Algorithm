/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners.exchange_and_net_position;

import com.farao_community.farao.cse.data.DataUtil;
import com.farao_community.farao.cse.data.ntc.*;
import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.iidm.network.Country;
import com.rte_france.trm_algorithm.TrmException;
import jakarta.xml.bind.JAXBException;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class SplittingFactorsUtils {

    private SplittingFactorsUtils() {
        // Utility class
    }

    public static Map<String, Double> importSplittingFactorsFromAdaptedNtcDocs(OffsetDateTime targetDateTime, InputStream ntcAnnualPath, InputStream ntcReductionsPath) {

        YearlyNtcDocumentAdapted yearlyNtcDocument;
        DailyNtcDocumentAdapted dailyNtcDocument;
        try {
            yearlyNtcDocument = new YearlyNtcDocumentAdapted(targetDateTime, DataUtil.unmarshalFromInputStream(ntcAnnualPath, com.farao_community.farao.cse.data.xsd.ntc_adapted.NTCAnnualDocument.class));
            dailyNtcDocument = new DailyNtcDocumentAdapted(targetDateTime, DataUtil.unmarshalFromInputStream(ntcReductionsPath, com.farao_community.farao.cse.data.xsd.ntc_adapted.NTCReductionsDocument.class));

        } catch (JAXBException e) {
            throw new TrmException("An error occurred in the adapted NTC files import for " + targetDateTime + ": " + e);
        }

        Ntc ntc = new Ntc(yearlyNtcDocument, dailyNtcDocument, true);
        Map<String, Double> reducedSplittingFactors = new HashMap<>();
        ntc.computeReducedSplittingFactors().forEach((country, value) -> reducedSplittingFactors.put(new CountryEICode(Country.valueOf(country)).getCode(), value));

        return reducedSplittingFactors;

    }

    public static Map<String, Double> importSplittingFactorsFromNtcDocs(OffsetDateTime targetDateTime, InputStream ntcAnnualPath, InputStream ntcReductionsPath) {

        YearlyNtcDocument yearlyNtcDocument;
        DailyNtcDocument dailyNtcDocument;
        try {
            yearlyNtcDocument = new YearlyNtcDocument(targetDateTime, DataUtil.unmarshalFromInputStream(ntcAnnualPath, com.farao_community.farao.cse.data.xsd.NTCAnnualDocument.class));
            dailyNtcDocument = new DailyNtcDocument(targetDateTime, DataUtil.unmarshalFromInputStream(ntcReductionsPath, com.farao_community.farao.cse.data.xsd.NTCReductionsDocument.class));

        } catch (JAXBException e) {
            throw new TrmException("An error occurred in the NTC files import for " + targetDateTime + ": " + e);
        }

        Ntc ntc = new Ntc(yearlyNtcDocument, dailyNtcDocument, false);
        Map<String, Double> reducedSplittingFactors = new HashMap<>();
        ntc.computeReducedSplittingFactors().forEach((country, value) -> reducedSplittingFactors.put(new CountryEICode(Country.valueOf(country)).getCode(), value));

        return reducedSplittingFactors;

    }
}
