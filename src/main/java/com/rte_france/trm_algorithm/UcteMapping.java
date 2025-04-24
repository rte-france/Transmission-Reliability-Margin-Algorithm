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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * @author Sebastian Huaraca {@literal <sebastian.huaracalapa at rte-france.com>}
 */

public final class UcteMapping {
    private static final Logger LOGGER = LoggerFactory.getLogger(UcteMapping.class);

    public static MappingResults mapNetworks(Network networkReference, Network networkMarketBased, String marketBasedId) {

        String voltageLevelSide1 = getVoltageLevelSide1(marketBasedId);
        String voltageLevelSide2 = getVoltageLevelSide2(marketBasedId);
        String orderCode = getOrderCode(marketBasedId);

        List<Line> matchLine = networkReference.getLineStream().toList();
        matchLine = matchLine.stream().filter(n -> getVoltageLevelSide1(n.getId()).equals(voltageLevelSide1)).toList();
        matchLine = matchLine.stream().filter(n -> getVoltageLevelSide2(n.getId()).equals(voltageLevelSide2)).toList();
        matchLine = matchLine.stream().filter(n -> getOrderCode(n.getId()).equals(orderCode)).toList();
        return createMappingResults(marketBasedId, matchLine);
    }

    public static List<MappingResults> mapNetworks(Network networkReference, Network networkMarketBased, List<String> marketBasedId) {
        return marketBasedId.stream().map(id -> {

            String voltageLevelSide1 = getVoltageLevelSide1(id);
            String voltageLevelSide2 = getVoltageLevelSide2(id);
            String orderCode = getOrderCode(id);
            Optional<String> elementName = Optional.ofNullable(networkMarketBased.getLine(id).getProperty("elementName"));
            String elementName1 = elementName.map(s -> s.substring(0, 4)).orElse("");
            String elementName2 = elementName.map(s -> s.substring(8)).orElse("");

            List<Line> matchLine = networkReference.getLineStream()
                    .filter(n -> getVoltageLevelSide1(n.getId()).equals(voltageLevelSide1))
                    .filter(n -> getVoltageLevelSide2(n.getId()).equals(voltageLevelSide2))
                    .filter(n -> getOrderCode(n.getId()).equals(orderCode)).toList();
            if (matchLine.isEmpty()) {
                matchLine = networkReference.getLineStream()
                        .filter(n -> getVoltageLevelSide1(n.getId()).equals(voltageLevelSide1))
                        .filter(n -> getVoltageLevelSide2(n.getId()).equals(voltageLevelSide2))
                        .filter(n -> n.getProperty("elementName").substring(0, 4).equals(elementName1)
                                && n.getProperty("elementName").substring(8).equals(elementName2)).toList();
            }
            return createMappingResults(id, matchLine);
        }
        ).toList();
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
