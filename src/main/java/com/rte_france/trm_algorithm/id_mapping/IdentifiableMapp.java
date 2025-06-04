package com.rte_france.trm_algorithm.id_mapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IdentifiableMapp {
    private final Map<String, String> mappingFromMarketBasedToReference;
    private final Map<String, String> mappingFromReferenceToMarketBased;
    private static final Logger LOGGER = LoggerFactory.getLogger(IdentifiableMapp.class);

    IdentifiableMapp(Map<String, String> mappingFromMarketBasedToReference, Map<String, String> mappingFromReferenceToMarketBased) {
        this.mappingFromMarketBasedToReference = mappingFromMarketBasedToReference;
        this.mappingFromReferenceToMarketBased = mappingFromReferenceToMarketBased;
    }

    public String idInReference(String s) {
        return mappingFromMarketBasedToReference.computeIfAbsent(s, idInMarket -> {
            throw new IdMappingNotFoundException();
        });
    }

    public String idInMarketBased(String s) {
        return mappingFromReferenceToMarketBased.computeIfAbsent(s, idInReference -> {
            throw new IdMappingNotFoundException();
        });
    }

    public static class IdentifiableMappBuilder {
        Map<String, String> mappingFromMarketBasedToReference = new HashMap<>();
        Map<String, String> mappingFromReferenceToMarketBased = new HashMap<>();
        List<String> invalidatedInMarketBased = new ArrayList<>();
        List<String> invalidatedInReference = new ArrayList<>();

        IdentifiableMapp build() {
            return new IdentifiableMapp(mappingFromMarketBasedToReference, mappingFromReferenceToMarketBased);
        }

        public void addMappingOrInvalidateDuplicates(String idMarketBased, String idReference) {
            if (invalidatedInMarketBased.contains(idMarketBased)) {
                LOGGER.error("Replicated branches found for: {} in reference", idMarketBased);
                return;
            }
            if (invalidatedInReference.contains(idReference)) {
                LOGGER.error("Replicated branches found for: {} in marketBased", idReference);
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
