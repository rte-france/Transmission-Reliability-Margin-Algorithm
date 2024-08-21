/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.powsybl.iidm.network.HvdcLine;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControl;
import com.powsybl.iidm.network.extensions.HvdcAngleDroopActivePowerControlAdder;
import com.rte_france.trm_algorithm.TrmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class HvdcAligner implements OperationalConditionAligner {
    private static final Logger LOGGER = LoggerFactory.getLogger(HvdcAligner.class);

    private static void alignAngleDroopActivePowerExtension(HvdcLine referenceHvdcLine, HvdcLine marketBasedHvdcLine) {
        HvdcAngleDroopActivePowerControl referenceExtension = referenceHvdcLine.getExtension(HvdcAngleDroopActivePowerControl.class);
        if (Objects.nonNull(marketBasedHvdcLine.getExtension(HvdcAngleDroopActivePowerControl.class))) {
            marketBasedHvdcLine.removeExtension(HvdcAngleDroopActivePowerControl.class);
            LOGGER.debug("Removed angle droop active power extension from market-based HVDC \"{}\"", marketBasedHvdcLine.getId());
        }
        if (Objects.nonNull(referenceExtension)) {
            marketBasedHvdcLine.newExtension(HvdcAngleDroopActivePowerControlAdder.class)
                .withDroop(referenceExtension.getDroop())
                .withP0(referenceExtension.getP0())
                .withEnabled(referenceExtension.isEnabled())
                .add();
            LOGGER.debug("Copied angle droop active power extension from reference HVDC to market-based HVDC \"{}\"", marketBasedHvdcLine.getId());
        }
    }

    private static void alignActivePowerSetpoints(HvdcLine referenceHvdcLine, HvdcLine marketBasedHvdcLine) {
        marketBasedHvdcLine.setActivePowerSetpoint(referenceHvdcLine.getActivePowerSetpoint());
        LOGGER.debug("Aligned market-based HVDC \"{}\" power set point at {} MW", marketBasedHvdcLine.getId(), marketBasedHvdcLine.getActivePowerSetpoint());
    }

    @Override
    public void align(Network referenceNetwork, Network marketBasedNetwork) {
        LOGGER.info("Aligning HVDC power set points and angle droop active power mode");
        referenceNetwork.getHvdcLineStream().forEach(referenceHvdcLine -> {
            String id = referenceHvdcLine.getId();
            HvdcLine marketBasedHvdcLine = marketBasedNetwork.getHvdcLine(id);
            if (Objects.isNull(marketBasedHvdcLine)) {
                throw new TrmException("HvdcLine with id " + id + " not found");
            }
            alignActivePowerSetpoints(referenceHvdcLine, marketBasedHvdcLine);
            alignAngleDroopActivePowerExtension(referenceHvdcLine, marketBasedHvdcLine);
        });
    }
}
