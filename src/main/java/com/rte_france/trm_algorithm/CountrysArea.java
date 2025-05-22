package com.rte_france.trm_algorithm;

import com.powsybl.iidm.network.Country;

public enum CountrysArea {
    FR("FRANCE"),
    IT("ITALY"),
    SI("SLOVENIA"),
    CH("SWITZERLAND"),
    AT("AUSTRIA");

    private final String name;

    private CountrysArea(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
