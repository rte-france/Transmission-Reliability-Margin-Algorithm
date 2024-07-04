/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.powsybl.iidm.network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class PstAligner {
    private static final Logger LOGGER = LoggerFactory.getLogger(PstAligner.class);

    private PstAligner() {
        // Utility class
    }

    public static Result align(Network referenceNetwork, Network marketBasedNetwork) {
        LOGGER.info("Aligning PSTs tap positions");
        return Result.builder()
            .addRatioTapChangerResults(alignRatioTapChanger(referenceNetwork, marketBasedNetwork))
            .addPhaseTapChangerResults(alignPhaseTapChanger(referenceNetwork, marketBasedNetwork))
            .build();
    }

    private static Map<String, Boolean> alignRatioTapChanger(Network referenceNetwork, Network marketBasedNetwork) {
        return referenceNetwork.getTwoWindingsTransformerStream()
            .filter(RatioTapChangerHolder::hasRatioTapChanger)
            .collect(Collectors.toMap(
                Identifiable::getId,
                referenceTwoWindingsTransformer -> alignRatio(marketBasedNetwork, referenceTwoWindingsTransformer)
            ));
    }

    private static boolean alignRatio(Network marketBasedNetwork, TwoWindingsTransformer referenceTwoWindingsTransformer) {
        int referenceTapPosition = referenceTwoWindingsTransformer.getRatioTapChanger().getTapPosition();
        String id = referenceTwoWindingsTransformer.getId();
        TwoWindingsTransformer twoWindingsTransformer = marketBasedNetwork.getTwoWindingsTransformer(id);
        if (Objects.isNull(twoWindingsTransformer)) {
            LOGGER.error("Reference two windings transformer '{}' not found in market based network", id);
            return false;
        }
        if (!twoWindingsTransformer.hasRatioTapChanger()) {
            LOGGER.error("Reference two windings transformer '{}' does not have a ratio tap changer in the market-based network", id);
            return false;
        }
        twoWindingsTransformer.getRatioTapChanger().setTapPosition(referenceTapPosition);
        return true;
    }

    private static Map<String, Boolean> alignPhaseTapChanger(Network referenceNetwork, Network marketBasedNetwork) {
        return referenceNetwork.getTwoWindingsTransformerStream()
            .filter(PhaseTapChangerHolder::hasPhaseTapChanger)
            .collect(Collectors.toMap(
                Identifiable::getId,
                referenceTwoWindingsTransformer -> alignPhase(marketBasedNetwork, referenceTwoWindingsTransformer)
            ));
    }

    private static boolean alignPhase(Network marketBasedNetwork, TwoWindingsTransformer referenceTwoWindingsTransformer) {
        int referenceTapPosition = referenceTwoWindingsTransformer.getPhaseTapChanger().getTapPosition();
        String id = referenceTwoWindingsTransformer.getId();
        TwoWindingsTransformer twoWindingsTransformer = marketBasedNetwork.getTwoWindingsTransformer(id);
        if (Objects.isNull(twoWindingsTransformer)) {
            LOGGER.error("Two windings transformer '{}' not found in market based network", id);
            return false;
        }
        if (!twoWindingsTransformer.hasPhaseTapChanger()) {
            LOGGER.error("Reference two windings transformer '{}' does not have a phase tap changer in the market-based network", id);
            return false;
        }
        twoWindingsTransformer.getPhaseTapChanger().setTapPosition(referenceTapPosition);
        return true;
    }

    public static final class Result {
        private final Map<String, Boolean> ratioTapChangerResults;
        private final Map<String, Boolean> phaseTapChangerResults;

        private Result(Map<String, Boolean> ratioTapChangerResults, Map<String, Boolean> phaseTapChangerResults) {
            this.ratioTapChangerResults = ratioTapChangerResults;
            this.phaseTapChangerResults = phaseTapChangerResults;
        }

        public static Builder builder() {
            return new Builder();
        }

        public Map<String, Boolean> getRatioTapChangerResults() {
            return ratioTapChangerResults;
        }

        public Map<String, Boolean> getPhaseTapChangerResults() {
            return phaseTapChangerResults;
        }

        public static final class Builder {
            private Map<String, Boolean> ratioTapChangerResults;
            private Map<String, Boolean> phaseTapChangerResults;

            private Builder() {
                // Builder pattern
            }

            public Builder addRatioTapChangerResults(Map<String, Boolean> ratioTapChangerResults) {
                this.ratioTapChangerResults = ratioTapChangerResults;
                return this;
            }

            public Builder addPhaseTapChangerResults(Map<String, Boolean> phaseTapChangerResults) {
                this.phaseTapChangerResults = phaseTapChangerResults;
                return this;
            }

            public Result build() {
                Objects.requireNonNull(ratioTapChangerResults);
                Objects.requireNonNull(phaseTapChangerResults);
                return new Result(ratioTapChangerResults, phaseTapChangerResults);
            }
        }
    }
}
