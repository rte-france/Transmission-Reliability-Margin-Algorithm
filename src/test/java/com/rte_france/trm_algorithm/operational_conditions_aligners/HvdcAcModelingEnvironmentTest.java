/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.HvdcCreationParameters;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.SwePreprocessorParameters;
import com.farao_community.farao.gridcapa_swe_commons.hvdc.parameters.json.JsonSwePreprocessorImporter;
import com.powsybl.iidm.network.Network;
import com.rte_france.trm_algorithm.TestUtils;
import com.rte_france.trm_algorithm.TrmException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class HvdcAcModelingEnvironmentTest {
    private static void assertNoHvdcModeling(Network referenceNetwork, Network marketBasedNetwork) {
        assertEquals(0, referenceNetwork.getHvdcLineCount());
        assertEquals(0, marketBasedNetwork.getHvdcLineCount());
    }

    @Test
    void testSpy() {
        Network referenceNetwork = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16Nodes.xiidm");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/hvdc/TestCase16Nodes.xiidm");
        SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("hvdc/SwePreprocessorParameters.json"));
        Set<HvdcCreationParameters> creationParametersSet = params.getHvdcCreationParametersSet();

        SpyAligner spyAligner = new SpyAligner();
        HvdcAcModelingEnvironment hvdcAcModelingEnvironment = new HvdcAcModelingEnvironment(creationParametersSet, spyAligner);

        assertNoHvdcModeling(referenceNetwork, marketBasedNetwork);
        hvdcAcModelingEnvironment.align(referenceNetwork, marketBasedNetwork);
        assertNoHvdcModeling(referenceNetwork, marketBasedNetwork);
    }

    @Test
    void testDcEquivalentModel() {
        SwePreprocessorParameters params = JsonSwePreprocessorImporter.read(getClass().getResourceAsStream("hvdc/SwePreprocessorParametersWithoutAngleDroopActivePowerControl.json"));
        Set<HvdcCreationParameters> creationParametersSet = params.getHvdcCreationParametersSet();

        NoOpAligner noOpAligner = new NoOpAligner();
        TrmException trmException = assertThrows(TrmException.class, () -> new HvdcAcModelingEnvironment(creationParametersSet, noOpAligner));
        assertEquals("HVDC \"HVDC_FR4-DE1\" has a DC control mode.", trmException.getMessage());
    }

    private static class SpyAligner implements OperationalConditionAligner {
        private static void assertHvdcModeling(Network network) {
            assertEquals(2, network.getHvdcLineCount());
            assertNotNull(network.getHvdcLine("HVDC_BE2-FR3"));
            assertNotNull(network.getHvdcLine("HVDC_FR4-DE1"));
        }

        @Override
        public void align(Network referenceNetwork, Network marketBasedNetwork) {
            assertHvdcModeling(referenceNetwork);
            assertHvdcModeling(marketBasedNetwork);
        }
    }

    private static class NoOpAligner implements OperationalConditionAligner {
        @Override
        public void align(Network referenceNetwork, Network marketBasedNetwork) {
            // nothing
        }
    }
}
