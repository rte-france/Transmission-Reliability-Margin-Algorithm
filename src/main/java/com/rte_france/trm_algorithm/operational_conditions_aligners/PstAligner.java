/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChangerHolder;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class PstAligner {
    private static final Logger LOGGER = LoggerFactory.getLogger(PstAligner.class);

    private PstAligner() {
        // Utility class
    }

    public static Map<String, Boolean> align(Network referenceNetwork, Network marketBasedNetwork) {
        return referenceNetwork.getTwoWindingsTransformerStream()
            .filter(PhaseTapChangerHolder::hasPhaseTapChanger)
            .collect(Collectors.toMap(
                Identifiable::getId,
                referenceTwoWindingsTransformer -> alignPst(marketBasedNetwork, referenceTwoWindingsTransformer)
            ));
    }

    private static boolean alignPst(Network marketBasedNetwork, TwoWindingsTransformer referenceTwoWindingsTransformer) {
        int referenceTapPosition = referenceTwoWindingsTransformer.getPhaseTapChanger().getTapPosition();
        String id = referenceTwoWindingsTransformer.getId();
        TwoWindingsTransformer twoWindingsTransformer = marketBasedNetwork.getTwoWindingsTransformer(id);
        if (Objects.isNull(twoWindingsTransformer)) {
            LOGGER.error("Two windings transformer '{}' not found in market based network", id);
            return false;
        }
        twoWindingsTransformer.getPhaseTapChanger().setTapPosition(referenceTapPosition);
        return true;
    }
}
