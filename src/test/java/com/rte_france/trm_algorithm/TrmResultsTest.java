/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import com.rte_france.trm_algorithm.operational_conditions_aligners.ExchangeAligner;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class TrmResultsTest {
    @Test
    void testBuildEmptyResults() {
        ExchangeAligner.Result exchangeAlignerResult = ExchangeAligner.Result.getBuilder()
            .addExchangeAlignerStatus(ExchangeAligner.Status.FAILED)
            .addInitialMarketBasedNetPositions(Collections.emptyMap())
            .addReferenceNetPositions(Collections.emptyMap())
            .build();
        TrmResults trmResults = TrmResults.getBuilder()
            .addUncertainties(Collections.emptyMap())
            .addCracAlignmentResults(Collections.emptyMap())
            .addPstAlignmentResults(Collections.emptyMap())
            .addExchangeAlignerResult(exchangeAlignerResult)
            .build();
        assertTrue(trmResults.getUncertaintiesMap().isEmpty());
        assertTrue(trmResults.getCracAlignmentResults().isEmpty());
        assertTrue(trmResults.getPstAlignmentResults().isEmpty());
        assertEquals(ExchangeAligner.Status.FAILED, trmResults.getExchangeAlignerResult().getStatus());
        assertTrue(trmResults.getExchangeAlignerResult().getInitialMarketBasedNetPositions().isEmpty());
        assertTrue(trmResults.getExchangeAlignerResult().getReferenceNetPositions().isEmpty());
    }
}
