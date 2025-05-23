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
import java.util.stream.Stream;

/**
 * @author Sebastian Huaraca {@literal <sebastian.huaracalapa at rte-france.com>}
 */

final class UcteMapping {
    private static final Logger LOGGER = LoggerFactory.getLogger(UcteMapping.class);
    private static final UcteNetworkAnalyzerProperties UCTE_NETWORK_ANALYZER_PROPERTIES = new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.REPLACE_8TH_CHARACTER_WITH_WILDCARD);;

    static List<MappingResults> mapNetworks(Network networkReference, Network networkMarketBased) {
        return networkMarketBased.getBranchStream().map(branch -> mapNetworks(networkReference, networkMarketBased, branch)).toList();
    }

    static List<MappingResults> mapNetworks(Network networkReference, Network networkMarketBased, Country... filtersCountries) {
        return networkMarketBased.getBranchStream()
                .filter(branch -> isBranchConnectedToAnyGivenCountry(branch, filtersCountries))
                .map(line -> mapNetworks(networkReference, networkMarketBased, line)).toList();
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

    static MappingResults mapNetworks(Network networkReference, Network networkMarketBased, Branch branch) {
        String voltageLevelSide1 = getVoltageLevelSide1(branch.getId());
        String voltageLevelSide2 = getVoltageLevelSide2(branch.getId());
        String orderCode = getOrderCode(branch.getId());
        Optional<String> optionalElementName = Optional.ofNullable(networkMarketBased.getBranch(branch.getId()).getProperty("elementName"));
        UcteNetworkAnalyzer analyser = new UcteNetworkAnalyzer(networkReference, UCTE_NETWORK_ANALYZER_PROPERTIES);
        if (optionalElementName.isEmpty() || optionalElementName.get().equals(orderCode)) {
            UcteMatchingResult resultOrderCode = analyser.findTopologicalElement(voltageLevelSide1, voltageLevelSide2, orderCode);
            String statusResultOrderCode = getStatusResult(resultOrderCode);
            Integer valueOrderCode = valueMatch(statusResultOrderCode);

            String statusResultElementName = "";
            Integer valueElementName = valueMatch(statusResultElementName);
            switch (valueElementName + valueOrderCode) {
                case 0 -> {
                    LOGGER.error("No matching Line found for: {}", branch.getId());
                    return new MappingResults(branch.getId(), "", false);
                }
                case 1 -> {
                    String matchedId = !statusResultOrderCode.isEmpty() ? statusResultOrderCode : statusResultElementName;
                    return new MappingResults(branch.getId(), matchedId, true);
                }
                case 2 -> {
                    if (statusResultElementName.equals(statusResultOrderCode)) {
                        return new MappingResults(branch.getId(), statusResultOrderCode, true);
                    } else {
                        LOGGER.error("Different matching lines found for: {}", branch.getId());
                        return new MappingResults(branch.getId(), "", false);
                    }
                }
                default -> {
                    if (valueElementName + valueOrderCode > 2) {
                        LOGGER.error("Several matching Lines found for: {}", branch.getId());
                        return new MappingResults(branch.getId(), "", false);
                    }
                }
            }
        } else {
            UcteMatchingResult resultOrderCode = analyser.findTopologicalElement(voltageLevelSide1, voltageLevelSide2, orderCode);
            String statusResultOrderCode = getStatusResult(resultOrderCode);
            Integer valueOrderCode = valueMatch(statusResultOrderCode);
            UcteMatchingResult resultElementName = analyser.findTopologicalElement(voltageLevelSide1, voltageLevelSide2, networkMarketBased.getBranch(branch.getId()).getProperty("elementName"));
            String statusResultElementName = getStatusResult(resultElementName);
            Integer valueElementName = valueMatch(statusResultElementName);
            switch (valueElementName + valueOrderCode) {
                case 0 -> {
                    LOGGER.error("No matching Line found for: {}", branch.getId());
                    return new MappingResults(branch.getId(), "", false);
                }
                case 1 -> {
                    String matchedId = !statusResultOrderCode.isEmpty() ? statusResultOrderCode : statusResultElementName;
                    return new MappingResults(branch.getId(), matchedId, true);
                }
                case 2 -> {
                    if (statusResultElementName.equals(statusResultOrderCode)) {
                        return new MappingResults(branch.getId(), statusResultOrderCode, true);
                    } else {
                        LOGGER.error("Different matching lines found for: {}", branch.getId());
                        return new MappingResults(branch.getId(), "", false);
                    }
                }
                default -> {
                    if (valueElementName + valueOrderCode > 2) {
                        LOGGER.error("Several matching Lines found for: {}", branch.getId());
                        return new MappingResults(branch.getId(), "", false);
                    }
                }
            }
        }
        return null;
    }

    private static String getStatusResult(UcteMatchingResult resultOrderCode) {
        return switch (resultOrderCode.getStatus()) {
            case NOT_FOUND:
                yield "";
            case SINGLE_MATCH:
                yield resultOrderCode.getIidmIdentifiable().getId();
            case SEVERAL_MATCH:
                yield "SEVERAL_MATCH";
        };
    }

    static int valueMatch(String valor) {
        if (valor == null || valor.isEmpty()) {
            return 0;
        } else if (valor.equals("SEVERAL_MATCH")) {
            return 10;
        } else {
            return 1;
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

    static List <MappingResults> tieLines(Network networkReference, Network networkMarketBased) {
        List <TieLine> marketBasedTieLine = networkMarketBased.getTieLineStream().filter(item -> !item.isFictitious())
                .filter(item -> Set.of(Country.IT, Country.FR, Country.SI, Country.CH, Country.AT).contains(item.getTerminal1().getVoltageLevel().getSubstation().get().getCountry().get()) ||
                        Set.of(Country.IT, Country.FR, Country.SI, Country.CH, Country.AT).contains(item.getTerminal2().getVoltageLevel().getSubstation().get().getCountry().get())).toList();

        return marketBasedTieLine.stream().map(tieline -> {
            String pairingKey = tieline.getDanglingLine1().getPairingKey();
            Stream<TieLine> listTieLinesReference = networkReference.getTieLineStream();
            List<TieLine> match = new ArrayList<>(List.of());
            listTieLinesReference.forEach(elementTieLine -> {
                if (elementTieLine.getPairingKey().equals(pairingKey)) {
                    match.add(elementTieLine);
                }
            });
            int nombreTieLines = match.size();
            if (nombreTieLines > 1) {
                LOGGER.error("Several matching Tielines found for: {}", tieline.getId());
            } else if (nombreTieLines == 0) {
                LOGGER.error("No matching Tieline found for: {}", tieline.getId());
            } else {
                return new MappingResults(tieline.getId(), match.get(0).getId(), true);
            }
            return new MappingResults(tieline.getId(), "", false);
        }).toList();
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
