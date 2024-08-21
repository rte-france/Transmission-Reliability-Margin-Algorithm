/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners.exchange_and_net_position;

import com.powsybl.iidm.network.Country;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public interface ExchangeAndNetPositionInterface extends NetPositionInterface {
    double getExchange(Country countrySource, Country countrySink);

    double getMaxAbsoluteExchangeDifference(ExchangeAndNetPositionInterface otherExchangeAndNetPosition);

    double getExchangeToExterior(Country countrySource);
}