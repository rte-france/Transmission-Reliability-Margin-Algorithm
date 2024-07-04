/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class TrmExporterTest {
    @Test
    void testEmptyExport() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TrmResults trmResult = TestUtils.mockTrmResults().build();

        TrmExporter.export(outputStream, trmResult);

        assertEquals("Branch ID;Uncertainty;Market-based flow;Reference flow;Zonal PTDF", outputStream.toString());
    }

    @Test
    void testOneRow() throws IOException {
        Network network = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TrmResults trmResult = TestUtils.mockTrmResults()
            .addUncertainties(Map.of("toto", new UncertaintyResult(network.getBranch("FGEN1 11 BLOAD 11 1"), 100., 112., -1.)))
            .build();

        TrmExporter.export(outputStream, trmResult);

        assertEquals("""
                Branch ID;Uncertainty;Market-based flow;Reference flow;Zonal PTDF
                toto;12.0;100.0;112.0;-1.0""",
            outputStream.toString());
    }
}
