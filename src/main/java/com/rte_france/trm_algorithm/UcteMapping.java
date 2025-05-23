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
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Sebastian Huaraca {@literal <sebastian.huaracalapa at rte-france.com>}
 */

public final class UcteMapping {
    private static final Logger LOGGER = LoggerFactory.getLogger(UcteMapping.class);

    public static List<MappingResults> mapNetworks(Network networkReference, Network networkMarketBased) {
        filterNetwork(networkReference);
        filterNetwork(networkMarketBased);
        return networkMarketBased.getLineStream().map(line -> mapNetworks(networkReference, networkMarketBased, line)).toList();
    }

    public static List<MappingResults> mapNetworks(Network networkReference, Network networkMarketBased, Country... filtersCountries) {
        filterNetwork(networkReference, filtersCountries);
        filterNetwork(networkMarketBased, filtersCountries);
        return networkMarketBased.getLineStream().map(line -> mapNetworks(networkReference, networkMarketBased, line)).toList();
    }

    public static MappingResults mapNetworks(Network networkReference, Network networkMarketBased, Line line) {
        String voltageLevelSide1 = getVoltageLevelSide1(line.getId());
        String voltageLevelSide2 = getVoltageLevelSide2(line.getId());
        String orderCode = getOrderCode(line.getId());
        UcteNetworkAnalyzer analyser = new UcteNetworkAnalyzer(networkReference, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.REPLACE_8TH_CHARACTER_WITH_WILDCARD));
        UcteMatchingResult resultOrderCode = analyser.findTopologicalElement(voltageLevelSide1, voltageLevelSide2, orderCode);
        String statusResultOrderCode = getStatusResult(resultOrderCode);
        Integer valueOrderCode = valueMatch(statusResultOrderCode);
        Optional<String> optionalElementName = Optional.ofNullable(networkMarketBased.getLine(line.getId()).getProperty("elementName"));
        String statusResultElementName;
        if (optionalElementName.isPresent()) {
            UcteMatchingResult resultElementName = analyser.findTopologicalElement(voltageLevelSide1, voltageLevelSide2, networkMarketBased.getLine(line.getId()).getProperty("elementName"));
            statusResultElementName = getStatusResult(resultElementName);
        } else {
            statusResultElementName = "";
        }
        Integer valueElementName = valueMatch(statusResultElementName);
        switch (valueElementName + valueOrderCode) {
            case 0 -> {
                LOGGER.error("No matching Line found for: {}", line.getId());
                return new MappingResults(line.getId(), "", false);
            }
            case 1 -> {
                String matchedId = !statusResultOrderCode.isEmpty() ? statusResultOrderCode : statusResultElementName;
                return new MappingResults(line.getId(), matchedId, true);
            }
            case 2 -> {
                if (statusResultElementName.equals(statusResultOrderCode)) {
                    return new MappingResults(line.getId(), statusResultOrderCode, true);
                } else {
                    LOGGER.error("Different matching lines found for: {}", line.getId());
                    return new MappingResults(line.getId(), "", false);
                }
            }
            default -> {
                if (valueElementName + valueOrderCode > 2) {
                    LOGGER.error("Several matching Lines found for: {}", line.getId());
                    return new MappingResults(line.getId(), "", false);
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

    public static int valueMatch(String valor) {
        if (valor == null || valor.isEmpty()) {
            return 0;
        } else if (valor.equals("SEVERAL_MATCH")) {
            return 10;
        } else {
            return 1;
        }
    }

    public static void filterNetwork(Network network) {
        Set<String> linesToRemoveIds = network.getLineStream()
                .filter(Identifiable::isFictitious)
                .map(Line::getId)
                .collect(Collectors.toSet());
        linesToRemoveIds.forEach(id -> network.getLine(id).remove());
    }

    public static void filterNetwork(Network network, Country... filtersCountries) {
        Set<String> linesToRemoveIds = network.getLineStream()
                .filter(line -> {
                    Country country = line.getTerminal1()
                            .getVoltageLevel()
                            .getSubstation().get()
                            .getCountry().get();
                    boolean isCountryNotInEnum = Set.of(filtersCountries).stream()
                            .noneMatch(e -> e.name().equals(country.name()));
                    return isCountryNotInEnum || line.isFictitious();
                })
                .map(Line::getId)
                .collect(Collectors.toSet());
        linesToRemoveIds.forEach(id -> network.getLine(id).remove());
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

    public static List <MappingResults> tieLines(Network networkReference, Network networkMarketBased) {
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

    public static void duplicateCheck(List <MappingResults> listMappingResults) {
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
