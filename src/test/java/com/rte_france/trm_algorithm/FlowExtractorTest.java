/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class FlowExtractorTest {
    public static final double EPSILON = 1e-3;

    @Test
    void testExtractFlowTwoBranches() {
        Network network = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");

        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        FlowExtractor flowExtractor = new FlowExtractor(loadFlowParameters);

        Set<Branch> branchSet = Set.of(network.getBranch("FGEN1 11 BLOAD 11 1"), network.getBranch("BLOAD 11 BGEN2 11 1"));
        Map<String, Double> result = flowExtractor.extract(network, branchSet);
        assertEquals(100.125, result.get("FGEN1 11 BLOAD 11 1"), EPSILON);
        assertEquals(-99.937, result.get("BLOAD 11 BGEN2 11 1"), EPSILON);
    }

    @Test
    void forceAcLoadFlow() {
        Network network = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");

        LoadFlowParameters loadFlowParameters = new LoadFlowParameters()
            .setDc(true);
        FlowExtractor flowExtractor = new FlowExtractor(loadFlowParameters);

        Set<Branch> branchSet = Set.of(network.getBranch("FGEN1 11 BLOAD 11 1"));
        Map<String, Double> result = flowExtractor.extract(network, branchSet);
        assertEquals(100.125, result.get("FGEN1 11 BLOAD 11 1"), EPSILON);
    }
}
