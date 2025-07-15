/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.farao_community.farao.dichotomy.shift.ShiftDispatcher;
import com.powsybl.glsk.commons.CountryEICode;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.powsybl.iidm.network.Country.*;

/**
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

/* THIS FILE SHOULD BE IMPORTED FROM gridcapa-cse */

public class CseD2ccShiftDispatcherTmp implements ShiftDispatcher {
    private static final Set<String> BORDER_COUNTRIES = Set.of(new CountryEICode(FR).getCode(), new CountryEICode(CH).getCode(), new CountryEICode(AT).getCode(), new CountryEICode(SI).getCode());

    private final Logger businessLogger;
    private final Map<String, Double> reducedSplittingFactors;
    private final Map<String, Double> ntcs;

    public CseD2ccShiftDispatcherTmp(Logger businessLogger, Map<String, Double> reducedSplittingFactors, Map<String, Double> ntcs) {
        this.businessLogger = businessLogger;
        this.reducedSplittingFactors = reducedSplittingFactors;
        this.ntcs = ntcs;
    }

    @Override
    public Map<String, Double> dispatch(double value) {
        Map<String, Double> shifts = new HashMap<>();
        BORDER_COUNTRIES.forEach(borderCountry ->
                shifts.put(borderCountry, reducedSplittingFactors.get(borderCountry) * (value - ntcs.values().stream().mapToDouble(Double::doubleValue).sum())));

        shifts.put(new CountryEICode(IT).getCode(),
                -shifts.values().stream().mapToDouble(Double::doubleValue).sum());
        logShifts(value, shifts);
        return shifts;
    }

    private void logShifts(double value, Map<String, Double> shifts) {
        for (Map.Entry<String, Double> entry : shifts.entrySet()) {
            businessLogger.info("Summary : Shift target value: {}, for area {} : {}.", value, entry.getKey(), entry.getValue());
        }
    }

}
