/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.rte_france.trm_algorithm.operational_conditions_aligners;

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
import com.rte_france.trm_algorithm.TestUtils;
import com.rte_france.trm_algorithm.TrmException;
import com.rte_france.trm_algorithm.TrmUtils;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.rte_france.trm_algorithm.TrmUtils.getCountryGeneratorsScalable;
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
        ExchangeAligner.Result result = exchangeAligner.align(referenceNetwork, marketBasedNetwork, marketZonalScalable);
        assertEquals(ExchangeAligner.Status.FAILED, result.getStatus());
        assertEquals(BalanceComputationResult.Status.FAILED, result.getBalanceComputationResult().getStatus());
        Map<Country, Double> referenceNetPositions = result.getReferenceNetPositions();
        assertEquals(-983.684, referenceNetPositions.get(Country.ES), EPSILON);
        assertEquals(0.205, referenceNetPositions.get(Country.FR), EPSILON);
        assertEquals(983.4790208818534, referenceNetPositions.get(Country.PT), EPSILON);
        Map<Country, Double> initialMarketBasedNetPositions = result.getInitialMarketBasedNetPositions();
        assertEquals(-0.411, initialMarketBasedNetPositions.get(Country.ES), EPSILON);
        assertEquals(0.205, initialMarketBasedNetPositions.get(Country.FR), EPSILON);
        assertEquals(0.205, initialMarketBasedNetPositions.get(Country.PT), EPSILON);
        Map<Country, Double> newMarketBasedNetPositions = result.getNewMarketBasedNetPositions();
        assertEquals(-0.411, newMarketBasedNetPositions.get(Country.ES), EPSILON);
        assertEquals(0.205, newMarketBasedNetPositions.get(Country.FR), EPSILON);
        assertEquals(0.205, newMarketBasedNetPositions.get(Country.PT), EPSILON);
    }

    @Test
    void testDoNotAlignWithSameNetPositions() {
        Network referenceNetwork = TestUtils.importNetwork("operational_conditions_aligners/shift/TestCase_with_transformers.xiidm");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/shift/TestCase_with_transformers.xiidm");
        ZonalData<Scalable> marketZonalScalable = new ZonalDataImpl<>(Collections.emptyMap());
        ExchangeAligner exchangeAligner = new ExchangeAligner(new BalanceComputationParameters(), LoadFlow.find(), LocalComputationManager.getDefault());
        ExchangeAligner.Result result = exchangeAligner.align(referenceNetwork, marketBasedNetwork, marketZonalScalable);
        assertEquals(ExchangeAligner.Status.ALREADY_ALIGNED, result.getStatus());
        assertNull(result.getBalanceComputationResult());
        Map<Country, Double> referenceNetPositions = result.getReferenceNetPositions();
        assertEquals(-0.411, referenceNetPositions.get(Country.ES), EPSILON);
        assertEquals(0.205, referenceNetPositions.get(Country.FR), EPSILON);
        assertEquals(0.205, referenceNetPositions.get(Country.PT), EPSILON);
        Map<Country, Double> initialMarketBasedNetPositions = result.getInitialMarketBasedNetPositions();
        assertEquals(-0.411, initialMarketBasedNetPositions.get(Country.ES), EPSILON);
        assertEquals(0.205, initialMarketBasedNetPositions.get(Country.FR), EPSILON);
        assertEquals(0.205, initialMarketBasedNetPositions.get(Country.PT), EPSILON);
        assertNull(result.getNewMarketBasedNetPositions());
    }

    @Test
    void testWithSwe() {
        Network referenceNetwork = TestUtils.importNetwork("operational_conditions_aligners/shift/TestCase_with_transformers.xiidm");
        LoadFlow.run(referenceNetwork);

        CimGlskDocument doc = CimGlskDocument.importGlsk(getClass().getResourceAsStream("shift/TestCase_with_transformers_glsk.xml"));
        Instant instant = LocalDateTime.of(2023, 7, 31, 7, 30).toInstant(ZoneOffset.UTC);
        ZonalData<Scalable> referenceZonalScalable = doc.getZonalScalable(referenceNetwork, instant);
        List<BalanceComputationArea> areas = new ArrayList<>();
        Scalable scalableES = referenceZonalScalable.getData(new EICode(Country.ES).getAreaCode());
        Scalable scalableFR = TrmUtils.getCountryGeneratorsScalable(referenceNetwork, Country.FR);
        Scalable scalablePT = referenceZonalScalable.getData(new EICode(Country.PT).getAreaCode());
        areas.add(new BalanceComputationArea("ES", new CountryAreaFactory(Country.ES), scalableES, -1200.));
        areas.add(new BalanceComputationArea("FR", new CountryAreaFactory(Country.FR), scalableFR, 500.));
        areas.add(new BalanceComputationArea("PT", new CountryAreaFactory(Country.PT), scalablePT, 700.));
        BalanceComputationFactory balanceComputationFactory = new BalanceComputationFactoryImpl();
        LoadFlow.Runner loadFlowRunner = LoadFlow.find();
        ComputationManager computationManager = LocalComputationManager.getDefault();
        BalanceComputation balanceComputation = balanceComputationFactory.create(areas, loadFlowRunner, computationManager);
        BalanceComputationParameters parameters = new BalanceComputationParameters().setThresholdNetPosition(1e-2);
        BalanceComputationResult balanceComputationResult = balanceComputation.run(referenceNetwork, referenceNetwork.getVariantManager().getWorkingVariantId(), parameters).join();
        assertEquals(BalanceComputationResult.Status.SUCCESS, balanceComputationResult.getStatus());

        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/shift/TestCase_with_transformers.xiidm");
        ZonalData<Scalable> marketZonalScalable = doc.getZonalScalable(marketBasedNetwork, instant);
        marketZonalScalable.addAll(new ZonalDataImpl<>(Map.of(new EICode(Country.FR).getAreaCode(), getCountryGeneratorsScalable(referenceNetwork, Country.FR))));
        ExchangeAligner exchangeAligner = new ExchangeAligner(parameters, LoadFlow.find(), LocalComputationManager.getDefault());
        ExchangeAligner.Result result = exchangeAligner.align(referenceNetwork, marketBasedNetwork, marketZonalScalable);
        assertEquals(ExchangeAligner.Status.ALIGNED_WITH_BALANCE_ADJUSTMENT, result.getStatus());
        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getBalanceComputationResult().getStatus());
        Map<Country, Double> referenceNetPositions = result.getReferenceNetPositions();
        assertEquals(-1200, referenceNetPositions.get(Country.ES), EPSILON);
        assertEquals(500, referenceNetPositions.get(Country.FR), EPSILON);
        assertEquals(700, referenceNetPositions.get(Country.PT), EPSILON);
        Map<Country, Double> initialMarketBasedNetPositions = result.getInitialMarketBasedNetPositions();
        assertEquals(-0.411, initialMarketBasedNetPositions.get(Country.ES), EPSILON);
        assertEquals(0.205, initialMarketBasedNetPositions.get(Country.FR), EPSILON);
        assertEquals(0.205, initialMarketBasedNetPositions.get(Country.PT), EPSILON);
        Map<Country, Double> newMarketBasedNetPositions = result.getNewMarketBasedNetPositions();
        assertEquals(-1200, newMarketBasedNetPositions.get(Country.ES), EPSILON);
        assertEquals(500, newMarketBasedNetPositions.get(Country.FR), EPSILON);
        assertEquals(700, newMarketBasedNetPositions.get(Country.PT), EPSILON);
    }

    @Test
    void testWith16Nodes() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        LoadFlow.run(referenceNetwork);

        List<BalanceComputationArea> areas = new ArrayList<>();
        Scalable scalableBE = TrmUtils.getCountryGeneratorsScalable(referenceNetwork, Country.BE);
        Scalable scalableDE = TrmUtils.getCountryGeneratorsScalable(referenceNetwork, Country.DE);
        Scalable scalableFR = TrmUtils.getCountryGeneratorsScalable(referenceNetwork, Country.FR);
        Scalable scalableNL = TrmUtils.getCountryGeneratorsScalable(referenceNetwork, Country.NL);
        areas.add(new BalanceComputationArea("BE", new CountryAreaFactory(Country.BE), scalableBE, -2500.));
        areas.add(new BalanceComputationArea("BE", new CountryAreaFactory(Country.DE), scalableDE, -1500.));
        areas.add(new BalanceComputationArea("FR", new CountryAreaFactory(Country.FR), scalableFR, 3000.));
        areas.add(new BalanceComputationArea("FR", new CountryAreaFactory(Country.NL), scalableNL, 1000.));
        BalanceComputationFactory balanceComputationFactory = new BalanceComputationFactoryImpl();
        LoadFlow.Runner loadFlowRunner = LoadFlow.find();
        ComputationManager computationManager = LocalComputationManager.getDefault();
        BalanceComputation balanceComputation = balanceComputationFactory.create(areas, loadFlowRunner, computationManager);
        BalanceComputationParameters parameters = new BalanceComputationParameters();
        BalanceComputationResult balanceComputationResult = balanceComputation.run(referenceNetwork, referenceNetwork.getVariantManager().getWorkingVariantId(), parameters).join();
        assertEquals(BalanceComputationResult.Status.SUCCESS, balanceComputationResult.getStatus());

        Network marketBasedNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        ZonalData<Scalable> marketZonalScalable = TrmUtils.getAutoScalable(marketBasedNetwork);
        ExchangeAligner exchangeAligner = new ExchangeAligner(new BalanceComputationParameters(), LoadFlow.find(), LocalComputationManager.getDefault());
        ExchangeAligner.Result result = exchangeAligner.align(referenceNetwork, marketBasedNetwork, marketZonalScalable);
        assertEquals(ExchangeAligner.Status.ALIGNED_WITH_BALANCE_ADJUSTMENT, result.getStatus());
        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getBalanceComputationResult().getStatus());
        Map<Country, Double> referenceNetPositions = result.getReferenceNetPositions();
        assertEquals(-2500, referenceNetPositions.get(Country.BE), EPSILON);
        assertEquals(-1500, referenceNetPositions.get(Country.DE), EPSILON);
        assertEquals(3000, referenceNetPositions.get(Country.FR), EPSILON);
        assertEquals(1000, referenceNetPositions.get(Country.NL), EPSILON);
        Map<Country, Double> initialMarketBasedNetPositions = result.getInitialMarketBasedNetPositions();
        assertEquals(2500, initialMarketBasedNetPositions.get(Country.BE), EPSILON);
        assertEquals(-2000, initialMarketBasedNetPositions.get(Country.DE), EPSILON);
        assertEquals(0, initialMarketBasedNetPositions.get(Country.FR), EPSILON);
        assertEquals(-500, initialMarketBasedNetPositions.get(Country.NL), EPSILON);
        Map<Country, Double> newMarketBasedNetPositions = result.getNewMarketBasedNetPositions();
        assertEquals(-2500, newMarketBasedNetPositions.get(Country.BE), EPSILON);
        assertEquals(-1500, newMarketBasedNetPositions.get(Country.DE), EPSILON);
        assertEquals(3000, newMarketBasedNetPositions.get(Country.FR), EPSILON);
        assertEquals(1000, newMarketBasedNetPositions.get(Country.NL), EPSILON);
    }

    @Test
    void testSameNetwork16Nodes() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        LoadFlow.run(referenceNetwork);

        Network marketBasedNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        ZonalData<Scalable> marketZonalScalable = TrmUtils.getAutoScalable(marketBasedNetwork);
        ExchangeAligner exchangeAligner = new ExchangeAligner(new BalanceComputationParameters(), LoadFlow.find(), LocalComputationManager.getDefault());
        ExchangeAligner.Result result = exchangeAligner.align(referenceNetwork, marketBasedNetwork, marketZonalScalable);
        assertEquals(ExchangeAligner.Status.ALREADY_ALIGNED, result.getStatus());
        assertNull(result.getBalanceComputationResult());
        Map<Country, Double> referenceNetPositions = result.getReferenceNetPositions();
        assertEquals(2500, referenceNetPositions.get(Country.BE), EPSILON);
        assertEquals(-2000, referenceNetPositions.get(Country.DE), EPSILON);
        assertEquals(0, referenceNetPositions.get(Country.FR), EPSILON);
        assertEquals(-500, referenceNetPositions.get(Country.NL), EPSILON);
        Map<Country, Double> initialMarketBasedNetPositions = result.getInitialMarketBasedNetPositions();
        assertEquals(2500, initialMarketBasedNetPositions.get(Country.BE), EPSILON);
        assertEquals(-2000, initialMarketBasedNetPositions.get(Country.DE), EPSILON);
        assertEquals(0, initialMarketBasedNetPositions.get(Country.FR), EPSILON);
        assertEquals(-500, initialMarketBasedNetPositions.get(Country.NL), EPSILON);
        assertNull(result.getNewMarketBasedNetPositions());
    }

    @Test
    void testDifferentNetworks() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/pst/NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct");
        ZonalData<Scalable> marketZonalScalable = TrmUtils.getAutoScalable(marketBasedNetwork);
        ExchangeAligner exchangeAligner = new ExchangeAligner(new BalanceComputationParameters(), LoadFlow.find(), LocalComputationManager.getDefault());
        TrmException exception = assertThrows(TrmException.class, () -> exchangeAligner.align(referenceNetwork, marketBasedNetwork, marketZonalScalable));
        assertEquals("Scalable not found: 10YCB-GERMANY--8", exception.getMessage());
    }
}
