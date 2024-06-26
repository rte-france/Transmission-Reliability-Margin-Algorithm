/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import com.rte_france.trm_algorithm.operational_conditions_aligners.ExchangeAligner;

import java.util.Map;
import java.util.Objects;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class TrmResults {

    private final Map<String, UncertaintyResult> uncertaintiesMap;
    private final Map<String, Boolean> cracAlignmentResults;
    private final Map<String, Boolean> pstAlignmentResults;
    private final ExchangeAligner.Result exchangeAlignerResult;

    private TrmResults(Map<String, UncertaintyResult> uncertaintiesMap,
                       Map<String, Boolean> cracAlignmentResults,
                       Map<String, Boolean> pstAlignmentResults,
                       ExchangeAligner.Result exchangeAlignerResult) {
        this.uncertaintiesMap = uncertaintiesMap;
        this.cracAlignmentResults = cracAlignmentResults;
        this.pstAlignmentResults = pstAlignmentResults;
        this.exchangeAlignerResult = exchangeAlignerResult;
    }

    public static TrmResults.Builder getBuilder() {
        return new Builder();
    }

    public Map<String, UncertaintyResult> getUncertaintiesMap() {
        return uncertaintiesMap;
    }

    public Map<String, Boolean> getCracAlignmentResults() {
        return cracAlignmentResults;
    }

    public Map<String, Boolean> getPstAlignmentResults() {
        return pstAlignmentResults;
    }

    public ExchangeAligner.Result getExchangeAlignerResult() {
        return exchangeAlignerResult;
    }

    public static final class Builder {
        private Map<String, UncertaintyResult> uncertaintiesMap;
        private Map<String, Boolean> cracAlignmentResults;
        private Map<String, Boolean> pstAlignmentResults;
        private ExchangeAligner.Result exchangeAlignerResult;

        private Builder() {
            // Builder pattern
        }

        public Builder addUncertainties(Map<String, UncertaintyResult> uncertaintiesMap) {
            this.uncertaintiesMap = uncertaintiesMap;
            return this;
        }

        public Builder addCracAlignmentResults(Map<String, Boolean> cracAlignmentResults) {
            this.cracAlignmentResults = cracAlignmentResults;
            return this;
        }

        public Builder addPstAlignmentResults(Map<String, Boolean> pstAlignmentResults) {
            this.pstAlignmentResults = pstAlignmentResults;
            return this;
        }

        public Builder addExchangeAlignerResult(ExchangeAligner.Result exchangeAlignerResult) {
            this.exchangeAlignerResult = exchangeAlignerResult;
            return this;
        }

        public TrmResults build() {
            Objects.requireNonNull(uncertaintiesMap);
            Objects.requireNonNull(cracAlignmentResults);
            Objects.requireNonNull(pstAlignmentResults);
            Objects.requireNonNull(exchangeAlignerResult);
            return new TrmResults(uncertaintiesMap,
                cracAlignmentResults,
                pstAlignmentResults,
                exchangeAlignerResult);
        }
    }
}
