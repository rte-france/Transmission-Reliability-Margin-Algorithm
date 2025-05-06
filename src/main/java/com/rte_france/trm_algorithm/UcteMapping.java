/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TieLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

import com.powsybl.openrao.data.cracio.commons.ucte.*;

/**
 * @author Sebastian Huaraca {@literal <sebastian.huaracalapa at rte-france.com>}
 */

public final class UcteMapping {
    private static final Logger LOGGER = LoggerFactory.getLogger(UcteMapping.class);

    public static MappingResults mapNetworks(Network networkReference, Network networkMarketBased, Line marketBasedLine) {

        String voltageLevelSide1 = getVoltageLevelSide1(marketBasedLine.getId());
        String voltageLevelSide2 = getVoltageLevelSide2(marketBasedLine.getId());
        String orderCode = getOrderCode(marketBasedLine.getId());
        Optional<String> optionalElementName = Optional.ofNullable(networkMarketBased.getLine(marketBasedLine.getId()).getProperty("elementName"));

        String elementName = null;
        if (optionalElementName.isPresent()) {
            elementName = optionalElementName.get();
        }

        //Filter 1 : Code order
        UcteNetworkAnalyzer analyser = new UcteNetworkAnalyzer(networkReference, new UcteNetworkAnalyzerProperties(UcteNetworkAnalyzerProperties.BusIdMatchPolicy.REPLACE_8TH_CHARACTER_WITH_WILDCARD));
        UcteMatchingResult resultOrderCode = analyser.findTopologicalElement(voltageLevelSide1, voltageLevelSide2, orderCode);

        //Filter 2 : ElementName
        if (resultOrderCode.getStatus() == UcteMatchingResult.MatchStatus.NOT_FOUND && optionalElementName.isPresent()) {
            return notFoundResult(networkReference, marketBasedLine, analyser, voltageLevelSide1, voltageLevelSide2, elementName);
        } else if (resultOrderCode.getStatus() == UcteMatchingResult.MatchStatus.SINGLE_MATCH && optionalElementName.isPresent()) {
            return singleMatchResult(networkReference, marketBasedLine, analyser, voltageLevelSide1, voltageLevelSide2, elementName, resultOrderCode);
        } else if (resultOrderCode.getStatus() == UcteMatchingResult.MatchStatus.SEVERAL_MATCH && optionalElementName.isPresent()) {
            return severalMatchResult("Several matching lines found for: {}", marketBasedLine);
        }
        if (resultOrderCode.getStatus() ==UcteMatchingResult.MatchStatus.NOT_FOUND && optionalElementName.isEmpty()) {
            LOGGER.error("No matching line found for: {}", marketBasedLine.getId());
            return new MappingResults(marketBasedLine.getId(), "", false);
        }
        List<Line> line1 = List.of(networkReference.getLine(resultOrderCode.getIidmIdentifiable().getId()));
        return createMappingResults(marketBasedLine.getId(), line1);
    }

    private static MappingResults severalMatchResult(String s, Line marketBasedLine) {
        LOGGER.error(s, marketBasedLine.getId());
        return new MappingResults(marketBasedLine.getId(), "", false);
    }

    private static MappingResults singleMatchResult(Network networkReference, Line marketBasedLine, UcteNetworkAnalyzer analyser, String voltageLevelSide1, String voltageLevelSide2, String elementName, UcteMatchingResult resultOrderCode) {
        boolean match;
        UcteMatchingResult resultElementName = analyser.findTopologicalElement(voltageLevelSide1, voltageLevelSide2, elementName);
        if (resultElementName.getStatus() == UcteMatchingResult.MatchStatus.NOT_FOUND) {
            match = true;
            //networkReference.getLine(resultOrderCode.getIidmIdentifiable().getId()).remove();
            return new MappingResults(marketBasedLine.getId(), resultOrderCode.getIidmIdentifiable().getId(), match);
        } else if (resultElementName.getStatus() == UcteMatchingResult.MatchStatus.SINGLE_MATCH) {
            if (resultOrderCode.getIidmIdentifiable().getId().equals(resultElementName.getIidmIdentifiable().getId())) {
                match = resultElementName.hasMatched();
                //networkReference.getLine(resultElementName.getIidmIdentifiable().getId()).remove();
                return new MappingResults(marketBasedLine.getId(), resultElementName.getIidmIdentifiable().getId(), match);
            } else {
                return severalMatchResult("Different matching lines found for: {}", marketBasedLine);
            }
        } else {
            return severalMatchResult("Several matching lines found for: {}", marketBasedLine);
        }
    }

    private static MappingResults notFoundResult(Network networkReference, Line marketBasedLine, UcteNetworkAnalyzer analyser, String voltageLevelSide1, String voltageLevelSide2, String elementName) {
        boolean match;
        UcteMatchingResult resultElementName = analyser.findTopologicalElement(voltageLevelSide1, voltageLevelSide2, elementName);
        if (resultElementName.getStatus() == UcteMatchingResult.MatchStatus.SINGLE_MATCH) {
            match = resultElementName.hasMatched();
            //networkReference.getLine(resultElementName.getIidmIdentifiable().getId()).remove();
            return new MappingResults(marketBasedLine.getId(), resultElementName.getIidmIdentifiable().getId(), match);
        } else {
            match = false;
            if (resultElementName.getStatus() == UcteMatchingResult.MatchStatus.NOT_FOUND) {
                LOGGER.error("No matching line found for: {}", marketBasedLine.getId());
            } else {
                LOGGER.error("Several matching lines found for: {}", marketBasedLine.getId());
            }
            return new MappingResults(marketBasedLine.getId(), "", match);
        }
    }

    public static List<MappingResults> mapNetworks(Network networkReference, Network networkMarketBased) {
        List <Line> marketBasedLine = networkMarketBased.getLineStream().toList();
        return marketBasedLine.stream().map(line -> {
            return mapNetworks(networkReference, networkMarketBased, line);
        }).toList();
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
        List <TieLine> marketBasedTieLine = networkMarketBased.getTieLineStream().toList();
        return marketBasedTieLine.stream().map(tieline -> {
            String pairingKey = tieline.getDanglingLine1().getPairingKey();
            Stream<TieLine> listTieLinesReference = networkReference.getTieLineStream();
            List<TieLine> match = new java.util.ArrayList<>(List.of());
            listTieLinesReference.forEach(tieline2 -> {
                if (tieline2.getPairingKey().equals(pairingKey)) {
                    match.add(tieline2);
                    networkReference.getTieLine(tieline2.getId()).remove();
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
            List<MappingResults> same = entry.getValue();
            if (same.size() > 1) {
                LOGGER.error("Duplicates values found for: {}", entry.getKey());
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
    private UcteMapping() {
    }
}
