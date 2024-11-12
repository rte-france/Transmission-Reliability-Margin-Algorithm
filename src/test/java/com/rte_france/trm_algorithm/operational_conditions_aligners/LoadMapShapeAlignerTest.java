/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.rte_france.trm_algorithm.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class LoadMapShapeAlignerTest {
    private static final double EPSILON = 1e-1;

    @Test
    void testAligningInjectionsMapShape() {
        Network referenceNetwork = TestUtils.importNetwork("operational_conditions_aligners/injection_shape/reference.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/injection_shape/marketBased.uct");

        assertEquals(110.0, referenceNetwork.getGenerator("F000011 _generator").getTargetP(), EPSILON);
        assertEquals(10.0, referenceNetwork.getGenerator("F000012 _generator").getTargetP(), EPSILON);
        assertEquals(20.0, referenceNetwork.getLoad("F000012 _load").getP0(), EPSILON);
        assertEquals(10.0, referenceNetwork.getLoad("F000013 _load").getP0(), EPSILON);
        assertEquals(140.0, referenceNetwork.getLoad("E000011 _load").getP0(), EPSILON);
        assertEquals(10.0, referenceNetwork.getLoad("E000012 _load").getP0(), EPSILON);
        assertEquals(50.0, referenceNetwork.getGenerator("E000012 _generator").getTargetP(), EPSILON);
        assertEquals(10.0, referenceNetwork.getGenerator("E000013 _generator").getTargetP(), EPSILON);

        assertEquals(50.0, marketBasedNetwork.getGenerator("F000011 _generator").getTargetP(), EPSILON);
        assertEquals(10.0, marketBasedNetwork.getGenerator("F000012 _generator").getTargetP(), EPSILON);
        assertEquals(10.0, marketBasedNetwork.getLoad("F000012 _load").getP0(), EPSILON);
        assertEquals(35.0, marketBasedNetwork.getLoad("F000013 _load").getP0(), EPSILON);
        assertEquals(90.0, marketBasedNetwork.getLoad("E000011 _load").getP0(), EPSILON);
        assertEquals(40.0, marketBasedNetwork.getLoad("E000012 _load").getP0(), EPSILON);
        assertEquals(15.0, marketBasedNetwork.getGenerator("E000012 _generator").getTargetP(), EPSILON);
        assertEquals(100.0, marketBasedNetwork.getGenerator("E000013 _generator").getTargetP(), EPSILON);

        LoadMapShapeAligner aligner = new LoadMapShapeAligner();
        aligner.align(referenceNetwork, marketBasedNetwork);

        assertEquals(50.0, marketBasedNetwork.getGenerator("F000011 _generator").getTargetP(), EPSILON);
        assertEquals(10.0, marketBasedNetwork.getGenerator("F000012 _generator").getTargetP(), EPSILON);
        assertEquals(30.0, marketBasedNetwork.getLoad("F000012 _load").getP0(), EPSILON);
        assertEquals(15.0, marketBasedNetwork.getLoad("F000013 _load").getP0(), EPSILON);
        assertEquals(121.3, marketBasedNetwork.getLoad("E000011 _load").getP0(), EPSILON);
        assertEquals(8.7, marketBasedNetwork.getLoad("E000012 _load").getP0(), EPSILON);
        assertEquals(15.0, marketBasedNetwork.getGenerator("E000012 _generator").getTargetP(), EPSILON);
        assertEquals(100.0, marketBasedNetwork.getGenerator("E000013 _generator").getTargetP(), EPSILON);
    }

    @Test
    void testAligningInjectionsMapShapeWithCountry() {
        Network referenceNetwork = TestUtils.importNetwork("operational_conditions_aligners/injection_shape/reference.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/injection_shape/marketBased.uct");

        assertEquals(110.0, referenceNetwork.getGenerator("F000011 _generator").getTargetP(), EPSILON);
        assertEquals(10.0, referenceNetwork.getGenerator("F000012 _generator").getTargetP(), EPSILON);
        assertEquals(20.0, referenceNetwork.getLoad("F000012 _load").getP0(), EPSILON);
        assertEquals(10.0, referenceNetwork.getLoad("F000013 _load").getP0(), EPSILON);
        assertEquals(140.0, referenceNetwork.getLoad("E000011 _load").getP0(), EPSILON);
        assertEquals(10.0, referenceNetwork.getLoad("E000012 _load").getP0(), EPSILON);
        assertEquals(50.0, referenceNetwork.getGenerator("E000012 _generator").getTargetP(), EPSILON);
        assertEquals(10.0, referenceNetwork.getGenerator("E000013 _generator").getTargetP(), EPSILON);

        assertEquals(50.0, marketBasedNetwork.getGenerator("F000011 _generator").getTargetP(), EPSILON);
        assertEquals(10.0, marketBasedNetwork.getGenerator("F000012 _generator").getTargetP(), EPSILON);
        assertEquals(10.0, marketBasedNetwork.getLoad("F000012 _load").getP0(), EPSILON);
        assertEquals(35.0, marketBasedNetwork.getLoad("F000013 _load").getP0(), EPSILON);
        assertEquals(90.0, marketBasedNetwork.getLoad("E000011 _load").getP0(), EPSILON);
        assertEquals(40.0, marketBasedNetwork.getLoad("E000012 _load").getP0(), EPSILON);
        assertEquals(15.0, marketBasedNetwork.getGenerator("E000012 _generator").getTargetP(), EPSILON);
        assertEquals(100.0, marketBasedNetwork.getGenerator("E000013 _generator").getTargetP(), EPSILON);

        LoadMapShapeAligner aligner = new LoadMapShapeAligner(Country.FR);
        aligner.align(referenceNetwork, marketBasedNetwork);

        assertEquals(50.0, marketBasedNetwork.getGenerator("F000011 _generator").getTargetP(), EPSILON);
        assertEquals(10.0, marketBasedNetwork.getGenerator("F000012 _generator").getTargetP(), EPSILON);
        assertEquals(30.0, marketBasedNetwork.getLoad("F000012 _load").getP0(), EPSILON);
        assertEquals(15.0, marketBasedNetwork.getLoad("F000013 _load").getP0(), EPSILON);
        assertEquals(90.0, marketBasedNetwork.getLoad("E000011 _load").getP0(), EPSILON);
        assertEquals(40.0, marketBasedNetwork.getLoad("E000012 _load").getP0(), EPSILON);
        assertEquals(15.0, marketBasedNetwork.getGenerator("E000012 _generator").getTargetP(), EPSILON);
        assertEquals(100.0, marketBasedNetwork.getGenerator("E000013 _generator").getTargetP(), EPSILON);
    }

    @Test
    void testAligningInjectionsMapShapeCountriesVarargs() {
        Network referenceNetwork = TestUtils.importNetwork("operational_conditions_aligners/injection_shape/reference.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/injection_shape/marketBased.uct");

        assertEquals(110.0, referenceNetwork.getGenerator("F000011 _generator").getTargetP(), EPSILON);
        assertEquals(10.0, referenceNetwork.getGenerator("F000012 _generator").getTargetP(), EPSILON);
        assertEquals(20.0, referenceNetwork.getLoad("F000012 _load").getP0(), EPSILON);
        assertEquals(10.0, referenceNetwork.getLoad("F000013 _load").getP0(), EPSILON);
        assertEquals(140.0, referenceNetwork.getLoad("E000011 _load").getP0(), EPSILON);
        assertEquals(10.0, referenceNetwork.getLoad("E000012 _load").getP0(), EPSILON);
        assertEquals(50.0, referenceNetwork.getGenerator("E000012 _generator").getTargetP(), EPSILON);
        assertEquals(10.0, referenceNetwork.getGenerator("E000013 _generator").getTargetP(), EPSILON);

        assertEquals(50.0, marketBasedNetwork.getGenerator("F000011 _generator").getTargetP(), EPSILON);
        assertEquals(10.0, marketBasedNetwork.getGenerator("F000012 _generator").getTargetP(), EPSILON);
        assertEquals(10.0, marketBasedNetwork.getLoad("F000012 _load").getP0(), EPSILON);
        assertEquals(35.0, marketBasedNetwork.getLoad("F000013 _load").getP0(), EPSILON);
        assertEquals(90.0, marketBasedNetwork.getLoad("E000011 _load").getP0(), EPSILON);
        assertEquals(40.0, marketBasedNetwork.getLoad("E000012 _load").getP0(), EPSILON);
        assertEquals(15.0, marketBasedNetwork.getGenerator("E000012 _generator").getTargetP(), EPSILON);
        assertEquals(100.0, marketBasedNetwork.getGenerator("E000013 _generator").getTargetP(), EPSILON);

        LoadMapShapeAligner aligner = new LoadMapShapeAligner(Country.FR, Country.ES);
        aligner.align(referenceNetwork, marketBasedNetwork);

        assertEquals(50.0, marketBasedNetwork.getGenerator("F000011 _generator").getTargetP(), EPSILON);
        assertEquals(10.0, marketBasedNetwork.getGenerator("F000012 _generator").getTargetP(), EPSILON);
        assertEquals(30.0, marketBasedNetwork.getLoad("F000012 _load").getP0(), EPSILON);
        assertEquals(15.0, marketBasedNetwork.getLoad("F000013 _load").getP0(), EPSILON);
        assertEquals(121.3, marketBasedNetwork.getLoad("E000011 _load").getP0(), EPSILON);
        assertEquals(8.7, marketBasedNetwork.getLoad("E000012 _load").getP0(), EPSILON);
        assertEquals(15.0, marketBasedNetwork.getGenerator("E000012 _generator").getTargetP(), EPSILON);
        assertEquals(100.0, marketBasedNetwork.getGenerator("E000013 _generator").getTargetP(), EPSILON);
    }

    @Test
    void testAligningInjectionsMapShapeCountriesSet() {
        Network referenceNetwork = TestUtils.importNetwork("operational_conditions_aligners/injection_shape/reference.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/injection_shape/marketBased.uct");

        assertEquals(110.0, referenceNetwork.getGenerator("F000011 _generator").getTargetP(), EPSILON);
        assertEquals(10.0, referenceNetwork.getGenerator("F000012 _generator").getTargetP(), EPSILON);
        assertEquals(20.0, referenceNetwork.getLoad("F000012 _load").getP0(), EPSILON);
        assertEquals(10.0, referenceNetwork.getLoad("F000013 _load").getP0(), EPSILON);
        assertEquals(140.0, referenceNetwork.getLoad("E000011 _load").getP0(), EPSILON);
        assertEquals(10.0, referenceNetwork.getLoad("E000012 _load").getP0(), EPSILON);
        assertEquals(50.0, referenceNetwork.getGenerator("E000012 _generator").getTargetP(), EPSILON);
        assertEquals(10.0, referenceNetwork.getGenerator("E000013 _generator").getTargetP(), EPSILON);

        assertEquals(50.0, marketBasedNetwork.getGenerator("F000011 _generator").getTargetP(), EPSILON);
        assertEquals(10.0, marketBasedNetwork.getGenerator("F000012 _generator").getTargetP(), EPSILON);
        assertEquals(10.0, marketBasedNetwork.getLoad("F000012 _load").getP0(), EPSILON);
        assertEquals(35.0, marketBasedNetwork.getLoad("F000013 _load").getP0(), EPSILON);
        assertEquals(90.0, marketBasedNetwork.getLoad("E000011 _load").getP0(), EPSILON);
        assertEquals(40.0, marketBasedNetwork.getLoad("E000012 _load").getP0(), EPSILON);
        assertEquals(15.0, marketBasedNetwork.getGenerator("E000012 _generator").getTargetP(), EPSILON);
        assertEquals(100.0, marketBasedNetwork.getGenerator("E000013 _generator").getTargetP(), EPSILON);

        LoadMapShapeAligner aligner = new LoadMapShapeAligner(Set.of(Country.FR, Country.ES));
        aligner.align(referenceNetwork, marketBasedNetwork);

        assertEquals(50.0, marketBasedNetwork.getGenerator("F000011 _generator").getTargetP(), EPSILON);
        assertEquals(10.0, marketBasedNetwork.getGenerator("F000012 _generator").getTargetP(), EPSILON);
        assertEquals(30.0, marketBasedNetwork.getLoad("F000012 _load").getP0(), EPSILON);
        assertEquals(15.0, marketBasedNetwork.getLoad("F000013 _load").getP0(), EPSILON);
        assertEquals(121.3, marketBasedNetwork.getLoad("E000011 _load").getP0(), EPSILON);
        assertEquals(8.7, marketBasedNetwork.getLoad("E000012 _load").getP0(), EPSILON);
        assertEquals(15.0, marketBasedNetwork.getGenerator("E000012 _generator").getTargetP(), EPSILON);
        assertEquals(100.0, marketBasedNetwork.getGenerator("E000013 _generator").getTargetP(), EPSILON);
    }
}
