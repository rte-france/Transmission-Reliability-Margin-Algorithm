/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class InTrmTest {

    @Test
    void testInNetworkTestIsFullyConverged() {
        Network network = TestUtils.importNetwork("TestCaseIn/NETWORK_TEST_IN.uct");
        var lfResult = LoadFlow.run(network);
        assertTrue(lfResult.isFullyConverged());
    }

    @Test
    void testWithShiftingFactors() {
        /*
        FR : 0.3
        CH : 0.25
        AT : 0.25
        SI : 0.2

Via Shift
        IT : -1950 MW -> -2950 MW
          IGENE111 : 500 MW -> 100 MW
          IGENE211 : 750 MW -> 150 MW
          IGENE311 : 750 MW -> 750 MW
        FR : 600 MW -> 900 MW
          FGENE111 : 1000 MW -> 1187.5 MW
          FGENE211 : 500 MW -> 593.75 MW
          FGENE311 : 100 MW -> 118.75 MW
        CH : 350 MW -> 500 MW
          SGENE111 : 250 MW -> 275 MW
          SGENE211 : 250 MW -> 375 MW
        DE : 500 MW -> 600 MW
          DGENE111 : 1000 MW -> 1100 MW
        AT : 400 MW -> 650 MW
          0GENE111 : 700 MW -> 950 MW
        SI : 100 MW -> 300 MW
          LGENE111 : 200 MW -> 300 MW
          LLOAD111 : -100 MW -> 0 MW
Reference
        IT : -1950 MW -> -2950 MW
          IGENE111 : 500 MW -> 300 MW
          IGENE211 : 750 MW -> 250 MW
          IGENE311 : 750 MW -> 450 MW
        FR : 600 MW -> 900 MW
          FGENE111 : 1000 MW -> 1200 MW
          FGENE211 : 500 MW -> 600 MW
          FGENE311 : 100 MW -> 100 MW
        CH : 350 MW -> 400 MW
          SGENE111 : 250 MW -> 275 MW
          SGENE211 : 250 MW -> 275 MW
        DE : 500 MW -> 700 MW
          DGENE111 : 1000 MW -> 1200 MW
        AT : 400 MW -> 650 MW
          0GENE111 : 700 MW -> 850 MW
          0LOAD111 : -100 MW -> -50 MW
          0LOAD211 : -100 MW -> -50 MW
        SI : 100 MW -> 300 MW
          LGENE111 : 200 MW -> 500 MW
          LLOAD111 : -100 MW -> -200 MW

         HVDC : 300 MW sur chaque
         */
    }
}
