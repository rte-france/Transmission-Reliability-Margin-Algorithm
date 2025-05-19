/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;
import com.powsybl.flow_decomposition.XnecProvider;
import com.powsybl.flow_decomposition.xnec_provider.XnecProviderInterconnection;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.cse.CseGlskDocument;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.rte_france.trm_algorithm.operational_conditions_aligners.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class InTrmTest {

    public static final double EPSILON = 1e-3;

    TrmAlgorithm setUp() {
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        DanglingLineAligner danglingLineAligner = new DanglingLineAligner();
        PstAligner pstAligner = new PstAligner();
        // Missing aligners for IN region
        OperationalConditionAligner operationalConditionAligner = new OperationalConditionAlignerPipeline(danglingLineAligner, pstAligner);
        return new TrmAlgorithm(loadFlowParameters, operationalConditionAligner);
    }

    @Test
    void testInNetworkTestIsFullyConverged() {
        Network network = TestUtils.importNetwork("TestCaseIn/NETWORK_TEST_IN_MARKET.uct");
        var lfResult = LoadFlow.run(network);
        assertTrue(lfResult.isFullyConverged());
    }

    @Test
    @Disabled("Fails as all alignments are not yet implemented")
    void testAllDifferencesShouldBeFixedByAlignment() {
        /*
        This test aims at checking that we correctly aligned all that should be.
        Between Market and Reference file provided in tests, only difference are:
        - HVDC set points between FR and ES in equivalent format
        - PST set point for Merchant line in Switzerland
        - All injections that are shifted from market IN import to reference one following GLSK values and shifting factors
            - FR: 0.3
            - CH: 0.25
            - AT: 0.25
            - SI: 0.2

        The uncertainties should be 0 when all these alignments are implemented

        DE country is only used to illustrate the possibility for CH GLSK to reference DE nodes.

        Below are injections modification when passing from market file to the one shifted to the reference italian import,
        using given shifting factors and GLSK
        - IT: Market -1950 MW / Reference -2950 MW
            - IGENE111: 500 MW -> 100 MW
            - IGENE211: 750 MW -> 150 MW
            - IGENE311: 750 MW -> 750 MW
        - FR: Market 600 MW / Reference 900 MW
            - FGENE111: 1000 MW -> 1187.5 MW
            - FGENE211: 500 MW -> 593.75 MW
            - FGENE311: 100 MW -> 118.75 MW
        - CH + DE: Market 850 MW / Reference 1100 MW
            - SGENE111: 250 MW -> 275 MW
            - SGENE211: 250 MW -> 375 MW
            - DGENE111: 1000 MW -> 1100 MW
        - AT: Market 400 MW / Reference 650 MW
            - OGENE111: 700 MW -> 950 MW
        - SI: Market 100 MW / Reference 300 MW
            - LGENE111: 200 MW -> 300 MW
            - LLOAD111: -100 MW -> 0 MW
         */
        Network referenceNetwork = TestUtils.importNetwork("TestCaseIn/NETWORK_TEST_IN_REFERENCE.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCaseIn/NETWORK_TEST_IN_MARKET.uct");
        GlskDocument glskDocument = CseGlskDocument.importGlsk(getClass().getResourceAsStream("TestCaseIn/GLSK_FILE.xml"), true, true);
        XnecProvider xnecProvider = new XnecProviderInterconnection();
        TrmAlgorithm trmAlgorithm = setUp();
        TrmResults trmResults = trmAlgorithm.computeUncertainties(referenceNetwork, marketBasedNetwork, xnecProvider, TrmUtils.getAutoGlsk(marketBasedNetwork));
        Map<String, UncertaintyResult> result = trmResults.getUncertaintiesMap();
        assertEquals(15, result.size());
        result.forEach((xnec, uncertainty) -> assertEquals(0.0, uncertainty.getUncertainty(), EPSILON));
    }
}
