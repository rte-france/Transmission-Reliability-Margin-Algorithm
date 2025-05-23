/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import com.powsybl.iidm.network.*;
import com.powsybl.openrao.data.crac.io.commons.ucte.UcteMatchingResult;
import com.powsybl.openrao.data.crac.io.commons.ucte.UcteNetworkAnalyzer;
import com.powsybl.openrao.data.crac.io.commons.ucte.UcteNetworkAnalyzerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/**
 * @author Sebastian Huaraca {@literal <sebastian.huaracalapa at rte-france.com>}
 */

final class UcteMapping {
    private static final Logger LOGGER = LoggerFactory.getLogger(UcteMapping.class);
    private static final UcteNetworkAnalyzerProperties UCTE_NETWORK_ANALYZER_PROPERTIES = new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.REPLACE_8TH_CHARACTER_WITH_WILDCARD);

    static List<MappingResults> mapNetworks(Network networkReference, Network networkMarketBased) {
        UcteNetworkAnalyzer analyser = new UcteNetworkAnalyzer(networkReference, UCTE_NETWORK_ANALYZER_PROPERTIES);
        return networkMarketBased.getBranchStream().map(branch -> mapNetworks(analyser, networkMarketBased, branch)).toList();
    }

    static List<MappingResults> mapNetworks(Network networkReference, Network networkMarketBased, Country... filtersCountries) {
        UcteNetworkAnalyzer analyser = new UcteNetworkAnalyzer(networkReference, UCTE_NETWORK_ANALYZER_PROPERTIES);
        return networkMarketBased.getBranchStream()
                .filter(branch -> isBranchConnectedToAnyGivenCountry(branch, filtersCountries))
                .map(line -> mapNetworks(analyser, networkMarketBased, line)).toList();
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

    private static MappingResults mapNetworks(UcteNetworkAnalyzer analyser, Network networkMarketBased, Branch branch) {
        String voltageLevelSide1 = getVoltageLevelSide1(branch.getId());
        String voltageLevelSide2 = getVoltageLevelSide2(branch.getId());
        String orderCode = getOrderCode(branch.getId());
        Optional<String> optionalElementName = Optional.ofNullable(networkMarketBased.getBranch(branch.getId()).getProperty("elementName"));
        if (optionalElementName.isEmpty() || optionalElementName.get().equals(orderCode)) {
            UcteMatchingResult resultOrderCode = analyser.findTopologicalElement(voltageLevelSide1, voltageLevelSide2, orderCode);
            switch (resultOrderCode.getStatus()) {
                case NOT_FOUND -> {
                    LOGGER.error("No matching Line found for: {}", branch.getId());
                    return MappingResults.notFound(branch.getId());
                }
                case SINGLE_MATCH -> {
                    return MappingResults.mappingFound(branch.getId(), resultOrderCode.getIidmIdentifiable().getId());
                }
                case SEVERAL_MATCH -> {
                    LOGGER.error("Different matching lines found for: {}", branch.getId());
                    return MappingResults.notFound(branch.getId());
                }
            }
        } else {
            UcteMatchingResult resultOrderCode = analyser.findTopologicalElement(voltageLevelSide1, voltageLevelSide2, orderCode);
            UcteMatchingResult resultElementName = analyser.findTopologicalElement(voltageLevelSide1, voltageLevelSide2, networkMarketBased.getBranch(branch.getId()).getProperty("elementName"));
            if (resultOrderCode.getStatus() == UcteMatchingResult.MatchStatus.NOT_FOUND &&
                    resultElementName.getStatus() == UcteMatchingResult.MatchStatus.NOT_FOUND) {
                LOGGER.error("No matching Line found for: {}", branch.getId());
                return MappingResults.notFound(branch.getId());
            }
            if (resultOrderCode.getStatus() == UcteMatchingResult.MatchStatus.SEVERAL_MATCH ||
                resultElementName.getStatus() == UcteMatchingResult.MatchStatus.SEVERAL_MATCH ||
                resultOrderCode.getStatus() == UcteMatchingResult.MatchStatus.SINGLE_MATCH && resultElementName.getStatus() == UcteMatchingResult.MatchStatus.SINGLE_MATCH && !resultOrderCode.getIidmIdentifiable().equals(resultElementName.getIidmIdentifiable())) {
                LOGGER.error("Different matching lines found for: {}", branch.getId());
                return MappingResults.notFound(branch.getId());
            }
            if (resultOrderCode.getStatus() == UcteMatchingResult.MatchStatus.SINGLE_MATCH) {
                return MappingResults.mappingFound(branch.getId(), resultOrderCode.getIidmIdentifiable().getId());
            } else {
                return MappingResults.mappingFound(branch.getId(), resultElementName.getIidmIdentifiable().getId());
            }
        }
        return null;
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

    static void duplicateCheck(List <MappingResults> listMappingResults) {
        Map<String, List<MappingResults>> grouped = new HashMap<>();
        for (MappingResults results : listMappingResults) {
            grouped
                    .computeIfAbsent(results.lineFromReferenceNetwork(), k -> new ArrayList<>())
                    .add(results);
        }
        for (Map.Entry<String, List<MappingResults>> entry : grouped.entrySet()) {
            if (entry.getValue().size() > 1) {
                LOGGER.error("Duplicates values found for: {}", entry.getKey());
            }
        }
    }

    //Utility class
    private UcteMapping() {
    }
}
