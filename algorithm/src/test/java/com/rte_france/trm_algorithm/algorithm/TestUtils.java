/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.algorithm;

import com.powsybl.commons.PowsyblException;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.powsybl.iidm.modification.scalable.ProportionalScalable;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.*;
import com.powsybl.openrao.commons.EICode;
import com.powsybl.openrao.data.cracapi.Crac;
import com.powsybl.openrao.data.cracapi.CracFactory;
import com.powsybl.openrao.data.cracapi.networkaction.ActionType;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.sensitivity.WeightedSensitivityVariable;

import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class contains helper functions for tests.
 *
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class TestUtils {
    private static final double MINIMAL_ABS_POWER_VALUE = 1e-5;

    private TestUtils() {
        // utility class
    }

    public static Network importNetwork(String networkResourcePath) {
        String networkName = Paths.get(networkResourcePath).getFileName().toString();
        return Network.read(networkName, TestUtils.class.getResourceAsStream(networkResourcePath));
    }

    public static ZonalData<SensitivityVariableSet> getAutoGlsk(Network network) {
        Map<Country, Map<String, Double>> glsks = getGlskMap(network);
        return convertToZonalGlsk(glsks);
    }

    private static Map<Country, Map<String, Double>> getGlskMap(Network network) {
        return normalizeGlsks(initializeGlskMapWithGeneratorPowers(network));
    }

    private static Map<Country, Map<String, Double>> initializeGlskMapWithGeneratorPowers(Network network) {
        Map<Country, Map<String, Double>> glsks =
            network.getCountries()
                .stream()
                .collect(Collectors.toMap(Function.identity(), country -> new HashMap<>()));

        network.getGeneratorStream()
            .filter(TestUtils::isCorrect)
            .forEach(generator -> {
                Country generatorCountry = getTerminalCountry(generator.getTerminal());
                glsks.get(generatorCountry).put(generator.getId(), generator.getTargetP());
            });
        return glsks;
    }

    private static Country getTerminalCountry(Terminal terminal) {
        Optional<Substation> optionalSubstation = terminal.getVoltageLevel().getSubstation();
        if (optionalSubstation.isEmpty()) {
            throw new PowsyblException(String.format("Voltage level %s does not belong to any substation. " +
                "Cannot retrieve country info needed for the algorithm.", terminal.getVoltageLevel().getId()));
        }
        Substation substation = optionalSubstation.get();
        Optional<Country> optionalCountry = substation.getCountry();
        if (optionalCountry.isEmpty()) {
            throw new PowsyblException(String.format("Substation %s does not have country property " +
                "needed for the algorithm.", substation.getId()));
        }
        return optionalCountry.get();
    }

    private static Map<Country, Map<String, Double>> normalizeGlsks(Map<Country, Map<String, Double>> glsks) {
        glsks.forEach((country, glsk) -> {
            double glskSum = glsk.values().stream().mapToDouble(factor -> factor).sum();
            if (glskSum == 0.0) {
                glsk.forEach((key, value) -> glsk.put(key, 1.0 / glsk.size()));
            } else {
                glsk.forEach((key, value) -> glsk.put(key, value / glskSum));
            }
        });
        return glsks;
    }

    private static ZonalData<SensitivityVariableSet> convertToZonalGlsk(Map<Country, Map<String, Double>> glsks) {
        return new ZonalDataImpl<>(glsks.keySet().stream()
            .collect(Collectors.toMap(
                Enum::name,
                country -> new SensitivityVariableSet(
                    country.name(),
                    glsks.get(country).entrySet().stream().map(entry -> new WeightedSensitivityVariable(entry.getKey(), entry.getValue())).toList()
                )
            ))
        );
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

    public static ZonalData<Scalable> getAutoScalable(Network network) {
        Map<String, Scalable> scalableZonalData = network.getCountries().stream()
            .collect(Collectors.toMap(
                country -> new EICode(country).getAreaCode(),
                country -> getCountryGeneratorsScalable(network, country)
            ));
        return new ZonalDataImpl<>(scalableZonalData);
    }

    public static ProportionalScalable getCountryGeneratorsScalable(Network network, Country country) {
        List<Scalable> scalables = new ArrayList<>();
        List<Double> percentages = new ArrayList<>();
        List<Generator> generators = network.getGeneratorStream()
            .filter(generator -> country.equals(generator.getTerminal().getVoltageLevel().getSubstation().map(Substation::getNullableCountry).orElse(null)))
            .filter(TestUtils::isCorrect)
            .toList();
        double totalAbsoluteCountryP = generators.stream().mapToDouble(TestUtils::absoluteTargetP).sum();
        generators.forEach(generator -> {
            double generatorPercentage = 100 * TestUtils.absoluteTargetP(generator) / totalAbsoluteCountryP;
            percentages.add(generatorPercentage);
            scalables.add(Scalable.onGenerator(generator.getId()));
        });

        return Scalable.proportional(percentages, scalables);
    }

    private static double absoluteTargetP(Generator generator) {
        return Math.max(MINIMAL_ABS_POWER_VALUE, Math.abs(generator.getTargetP()));
    }

    private static boolean isCorrect(Injection<?> injection) {
        return injection != null &&
            injection.getTerminal().isConnected() &&
            injection.getTerminal().getBusView().getBus() != null &&
            injection.getTerminal().getBusView().getBus().isInMainSynchronousComponent();
    }
}
