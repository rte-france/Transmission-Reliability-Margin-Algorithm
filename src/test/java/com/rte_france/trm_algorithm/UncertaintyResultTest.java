/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

class UncertaintyResultTest {
    @Test
    void computeUncertainty() {
        Network network = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");
        UncertaintyResult uncertaintyResult = new UncertaintyResult(network.getBranch("FGEN1 11 BLOAD 11 1"), 100., 112., -1.);
        assertEquals(12., uncertaintyResult.getUncertainty());
        assertEquals(100., uncertaintyResult.getMarketBasedFlow());
        assertEquals(112., uncertaintyResult.getReferenceFlow());
        assertEquals(-1., uncertaintyResult.getReferenceZonalPtdf());
        assertEquals("FGEN1 11 BLOAD 11 1", uncertaintyResult.getReferenceBranchName());
        assertEquals(Country.FR, uncertaintyResult.getReferenceCountry(TwoSides.ONE));
        assertEquals(Country.BE, uncertaintyResult.getReferenceCountry(TwoSides.TWO));
    }

    @Test
    void computeUncertaintyWithNullPtdf() {
        Network network = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");
        UncertaintyResult uncertaintyResult = new UncertaintyResult(network.getBranch("FGEN1 11 BLOAD 11 1"), 100., 112., 0.);
        assertEquals(Double.NEGATIVE_INFINITY, uncertaintyResult.getUncertainty());
    }
}
