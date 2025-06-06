/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.id_mapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Sebastian Huaraca {@literal <sebastian.huaracalapa at rte-france.com>}
 */
public class IdentifiableMapping {
    public final Map<String, String> mappingFromMarketBasedToReference;
    public final Map<String, String> mappingFromReferenceToMarketBased;
    private static final Logger LOGGER = LoggerFactory.getLogger(IdentifiableMapping.class);

    IdentifiableMapping(Map<String, String> mappingFromMarketBasedToReference, Map<String, String> mappingFromReferenceToMarketBased) {
        this.mappingFromMarketBasedToReference = mappingFromMarketBasedToReference;
        this.mappingFromReferenceToMarketBased = mappingFromReferenceToMarketBased;
    }

    public String idInReference(String s) {
        return mappingFromMarketBasedToReference.computeIfAbsent(s, idInMarket -> {
            throw new IdMappingNotFoundException("Id - " + s + " - not found in reference");
        });
    }

    public String idInMarketBased(String s) {
        return mappingFromReferenceToMarketBased.computeIfAbsent(s, idInReference -> {
            throw new IdMappingNotFoundException("Id - " + s + " - not found in marketBased");
        });
    }

    public static class IdentifiableMappingBuilder {
        Map<String, String> mappingFromMarketBasedToReference = new HashMap<>();
        Map<String, String> mappingFromReferenceToMarketBased = new HashMap<>();
        List<String> invalidatedInMarketBased = new ArrayList<>();
        List<String> invalidatedInReference = new ArrayList<>();

        IdentifiableMapping build() {
            return new IdentifiableMapping(mappingFromMarketBasedToReference, mappingFromReferenceToMarketBased);
        }

        public void addMappingOrInvalidateDuplicates(String idMarketBased, String idReference) {
            if (invalidatedInMarketBased.contains(idMarketBased)) {
                LOGGER.error("Duplicated branches found for: {} in marketBased", idMarketBased);
                return;
            }
            if (invalidatedInReference.contains(idReference)) {
                LOGGER.error("Duplicated branches found for: {} in reference", idReference);
                return;
            }

            if (mappingFromMarketBasedToReference.containsKey(idMarketBased) && mappingFromMarketBasedToReference.get(idMarketBased) != idReference) {
                String previousReference = mappingFromMarketBasedToReference.get(idMarketBased);
                mappingFromMarketBasedToReference.remove(idMarketBased);
                mappingFromReferenceToMarketBased.remove(idReference);
                invalidatedInMarketBased.add(idMarketBased);
                invalidatedInReference.add(idReference);
                invalidatedInReference.add(previousReference);
                return;
            }

            if (mappingFromReferenceToMarketBased.containsKey(idReference) && mappingFromReferenceToMarketBased.get(idReference) != idMarketBased) {
                String previousMarketBased = mappingFromReferenceToMarketBased.get(idReference);
                mappingFromMarketBasedToReference.remove(idMarketBased);
                mappingFromReferenceToMarketBased.remove(idReference);
                invalidatedInMarketBased.add(idMarketBased);
                invalidatedInMarketBased.add(previousMarketBased);
                invalidatedInReference.add(idReference);
                return;
            }

            mappingFromReferenceToMarketBased.put(idReference, idMarketBased);
            mappingFromMarketBasedToReference.put(idMarketBased, idReference);
        }
    }
}
