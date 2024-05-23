/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.algorithm.operational_conditions_aligners;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.rte_france.trm_algorithm.algorithm.TrmException;

import java.util.Objects;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class PstAligner {

    private PstAligner() { } // Utility class

    public static void align(Network referenceNetwork, Network marketBasedNetwork) {
        referenceNetwork.getTwoWindingsTransformerStream().forEach(referenceTwoWindingsTransformer -> {
            int referenceTapPosition = referenceTwoWindingsTransformer.getPhaseTapChanger().getTapPosition();
            String id = referenceTwoWindingsTransformer.getId();
            TwoWindingsTransformer twoWindingsTransformer = marketBasedNetwork.getTwoWindingsTransformer(id);
            if (Objects.isNull(twoWindingsTransformer)) {
                throw new TrmException(String.format("Two windings transformer %s not found", id));
            }
            twoWindingsTransformer.getPhaseTapChanger().setTapPosition(referenceTapPosition);
        });
    }
}
