/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import com.powsybl.balances_adjustment.balance_computation.BalanceComputationParameters;
import com.powsybl.computation.ComputationManager;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.openrao.data.cracapi.Crac;
import com.rte_france.trm_algorithm.operational_conditions_aligners.CracAligner;
import com.rte_france.trm_algorithm.operational_conditions_aligners.ExchangeAligner;
import com.rte_france.trm_algorithm.operational_conditions_aligners.HvdcAligner;
import com.rte_france.trm_algorithm.operational_conditions_aligners.PstAligner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public class OperationalConditionAligner {
    private static final Logger LOGGER = LoggerFactory.getLogger(OperationalConditionAligner.class);

    private final ExchangeAligner exchangeAligner;

    OperationalConditionAligner(BalanceComputationParameters balanceComputationParameters, LoadFlow.Runner loadFlowRunner, ComputationManager computationManager) {
        this.exchangeAligner = new ExchangeAligner(balanceComputationParameters, loadFlowRunner, computationManager);
    }

    public void align(Network referenceNetwork, Network marketBasedNetwork, Crac crac, ZonalData<Scalable> marketZonalScalable, TrmResults.Builder builder) {
        LOGGER.info("Aligning operational conditions of market-based network on reference network");
        builder.addCracAlignmentResults(CracAligner.align(referenceNetwork, marketBasedNetwork, crac));
        HvdcAligner.align(referenceNetwork, marketBasedNetwork);
        builder.addPstAlignmentResults(PstAligner.align(referenceNetwork, marketBasedNetwork));
        builder.addExchangeAlignerResult(exchangeAligner.align(referenceNetwork, marketBasedNetwork, marketZonalScalable));
    }
}
