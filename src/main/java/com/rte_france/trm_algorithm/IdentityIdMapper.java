package com.rte_france.trm_algorithm;

public class IdentityIdMapper implements IdentifiableMapper {
    @Override
    public String getIdInMarket(String idInReference) {
        return idInReference;
    }

    @Override
    public String getIdInReference(String idInMarket) {
        return "";
    }
}
