/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.algorithm.operational_conditions_aligners;

import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControlAdder;
import com.rte_france.trm_algorithm.algorithm.TrmException;

import java.util.Objects;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class HvdcAligner {

    private HvdcAligner() { } // utility class

    public static void align(Network referenceNetwork, Network marketBasedNetwork) {
        referenceNetwork.getHvdcLineStream().forEach(referenceHvdcLine -> {
            String id = referenceHvdcLine.getId();
            HvdcLine hvdcLine = marketBasedNetwork.getHvdcLine(id);
            if (Objects.isNull(hvdcLine)) {
                throw new TrmException("HvdcLine with id " + id + " not found");
            }
            alignActivePowerSetpoints(referenceHvdcLine, hvdcLine);
            alignAngleDroopActivePowerExtension(referenceHvdcLine, hvdcLine);
        });
    }

    private static void alignAngleDroopActivePowerExtension(HvdcLine referenceHvdcLine, HvdcLine hvdcLine) {
        HvdcAngleDroopActivePowerControl extension = referenceHvdcLine.getExtension(HvdcAngleDroopActivePowerControl.class);
        if (Objects.isNull(extension)) {
            hvdcLine.removeExtension(HvdcAngleDroopActivePowerControl.class);
        } else {
            hvdcLine.newExtension(HvdcAngleDroopActivePowerControlAdder.class)
                .withDroop(extension.getDroop())
                .withP0(extension.getP0())
                .withEnabled(extension.isEnabled())
                .add();
        }
    }

    private static void alignActivePowerSetpoints(HvdcLine referenceHvdcLine, HvdcLine hvdcLine) {
        hvdcLine.setActivePowerSetpoint(referenceHvdcLine.getActivePowerSetpoint());
    }
}
