/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.powsybl.iidm.modification.scalable.ProportionalScalable;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.*;
import com.powsybl.openrao.commons.EICode;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.sensitivity.WeightedSensitivityVariable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
public final class TrmUtils {
    private static final double MINIMAL_ABS_POWER_VALUE = 1e-5;

    private TrmUtils() {
        // Utility class
    }

    public static ProportionalScalable getCountryGeneratorsScalable(Network network, Country country) {
        List<Scalable> scalables = new ArrayList<>();
        List<Double> percentages = new ArrayList<>();
        List<Generator> generators = network.getGeneratorStream()
            .filter(generator -> country.equals(generator.getTerminal().getVoltageLevel().getSubstation().map(Substation::getNullableCountry).orElse(null)))
            .filter(TrmUtils::isCorrect)
            .toList();
        double totalAbsoluteCountryP = generators.stream().mapToDouble(TrmUtils::absoluteTargetP).sum();
        generators.forEach(generator -> {
            double generatorPercentage = 100 * absoluteTargetP(generator) / totalAbsoluteCountryP;
            percentages.add(generatorPercentage);
            scalables.add(Scalable.onGenerator(generator.getId()));
        });

        return Scalable.proportional(percentages, scalables);
    }

    private static double absoluteTargetP(Generator generator) {
        return Math.max(MINIMAL_ABS_POWER_VALUE, Math.abs(generator.getTargetP()));
    }

    static boolean isCorrect(Injection<?> injection) {
        return injection != null &&
            injection.getTerminal().isConnected() &&
            injection.getTerminal().getBusView().getBus() != null &&
            injection.getTerminal().getBusView().getBus().isInMainSynchronousComponent();
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
            .filter(TrmUtils::isCorrect)
            .forEach(generator -> {
                Country generatorCountry = getCountry(generator.getTerminal());
                glsks.get(generatorCountry).put(generator.getId(), generator.getTargetP());
            });
        return glsks;
    }

    public static Country getCountry(Terminal terminal) {
        VoltageLevel voltageLevel = terminal.getVoltageLevel();
        Optional<Substation> optionalSubstation = voltageLevel.getSubstation();
        if (optionalSubstation.isEmpty()) {
            throw new TrmException(String.format("Should never get here: substation of voltage level '%s' not found", voltageLevel.getId()));
        }
        Substation substation = optionalSubstation.get();
        Optional<Country> optionalCountry = substation.getCountry();
        if (optionalCountry.isEmpty()) {
            throw new TrmException(String.format("Optional country of substation '%s' is empty", substation.getId()));
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

    public static ZonalData<Scalable> getAutoScalable(Network network) {
        Map<String, Scalable> scalableZonalData = network.getCountries().stream()
            .collect(Collectors.toMap(
                country -> new EICode(country).getAreaCode(),
                country -> getCountryGeneratorsScalable(network, country)
            ));
        return new ZonalDataImpl<>(scalableZonalData);
    }
}
