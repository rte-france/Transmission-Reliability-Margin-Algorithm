/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.id_mapping;

import com.powsybl.iidm.network.*;
import com.powsybl.openrao.data.crac.io.commons.ucte.UcteMatchingResult;
import com.powsybl.openrao.data.crac.io.commons.ucte.UcteNetworkAnalyzer;
import com.powsybl.openrao.data.crac.io.commons.ucte.UcteNetworkAnalyzerProperties;
import java.util.*;

/**
 * @author Sebastian Huaraca {@literal <sebastian.huaracalapa at rte-france.com>}
 */

public final class UcteMapper {
    private static final UcteNetworkAnalyzerProperties UCTE_NETWORK_ANALYZER_PROPERTIES = new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.REPLACE_8TH_CHARACTER_WITH_WILDCARD);

    public static IdentifiableMapping mapNetworks(Network networkReference, Network networkMarketBased) {
        IdentifiableMapping.IdentifiableMappingBuilder builder = new IdentifiableMapping.IdentifiableMappingBuilder();
        UcteNetworkAnalyzer analyser = new UcteNetworkAnalyzer(networkReference, UCTE_NETWORK_ANALYZER_PROPERTIES);
        networkMarketBased.getBranchStream().forEach(branch -> mapNetworks(analyser, builder, networkMarketBased, branch));
        return builder.build();
    }

    public static IdentifiableMapping mapNetworks(Network networkReference, Network networkMarketBased, Country... chosenCountries) {
        IdentifiableMapping.IdentifiableMappingBuilder builder = new IdentifiableMapping.IdentifiableMappingBuilder();
        UcteNetworkAnalyzer analyser = new UcteNetworkAnalyzer(networkReference, UCTE_NETWORK_ANALYZER_PROPERTIES);
        networkMarketBased.getBranchStream()
                .filter(branch -> isBranchConnectedToAnyGivenCountry(branch, chosenCountries))
                .forEach(line -> mapNetworks(analyser, builder, networkMarketBased, line));
        return builder.build();
    }

    private static boolean isBranchConnectedToAnyGivenCountry(Branch branch, Country... countries) {
        return Arrays.stream(countries).anyMatch(country -> isBranchConnectedToCountry(branch, country));
    }

    private static boolean isBranchConnectedToCountry(Branch branch, Country country) {
        Optional<Country> country1 = branch.getTerminal1().getVoltageLevel()
                .getSubstation().flatMap(Substation::getCountry);
        Optional<Country> country2 = branch.getTerminal2().getVoltageLevel()
                .getSubstation().flatMap(Substation::getCountry);
        return country1.isPresent() && country1.get().equals(country) ||
                country2.isPresent() && country2.get().equals(country);
    }

    private static void mapNetworks(UcteNetworkAnalyzer analyser, IdentifiableMapping.IdentifiableMappingBuilder builder, Network networkMarketBased, Branch branch) {
        String voltageLevelSide1 = getVoltageLevelSide1(branch.getId());
        String voltageLevelSide2 = getVoltageLevelSide2(branch.getId());
        String orderCode = getOrderCode(branch.getId());
        Optional<String> optionalElementName = Optional.ofNullable(networkMarketBased.getBranch(branch.getId()).getProperty("elementName"));
        UcteMatchingResult resultOrderCode = analyser.findTopologicalElement(voltageLevelSide1, voltageLevelSide2, orderCode);
        if (resultOrderCode.getStatus() == UcteMatchingResult.MatchStatus.SINGLE_MATCH) {
            builder.addMappingOrInvalidateDuplicates(branch.getId(), resultOrderCode.getIidmIdentifiable().getId());
        }
        if (optionalElementName.isPresent()) {
            UcteMatchingResult resultElementName = analyser.findTopologicalElement(voltageLevelSide1, voltageLevelSide2, networkMarketBased.getBranch(branch.getId()).getProperty("elementName"));
            if (resultElementName.getStatus() == UcteMatchingResult.MatchStatus.SINGLE_MATCH) {
                builder.addMappingOrInvalidateDuplicates(branch.getId(), resultElementName.getIidmIdentifiable().getId());
            }
        }
    }

    private static String getOrderCode(String id) {
        return id.substring(18);
    }

    private static String getVoltageLevelSide2(String id) {
        return id.substring(9, 16);
    }

    private static String getVoltageLevelSide1(String id) {
        return id.substring(0, 7);
    }

    //Utility class
    private UcteMapper() {
    }
}
