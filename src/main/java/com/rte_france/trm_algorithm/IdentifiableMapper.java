package com.rte_france.trm_algorithm;

public interface IdentifiableMapper {
    String getIdInMarket(String idInReference);
    String getIdInReference(String idInMarket);
}
