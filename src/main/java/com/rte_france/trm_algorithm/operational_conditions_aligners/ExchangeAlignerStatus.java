/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public enum ExchangeAlignerStatus {
    ALIGNED_WITH_BALANCE_ADJUSTMENT,
    ALIGNED_WITH_SHIFT,
    ALREADY_ALIGNED,
    NOT_ALIGNED,
    TARGET_NET_POSITION_REACHED_BUT_EXCHANGE_NOT_ALIGNED,
}
