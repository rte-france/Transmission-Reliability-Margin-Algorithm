/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TieLine;
import com.rte_france.trm_algorithm.TrmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class DanglingLineAligner implements OperationalConditionAligner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DanglingLineAligner.class);
    private Map<String, Status> result = new HashMap<>();
    private final Predicate<DanglingLine> danglingLineFilteringPredicate;

    public DanglingLineAligner() {
        danglingLineFilteringPredicate = dl -> true;
    }

    public DanglingLineAligner(String... manyDanglingLineIds) {
        this(Set.of(manyDanglingLineIds));
    }

    public DanglingLineAligner(Set<String> danglingLineIds) {
        danglingLineFilteringPredicate = dl -> danglingLineIds.contains(dl.getId());
    }

    private static Status align(DanglingLine referenceDanglingLine, DanglingLine marketBasedDanglingLine) {
        if (Objects.isNull(marketBasedDanglingLine)) {
            LOGGER.error("Could not find reference dangling line \"{}\" (\"{}\") in market-based network", referenceDanglingLine.getId(), referenceDanglingLine.getNameOrId());
            return Status.NOT_FOUND_IN_MARKET_BASED_NETWORK;
        }
        Optional<TieLine> optionalMarketBasedTieLine = marketBasedDanglingLine.getTieLine();
        if (optionalMarketBasedTieLine.isPresent()) {
            TieLine marketBasedTieLine = optionalMarketBasedTieLine.get();
            if (referenceDanglingLine.getTieLine().isPresent()) {
                LOGGER.info("Reference and market-based dangling lines \"{}\" (\"{}\") are already paired. Market-based dangling line will not be aligned", referenceDanglingLine.getId(), referenceDanglingLine.getNameOrId());
                return Status.PAIRED_DANGLING_LINE_IN_BOTH_NETWORKS;
            } else {
                return unpairTieLine(referenceDanglingLine, marketBasedTieLine);
            }
        } else {
            Optional<TieLine> optionalReferenceTieLine = referenceDanglingLine.getTieLine();
            if (optionalReferenceTieLine.isPresent()) {
                TieLine referenceTieLine = optionalReferenceTieLine.get();
                throw new TrmException(String.format("Reference dangling line \"%s\" (\"%s\") has been paired \"%s\" (\"%s\") but market-based dangling line \"%s\" (\"%s\") has not been paired", referenceDanglingLine.getId(), referenceDanglingLine.getNameOrId(), referenceTieLine.getId(), referenceTieLine.getNameOrId(), marketBasedDanglingLine.getId(), marketBasedDanglingLine.getNameOrId()));
            } else {
                alignP0AndQ0(referenceDanglingLine, marketBasedDanglingLine);
                return Status.ALIGNED;
            }
        }
    }

    private static Status unpairTieLine(DanglingLine referenceDanglingLine, TieLine marketBasedTieLine) {
        marketBasedTieLine.remove();
        if (marketBasedTieLine.getDanglingLine1().getId().equals(referenceDanglingLine.getId())) {
            alignP0AndQ0(referenceDanglingLine, marketBasedTieLine.getDanglingLine1());
            marketBasedTieLine.getDanglingLine2().remove();
        } else {
            marketBasedTieLine.getDanglingLine1().remove();
            alignP0AndQ0(referenceDanglingLine, marketBasedTieLine.getDanglingLine2());
        }
        LOGGER.info("Market-based tie line \"{}\" (\"{}\") has been unpaired", marketBasedTieLine.getId(), marketBasedTieLine.getNameOrId());
        return Status.DANGLING_LINE_MERGED_IN_MARKET_BASED_NETWORK;
    }

    private static void alignP0AndQ0(DanglingLine referenceDanglingLine, DanglingLine marketBasedDanglingLine) {
        marketBasedDanglingLine.setP0(referenceDanglingLine.getP0());
        marketBasedDanglingLine.setQ0(referenceDanglingLine.getQ0());
        LOGGER.trace("Aligned market-based dangling line \"{}\" (\"{}\") at P0={} and Q0={}", marketBasedDanglingLine.getId(), marketBasedDanglingLine.getNameOrId(), marketBasedDanglingLine.getP0(), marketBasedDanglingLine.getQ0());
    }

    public Map<String, Status> getResult() {
        return result;
    }

    @Override
    public void align(Network referenceNetwork, Network marketBasedNetwork) {
        LOGGER.info("Aligning dangling lines");
        result = referenceNetwork.getDanglingLineStream()
                .filter(danglingLineFilteringPredicate)
                .collect(Collectors.toMap(
                    Identifiable::getId,
                    referenceDanglingLine -> align(referenceDanglingLine, marketBasedNetwork.getDanglingLine(referenceDanglingLine.getId()))
        ));
    }

    public enum Status {
        ALIGNED,
        DANGLING_LINE_MERGED_IN_MARKET_BASED_NETWORK,
        NOT_FOUND_IN_MARKET_BASED_NETWORK,
        PAIRED_DANGLING_LINE_IN_BOTH_NETWORKS,
    }
}
