/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import com.powsybl.iidm.network.TwoSides;

import java.io.IOException;
import java.io.Writer;
import java.time.ZonedDateTime;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class TrmExporter {

    private TrmExporter() {
        // utility class
    }

    public static void export(Writer writer, TrmResults trmResult, ZonedDateTime caseDate) throws IOException {
        for (var entry : trmResult.getUncertaintiesMap().entrySet()) {
            export(writer, caseDate, entry.getKey(), entry.getValue());
        }
    }

    private static void export(Writer writer, ZonedDateTime caseDate, String branchId, UncertaintyResult uncertaintyResult) throws IOException {
        writer.write(String.valueOf(caseDate));
        writer.write(";");
        writer.write(branchId);
        writer.write(";");
        writer.write(uncertaintyResult.getReferenceBranchName());
        writer.write(";");
        writer.write(String.valueOf(uncertaintyResult.getReferenceCountry(TwoSides.ONE)));
        writer.write(";");
        writer.write(String.valueOf(uncertaintyResult.getReferenceCountry(TwoSides.TWO)));
        writer.write(";");
        writer.write(String.valueOf(uncertaintyResult.getUncertainty()));
        writer.write(";");
        writer.write(String.valueOf(uncertaintyResult.getMarketBasedFlow()));
        writer.write(";");
        writer.write(String.valueOf(uncertaintyResult.getReferenceFlow()));
        writer.write(";");
        writer.write(String.valueOf(uncertaintyResult.getReferenceZonalPtdf()));
        writer.write(System.lineSeparator());
    }

    public static void exportHeader(Writer writer) throws IOException {
        writer.write("Case date;");
        writer.write("Branch ID;");
        writer.write("Branch name;");
        writer.write("Country Side 1;");
        writer.write("Country Side 2;");
        writer.write("Uncertainty;");
        writer.write("Market-based flow;");
        writer.write("Reference flow;");
        writer.write("Zonal PTDF");
        writer.write(System.lineSeparator());
    }
}
