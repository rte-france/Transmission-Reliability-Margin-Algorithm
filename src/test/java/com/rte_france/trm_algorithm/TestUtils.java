/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import com.powsybl.iidm.network.*;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracFactory;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;

import java.nio.file.Paths;

/**
 * This class contains helper functions for tests.
 *
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class TestUtils {

    private TestUtils() {
        // utility class
    }

    public static Network importNetwork(String networkResourcePath) {
        String networkName = Paths.get(networkResourcePath).getFileName().toString();
        return Network.read(networkName, TestUtils.class.getResourceAsStream(networkResourcePath));
    }

    public static Crac getIdealTopologicalAlignerCrac(Network network) {
        Crac crac = CracFactory.findDefault().create("auto-crac");
        network.getBranches().forEach(branch -> addNetworkActions(crac, branch));
        return crac;
    }

    private static void addNetworkActions(Crac crac, Branch<?> branch) {
        addNetworkAction(crac, branch, ActionType.OPEN);
        addNetworkAction(crac, branch, ActionType.CLOSE);
    }

    private static void addNetworkAction(Crac crac, Branch<?> branch, ActionType actionType) {
        crac.newNetworkAction().withId(String.format("Topological action with branch:\"%s\", actionType:%s", branch.getId(), actionType))
            .newTopologicalAction().withNetworkElement(branch.getId()).withActionType(actionType).add()
            .add();
    }
}
