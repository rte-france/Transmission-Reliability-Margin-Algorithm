/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.farao_community.farao.cse.data.DataUtil;
import com.farao_community.farao.cse.data.ntc.DailyNtcDocument;
import com.farao_community.farao.cse.data.ntc.Ntc;
import com.farao_community.farao.cse.data.ntc.YearlyNtcDocument;
import com.farao_community.farao.cse.data.xsd.NTCAnnualDocument;
import com.farao_community.farao.cse.data.xsd.NTCReductionsDocument;
import com.farao_community.farao.dichotomy.api.exceptions.GlskLimitationException;
import com.farao_community.farao.dichotomy.api.exceptions.ShiftingException;
import com.farao_community.farao.dichotomy.shift.LinearScaler;
import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;

import com.rte_france.trm_algorithm.TrmUtils;
import com.rte_france.trm_algorithm.operational_conditions_aligners.exchange_and_net_position.ExchangeAndNetPosition;
import jakarta.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.powsybl.iidm.network.Country.*;
import static java.lang.Math.abs;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class ItalyNorthExchangeAligner implements OperationalConditionAligner {
    private static final double EXCHANGE_EPSILON = 1e-1;
    private static final Logger LOGGER = LoggerFactory.getLogger(ItalyNorthExchangeAligner.class);
    private final Map<String, Double> reducedSplittingFactors;

    public ItalyNorthExchangeAligner(Map<String, Double> reducedSplittingFactors) {
        this.reducedSplittingFactors = reducedSplittingFactors;
    }

    @Override
    public void align(Network referenceNetwork, Network marketBasedNetwork) {
        LOGGER.info("Aligning North Italian exchanges");

        ExchangeAndNetPosition referenceExchangeAndNetPosition = computeExchangeAndNetPosition(referenceNetwork);
        ExchangeAndNetPosition initialMarketBasedExchangeAndNetPosition = computeExchangeAndNetPosition(marketBasedNetwork);

        ZonalData<Scalable> zonalScalable = TrmUtils.getAutoScalable(marketBasedNetwork);

        try {
            // value : (opposite to) the Italian NP in the reference network
            // ntc : ntc in the market based network
            Map<String, Double> ntcs = updateMarketBasedNtcs(initialMarketBasedExchangeAndNetPosition);

            LOGGER.info("Initial " + npSummaryForLogs(initialMarketBasedExchangeAndNetPosition, referenceExchangeAndNetPosition));
            shiftNetwork(marketBasedNetwork, reducedSplittingFactors, ntcs, zonalScalable, referenceExchangeAndNetPosition);

            referenceExchangeAndNetPosition = computeExchangeAndNetPosition(referenceNetwork);
            ExchangeAndNetPosition marketBasedExchangeAndNetPosition = computeExchangeAndNetPosition(marketBasedNetwork);

            int nbIterations = 1;
            while (nbIterations < 20 & abs(referenceExchangeAndNetPosition.getNetPosition(Country.IT) - marketBasedExchangeAndNetPosition.getNetPosition(Country.IT)) > EXCHANGE_EPSILON) {

                nbIterations++;
                LOGGER.info(npSummaryForLogs(marketBasedExchangeAndNetPosition, referenceExchangeAndNetPosition) + " Iteration " + nbIterations + " will be run.");

                ntcs = updateMarketBasedNtcs(marketBasedExchangeAndNetPosition);

                shiftNetwork(marketBasedNetwork, reducedSplittingFactors, ntcs, zonalScalable, referenceExchangeAndNetPosition);
                referenceExchangeAndNetPosition = computeExchangeAndNetPosition(referenceNetwork);
                marketBasedExchangeAndNetPosition = computeExchangeAndNetPosition(marketBasedNetwork);
            }

            if (abs(referenceExchangeAndNetPosition.getNetPosition(Country.IT) - marketBasedExchangeAndNetPosition.getNetPosition(Country.IT)) > EXCHANGE_EPSILON) {
                LOGGER.error("North Italian exchange aligner failed: nb max iterations is reached. " + npSummaryForLogs(initialMarketBasedExchangeAndNetPosition, referenceExchangeAndNetPosition));
            }
            LOGGER.info("Both networks are aligned. North Italian NP is " + marketBasedExchangeAndNetPosition.getNetPosition(Country.IT) + " MW.");

            finalDebugLoggerNp("reference", referenceExchangeAndNetPosition);
            finalDebugLoggerNp("initial market-based", initialMarketBasedExchangeAndNetPosition);
            finalDebugLoggerNp("final market-based", marketBasedExchangeAndNetPosition);
            finalDebugLoggerShift(initialMarketBasedExchangeAndNetPosition, marketBasedExchangeAndNetPosition);

        } catch (ShiftingException | GlskLimitationException e) {
            throw new RuntimeException(e);
        }
    }

    static Map<String, Double> importSplittingFactorsFromNtcDocs(OffsetDateTime targetDateTime, String ntcAnnualPath, String ntcReductionsPath) {
        try (InputStream yearlyData = ItalyNorthExchangeAligner.class.getResourceAsStream(ntcAnnualPath);
             InputStream dailyData = ItalyNorthExchangeAligner.class.getResourceAsStream(ntcReductionsPath)
        ) {
            YearlyNtcDocument yearlyNtcDocument = new YearlyNtcDocument(targetDateTime, DataUtil.unmarshalFromInputStream(yearlyData, NTCAnnualDocument.class));
            DailyNtcDocument dailyNtcDocument = new DailyNtcDocument(targetDateTime, DataUtil.unmarshalFromInputStream(dailyData, NTCReductionsDocument.class));
            Ntc ntc = new Ntc(yearlyNtcDocument, dailyNtcDocument, false);

            Map<String, Double> reducedSplittingFactors = new HashMap<>();
            ntc.computeReducedSplittingFactors().forEach((country, value) -> {
                reducedSplittingFactors.put(new CountryEICode(Country.valueOf(country)).getCode(), value);
            });

            return reducedSplittingFactors;

        } catch (IOException | JAXBException e) {
            throw new RuntimeException("An error occured in the NTC files import for " + targetDateTime + ": ", e);
        }
    }

    private static void finalDebugLoggerNp(String networkName, ExchangeAndNetPosition referenceExchangeAndNetPosition) {
        LOGGER.debug("Net positions in the " + networkName
                + " network: " + printNpCountry(IT, referenceExchangeAndNetPosition) + ", "
                + printNpCountry(AT, referenceExchangeAndNetPosition) + ", "
                + printNpCountry(CH, referenceExchangeAndNetPosition) + ", "
                + printNpCountry(FR, referenceExchangeAndNetPosition) + ", "
                + printNpCountry(SI, referenceExchangeAndNetPosition) + ", "
                + printNpCountry(DE, referenceExchangeAndNetPosition) + ".");
    }

    private static void finalDebugLoggerShift(ExchangeAndNetPosition initialMarketBasedExchangeAndNetPosition, ExchangeAndNetPosition marketBasedExchangeAndNetPosition) {
        LOGGER.debug("Shift performed on the market-based network: "
                + printShiftCountry(IT, marketBasedExchangeAndNetPosition, initialMarketBasedExchangeAndNetPosition) + ", "
                + printShiftCountry(AT, marketBasedExchangeAndNetPosition, initialMarketBasedExchangeAndNetPosition) + ", "
                + printShiftCountry(CH, marketBasedExchangeAndNetPosition, initialMarketBasedExchangeAndNetPosition) + ", "
                + printShiftCountry(FR, marketBasedExchangeAndNetPosition, initialMarketBasedExchangeAndNetPosition) + ", "
                + printShiftCountry(SI, marketBasedExchangeAndNetPosition, initialMarketBasedExchangeAndNetPosition) + ", "
                + printShiftCountry(DE, marketBasedExchangeAndNetPosition, initialMarketBasedExchangeAndNetPosition) + ".");
    }

    private static String printNpCountry(Country country, ExchangeAndNetPosition exchangeAndNetPosition) {
        return country.getName() + " = " +  Math.round(exchangeAndNetPosition.getNetPosition(country) * 1000.) / 1000. + " MW";
    }

    private static String printShiftCountry(Country country, ExchangeAndNetPosition marketBasedExchangeAndNetPosition, ExchangeAndNetPosition initialMarketBasedExchangeAndNetPosition) {
        return country.getName() + " = " +  Math.round((marketBasedExchangeAndNetPosition.getNetPosition(country) - initialMarketBasedExchangeAndNetPosition.getNetPosition(country)) * 1000.) / 1000. + " MW";
    }

    private static String npSummaryForLogs(ExchangeAndNetPosition marketBasedExchangeAndNetPosition, ExchangeAndNetPosition referenceExchangeAndNetPosition) {
        return "North Italian NP is " + marketBasedExchangeAndNetPosition.getNetPosition(Country.IT)
                + " MW for the market based network and " + referenceExchangeAndNetPosition.getNetPosition(Country.IT)
                + "MW for the reference network.";
    }

    private static void shiftNetwork(Network marketBasedNetwork, Map<String, Double> reducedSplittingFactors, Map<String, Double> ntcs, ZonalData<Scalable> zonalScalable, ExchangeAndNetPosition referenceExchangeAndNetPosition) throws GlskLimitationException, ShiftingException {
        ShiftDispatcher shiftDispatcher = new CseD2ccShiftDispatcherTmp(LOGGER, reducedSplittingFactors, ntcs);
        LinearScaler linearScaler = new LinearScaler(zonalScalable, shiftDispatcher);
        linearScaler.shiftNetwork(-referenceExchangeAndNetPosition.getNetPosition(IT), marketBasedNetwork);
    }

    ExchangeAndNetPosition computeExchangeAndNetPosition(Network network) {
        LoadFlow.run(network);
        return new ExchangeAndNetPosition(network);
    }

    private static Map<String, Double> updateMarketBasedNtcs(ExchangeAndNetPosition marketBasedExchangeAndNetPosition) {
        Map<String, Double> ntcs = Map.of(
                new CountryEICode(FR).getCode(), marketBasedExchangeAndNetPosition.getNetPosition(FR),
                new CountryEICode(CH).getCode(), marketBasedExchangeAndNetPosition.getNetPosition(CH) + marketBasedExchangeAndNetPosition.getNetPosition(DE),
                new CountryEICode(AT).getCode(), marketBasedExchangeAndNetPosition.getNetPosition(AT),
                new CountryEICode(SI).getCode(), marketBasedExchangeAndNetPosition.getNetPosition(SI)
        );
        return ntcs;
    }
}
