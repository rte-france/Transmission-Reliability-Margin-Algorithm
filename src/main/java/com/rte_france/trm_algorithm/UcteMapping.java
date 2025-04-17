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
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sebastian Huaraca {@literal <sebastian.huaracalapa at rte-france.com>}
 */

public final class UcteMapping {
    private static final Logger LOGGER = LoggerFactory.getLogger(UcteMapping.class);

    public static MappingResults mapNetworks(Network networkReference, Network networkMarketBased, String marketBasedId) {

        String voltageLevelSide1 = getVoltageLevelSide1(marketBasedId);
        String voltageLevelSide2 = getVoltageLevelSide2(marketBasedId);
        String orderCode = getOrderCode(marketBasedId);

        List<Line> matchLine = new ArrayList<>();
        networkReference.getLines().forEach(line -> {
            String idLine = line.getId();

            if (getVoltageLevelSide1(idLine).equals(voltageLevelSide1) && getVoltageLevelSide2(idLine).equals(voltageLevelSide2) && getOrderCode(idLine).equals(orderCode)) {
                matchLine.add(line);
            }

        });
        int nombreLines = matchLine.size();

        if (nombreLines > 1) {
            LOGGER.error("Several matching lines found for: {}", marketBasedId);
        } else if (nombreLines == 0) {
            LOGGER.error("No matching line found for: {}", marketBasedId);
        } else {
            return new MappingResults(marketBasedId, matchLine.get(0).getId(), true);
        }
        return new MappingResults(marketBasedId, "", false);
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

    private UcteMapping() {
    }

}
