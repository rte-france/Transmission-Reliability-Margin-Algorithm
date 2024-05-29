/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.algorithm;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class ZonalSensitivityComputerTest {

    public static final double EPSILON = 1e-3;

    @Test
    void testSimpleNetwork() {
        Network network = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");

        Set<Branch> branches = Set.of(network.getBranch("FFR2AA1  DDE3AA1  1"),
            network.getBranch("FFR1AA1  FFR2AA1  1"));
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase16Nodes/glsk_proportional_16nodes.xml"));
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(network);
        ZonalSensitivityComputer zonalSensitivityComputer = new ZonalSensitivityComputer(new LoadFlowParameters());
        Map<String, ZonalPtdfAndFlow> pdtf = zonalSensitivityComputer.run(network, branches, zonalGlsks);
        assertEquals(2, pdtf.keySet().size());
        assertEquals(0.315, pdtf.get("FFR2AA1  DDE3AA1  1").getZonalPtdf(), EPSILON);
        assertEquals(820.095, pdtf.get("FFR2AA1  DDE3AA1  1").getFlow(), EPSILON);
        assertEquals(0.058, pdtf.get("FFR1AA1  FFR2AA1  1").getZonalPtdf(), EPSILON);
        assertEquals(430.064, pdtf.get("FFR1AA1  FFR2AA1  1").getFlow(), EPSILON);
    }

    @Test
    void testSimpleNetworkWithoutBranch() {
        Network network = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");

        Set<Branch> branches = Collections.emptySet();
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase16Nodes/glsk_proportional_16nodes.xml"));
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(network);
        ZonalSensitivityComputer zonalSensitivityComputer = new ZonalSensitivityComputer(new LoadFlowParameters());
        Map<String, ZonalPtdfAndFlow> pdtf = zonalSensitivityComputer.run(network, branches, zonalGlsks);
        assertTrue(pdtf.keySet().isEmpty());
    }

    @Test
    void testForceAcSensitivityAnalysis() {
        Network network = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        Set<Branch> branches = Set.of(network.getBranch("FFR2AA1  DDE3AA1  1"));
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("TestCase16Nodes/glsk_proportional_16nodes.xml"));
        ZonalData<SensitivityVariableSet> zonalGlsks = ucteGlskDocument.getZonalGlsks(network);

        LoadFlowParameters loadFlowParameters = new LoadFlowParameters().setDc(true);

        ZonalSensitivityComputer zonalSensitivityComputer = new ZonalSensitivityComputer(loadFlowParameters);
        Map<String, ZonalPtdfAndFlow> pdtf = zonalSensitivityComputer.run(network, branches, zonalGlsks);
        assertEquals(1, pdtf.keySet().size());
        assertEquals(0.315, pdtf.get("FFR2AA1  DDE3AA1  1").getZonalPtdf(), EPSILON);
        assertEquals(820.095, pdtf.get("FFR2AA1  DDE3AA1  1").getFlow(), EPSILON);
    }
}
