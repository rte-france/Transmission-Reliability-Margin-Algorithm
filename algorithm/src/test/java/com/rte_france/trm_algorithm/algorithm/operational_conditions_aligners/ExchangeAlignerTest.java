/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.algorithm.operational_conditions_aligners;

import com.powsybl.balances_adjustment.balance_computation.*;
import com.powsybl.balances_adjustment.util.CountryAreaFactory;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.glsk.cim.CimGlskDocument;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.openrao.commons.EICode;
import com.rte_france.trm_algorithm.algorithm.TestUtils;
import com.rte_france.trm_algorithm.algorithm.TrmException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.rte_france.trm_algorithm.algorithm.TestUtils.getCountryGeneratorsScalable;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */
class ExchangeAlignerTest {
    public static final double EPSILON = 1e-1;

    @Test
    void testDoNotAlignWithoutScalableData() {
        Network referenceNetwork = TestUtils.importNetwork("operational_conditions_aligners/shift/TestCase_with_transformers.xiidm");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/shift/TestCase_with_transformers.xiidm");
        Load load1 = referenceNetwork.getLoad("ES_L2_1 _load");
        load1.setP0(load1.getP0() + 1000);
        Load load2 = referenceNetwork.getLoad("PT_L1_1 _load");
        load2.setP0(load2.getP0() - 1000);
        ZonalData<Scalable> marketZonalScalable = new ZonalDataImpl<>(Collections.emptyMap());
        ExchangeAligner exchangeAligner = new ExchangeAligner(new BalanceComputationParameters(), LoadFlow.find(), LocalComputationManager.getDefault());
        TrmException trmException = assertThrows(TrmException.class, () -> exchangeAligner.align(referenceNetwork, marketBasedNetwork, marketZonalScalable));
        assertEquals("Scalable not found: 10YFR-RTE------C", trmException.getMessage());
    }

    @Test
    void testAlignmentFail() {
        Network referenceNetwork = TestUtils.importNetwork("operational_conditions_aligners/shift/TestCase_with_transformers.xiidm");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/shift/TestCase_with_transformers.xiidm");
        Load load1 = referenceNetwork.getLoad("ES_L2_1 _load");
        load1.setP0(load1.getP0() + 1000);
        Load load2 = referenceNetwork.getLoad("PT_L1_1 _load");
        load2.setP0(load2.getP0() - 1000);

        Map<String, Scalable> scalableZonalData = referenceNetwork.getCountries().stream()
                .collect(Collectors.toMap(
                        country -> new EICode(country).getAreaCode(),
                        country -> Scalable.onGenerator("toto")
                ));

        ZonalData<Scalable> marketZonalScalable = new ZonalDataImpl<>(scalableZonalData);
        ExchangeAligner exchangeAligner = new ExchangeAligner(new BalanceComputationParameters(), LoadFlow.find(), LocalComputationManager.getDefault());
        TrmException trmException = assertThrows(TrmException.class, () -> exchangeAligner.align(referenceNetwork, marketBasedNetwork, marketZonalScalable));
        assertEquals("Balance computation failed: FAILED", trmException.getMessage());
    }

    @Test
    void testDoNotAlignWithSameNetPositions() {
        Network referenceNetwork = TestUtils.importNetwork("operational_conditions_aligners/shift/TestCase_with_transformers.xiidm");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/shift/TestCase_with_transformers.xiidm");
        ZonalData<Scalable> marketZonalScalable = new ZonalDataImpl<>(Collections.emptyMap());
        ExchangeAligner exchangeAligner = new ExchangeAligner(new BalanceComputationParameters(), LoadFlow.find(), LocalComputationManager.getDefault());
        assertEquals(ExchangeAligner.Status.ALREADY_ALIGNED, exchangeAligner.align(referenceNetwork, marketBasedNetwork, marketZonalScalable));
    }

    @Test
    void testWithSwe() {
        Network referenceNetwork = TestUtils.importNetwork("operational_conditions_aligners/shift/TestCase_with_transformers.xiidm");
        LoadFlow.run(referenceNetwork);
        CountryAreaFactory countryAreaES = new CountryAreaFactory(Country.ES);
        CountryAreaFactory countryAreaFR = new CountryAreaFactory(Country.FR);
        CountryAreaFactory countryAreaPT = new CountryAreaFactory(Country.PT);

        assertEquals(-0.4, countryAreaES.create(referenceNetwork).getNetPosition(), EPSILON);
        assertEquals(0.2, countryAreaFR.create(referenceNetwork).getNetPosition(), EPSILON);
        assertEquals(0.2, countryAreaPT.create(referenceNetwork).getNetPosition(), EPSILON);

        CimGlskDocument doc = CimGlskDocument.importGlsk(getClass().getResourceAsStream("shift/TestCase_with_transformers_glsk.xml"));
        Instant instant = LocalDateTime.of(2023, 7, 31, 7, 30).toInstant(ZoneOffset.UTC);
        ZonalData<Scalable> referenceZonalScalable = doc.getZonalScalable(referenceNetwork, instant);
        List<BalanceComputationArea> areas = new ArrayList<>();
        Scalable scalableES = referenceZonalScalable.getData(new EICode(Country.ES).getAreaCode());
        Scalable scalableFR = TestUtils.getCountryGeneratorsScalable(referenceNetwork, Country.FR);
        Scalable scalablePT = referenceZonalScalable.getData(new EICode(Country.PT).getAreaCode());
        areas.add(new BalanceComputationArea("ES", countryAreaES, scalableES, -1200.));
        areas.add(new BalanceComputationArea("FR", countryAreaFR, scalableFR, 500.));
        areas.add(new BalanceComputationArea("PT", countryAreaPT, scalablePT, 700.));
        BalanceComputationFactory balanceComputationFactory = new BalanceComputationFactoryImpl();
        LoadFlow.Runner loadFlowRunner = LoadFlow.find();
        ComputationManager computationManager = LocalComputationManager.getDefault();
        BalanceComputation balanceComputation = balanceComputationFactory.create(areas, loadFlowRunner, computationManager);
        BalanceComputationParameters parameters = new BalanceComputationParameters().setThresholdNetPosition(1e-2);
        BalanceComputationResult result = balanceComputation.run(referenceNetwork, referenceNetwork.getVariantManager().getWorkingVariantId(), parameters).join();

        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getStatus());
        assertEquals(-1200, countryAreaES.create(referenceNetwork).getNetPosition(), EPSILON);
        assertEquals(500, countryAreaFR.create(referenceNetwork).getNetPosition(), EPSILON);
        assertEquals(700, countryAreaPT.create(referenceNetwork).getNetPosition(), EPSILON);

        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/shift/TestCase_with_transformers.xiidm");
        ZonalData<Scalable> marketZonalScalable = doc.getZonalScalable(marketBasedNetwork, instant);
        marketZonalScalable.addAll(new ZonalDataImpl<>(Map.of(new EICode(Country.FR).getAreaCode(), getCountryGeneratorsScalable(referenceNetwork, Country.FR))));
        ExchangeAligner exchangeAligner = new ExchangeAligner(new BalanceComputationParameters(), LoadFlow.find(), LocalComputationManager.getDefault());
        ExchangeAligner.Status status = exchangeAligner.align(referenceNetwork, marketBasedNetwork, marketZonalScalable);
        assertEquals(ExchangeAligner.Status.ALIGNED_WITH_BALANCE_ADJUSTMENT, status);
        assertEquals(-1200, countryAreaES.create(referenceNetwork).getNetPosition(), EPSILON);
        assertEquals(500, countryAreaFR.create(referenceNetwork).getNetPosition(), EPSILON);
        assertEquals(700, countryAreaPT.create(referenceNetwork).getNetPosition(), EPSILON);
    }

    @Test
    void testWith16Nodes() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        LoadFlow.run(referenceNetwork);
        CountryAreaFactory countryAreaBE = new CountryAreaFactory(Country.BE);
        CountryAreaFactory countryAreaDE = new CountryAreaFactory(Country.DE);
        CountryAreaFactory countryAreaFR = new CountryAreaFactory(Country.FR);
        CountryAreaFactory countryAreaNL = new CountryAreaFactory(Country.NL);

        assertEquals(2500, countryAreaBE.create(referenceNetwork).getNetPosition(), EPSILON);
        assertEquals(-2000, countryAreaDE.create(referenceNetwork).getNetPosition(), EPSILON);
        assertEquals(0, countryAreaFR.create(referenceNetwork).getNetPosition(), EPSILON);
        assertEquals(-500, countryAreaNL.create(referenceNetwork).getNetPosition(), EPSILON);

        List<BalanceComputationArea> areas = new ArrayList<>();
        Scalable scalableBE = TestUtils.getCountryGeneratorsScalable(referenceNetwork, Country.BE);
        Scalable scalableDE = TestUtils.getCountryGeneratorsScalable(referenceNetwork, Country.DE);
        Scalable scalableFR = TestUtils.getCountryGeneratorsScalable(referenceNetwork, Country.FR);
        Scalable scalableNL = TestUtils.getCountryGeneratorsScalable(referenceNetwork, Country.NL);
        areas.add(new BalanceComputationArea("BE", countryAreaBE, scalableBE, -2500.));
        areas.add(new BalanceComputationArea("BE", countryAreaDE, scalableDE, -1500.));
        areas.add(new BalanceComputationArea("FR", countryAreaFR, scalableFR, 3000.));
        areas.add(new BalanceComputationArea("FR", countryAreaNL, scalableNL, 1000.));
        BalanceComputationFactory balanceComputationFactory = new BalanceComputationFactoryImpl();
        LoadFlow.Runner loadFlowRunner = LoadFlow.find();
        ComputationManager computationManager = LocalComputationManager.getDefault();
        BalanceComputation balanceComputation = balanceComputationFactory.create(areas, loadFlowRunner, computationManager);
        BalanceComputationParameters parameters = new BalanceComputationParameters();
        BalanceComputationResult result = balanceComputation.run(referenceNetwork, referenceNetwork.getVariantManager().getWorkingVariantId(), parameters).join();

        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getStatus());
        assertEquals(-2500, countryAreaBE.create(referenceNetwork).getNetPosition(), EPSILON);
        assertEquals(-1500, countryAreaDE.create(referenceNetwork).getNetPosition(), EPSILON);
        assertEquals(3000, countryAreaFR.create(referenceNetwork).getNetPosition(), EPSILON);
        assertEquals(1000, countryAreaNL.create(referenceNetwork).getNetPosition(), EPSILON);

        Network marketBasedNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        ZonalData<Scalable> marketZonalScalable = TestUtils.getAutoScalable(marketBasedNetwork);
        ExchangeAligner exchangeAligner = new ExchangeAligner(new BalanceComputationParameters(), LoadFlow.find(), LocalComputationManager.getDefault());
        ExchangeAligner.Status status = exchangeAligner.align(referenceNetwork, marketBasedNetwork, marketZonalScalable);
        assertEquals(ExchangeAligner.Status.ALIGNED_WITH_BALANCE_ADJUSTMENT, status);
        assertEquals(-2500, countryAreaBE.create(marketBasedNetwork).getNetPosition(), EPSILON);
        assertEquals(-1500, countryAreaDE.create(marketBasedNetwork).getNetPosition(), EPSILON);
        assertEquals(3000, countryAreaFR.create(marketBasedNetwork).getNetPosition(), EPSILON);
        assertEquals(1000, countryAreaNL.create(marketBasedNetwork).getNetPosition(), EPSILON);
    }

    @Test
    void testSameNetwork16Nodes() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        LoadFlow.run(referenceNetwork);
        CountryAreaFactory countryAreaBE = new CountryAreaFactory(Country.BE);
        CountryAreaFactory countryAreaDE = new CountryAreaFactory(Country.DE);
        CountryAreaFactory countryAreaFR = new CountryAreaFactory(Country.FR);
        CountryAreaFactory countryAreaNL = new CountryAreaFactory(Country.NL);

        assertEquals(2500, countryAreaBE.create(referenceNetwork).getNetPosition(), EPSILON);
        assertEquals(-2000, countryAreaDE.create(referenceNetwork).getNetPosition(), EPSILON);
        assertEquals(0, countryAreaFR.create(referenceNetwork).getNetPosition(), EPSILON);
        assertEquals(-500, countryAreaNL.create(referenceNetwork).getNetPosition(), EPSILON);

        Network marketBasedNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        ZonalData<Scalable> marketZonalScalable = TestUtils.getAutoScalable(marketBasedNetwork);
        ExchangeAligner exchangeAligner = new ExchangeAligner(new BalanceComputationParameters(), LoadFlow.find(), LocalComputationManager.getDefault());
        ExchangeAligner.Status status = exchangeAligner.align(referenceNetwork, marketBasedNetwork, marketZonalScalable);
        assertEquals(ExchangeAligner.Status.ALREADY_ALIGNED, status);
        assertEquals(2500, countryAreaBE.create(marketBasedNetwork).getNetPosition(), EPSILON);
        assertEquals(-2000, countryAreaDE.create(marketBasedNetwork).getNetPosition(), EPSILON);
        assertEquals(0, countryAreaFR.create(marketBasedNetwork).getNetPosition(), EPSILON);
        assertEquals(-500, countryAreaNL.create(marketBasedNetwork).getNetPosition(), EPSILON);
    }

    @Test
    void testDifferentNetworks() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/pst/NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct");
        ZonalData<Scalable> marketZonalScalable = TestUtils.getAutoScalable(marketBasedNetwork);
        ExchangeAligner exchangeAligner = new ExchangeAligner(new BalanceComputationParameters(), LoadFlow.find(), LocalComputationManager.getDefault());
        TrmException exception = assertThrows(TrmException.class, () -> exchangeAligner.align(referenceNetwork, marketBasedNetwork, marketZonalScalable));
        assertEquals("Scalable not found: 10YCB-GERMANY--8", exception.getMessage());
    }
}
