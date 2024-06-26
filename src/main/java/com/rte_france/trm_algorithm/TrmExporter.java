/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class TrmExporter {

    private TrmExporter() {
        // utility class
    }

    public static void export(OutputStream outputStream, TrmResults trmResult) throws IOException {
        outputStream.write("Branch ID;Uncertainty;Market-based flow;Reference flow;Zonal PTDF".getBytes());

        trmResult.getUncertaintiesMap().forEach((key, value) -> {
            try {
                outputStream.write(String.format("%n%s;%s;%s;%s;%s", key, value.getUncertainty(), value.getMarketBasedFlow(), value.getReferenceFlow(), value.getReferenceZonalPtdf()).getBytes());
            } catch (IOException e) {
                throw new TrmException(e);
            }
        });
    }
}
