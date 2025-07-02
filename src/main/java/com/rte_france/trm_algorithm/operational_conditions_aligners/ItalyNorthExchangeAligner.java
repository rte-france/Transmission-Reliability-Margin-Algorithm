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

import com.powsybl.loadflow.LoadFlowParameters;
import com.rte_france.trm_algorithm.TrmException;
import com.rte_france.trm_algorithm.TrmUtils;
import com.rte_france.trm_algorithm.operational_conditions_aligners.exchange_and_net_position.ExchangeAndNetPosition;
import com.rte_france.trm_algorithm.operational_conditions_aligners.exchange_and_net_position.ExchangeAndNetPositionInterface;
import jakarta.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.powsybl.iidm.network.Country.*;
import static com.rte_france.trm_algorithm.operational_conditions_aligners.ExchangeAlignerStatus.*;
import static java.lang.Math.abs;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class ItalyNorthExchangeAligner implements OperationalConditionAligner {
    private static final double EXCHANGE_EPSILON = 1e-1;
    private static final Logger LOGGER = LoggerFactory.getLogger(ItalyNorthExchangeAligner.class);
    private final LoadFlowParameters loadFlowParameters;
    private final Map<String, Double> reducedSplittingFactors;
    private ItalyNorthExchangeAlignerResult result = null;

    public ItalyNorthExchangeAligner(LoadFlowParameters loadFlowParameters, Map<String, Double> reducedSplittingFactors) {
        this.loadFlowParameters = loadFlowParameters;
        this.reducedSplittingFactors = reducedSplittingFactors;
    }

    public ItalyNorthExchangeAlignerResult getItalyNorthExchangeAlignerResult() {
        return result;
    }

    @Override
    public void align(Network referenceNetwork, Network marketBasedNetwork) {
        LOGGER.info("Aligning North Italian exchanges");

        ExchangeAndNetPosition referenceExchangeAndNetPosition = computeExchangeAndNetPosition(referenceNetwork);
        ExchangeAndNetPosition initialMarketBasedExchangeAndNetPosition = computeExchangeAndNetPosition(marketBasedNetwork);

        ZonalData<Scalable> zonalScalable = TrmUtils.getAutoScalable(marketBasedNetwork);

        ItalyNorthExchangeAlignerResult.Builder builder = ItalyNorthExchangeAlignerResult.builder()
                .addReferenceExchangeAndNetPosition(referenceExchangeAndNetPosition)
                .addInitialMarketBasedExchangeAndNetPositions(initialMarketBasedExchangeAndNetPosition);

        if (abs(referenceExchangeAndNetPosition.getNetPosition(IT) - initialMarketBasedExchangeAndNetPosition.getNetPosition(IT)) < EXCHANGE_EPSILON) {
            LOGGER.info("No significant exchange difference. Exchange alignment ignored!");
            result = builder.addNewMarketBasedExchangeAndNetPositions(initialMarketBasedExchangeAndNetPosition).addExchangeAlignerStatus(ALREADY_ALIGNED).build();
            return;
        }

        try {
            LOGGER.info("Initial {}", npSummaryForLogs(initialMarketBasedExchangeAndNetPosition, referenceExchangeAndNetPosition));
            shiftNetwork(marketBasedNetwork, reducedSplittingFactors, zonalScalable, initialMarketBasedExchangeAndNetPosition, referenceExchangeAndNetPosition);

            ExchangeAndNetPosition newMarketBasedExchangeAndNetPosition = computeExchangeAndNetPosition(marketBasedNetwork);

            int nbIterations = 1;
            while (nbIterations < 20 & abs(referenceExchangeAndNetPosition.getNetPosition(IT) - newMarketBasedExchangeAndNetPosition.getNetPosition(IT)) >= EXCHANGE_EPSILON) {

                nbIterations++;
                LOGGER.info("{} Iteration {} will be run.",
                        npSummaryForLogs(newMarketBasedExchangeAndNetPosition, referenceExchangeAndNetPosition), nbIterations);

                shiftNetwork(marketBasedNetwork, reducedSplittingFactors, zonalScalable, newMarketBasedExchangeAndNetPosition, referenceExchangeAndNetPosition);
                referenceExchangeAndNetPosition = computeExchangeAndNetPosition(referenceNetwork);
                newMarketBasedExchangeAndNetPosition = computeExchangeAndNetPosition(marketBasedNetwork);
            }

            builder.addNewMarketBasedExchangeAndNetPositions(newMarketBasedExchangeAndNetPosition);

            if (abs(referenceExchangeAndNetPosition.getNetPosition(IT) - newMarketBasedExchangeAndNetPosition.getNetPosition(IT)) >= EXCHANGE_EPSILON) {
                LOGGER.error("North Italian exchange aligner failed: nb max iterations is reached. {}",
                        npSummaryForLogs(initialMarketBasedExchangeAndNetPosition, referenceExchangeAndNetPosition));
                result = builder.addExchangeAlignerStatus(NOT_ALIGNED).build();
                return;
            }
            LOGGER.info("Both networks are aligned. North Italian NP is {} MW.", newMarketBasedExchangeAndNetPosition.getNetPosition(IT));
            result = builder.addExchangeAlignerStatus(ALIGNED_WITH_SHIFT).build();

        } catch (ShiftingException | GlskLimitationException e) {
            throw new TrmException(e);
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
            ntc.computeReducedSplittingFactors().forEach((country, value) -> reducedSplittingFactors.put(new CountryEICode(Country.valueOf(country)).getCode(), value));

            return reducedSplittingFactors;

        } catch (IOException | JAXBException e) {
            throw new TrmException("An error occurred in the NTC files import for " + targetDateTime + ": " + e);
        }
    }

    private static void shiftNetwork(Network marketBasedNetwork, Map<String, Double> reducedSplittingFactors, ZonalData<Scalable> zonalScalable, ExchangeAndNetPosition marketBasedExchangeAndNetPosition, ExchangeAndNetPosition referenceExchangeAndNetPosition) throws GlskLimitationException, ShiftingException {
        // shift target value: Italian import = opposite to the Italian NP in the reference network
        // ntc: ntc in the market based network
        Map<String, Double> ntcs = updateMarketBasedNtcs(marketBasedExchangeAndNetPosition);
        ShiftDispatcher shiftDispatcher = new CseD2ccShiftDispatcherTmp(LOGGER, reducedSplittingFactors, ntcs);
        LinearScaler linearScaler = new LinearScaler(zonalScalable, shiftDispatcher);
        linearScaler.shiftNetwork(-referenceExchangeAndNetPosition.getNetPosition(IT), marketBasedNetwork);
    }

    ExchangeAndNetPosition computeExchangeAndNetPosition(Network network) {
        LoadFlow.run(network, loadFlowParameters);
        return new ExchangeAndNetPosition(network);
    }

    private static Map<String, Double> updateMarketBasedNtcs(ExchangeAndNetPositionInterface marketBasedExchangeAndNetPosition) {
        return Map.of(
                new CountryEICode(FR).getCode(), marketBasedExchangeAndNetPosition.getNetPosition(FR),
                new CountryEICode(CH).getCode(), marketBasedExchangeAndNetPosition.getNetPosition(CH) + marketBasedExchangeAndNetPosition.getNetPosition(DE),
                new CountryEICode(AT).getCode(), marketBasedExchangeAndNetPosition.getNetPosition(AT),
                new CountryEICode(SI).getCode(), marketBasedExchangeAndNetPosition.getNetPosition(SI)
        );
    }

    private static String npSummaryForLogs(ExchangeAndNetPositionInterface marketBasedExchangeAndNetPosition, ExchangeAndNetPositionInterface referenceExchangeAndNetPosition) {
        return "North Italian net position is " + marketBasedExchangeAndNetPosition.getNetPosition(IT)
                + " MW for the market based network and " + referenceExchangeAndNetPosition.getNetPosition(IT)
                + " MW for the reference network.";
    }
}
