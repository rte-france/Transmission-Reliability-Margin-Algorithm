/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class ZonalSensitivityComputerTest {

    public static final double EPSILON = 1e-3;

    @Test
    void testSimpleNetwork() {
        Network network = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");

        List<String> branchIds = List.of("FFR2AA1  DDE3AA1  1", "FFR1AA1  FFR2AA1  1");
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase16Nodes/glsk_proportional_16nodes.xml"));
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(network);
        ZonalSensitivityComputer zonalSensitivityComputer = new ZonalSensitivityComputer(new LoadFlowParameters());
        Map<String, ZonalPtdfAndFlow> ptdf = zonalSensitivityComputer.run(network, branchIds, zonalGlsks);
        assertEquals(2, ptdf.size());
        assertEquals(0.315, ptdf.get("FFR2AA1  DDE3AA1  1").getZonalPtdf(), EPSILON);
        assertEquals(820.095, ptdf.get("FFR2AA1  DDE3AA1  1").getFlow(), EPSILON);
        assertEquals(0.058, ptdf.get("FFR1AA1  FFR2AA1  1").getZonalPtdf(), EPSILON);
        assertEquals(430.064, ptdf.get("FFR1AA1  FFR2AA1  1").getFlow(), EPSILON);
    }

    @Test
    void testSimpleNetworkWithoutBranch() {
        Network network = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");

        List<String> branchIds = Collections.emptyList();
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase16Nodes/glsk_proportional_16nodes.xml"));
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(network);
        ZonalSensitivityComputer zonalSensitivityComputer = new ZonalSensitivityComputer(new LoadFlowParameters());
        Map<String, ZonalPtdfAndFlow> ptdf = zonalSensitivityComputer.run(network, branchIds, zonalGlsks);
        assertTrue(ptdf.isEmpty());
    }

    @Test
    void testForceAcSensitivityAnalysis() {
        Network network = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        List<String> branchIds = List.of("FFR2AA1  DDE3AA1  1");
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase16Nodes/glsk_proportional_16nodes.xml"));
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(network);

        LoadFlowParameters loadFlowParameters = new LoadFlowParameters().setDc(true);

        ZonalSensitivityComputer zonalSensitivityComputer = new ZonalSensitivityComputer(loadFlowParameters);
        Map<String, ZonalPtdfAndFlow> ptdf = zonalSensitivityComputer.run(network, branchIds, zonalGlsks);
        assertEquals(1, ptdf.size());
        assertEquals(0.315, ptdf.get("FFR2AA1  DDE3AA1  1").getZonalPtdf(), EPSILON);
        assertEquals(820.095, ptdf.get("FFR2AA1  DDE3AA1  1").getFlow(), EPSILON);
    }

    @Test
    void testAcDivergence() {
        Network network = TestUtils.importNetwork("simple_networks/NETWORK_LOOP_FLOW_WITH_COUNTRIES.uct");
        List<String> branchIds = List.of("EGEN  11 FGEN  11 1");
        ZonalData<SensitivityVariableSet> zonalGlsks = TrmUtils.getAutoGlsk(network);
        ZonalSensitivityComputer zonalSensitivityComputer = new ZonalSensitivityComputer(new LoadFlowParameters());
        assertThrows(Exception.class, () -> zonalSensitivityComputer.run(network, branchIds, zonalGlsks));
    }
}
