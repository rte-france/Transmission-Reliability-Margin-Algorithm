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

import static java.util.Arrays.stream;

/**
 * @author Sebastian Huaraca {@literal <sebastian.huaracalapa at rte-france.com>}
 */

public final class UcteMapping {
    private static final Logger LOGGER = LoggerFactory.getLogger(UcteMapping.class);

    public static List<MappingResults> mapNetworks(Network networkReference, Network networkMarketBased) {
        List <Line> marketBasedLine = networkMarketBased.getLineStream()
                .filter(item -> Set.of(Country.IT, Country.FR, Country.SI, Country.CH, Country.AT).contains(item.getTerminal1().getVoltageLevel().getSubstation().get().getCountry().get()))
                .filter(item -> !item.isFictitious()).toList();
        return marketBasedLine.stream().map(line -> mapNetworks(networkReference, networkMarketBased, line)).toList();
    }

    public static MappingResults mapNetworks(Network networkReference, Network networkMarketBased, Line marketBasedLine) {
        String voltageLevelSide1 = getVoltageLevelSide1(marketBasedLine.getId());
        String voltageLevelSide2 = getVoltageLevelSide2(marketBasedLine.getId());
        String orderCode = getOrderCode(marketBasedLine.getId());
        Optional<String> optionalElementName = Optional.ofNullable(networkMarketBased.getLine(marketBasedLine.getId()).getProperty("elementName"));

        String elementName = null;
        if (optionalElementName.isPresent()) {
            elementName = optionalElementName.get();
        }

        Map<UcteMatchingResult.MatchStatus,String> dictionaryOrderCode = new HashMap<UcteMatchingResult.MatchStatus,String>();
        dictionaryOrderCode.put(UcteMatchingResult.MatchStatus.NOT_FOUND,"");
        dictionaryOrderCode.put(UcteMatchingResult.MatchStatus.SINGLE_MATCH,networkReference.getId());
        dictionaryOrderCode.put(UcteMatchingResult.MatchStatus.SEVERAL_MATCH,"SEVERAL_MATCH");

        Map<String, UcteMatchingResult.MatchStatus> resultsCodeOrdre = new HashMap<>(Map.of());
        //Filter 1 : Code order
        UcteNetworkAnalyzer analyser = new UcteNetworkAnalyzer(networkReference, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.REPLACE_8TH_CHARACTER_WITH_WILDCARD));
        UcteMatchingResult resultOrderCode = analyser.findTopologicalElement(voltageLevelSide1, voltageLevelSide2, orderCode);
        resultsCodeOrdre.put(marketBasedLine.getId(),resultOrderCode.getStatus());
        //Filter 2 : ElementName
        if (optionalElementName.isPresent()) {
            return switch (resultOrderCode.getStatus()) {
                case NOT_FOUND ->
                        notFoundResult(networkReference, marketBasedLine, analyser, voltageLevelSide1, voltageLevelSide2, elementName);
                case SINGLE_MATCH ->
                        singleMatchResult(networkReference, marketBasedLine, analyser, voltageLevelSide1, voltageLevelSide2, elementName, resultOrderCode);
                case SEVERAL_MATCH -> severalMatchResult("Several matching lines found for: {}", marketBasedLine);
            };
        } else {
            if (resultOrderCode.getStatus() == UcteMatchingResult.MatchStatus.NOT_FOUND) {
                LOGGER.error("No matching line found for: {}", marketBasedLine.getId());
                return new MappingResults(marketBasedLine.getId(), "", false);
            } else if (resultOrderCode.getStatus() == UcteMatchingResult.MatchStatus.SEVERAL_MATCH) {
                LOGGER.error("Several matching line found for: {}", marketBasedLine.getId());
                return new MappingResults(marketBasedLine.getId(), "", false);
            } else if (resultOrderCode.getStatus() == UcteMatchingResult.MatchStatus.SINGLE_MATCH) {
                List<Line> line1 = List.of(networkReference.getLine(resultOrderCode.getIidmIdentifiable().getId()));
                return createMappingResults(marketBasedLine.getId(), line1);
            } else {
                LOGGER.error("Data error: {}", marketBasedLine.getId());
                return new MappingResults(marketBasedLine.getId(), "", false);
            }
        }
    }

    private static MappingResults severalMatchResult(String s, Line marketBasedLine) {
        LOGGER.error(s, marketBasedLine.getId());
        return new MappingResults(marketBasedLine.getId(), "", false);
    }

    private static MappingResults singleMatchResult(Network networkReference, Line marketBasedLine, UcteNetworkAnalyzer analyser, String voltageLevelSide1, String voltageLevelSide2, String elementName, UcteMatchingResult resultOrderCode) {
        boolean match;
        UcteMatchingResult resultElementName = analyser.findTopologicalElement(voltageLevelSide1, voltageLevelSide2, elementName);
        switch (resultElementName.getStatus()) {
            case NOT_FOUND -> {
                return new MappingResults(marketBasedLine.getId(), resultOrderCode.getIidmIdentifiable().getId(), true);
            }
            case SINGLE_MATCH -> {
                if (resultOrderCode.getIidmIdentifiable().getId().equals(resultElementName.getIidmIdentifiable().getId())) {
                    match = resultElementName.hasMatched();
                    return new MappingResults(marketBasedLine.getId(), resultElementName.getIidmIdentifiable().getId(), match);
                } else {
                    return severalMatchResult("Different matching lines found for: {}", marketBasedLine);
                }
            }
            case SEVERAL_MATCH -> {
                return severalMatchResult("Several matching lines found for: {}", marketBasedLine);
            }
        }
        return  null;
    }

    private static MappingResults notFoundResult(Network networkReference, Line marketBasedLine, UcteNetworkAnalyzer analyser, String voltageLevelSide1, String voltageLevelSide2, String elementName) {
        boolean match;
        UcteMatchingResult resultElementName = analyser.findTopologicalElement(voltageLevelSide1, voltageLevelSide2, elementName);
        if (resultElementName.getStatus() == UcteMatchingResult.MatchStatus.SINGLE_MATCH) {
            match = resultElementName.hasMatched();
            return new MappingResults(marketBasedLine.getId(), resultElementName.getIidmIdentifiable().getId(), match);
        } else {
            if (resultElementName.getStatus() == UcteMatchingResult.MatchStatus.NOT_FOUND) {
                LOGGER.error("No matching line found for: {}", marketBasedLine.getId());
            } else {
                LOGGER.error("Several matching lines found for: {}", marketBasedLine.getId());
            }
            return new MappingResults(marketBasedLine.getId(), "", false);
        }
    }

    private static MappingResults createMappingResults(String id, List<Line> matchLine) {
        int nombreLines = matchLine.size();
        if (nombreLines > 1) {
            LOGGER.error("Several matching lines found for: {}", id);
        } else if (nombreLines == 0) {
            LOGGER.error("No matching line found for: {}", id);
        } else {
            return new MappingResults(id, matchLine.get(0).getId(), true);
        }
        return new MappingResults(id, "", false);
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

    public static List<MappingResults> mapNetworks2(Network networkReference, Network networkMarketBased) {
        List<MappingResults> finalList = new ArrayList<>(List.of());
        filterNetwork(networkReference);
        filterNetwork(networkMarketBased);
        networkMarketBased.getLines().forEach(line -> {
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
            switch (valueElementName+valueOrderCode) {
                case 0 -> {
                    finalList.add(new MappingResults(line.getId(), "", false));
                    LOGGER.error("No matching Line found for: {}", line.getId());
                }
                case 1 -> {
                    String matchedId = !statusResultOrderCode.isEmpty() ? statusResultOrderCode : statusResultElementName;
                    finalList.add(new MappingResults(line.getId(), matchedId, true));
                }
                case 2 -> {
                    if (statusResultElementName.equals(statusResultOrderCode)) {
                        finalList.add(new MappingResults(line.getId(), statusResultOrderCode, true));
                    } else {
                        finalList.add(new MappingResults(line.getId(), "", false));
                        LOGGER.error("Different matching lines found for: {}", line.getId());
                    }
                }
                default -> {
                    if (valueElementName+valueOrderCode>2) {
                        finalList.add(new MappingResults(line.getId(), "", false));
                        LOGGER.error("Several matching Lines found for: {}", line.getId());
                    }
                }
            }
        });
        return finalList;
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
                .filter(line -> {
                    Country country = line.getTerminal1()
                            .getVoltageLevel()
                            .getSubstation().get()
                            .getCountry().get();
                    boolean isCountryNotInEnum = Arrays.stream(CountrysArea.values())
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

    //Utility class
    private UcteMapping() {
    }
}
