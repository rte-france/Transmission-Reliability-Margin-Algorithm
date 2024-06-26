/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.test.FourSubstationsNodeBreakerFactory;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class TrmUtilsTest {
    private static final double EPSILON = 1e-3;

    @Test
    void testNetworkWithoutCountriesInSubstations() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        Terminal terminal = network.getLine("LINE_S2S3").getTerminal1();
        Exception exception = assertThrows(TrmException.class, () -> TrmUtils.getCountry(terminal));
        assertEquals("Optional country of substation 'S2' is empty", exception.getMessage());
    }

    @Test
    void testGetCountryGeneratorsScalable() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        Scalable scalableFR = TrmUtils.getCountryGeneratorsScalable(referenceNetwork, Country.FR);
        assertEquals(45000, scalableFR.maximumValue(referenceNetwork), EPSILON);
        assertEquals(-45000, scalableFR.minimumValue(referenceNetwork), EPSILON);
    }

    @Test
    void testThatGlskAreWellComputed() {
        Network network = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");

        ZonalData<SensitivityVariableSet> zonalGlsks = TrmUtils.getAutoGlsk(network);

        assertEquals(1.0, zonalGlsks.getData("BE").getVariable("BGEN2 11_generator").getWeight(), EPSILON);
        assertEquals(1.0, zonalGlsks.getData("FR").getVariable("FGEN1 11_generator").getWeight(), EPSILON);
    }

    @Test
    void testThatGlskIgnoreDisconnectedGenerators() {
        Network network = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");

        network.getGenerator("FGEN1 11_generator").getTerminal().disconnect();

        ZonalData<SensitivityVariableSet> zonalGlsks = TrmUtils.getAutoGlsk(network);

        assertEquals(1.0, zonalGlsks.getData("BE").getVariable("BGEN2 11_generator").getWeight(), EPSILON);
        assertTrue(zonalGlsks.getData("FR").getVariables().isEmpty());
    }

    @Test
    void testThatGlskWorkForNullTargetP() {
        String genBe = "BGEN2 11_generator";
        String genFr = "FGEN1 11_generator";
        Network network = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");

        network.getGenerator(genFr).setTargetP(0.0);

        ZonalData<SensitivityVariableSet> zonalGlsks = TrmUtils.getAutoGlsk(network);

        assertEquals(1.0, zonalGlsks.getData("BE").getVariable(genBe).getWeight(), EPSILON);
        assertEquals(1.0, zonalGlsks.getData("FR").getVariable(genFr).getWeight(), EPSILON);
    }

    @Test
    void testGetAutoScalable() {
        Network network = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");

        ZonalData<Scalable> zonalGlsks = TrmUtils.getAutoScalable(network);

        assertEquals(-1000.0, zonalGlsks.getData("10YBE----------2").minimumValue(network), EPSILON);
        assertEquals(1000.0, zonalGlsks.getData("10YBE----------2").maximumValue(network), EPSILON);
        assertEquals(-1000.0, zonalGlsks.getData("10YFR-RTE------C").minimumValue(network), EPSILON);
        assertEquals(1000.0, zonalGlsks.getData("10YFR-RTE------C").maximumValue(network), EPSILON);
    }
}
