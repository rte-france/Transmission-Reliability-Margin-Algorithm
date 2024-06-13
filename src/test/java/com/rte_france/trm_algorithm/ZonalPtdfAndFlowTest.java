/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class ZonalPtdfAndFlowTest {
    @Test
    void test() {
        ZonalPtdfAndFlow zonalPtdfAndFlow = new ZonalPtdfAndFlow(0.3, 100);
        assertEquals(0.3, zonalPtdfAndFlow.getZonalPtdf());
        assertEquals(100, zonalPtdfAndFlow.getFlow());
    }
}
