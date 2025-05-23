/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

/**
 * @author Sebastian Huaraca {@literal <sebastian.huaracalapa at rte-france.com>}
 */

public record MappingResults(String lineFromMarketBasedNetwork, String lineFromReferenceNetwork, boolean mappingFound) {
    public static MappingResults notFound(String branchId) {
        return new MappingResults(branchId, null, false);
    }

    public static MappingResults mappingFound(String branchId, String mappedId) {
        return new MappingResults(branchId, mappedId, true);
    }
}
