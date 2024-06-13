/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import com.powsybl.iidm.network.*;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class CneSelector {
    private CneSelector() {
        // utility class
    }

    static Set<Line> getNetworkElements(Network network) {
        return network.getLineStream()
            .filter(CneSelector::isAnInterconnection)
            .collect(Collectors.toSet());
    }

    private static boolean isAnInterconnection(Line line) {
        return getCountry(line.getTerminal1()) !=
            getCountry(line.getTerminal2());
    }

    private static Country getCountry(Terminal terminal) {
        Optional<Substation> substation = terminal.getVoltageLevel().getSubstation();
        if (substation.isPresent()) {
            Optional<Country> country = substation.get().getCountry();
            if (country.isPresent()) {
                return country.get();
            }
            throw new TrmException("Should never get here: country is empty");
        }
        throw new TrmException("Should never get here: substation is empty");
    }
}
