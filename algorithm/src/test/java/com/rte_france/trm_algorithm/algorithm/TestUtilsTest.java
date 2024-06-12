/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.algorithm;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class TestUtilsTest {
    private static final double EPSILON = 1e-3;

    @Test
    void testThatGlskAreWellComputed() {
        Network network = TestUtils.importNetwork("cne_selection/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");

        ZonalData<SensitivityVariableSet> zonalGlsks = TestUtils.getAutoGlsk(network);

        assertEquals(1.0, zonalGlsks.getData("BE").getVariable("BGEN2 11_generator").getWeight(), EPSILON);
        assertEquals(1.0, zonalGlsks.getData("FR").getVariable("FGEN1 11_generator").getWeight(), EPSILON);
    }

    @Test
    void testThatGlskIgnoreDisconnectedGenerators() {
        Network network = TestUtils.importNetwork("cne_selection/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");

        network.getGenerator("FGEN1 11_generator").getTerminal().disconnect();

        ZonalData<SensitivityVariableSet> zonalGlsks = TestUtils.getAutoGlsk(network);

        assertEquals(1.0, zonalGlsks.getData("BE").getVariable("BGEN2 11_generator").getWeight(), EPSILON);
        assertTrue(zonalGlsks.getData("FR").getVariables().isEmpty());
    }

    @Test
    void testThatGlskWorkForNullTargetP() {
        String genBe = "BGEN2 11_generator";
        String genFr = "FGEN1 11_generator";
        Network network = TestUtils.importNetwork("cne_selection/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");

        network.getGenerator(genFr).setTargetP(0.0);

        ZonalData<SensitivityVariableSet> zonalGlsks = TestUtils.getAutoGlsk(network);

        assertEquals(1.0, zonalGlsks.getData("BE").getVariable(genBe).getWeight(), EPSILON);
        assertEquals(1.0, zonalGlsks.getData("FR").getVariable(genFr).getWeight(), EPSILON);
    }

    @Test
    void testIdealTopologicalAlignerCrac() {
        Network network = TestUtils.importNetwork("cne_selection/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");

        Crac crac = TestUtils.getIdealTopologicalAlignerCrac(network);

        assertEquals(4, crac.getNetworkActions().size());
        assertEquals(1, crac.getNetworkAction("Topological action with branch:\"BLOAD 11 BGEN2 11 1\", actionType:CLOSE").getElementaryActions().size());
        assertEquals(1, crac.getNetworkAction("Topological action with branch:\"BLOAD 11 BGEN2 11 1\", actionType:OPEN").getElementaryActions().size());
        assertEquals(1, crac.getNetworkAction("Topological action with branch:\"FGEN1 11 BLOAD 11 1\", actionType:CLOSE").getElementaryActions().size());
        assertEquals(1, crac.getNetworkAction("Topological action with branch:\"FGEN1 11 BLOAD 11 1\", actionType:OPEN").getElementaryActions().size());
    }

    @Test
    void testGetAutoScalable() {
        Network network = TestUtils.importNetwork("cne_selection/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");

        ZonalData<Scalable> zonalGlsks = TestUtils.getAutoScalable(network);

        assertEquals(-1000.0, zonalGlsks.getData("10YBE----------2").minimumValue(network), EPSILON);
        assertEquals(1000.0, zonalGlsks.getData("10YBE----------2").maximumValue(network), EPSILON);
        assertEquals(-1000.0, zonalGlsks.getData("10YFR-RTE------C").minimumValue(network), EPSILON);
        assertEquals(1000.0, zonalGlsks.getData("10YFR-RTE------C").maximumValue(network), EPSILON);
    }
}

