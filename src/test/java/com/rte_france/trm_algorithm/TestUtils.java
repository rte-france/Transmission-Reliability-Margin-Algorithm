/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import com.powsybl.balances_adjustment.balance_computation.BalanceComputationResult;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracFactory;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.rte_france.trm_algorithm.operational_conditions_aligners.ExchangeAligner;
import com.rte_france.trm_algorithm.operational_conditions_aligners.ExchangeAlignerResult;
import com.rte_france.trm_algorithm.operational_conditions_aligners.PstAligner;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This class contains helper functions for tests.
 *
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class TestUtils {
    public static final double EPSILON = 1e-1;

    private TestUtils() {
        // utility class
    }

    public static void assertNetPositions(Map<Country, Double> expectedNetPositions, Map<Country, Double> actualNetPositions) {
        expectedNetPositions.keySet()
            .forEach(c -> assertEquals(expectedNetPositions.get(c), actualNetPositions.get(c), EPSILON));
    }

    public static void assertExchanges(Map<Country, Map<Country, Double>> expectedExchanges, Map<Country, Map<Country, Double>> actualExchanges) {
        expectedExchanges.keySet()
            .forEach(country1 -> expectedExchanges.get(country1).keySet()
                .forEach(country2 -> {
                    Double expected = expectedExchanges.get(country1).get(country2);
                    Double actual = actualExchanges.get(country1).get(country2);
                    assertEquals(expected, actual, EPSILON);
                })
            );
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

    static PstAligner.Result mockPstAlignerResult() {
        return PstAligner.Result.builder()
            .addPhaseTapChangerResults(Collections.emptyMap())
            .addRatioTapChangerResults(Collections.emptyMap())
            .build();
    }

    static ExchangeAlignerResult mockExchangeAlignerResult() {
        return ExchangeAlignerResult.builder()
            .addReferenceNetPositions(Collections.emptyMap())
            .addInitialMarketBasedNetPositions(Collections.emptyMap())
            .addReferenceExchange(Collections.emptyMap())
            .addInitialMarketBasedExchanges(Collections.emptyMap())
            .addInitialMaxAbsoluteExchangeDifference(0)
            .addTargetNetPosition(Collections.emptyMap())
            .addBalanceComputationResult(new BalanceComputationResult(BalanceComputationResult.Status.FAILED))
            .addNewMarketBasedNetPositions(Collections.emptyMap())
            .addNewMarketBasedExchanges(Collections.emptyMap())
            .addNewMaxAbsoluteExchangeDifference(0)
            .addExchangeAlignerStatus(ExchangeAligner.Status.NOT_ALIGNED)
            .build();
    }

    static TrmResults.Builder mockTrmResults() {
        return TrmResults.builder()
            .addUncertainties(Collections.emptyMap())
            .addCracAlignmentResults(Collections.emptyMap())
            .addPstAlignmentResults(mockPstAlignerResult())
            .addDanglingLineAlignerResults(Collections.emptyMap())
            .addExchangeAlignerResult(mockExchangeAlignerResult());
    }
}
