/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.powsybl.iidm.network.Country;
import com.rte_france.trm_algorithm.operational_conditions_aligners.exchange_and_net_position.EmptyExchangeAndNetPosition;
import com.rte_france.trm_algorithm.operational_conditions_aligners.exchange_and_net_position.ExchangeAndNetPositionInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static com.powsybl.iidm.network.Country.*;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class ItalyNorthExchangeAlignerResult {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItalyNorthExchangeAlignerResult.class);
    private final ExchangeAndNetPositionInterface referenceExchangeAndNetPosition;
    private final ExchangeAndNetPositionInterface initialMarketBasedExchangeAndNetPosition;
    private final ExchangeAndNetPositionInterface newMarketBasedExchangeAndNetPosition;
    private final ExchangeAlignerStatus status;

    public ItalyNorthExchangeAlignerResult(Builder builder) {
        referenceExchangeAndNetPosition = builder.referenceExchangeAndNetPosition;
        initialMarketBasedExchangeAndNetPosition = builder.initialMarketBasedExchangeAndNetPosition;
        newMarketBasedExchangeAndNetPosition = builder.newMarketBasedExchangeAndNetPosition;
        status = builder.status;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ExchangeAndNetPositionInterface getReferenceExchangeAndNetPosition() {
        return referenceExchangeAndNetPosition;
    }

    public ExchangeAndNetPositionInterface getInitialMarketBasedExchangeAndNetPosition() {
        return initialMarketBasedExchangeAndNetPosition;
    }

    public ExchangeAndNetPositionInterface getNewMarketBasedExchangeAndNetPosition() {
        return newMarketBasedExchangeAndNetPosition;
    }

    public ExchangeAlignerStatus getStatus() {
        return status;
    }

    public static final class Builder {
        private ExchangeAndNetPositionInterface referenceExchangeAndNetPosition;
        private ExchangeAndNetPositionInterface initialMarketBasedExchangeAndNetPosition;
        private ExchangeAndNetPositionInterface newMarketBasedExchangeAndNetPosition = new EmptyExchangeAndNetPosition();
        private ExchangeAlignerStatus status;

        private Builder() {
            // Builder pattern
        }

        public Builder addReferenceExchangeAndNetPosition(ExchangeAndNetPositionInterface referenceExchangeAndNetPosition) {
            this.referenceExchangeAndNetPosition = referenceExchangeAndNetPosition;
            return this;
        }

        public Builder addInitialMarketBasedExchangeAndNetPositions(ExchangeAndNetPositionInterface initialMarketBasedExchangeAndNetPosition) {
            this.initialMarketBasedExchangeAndNetPosition = initialMarketBasedExchangeAndNetPosition;
            return this;
        }

        public Builder addNewMarketBasedExchangeAndNetPositions(ExchangeAndNetPositionInterface newMarketBasedExchangeAndNetPosition) {
            this.newMarketBasedExchangeAndNetPosition = newMarketBasedExchangeAndNetPosition;
            return this;
        }

        public Builder addExchangeAlignerStatus(ExchangeAlignerStatus status) {
            this.status = status;
            return this;
        }

        public ItalyNorthExchangeAlignerResult build() {
            Objects.requireNonNull(referenceExchangeAndNetPosition, "referenceExchangeAndNetPosition must not be null");
            Objects.requireNonNull(initialMarketBasedExchangeAndNetPosition, "initialMarketBasedExchangeAndNetPosition must not be null");
            Objects.requireNonNull(status, "status must not be null");

            finalDebugLoggerNp("reference", referenceExchangeAndNetPosition);
            finalDebugLoggerNp("initial market-based", initialMarketBasedExchangeAndNetPosition);
            finalDebugLoggerNp("final market-based", newMarketBasedExchangeAndNetPosition);
            finalDebugLoggerShift(initialMarketBasedExchangeAndNetPosition, newMarketBasedExchangeAndNetPosition);

            return new ItalyNorthExchangeAlignerResult(this);
        }
    }

    private static void finalDebugLoggerNp(String networkName, ExchangeAndNetPositionInterface referenceExchangeAndNetPosition) {
        LOGGER.info("Net positions in the {} network: {}, {}, {}, {}, {}, {}.", networkName,
                printNpCountry(IT, referenceExchangeAndNetPosition),
                printNpCountry(AT, referenceExchangeAndNetPosition),
                printNpCountry(CH, referenceExchangeAndNetPosition),
                printNpCountry(FR, referenceExchangeAndNetPosition),
                printNpCountry(SI, referenceExchangeAndNetPosition),
                printNpCountry(DE, referenceExchangeAndNetPosition));
    }

    private static void finalDebugLoggerShift(ExchangeAndNetPositionInterface initialMarketBasedExchangeAndNetPosition, ExchangeAndNetPositionInterface marketBasedExchangeAndNetPosition) {
        LOGGER.info("Shift performed on the market-based network: {}, {}, {}, {}, {}, {}.",
                printShiftCountry(IT, marketBasedExchangeAndNetPosition, initialMarketBasedExchangeAndNetPosition),
                printShiftCountry(AT, marketBasedExchangeAndNetPosition, initialMarketBasedExchangeAndNetPosition),
                printShiftCountry(CH, marketBasedExchangeAndNetPosition, initialMarketBasedExchangeAndNetPosition),
                printShiftCountry(FR, marketBasedExchangeAndNetPosition, initialMarketBasedExchangeAndNetPosition),
                printShiftCountry(SI, marketBasedExchangeAndNetPosition, initialMarketBasedExchangeAndNetPosition),
                printShiftCountry(DE, marketBasedExchangeAndNetPosition, initialMarketBasedExchangeAndNetPosition));
    }

    private static String printNpCountry(Country country, ExchangeAndNetPositionInterface exchangeAndNetPosition) {
        return country.getName() + " = " +  Math.round(exchangeAndNetPosition.getNetPosition(country) * 1000.) / 1000. + " MW";
    }

    private static String printShiftCountry(Country country, ExchangeAndNetPositionInterface marketBasedExchangeAndNetPosition, ExchangeAndNetPositionInterface initialMarketBasedExchangeAndNetPosition) {
        return country.getName() + " = " +  Math.round((marketBasedExchangeAndNetPosition.getNetPosition(country) - initialMarketBasedExchangeAndNetPosition.getNetPosition(country)) * 1000.) / 1000. + " MW";
    }
}
