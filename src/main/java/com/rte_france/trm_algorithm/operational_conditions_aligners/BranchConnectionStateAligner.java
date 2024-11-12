/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author Sébastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class BranchConnectionStateAligner implements OperationalConditionAligner {
    private static final Logger LOGGER = LoggerFactory.getLogger(BranchConnectionStateAligner.class);

    public void align(Network referenceNetwork, Network marketBasedNetwork) {
        marketBasedNetwork.getBranches().forEach(branch -> {
            String branchId = branch.getId();
            Branch<?> referenceBranch = referenceNetwork.getBranch(branchId);

            if (Objects.isNull(referenceBranch)) {
                LOGGER.debug("Branch with id '{}' not found.", branchId);
            } else {
                alignBranch(branch, referenceBranch);
            }
        });
    }

    private static void alignBranch(Branch<?> branch, Branch<?> referenceBranch) {

        Terminal branchTerminal1 = branch.getTerminal1();
        Terminal branchTerminal2 = branch.getTerminal2();
        Terminal referenceBranchTerminal1 = referenceBranch.getTerminal1();
        Terminal referenceBranchTerminal2 = referenceBranch.getTerminal2();

        if (branchTerminal1.getVoltageLevel().getId().equals(referenceBranchTerminal1.getVoltageLevel().getId())) {
            if (branchTerminal2.getVoltageLevel().getId().equals(referenceBranchTerminal2.getVoltageLevel().getId())) {
                alignTerminal(branchTerminal1, referenceBranchTerminal1);
                alignTerminal(branchTerminal2, referenceBranchTerminal2);
            } else {
                branchNotFoundLogger(branchTerminal2, branch.getId());
            }
        } else if (branchTerminal1.getVoltageLevel().getId().equals(referenceBranchTerminal2.getVoltageLevel().getId())) {
            if (branchTerminal2.getVoltageLevel().getId().equals(referenceBranchTerminal1.getVoltageLevel().getId())) {
                alignTerminal(branchTerminal1, referenceBranchTerminal2);
                alignTerminal(branchTerminal2, referenceBranchTerminal1);
            } else {
                branchNotFoundLogger(branchTerminal2, branch.getId());
            }
        } else {
            branchNotFoundLogger(branchTerminal1, branch.getId());
        }
    }

    private static void branchNotFoundLogger(Terminal terminal, String branch) {
        LOGGER.debug("Terminal with voltageLevelId '{}' from branch with id '{}' not found.", terminal.getVoltageLevel().getId(), branch);
    }

    private static void alignTerminal(Terminal terminal, Terminal referenceTerminal) {
        if (terminal.isConnected() != referenceTerminal.isConnected()) {
            if (terminal.isConnected()) {
                terminal.disconnect();
            } else {
                terminal.connect();
            }
        }
    }
}
