/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import java.util.Map;
import java.util.Objects;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class TrmResults {

    private final Map<String, UncertaintyResult> uncertaintiesMap;

    private TrmResults(Builder builder) {
        this.uncertaintiesMap = builder.uncertaintiesMap;
    }

    public static TrmResults.Builder builder() {
        return new Builder();
    }

    public Map<String, UncertaintyResult> getUncertaintiesMap() {
        return uncertaintiesMap;
    }

    public static final class Builder {
        private Map<String, UncertaintyResult> uncertaintiesMap;

        private Builder() {
            // Builder pattern
        }

        public Builder addUncertainties(Map<String, UncertaintyResult> uncertaintiesMap) {
            this.uncertaintiesMap = uncertaintiesMap;
            return this;
        }

        public TrmResults build() {
            Objects.requireNonNull(uncertaintiesMap);
            return new TrmResults(this);
        }
    }
}
