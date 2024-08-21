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
        ExchangeAligner exchangeAligner = new ExchangeAligner(new BalanceComputationParameters(), LoadFlow.find(), LocalComputationManager.getDefault(), marketZonalScalable);
        TrmException trmException = assertThrows(TrmException.class, () -> exchangeAligner.align(referenceNetwork, marketBasedNetwork));
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
        ExchangeAligner exchangeAligner = new ExchangeAligner(new BalanceComputationParameters(), LoadFlow.find(), LocalComputationManager.getDefault(), marketZonalScalable);
        exchangeAligner.align(referenceNetwork, marketBasedNetwork);
        ExchangeAlignerResult result = exchangeAligner.getResult();
        assertEquals(ExchangeAligner.Status.NOT_ALIGNED, result.getStatus());
        assertEquals(BalanceComputationResult.Status.FAILED, result.getBalanceComputationResult().getStatus());
        TestUtils.assertNetPositions(Map.of(Country.ES, -983.6, Country.FR, 0.2, Country.PT, 983.4), result.getReferenceExchangeAndNetPosition());
        TestUtils.assertNetPositions(Map.of(Country.ES, -0.4, Country.FR, 0.2, Country.PT, 0.2), result.getInitialMarketBasedExchangeAndNetPosition());
        TestUtils.assertExchanges(Map.of(Country.FR, Map.of(Country.ES, 0.2), Country.PT, Map.of(Country.ES, 983.4)), result.getReferenceExchangeAndNetPosition());
        TestUtils.assertExchanges(Map.of(Country.FR, Map.of(Country.ES, 0.2), Country.PT, Map.of(Country.ES, 0.2)), result.getInitialMarketBasedExchangeAndNetPosition());
        assertEquals(983.2, result.getInitialMaxAbsoluteExchangeDifference(), EPSILON);
        TestUtils.assertNetPositions(Map.of(Country.ES, -983.6, Country.FR, 0.2, Country.PT, 983.4), result.getTargetNetPositions());
        TestUtils.assertNetPositions(Map.of(Country.ES, -0.4, Country.FR, 0.2, Country.PT, 0.2), result.getNewMarketBasedExchangeAndNetPosition());
        TestUtils.assertExchanges(Map.of(Country.FR, Map.of(Country.ES, 0.2), Country.PT, Map.of(Country.ES, 0.2)), result.getNewMarketBasedExchangeAndNetPosition());
        assertEquals(983.2, result.getNewMaxAbsoluteExchangeDifference(), EPSILON);
    }

    @Test
    void testDoNotAlignWithSameNetPositions() {
        Network referenceNetwork = TestUtils.importNetwork("operational_conditions_aligners/shift/TestCase_with_transformers.xiidm");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/shift/TestCase_with_transformers.xiidm");
        ZonalData<Scalable> marketZonalScalable = new ZonalDataImpl<>(Collections.emptyMap());
        ExchangeAligner exchangeAligner = new ExchangeAligner(new BalanceComputationParameters(), LoadFlow.find(), LocalComputationManager.getDefault(), marketZonalScalable);
        exchangeAligner.align(referenceNetwork, marketBasedNetwork);
        ExchangeAlignerResult result = exchangeAligner.getResult();
        assertEquals(ExchangeAligner.Status.ALREADY_ALIGNED, result.getStatus());
        assertNull(result.getBalanceComputationResult());
        TestUtils.assertNetPositions(Map.of(Country.ES, -0.4, Country.FR, 0.2, Country.PT, 0.2), result.getReferenceExchangeAndNetPosition());
        TestUtils.assertNetPositions(Map.of(Country.ES, -0.4, Country.FR, 0.2, Country.PT, 0.2), result.getInitialMarketBasedExchangeAndNetPosition());
        TestUtils.assertExchanges(Map.of(Country.FR, Map.of(Country.ES, 0.2), Country.PT, Map.of(Country.ES, 0.2)), result.getReferenceExchangeAndNetPosition());
        TestUtils.assertExchanges(Map.of(Country.FR, Map.of(Country.ES, 0.2), Country.PT, Map.of(Country.ES, 0.2)), result.getInitialMarketBasedExchangeAndNetPosition());
        assertEquals(0, result.getInitialMaxAbsoluteExchangeDifference(), EPSILON);
        TestUtils.assertNetPositions(Map.of(Country.ES, -0.4, Country.FR, 0.2, Country.PT, 0.2), result.getTargetNetPositions());
        assertTrue(result.getNewMarketBasedExchangeAndNetPosition().getCountries().isEmpty());
        assertEquals(Double.NaN, result.getNewMaxAbsoluteExchangeDifference());
    }

    @Test
    void testWithSwe() {
        Network referenceNetwork = TestUtils.importNetwork("operational_conditions_aligners/shift/TestCase_with_transformers.xiidm");
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
        ExchangeAligner exchangeAligner = new ExchangeAligner(parameters, LoadFlow.find(), LocalComputationManager.getDefault(), marketZonalScalable);
        exchangeAligner.align(referenceNetwork, marketBasedNetwork);
        ExchangeAlignerResult result = exchangeAligner.getResult();
        assertEquals(ExchangeAligner.Status.ALIGNED_WITH_BALANCE_ADJUSTMENT, result.getStatus());
        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getBalanceComputationResult().getStatus());
        TestUtils.assertNetPositions(Map.of(Country.ES, -1200., Country.FR, 500., Country.PT, 700.), result.getReferenceExchangeAndNetPosition());
        TestUtils.assertNetPositions(Map.of(Country.ES, -0.4, Country.FR, 0.2, Country.PT, 0.2), result.getInitialMarketBasedExchangeAndNetPosition());
        TestUtils.assertExchanges(Map.of(Country.FR, Map.of(Country.ES, 500.), Country.PT, Map.of(Country.ES, 700.)), result.getReferenceExchangeAndNetPosition());
        TestUtils.assertExchanges(Map.of(Country.FR, Map.of(Country.ES, 0.2), Country.PT, Map.of(Country.ES, 0.2)), result.getInitialMarketBasedExchangeAndNetPosition());
        assertEquals(699.8, result.getInitialMaxAbsoluteExchangeDifference(), EPSILON);
        TestUtils.assertNetPositions(Map.of(Country.ES, -1200., Country.FR, 500., Country.PT, 700.), result.getTargetNetPositions());
        TestUtils.assertNetPositions(Map.of(Country.ES, -1200., Country.FR, 500., Country.PT, 700.), result.getNewMarketBasedExchangeAndNetPosition());
        TestUtils.assertExchanges(Map.of(Country.FR, Map.of(Country.ES, 500.), Country.PT, Map.of(Country.ES, 700.)), result.getNewMarketBasedExchangeAndNetPosition());
        assertEquals(0, result.getNewMaxAbsoluteExchangeDifference(), EPSILON);
    }

    @Test
    void testWith16Nodes() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
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
        ExchangeAligner exchangeAligner = new ExchangeAligner(new BalanceComputationParameters(), LoadFlow.find(), LocalComputationManager.getDefault(), marketZonalScalable);
        exchangeAligner.align(referenceNetwork, marketBasedNetwork);
        ExchangeAlignerResult result = exchangeAligner.getResult();
        assertEquals(ExchangeAligner.Status.ALIGNED_WITH_BALANCE_ADJUSTMENT, result.getStatus());
        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getBalanceComputationResult().getStatus());
        TestUtils.assertNetPositions(Map.of(Country.BE, -2500., Country.DE, -1500., Country.FR, 3000., Country.NL, 1000.), result.getReferenceExchangeAndNetPosition());
        TestUtils.assertNetPositions(Map.of(Country.BE, 2500., Country.DE, -2000., Country.FR, 0., Country.NL, -500.), result.getInitialMarketBasedExchangeAndNetPosition());
        TestUtils.assertExchanges(Map.of(Country.FR, Map.of(Country.BE, 2090.9, Country.DE, 909.), Country.NL, Map.of(Country.BE, 409., Country.DE, 590.9)), result.getReferenceExchangeAndNetPosition());
        TestUtils.assertExchanges(Map.of(Country.FR, Map.of(Country.DE, 1243.), Country.NL, Map.of(Country.DE, 756.9), Country.BE, Map.of(Country.FR, 1243., Country.NL, 1256.9)), result.getInitialMarketBasedExchangeAndNetPosition());
        assertEquals(3333.9, result.getInitialMaxAbsoluteExchangeDifference(), EPSILON);
        TestUtils.assertNetPositions(Map.of(Country.BE, -2500., Country.DE, -1500., Country.FR, 3000., Country.NL, 1000.), result.getTargetNetPositions());
        TestUtils.assertNetPositions(Map.of(Country.BE, -2500., Country.DE, -1500., Country.FR, 3000., Country.NL, 1000.), result.getNewMarketBasedExchangeAndNetPosition());
        TestUtils.assertExchanges(Map.of(Country.FR, Map.of(Country.BE, 2090.9, Country.DE, 909.), Country.NL, Map.of(Country.BE, 409., Country.DE, 590.9)), result.getNewMarketBasedExchangeAndNetPosition());
        assertEquals(0, result.getNewMaxAbsoluteExchangeDifference(), EPSILON);
    }

    @Test
    void testSameNetwork16Nodes() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        ZonalData<Scalable> marketZonalScalable = TrmUtils.getAutoScalable(marketBasedNetwork);
        ExchangeAligner exchangeAligner = new ExchangeAligner(new BalanceComputationParameters(), LoadFlow.find(), LocalComputationManager.getDefault(), marketZonalScalable);
        exchangeAligner.align(referenceNetwork, marketBasedNetwork);
        ExchangeAlignerResult result = exchangeAligner.getResult();
        assertEquals(ExchangeAligner.Status.ALREADY_ALIGNED, result.getStatus());
        assertNull(result.getBalanceComputationResult());
        TestUtils.assertNetPositions(Map.of(Country.BE, 2500., Country.DE, -2000., Country.FR, 0., Country.NL, -500.), result.getReferenceExchangeAndNetPosition());
        TestUtils.assertNetPositions(Map.of(Country.BE, 2500., Country.DE, -2000., Country.FR, 0., Country.NL, -500.), result.getInitialMarketBasedExchangeAndNetPosition());
        TestUtils.assertExchanges(Map.of(Country.FR, Map.of(Country.DE, 1243.), Country.NL, Map.of(Country.DE, 756.9), Country.BE, Map.of(Country.FR, 1243., Country.NL, 1256.9)), result.getReferenceExchangeAndNetPosition());
        TestUtils.assertExchanges(Map.of(Country.FR, Map.of(Country.DE, 1243.), Country.NL, Map.of(Country.DE, 756.9), Country.BE, Map.of(Country.FR, 1243., Country.NL, 1256.9)), result.getInitialMarketBasedExchangeAndNetPosition());
        assertEquals(0, result.getInitialMaxAbsoluteExchangeDifference(), EPSILON);
        TestUtils.assertNetPositions(Map.of(Country.BE, 2500., Country.DE, -2000., Country.FR, 0., Country.NL, -500.), result.getTargetNetPositions());
        assertTrue(result.getNewMarketBasedExchangeAndNetPosition().getCountries().isEmpty());
        assertEquals(Double.NaN, result.getNewMaxAbsoluteExchangeDifference());
    }

    @Test
    void testSmallerMarketBasedNetwork() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("operational_conditions_aligners/pst/NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct");
        ZonalData<Scalable> marketZonalScalable = TrmUtils.getAutoScalable(marketBasedNetwork);
        ExchangeAligner exchangeAligner = new ExchangeAligner(new BalanceComputationParameters(), LoadFlow.find(), LocalComputationManager.getDefault(), marketZonalScalable);
        TrmException exception = assertThrows(TrmException.class, () -> exchangeAligner.align(referenceNetwork, marketBasedNetwork));
        assertEquals("Market based network contains countries [BE, FR]. It does not contain all reference network countries [BE, FR, DE, NL]", exception.getMessage());
    }

    @Test
    void testSmallerReferenceNetworkAligned() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16NodesWithoutNetherlandsAndBelgium.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16NodesWithoutNetherlands.uct");
        Load load1 = marketBasedNetwork.getLoad("FFR1AA1 _load");
        load1.setP0(load1.getP0() + 500);
        Load load2 = marketBasedNetwork.getLoad("DDE4AA1 _load");
        load2.setP0(load2.getP0() - 500);
        ZonalData<Scalable> marketZonalScalable = TrmUtils.getAutoScalable(marketBasedNetwork);
        ExchangeAligner exchangeAligner = new ExchangeAligner(new BalanceComputationParameters(), LoadFlow.find(), LocalComputationManager.getDefault(), marketZonalScalable);
        exchangeAligner.align(referenceNetwork, marketBasedNetwork);
        ExchangeAlignerResult result = exchangeAligner.getResult();
        assertEquals(ExchangeAligner.Status.ALIGNED_WITH_BALANCE_ADJUSTMENT, result.getStatus());
        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getBalanceComputationResult().getStatus());
        TestUtils.assertNetPositions(Map.of(Country.DE, -1111.1, Country.FR, 1111.1), result.getReferenceExchangeAndNetPosition());
        TestUtils.assertNetPositions(Map.of(Country.BE, 2346.1, Country.DE, -1653.8, Country.FR, -692.3), result.getInitialMarketBasedExchangeAndNetPosition());
        TestUtils.assertExchanges(Map.of(Country.FR, Map.of(Country.DE, 1111.1)), result.getReferenceExchangeAndNetPosition());
        TestUtils.assertExchanges(Map.of(Country.FR, Map.of(Country.DE, 1653.8), Country.BE, Map.of(Country.FR, 2346.1)), result.getInitialMarketBasedExchangeAndNetPosition());
        assertEquals(542.7, result.getInitialMaxAbsoluteExchangeDifference(), EPSILON);
        TestUtils.assertNetPositions(Map.of(Country.BE, 2346.1, Country.DE, -1111.1, Country.FR, -1235.), result.getTargetNetPositions());
        TestUtils.assertNetPositions(Map.of(Country.BE, 2346.1, Country.DE, -1111.1, Country.FR, -1235.), result.getNewMarketBasedExchangeAndNetPosition());
        TestUtils.assertExchanges(Map.of(Country.FR, Map.of(Country.DE, 1111.1), Country.BE, Map.of(Country.FR, 2346.1)), result.getNewMarketBasedExchangeAndNetPosition());
        assertEquals(0, result.getNewMaxAbsoluteExchangeDifference(), EPSILON);
    }

    @Test
    void testAligmentDoesOnlyValidForNTC() {
        Network referenceNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16NodesWithoutNetherlands.uct");
        Network marketBasedNetwork = TestUtils.importNetwork("TestCase16Nodes/TestCase16Nodes.uct");
        Load load1 = marketBasedNetwork.getLoad("FFR1AA1 _load");
        load1.setP0(load1.getP0() + 500);
        Load load2 = marketBasedNetwork.getLoad("DDE4AA1 _load");
        load2.setP0(load2.getP0() - 500);
        ZonalData<Scalable> marketZonalScalable = TrmUtils.getAutoScalable(marketBasedNetwork);
        ExchangeAligner exchangeAligner = new ExchangeAligner(new BalanceComputationParameters(), LoadFlow.find(), LocalComputationManager.getDefault(), marketZonalScalable);
        exchangeAligner.align(referenceNetwork, marketBasedNetwork);
        ExchangeAlignerResult result = exchangeAligner.getResult();
        assertEquals(ExchangeAligner.Status.TARGET_NET_POSITION_REACHED_BUT_EXCHANGE_NOT_ALIGNED, result.getStatus());
        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getBalanceComputationResult().getStatus());
        TestUtils.assertNetPositions(Map.of(Country.BE, 2346.1, Country.DE, -2153.8, Country.FR, -192.3), result.getReferenceExchangeAndNetPosition());
        TestUtils.assertNetPositions(Map.of(Country.BE, 2500., Country.DE, -1500., Country.FR, -500., Country.NL, -500.), result.getInitialMarketBasedExchangeAndNetPosition());
        TestUtils.assertExchanges(Map.of(Country.FR, Map.of(Country.DE, 2153.8), Country.BE, Map.of(Country.FR, 2346.1)), result.getReferenceExchangeAndNetPosition());
        TestUtils.assertExchanges(Map.of(Country.FR, Map.of(Country.DE, 785.9), Country.NL, Map.of(Country.DE, 714.), Country.BE, Map.of(Country.FR, 1285.9, Country.NL, 1214.)), result.getInitialMarketBasedExchangeAndNetPosition());
        assertEquals(1367.8, result.getInitialMaxAbsoluteExchangeDifference(), EPSILON);
        TestUtils.assertNetPositions(Map.of(Country.BE, 3560.1, Country.DE, -2867.8, Country.FR, -192.3, Country.NL, -500.), result.getTargetNetPositions());
        TestUtils.assertNetPositions(Map.of(Country.BE, 3560.1, Country.DE, -2867.8, Country.FR, -192.3, Country.NL, -500.), result.getNewMarketBasedExchangeAndNetPosition());
        TestUtils.assertExchanges(Map.of(Country.FR, Map.of(Country.DE, 1739.8), Country.BE, Map.of(Country.FR, 1932.1, Country.NL, 1628.), Country.NL, Map.of(Country.DE, 1128.)), result.getNewMarketBasedExchangeAndNetPosition());
        assertEquals(413.9, result.getNewMaxAbsoluteExchangeDifference(), EPSILON);
    }
}
