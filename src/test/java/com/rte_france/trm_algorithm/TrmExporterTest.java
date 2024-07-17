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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class TrmExporterTest {
    @Test
    void testEmptyExport() throws IOException {
        Writer writer = new StringWriter();
        TrmResults trmResult = TestUtils.mockTrmResults().build();

        TrmExporter.exportHeader(writer);
        TrmExporter.export(writer, trmResult, ZonedDateTime.now());

        assertEquals("Case date;Branch ID;Branch name;Country Side 1;Country Side 2;Uncertainty;Market-based flow;Reference flow;Zonal PTDF\n", writer.toString());
    }

    @Test
    void testOneRow() throws IOException {
        Network network = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");
        network.setCaseDate(ZonedDateTime.of(2024, 7, 15, 13, 14, 12, 0, ZoneId.of("UTC")));
        Writer writer = new StringWriter();
        TrmResults trmResult = TestUtils.mockTrmResults()
            .addUncertainties(Map.of("toto", new UncertaintyResult(network.getBranch("FGEN1 11 BLOAD 11 1"), 100., 112., -1.)))
            .build();

        TrmExporter.exportHeader(writer);
        TrmExporter.export(writer, trmResult, network.getCaseDate());

        assertEquals("""
                Case date;Branch ID;Branch name;Country Side 1;Country Side 2;Uncertainty;Market-based flow;Reference flow;Zonal PTDF
                2024-07-15T13:14:12Z[UTC];toto;FGEN1 11 BLOAD 11 1;FR;BE;12.0;100.0;112.0;-1.0
                """,
            writer.toString());
    }

    @Test
    void testTwoResults() throws IOException {
        Network network = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");

        Writer writer = new StringWriter();
        TrmExporter.exportHeader(writer);

        network.setCaseDate(ZonedDateTime.of(2024, 7, 15, 13, 14, 12, 0, ZoneId.of("UTC")));
        TrmResults trmResult1 = TestUtils.mockTrmResults()
            .addUncertainties(Map.of("toto1", new UncertaintyResult(network.getBranch("FGEN1 11 BLOAD 11 1"), 100., 112., -1.)))
            .build();
        TrmExporter.export(writer, trmResult1, network.getCaseDate());

        network.setCaseDate(ZonedDateTime.of(2024, 9, 25, 17, 4, 42, 0, ZoneId.of("UTC")));
        TrmResults trmResult2 = TestUtils.mockTrmResults()
            .addUncertainties(Map.of("toto2", new UncertaintyResult(network.getBranch("FGEN1 11 BLOAD 11 1"), 100., 111., -.5)))
            .build();
        TrmExporter.export(writer, trmResult2, network.getCaseDate());

        assertEquals("""
                Case date;Branch ID;Branch name;Country Side 1;Country Side 2;Uncertainty;Market-based flow;Reference flow;Zonal PTDF
                2024-07-15T13:14:12Z[UTC];toto1;FGEN1 11 BLOAD 11 1;FR;BE;12.0;100.0;112.0;-1.0
                2024-09-25T17:04:42Z[UTC];toto2;FGEN1 11 BLOAD 11 1;FR;BE;22.0;100.0;111.0;-0.5
                """,
            writer.toString());
    }

    @Test
    void testOneRowWithoutHeader() throws IOException {
        Network network = TestUtils.importNetwork("simple_networks/NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct");
        network.setCaseDate(ZonedDateTime.of(2024, 7, 15, 13, 14, 12, 0, ZoneId.of("UTC")));
        Writer writer = new StringWriter();
        TrmResults trmResult = TestUtils.mockTrmResults()
            .addUncertainties(Map.of("toto", new UncertaintyResult(network.getBranch("FGEN1 11 BLOAD 11 1"), 100., 112., -1.)))
            .build();

        TrmExporter.export(writer, trmResult, network.getCaseDate());

        assertEquals("2024-07-15T13:14:12Z[UTC];toto;FGEN1 11 BLOAD 11 1;FR;BE;12.0;100.0;112.0;-1.0\n", writer.toString());
    }
}
