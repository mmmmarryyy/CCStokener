package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.BattleStepStrings.*;
import static games.strategy.triplea.delegate.GameDataTestUtil.*;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParser;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.random.ScriptedRandomSource;
import games.strategy.net.GUID;
import games.strategy.triplea.attatchments.TerritoryAttachment;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.PlaceableUnits;
import games.strategy.triplea.util.DummyTripleAPlayer;
import games.strategy.triplea.xml.LoadGameUtil;
import games.strategy.util.IntegerMap;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;

public class AA50_41Test extends TestCase {

    private GameData m_data;

    @Override
    protected void setUp() throws Exception {
        m_data = LoadGameUtil.loadGame("AA50", "AA50-41.xml");
    }

    @Override
    protected void tearDown() throws Exception {
        m_data = null;
    }

    public void testDefendingTrasnportsAutoKilled() {
        Territory sz13 = m_data.getMap().getTerritory("13 Sea Zone");
        Territory sz12 = m_data.getMap().getTerritory("12 Sea Zone");
        PlayerID british = m_data.getPlayerList().getPlayerID("British");
        MoveDelegate moveDelegate = moveDelegate(m_data);
        ITestDelegateBridge bridge = getDelegateBridge(british);
        bridge.setStepName("CombatMove");
        moveDelegate.start(bridge, m_data);
        Route sz12To13 = new Route();
        sz12To13.setStart(sz12);
        sz12To13.add(sz13);
        String error = moveDelegate.move(sz12.getUnits().getUnits(), sz12To13);
        assertEquals(error, null);
        assertEquals(sz13.getUnits().size(), 3);
        moveDelegate.end();
        assertEquals(sz13.getUnits().size(), 3);
        BattleDelegate bd = battleDelegate(m_data);
        assertFalse(bd.getBattleTracker().getPendingBattleSites(false).isEmpty());
    }

    public void testUnplacedDie() {
        PlaceDelegate del = placeDelegate(m_data);
        del.start(getDelegateBridge(british(m_data)), m_data);
        addTo(british(m_data), transports(m_data).create(1, british(m_data)));
        del.end();
        assertEquals(1, british(m_data).getUnits().size());
    }

    public void testInfantryLoadOnlyTransports() {
        Territory gibraltar = territory("Gibraltar", m_data);
        PlayerID british = british(m_data);
        addTo(gibraltar, infantry(m_data).create(1, british));
        MoveDelegate moveDelegate = moveDelegate(m_data);
        ITestDelegateBridge bridge = getDelegateBridge(british);
        bridge.setStepName("CombatMove");
        moveDelegate.start(bridge, m_data);
        bridge.setRemote(new DummyTripleAPlayer());
        Territory sz9 = territory("9 Sea Zone", m_data);
        Territory sz13 = territory("13 Sea Zone", m_data);
        Route sz9ToSz13 = new Route(sz9, territory("12 Sea Zone", m_data), sz13);
        move(sz9.getUnits().getMatches(Matches.UnitIsTransport), sz9ToSz13);
        load(gibraltar.getUnits().getUnits(), new Route(gibraltar, sz13));
        moveDelegate.end();
        bridge.setStepName("combat");
        BattleDelegate battleDelegate = battleDelegate(m_data);
        battleDelegate.start(bridge, m_data);
        assertTrue(battleDelegate.getBattles().isEmpty());
    }

    public void testLoadedTransportAttackKillsLoadedUnits() {
        PlayerID british = british(m_data);
        MoveDelegate moveDelegate = moveDelegate(m_data);
        ITestDelegateBridge bridge = getDelegateBridge(british);
        bridge.setStepName("CombatMove");
        bridge.setRemote(new DummyTripleAPlayer() {

            @Override
            public boolean selectAttackSubs(Territory unitTerritory) {
                return true;
            }
        });
        moveDelegate.start(bridge, m_data);
        Territory sz9 = territory("9 Sea Zone", m_data);
        Territory sz7 = territory("7 Sea Zone", m_data);
        Territory uk = territory("United Kingdom", m_data);
        Route sz9ToSz7 = new Route(sz9, territory("8 Sea Zone", m_data), sz7);
        List<Unit> transports = sz9.getUnits().getMatches(Matches.UnitIsTransport);
        move(transports, sz9ToSz7);
        load(uk.getUnits().getMatches(Matches.UnitIsInfantry), new Route(uk, sz7));
        moveDelegate(m_data).end();
        bridge.setStepName("combat");
        BattleDelegate battleDelegate = battleDelegate(m_data);
        battleDelegate.start(bridge, m_data);
        assertEquals(2, new TransportTracker().transporting(transports.get(0)).size());
        assertValid(battleDelegate.fightBattle(sz7, false));
        assertTrue(sz7.getUnits().toString(), sz7.getUnits().getMatches(Matches.unitOwnedBy(british)).isEmpty());
    }

    public void testCanRetreatIntoEmptyEnemyTerritory() {
        Territory eastPoland = territory("East Poland", m_data);
        Territory ukraine = territory("Ukraine", m_data);
        Territory poland = territory("Poland", m_data);
        removeFrom(eastPoland, eastPoland.getUnits().getUnits());
        MoveDelegate moveDelegate = moveDelegate(m_data);
        ITestDelegateBridge delegateBridge = getDelegateBridge(germans(m_data));
        delegateBridge.setStepName("CombatMove");
        moveDelegate.start(delegateBridge, m_data);
        Territory bulgaria = territory("Bulgaria Romania", m_data);
        move(bulgaria.getUnits().getUnits(), new Route(bulgaria, ukraine));
        move(poland.getUnits().getMatches(Matches.UnitIsAir), new Route(poland, eastPoland, ukraine));
        MustFightBattle battle = (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(ukraine, false);
        assertFalse(battle.getAttackerRetreatTerritories().contains(eastPoland));
    }

    public void testCanRetreatIntoBlitzedTerritory() {
        Territory eastPoland = territory("East Poland", m_data);
        Territory ukraine = territory("Ukraine", m_data);
        Territory poland = territory("Poland", m_data);
        removeFrom(eastPoland, eastPoland.getUnits().getUnits());
        MoveDelegate moveDelegate = moveDelegate(m_data);
        ITestDelegateBridge delegateBridge = getDelegateBridge(germans(m_data));
        delegateBridge.setStepName("CombatMove");
        moveDelegate.start(delegateBridge, m_data);
        Territory bulgaria = territory("Bulgaria Romania", m_data);
        move(bulgaria.getUnits().getUnits(), new Route(bulgaria, ukraine));
        move(poland.getUnits().getMatches(Matches.UnitCanBlitz), new Route(poland, eastPoland, ukraine));
        MustFightBattle battle = (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(ukraine, false);
        assertTrue(battle.getAttackerRetreatTerritories().contains(eastPoland));
    }

    public void testMechanizedInfantry() {
        PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
        ITestDelegateBridge delegateBridge = getDelegateBridge(germans(m_data));
        TechTracker.addAdvance(germans, m_data, delegateBridge, TechAdvance.MECHANIZED_INFANTRY);
        MoveDelegate moveDelegate = moveDelegate(m_data);
        delegateBridge.setStepName("CombatMove");
        moveDelegate.start(delegateBridge, m_data);
        Territory poland = territory("Poland", m_data);
        Territory eastPoland = territory("East Poland", m_data);
        Territory belorussia = territory("Belorussia", m_data);
        UnitType infantryType = m_data.getUnitTypeList().getUnitType("infantry");
        removeFrom(eastPoland, eastPoland.getUnits().getUnits());
        Integer preCountIntPoland = poland.getUnits().size();
        Integer preCountIntBelorussia = belorussia.getUnits().size();
        Collection<Unit> moveUnits = poland.getUnits().getUnits(infantryType, 3);
        moveUnits.addAll(poland.getUnits().getMatches(Matches.UnitCanBlitz));
        String errorResults = moveDelegate.move(moveUnits, new Route(poland, eastPoland, belorussia));
        assertError(errorResults);
        moveUnits.clear();
        moveUnits.addAll(poland.getUnits().getUnits(infantryType, 2));
        moveUnits.addAll(poland.getUnits().getMatches(Matches.UnitCanBlitz));
        String validResults = moveDelegate.move(moveUnits, new Route(poland, eastPoland, belorussia));
        assertValid(validResults);
        Integer postCountIntPoland = poland.getUnits().size() + 4;
        Integer postCountIntBelorussia = belorussia.getUnits().size() - 4;
        assertEquals(preCountIntPoland, postCountIntPoland);
        assertEquals(preCountIntBelorussia, postCountIntBelorussia);
    }

    public void testJetPower() {
        PlayerID germans = m_data.getPlayerList().getPlayerID("Germans");
        ITestDelegateBridge delegateBridge = getDelegateBridge(germans(m_data));
        TechTracker.addAdvance(germans, m_data, delegateBridge, TechAdvance.JET_POWER);
        Territory poland = territory("Poland", m_data);
        Territory eastPoland = territory("East Poland", m_data);
        UnitType fighterType = m_data.getUnitTypeList().getUnitType("fighter");
        delegateBridge.setStepName("germanBattle");
        while (!m_data.getSequence().getStep().getName().equals("germanBattle")) {
            m_data.getSequence().next();
        }
        List<Unit> germanFighter = (List<Unit>) poland.getUnits().getUnits(fighterType, 1);
        delegateBridge.setRandomSource(new ScriptedRandomSource(new int[] { 3 }));
        DiceRoll roll1 = DiceRoll.rollDice(germanFighter, false, germans, delegateBridge, m_data, new MockBattle(eastPoland), "");
        assertEquals(1, roll1.getHits());
        delegateBridge.setRandomSource(new ScriptedRandomSource(new int[] { 4 }));
        DiceRoll roll2 = DiceRoll.rollDice(germanFighter, true, germans, delegateBridge, m_data, new MockBattle(eastPoland), "");
        assertEquals(0, roll2.getHits());
    }

    public void testBidPlace() {
        ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
        bridge.setStepName("placeBid");
        bidPlaceDelegate(m_data).start(bridge, m_data);
        addTo(british(m_data), infantry(m_data).create(20, british(m_data)));
        Territory uk = territory("United Kingdom", m_data);
        Collection<Unit> units = british(m_data).getUnits().getUnits();
        PlaceableUnits placeable = bidPlaceDelegate(m_data).getPlaceableUnits(units, uk);
        assertEquals(20, placeable.getMaxUnits());
        assertNull(placeable.getErrorMessage());
        String error = bidPlaceDelegate(m_data).placeUnits(units, uk);
        assertNull(error);
    }

    public void testFactoryPlace() throws Exception {
        URL url = this.getClass().getResource("DelegateTest.xml");
        InputStream input = url.openStream();
        m_data = (new GameParser()).parse(input);
        input.close();
        PlayerID british = m_data.getPlayerList().getPlayerID("British");
        ITestDelegateBridge delegateBridge = getDelegateBridge(british(m_data));
        Territory egypt = territory("Anglo Sudan Egypt", m_data);
        UnitType factoryType = m_data.getUnitTypeList().getUnitType("factory");
        PlaceDelegate placeDelegate = placeDelegate(m_data);
        delegateBridge.setStepName("Place");
        delegateBridge.setPlayerID(british);
        placeDelegate.start(delegateBridge, m_data);
        IntegerMap<UnitType> map = new IntegerMap<UnitType>();
        map.add(factoryType, 1);
        String response = placeDelegate.placeUnits(getUnits(map, british), egypt);
        assertValid(response);
        TerritoryAttachment ta = TerritoryAttachment.get(egypt);
        assertEquals(ta.getUnitProduction(), ta.getProduction());
    }

    public void testMoveUnitsThroughSubs() {
        ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
        bridge.setStepName("nonCombatMove");
        moveDelegate(m_data).start(bridge, m_data);
        Territory sz6 = territory("6 Sea Zone", m_data);
        Route route = new Route(sz6, territory("7 Sea Zone", m_data), territory("8 Sea Zone", m_data));
        String error = moveDelegate(m_data).move(sz6.getUnits().getUnits(), route);
        assertNull(error, error);
    }

    public void testMoveUnitsThroughTransports() {
        ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
        bridge.setStepName("nonCombatMove");
        moveDelegate(m_data).start(bridge, m_data);
        Territory sz12 = territory("12 Sea Zone", m_data);
        Route route = new Route(sz12, territory("13 Sea Zone", m_data), territory("14 Sea Zone", m_data));
        String error = moveDelegate(m_data).move(sz12.getUnits().getUnits(), route);
        assertNull(error, error);
    }

    public void testLoadThroughSubs() {
        ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
        bridge.setStepName("nonCombatMove");
        MoveDelegate moveDelegate = moveDelegate(m_data);
        moveDelegate.start(bridge, m_data);
        Territory sz8 = territory("8 Sea Zone", m_data);
        Territory sz7 = territory("7 Sea Zone", m_data);
        Territory sz6 = territory("6 Sea Zone", m_data);
        Territory uk = territory("United Kingdom", m_data);
        addTo(sz8, transports(m_data).create(1, british(m_data)));
        assertValid(moveDelegate.move(sz8.getUnits().getUnits(), new Route(sz8, sz7)));
        load(uk.getUnits().getMatches(Matches.UnitIsInfantry), new Route(uk, sz7));
        assertValid(moveDelegate.move(sz7.getUnits().getMatches(Matches.unitOwnedBy(british(m_data))), new Route(sz7, sz6)));
    }

    public void testAttackUndoAndAttackAgain() {
        MoveDelegate move = moveDelegate(m_data);
        ITestDelegateBridge bridge = getDelegateBridge(italians(m_data));
        bridge.setStepName("CombatMove");
        move.start(bridge, m_data);
        Territory sz14 = territory("14 Sea Zone", m_data);
        Territory sz13 = territory("13 Sea Zone", m_data);
        Territory sz12 = territory("12 Sea Zone", m_data);
        Route r = new Route(sz14, sz13, sz12);
        move(sz14.getUnits().getMatches(Matches.UnitIsTwoHit), r);
        move(sz14.getUnits().getMatches(Matches.UnitIsNotTransport), r);
        move.undoMove(1);
        move(sz14.getUnits().getMatches(Matches.UnitIsNotTransport), r);
        MustFightBattle mfb = (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(sz12, false);
        assertEquals(3, mfb.getAttackingUnits().size());
    }

    public void testAttackSubsOnSubs() {
        String defender = "Germans";
        String attacker = "British";
        Territory attacked = territory("31 Sea Zone", m_data);
        Territory from = territory("32 Sea Zone", m_data);
        addTo(from, submarine(m_data).create(1, british(m_data)));
        addTo(attacked, submarine(m_data).create(1, germans(m_data)));
        ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
        bridge.setStepName("CombatMove");
        moveDelegate(m_data).start(bridge, m_data);
        move(from.getUnits().getUnits(), new Route(from, attacked));
        moveDelegate(m_data).end();
        MustFightBattle battle = (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(attacked, false);
        List<String> steps = battle.determineStepStrings(true, bridge);
        assertEquals(Arrays.asList(defender + SUBS_SUBMERGE, attacker + SUBS_SUBMERGE, attacker + SUBS_FIRE, defender + SELECT_SUB_CASUALTIES, defender + SUBS_FIRE, attacker + SELECT_SUB_CASUALTIES, REMOVE_SNEAK_ATTACK_CASUALTIES, REMOVE_CASUALTIES, attacker + ATTACKER_WITHDRAW).toString(), steps.toString());
        bridge.setRemote(new DummyTripleAPlayer());
        ScriptedRandomSource randomSource = new ScriptedRandomSource(0, 0, ScriptedRandomSource.ERROR);
        bridge.setRandomSource(randomSource);
        battle.fight(bridge);
        assertEquals(2, randomSource.getTotalRolled());
        assertTrue(attacked.getUnits().isEmpty());
    }

    public void testAttackSubsOnDestroyer() {
        String defender = "Germans";
        String attacker = "British";
        Territory attacked = territory("31 Sea Zone", m_data);
        Territory from = territory("32 Sea Zone", m_data);
        addTo(from, submarine(m_data).create(1, british(m_data)));
        addTo(attacked, submarine(m_data).create(1, germans(m_data)));
        addTo(attacked, destroyer(m_data).create(1, germans(m_data)));
        ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
        bridge.setStepName("CombatMove");
        moveDelegate(m_data).start(bridge, m_data);
        move(from.getUnits().getUnits(), new Route(from, attacked));
        moveDelegate(m_data).end();
        MustFightBattle battle = (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(attacked, false);
        List<String> steps = battle.determineStepStrings(true, bridge);
        assertEquals(Arrays.asList(defender + SUBS_SUBMERGE, defender + SUBS_FIRE, attacker + SELECT_SUB_CASUALTIES, REMOVE_SNEAK_ATTACK_CASUALTIES, attacker + SUBS_FIRE, defender + SELECT_SUB_CASUALTIES, defender + FIRE, attacker + SELECT_CASUALTIES, REMOVE_CASUALTIES, attacker + ATTACKER_WITHDRAW).toString(), steps.toString());
        bridge.setRemote(new DummyTripleAPlayer());
        ScriptedRandomSource randomSource = new ScriptedRandomSource(0, ScriptedRandomSource.ERROR);
        bridge.setRandomSource(randomSource);
        battle.fight(bridge);
        assertEquals(1, randomSource.getTotalRolled());
        assertTrue(attacked.getUnits().getMatches(Matches.unitIsOwnedBy(british(m_data))).isEmpty());
        assertEquals(2, attacked.getUnits().size());
    }

    public void testAttackDestroyerAndSubsAgainstSub() {
        String defender = "Germans";
        String attacker = "British";
        Territory attacked = territory("31 Sea Zone", m_data);
        Territory from = territory("32 Sea Zone", m_data);
        addTo(from, submarine(m_data).create(1, british(m_data)));
        addTo(from, destroyer(m_data).create(1, british(m_data)));
        addTo(attacked, submarine(m_data).create(1, germans(m_data)));
        ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
        bridge.setStepName("CombatMove");
        moveDelegate(m_data).start(bridge, m_data);
        move(from.getUnits().getUnits(), new Route(from, attacked));
        moveDelegate(m_data).end();
        MustFightBattle battle = (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(attacked, false);
        List<String> steps = battle.determineStepStrings(true, bridge);
        assertEquals(Arrays.asList(attacker + SUBS_SUBMERGE, attacker + SUBS_FIRE, defender + SELECT_SUB_CASUALTIES, REMOVE_SNEAK_ATTACK_CASUALTIES, defender + SUBS_FIRE, attacker + SELECT_SUB_CASUALTIES, attacker + FIRE, defender + SELECT_CASUALTIES, REMOVE_CASUALTIES, attacker + ATTACKER_WITHDRAW).toString(), steps.toString());
        bridge.setRemote(new DummyTripleAPlayer());
        ScriptedRandomSource randomSource = new ScriptedRandomSource(0, ScriptedRandomSource.ERROR);
        bridge.setRandomSource(randomSource);
        battle.fight(bridge);
        assertEquals(1, randomSource.getTotalRolled());
        assertTrue(attacked.getUnits().getMatches(Matches.unitIsOwnedBy(germans(m_data))).isEmpty());
        assertEquals(2, attacked.getUnits().size());
    }

    public void testAttackDestroyerAndSubsAgainstSubAndDestroyer() {
        String defender = "Germans";
        String attacker = "British";
        Territory attacked = territory("31 Sea Zone", m_data);
        Territory from = territory("32 Sea Zone", m_data);
        addTo(from, submarine(m_data).create(1, british(m_data)));
        addTo(from, destroyer(m_data).create(1, british(m_data)));
        addTo(attacked, submarine(m_data).create(1, germans(m_data)));
        addTo(attacked, destroyer(m_data).create(1, germans(m_data)));
        ITestDelegateBridge bridge = getDelegateBridge(british(m_data));
        bridge.setStepName("CombatMove");
        moveDelegate(m_data).start(bridge, m_data);
        move(from.getUnits().getUnits(), new Route(from, attacked));
        moveDelegate(m_data).end();
        MustFightBattle battle = (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(attacked, false);
        List<String> steps = battle.determineStepStrings(true, bridge);
        assertEquals(Arrays.asList(attacker + SUBS_FIRE, defender + SELECT_SUB_CASUALTIES, defender + SUBS_FIRE, attacker + SELECT_SUB_CASUALTIES, attacker + FIRE, defender + SELECT_CASUALTIES, defender + FIRE, attacker + SELECT_CASUALTIES, REMOVE_CASUALTIES, attacker + ATTACKER_WITHDRAW).toString(), steps.toString());
        bridge.setRemote(new DummyTripleAPlayer() {

            @Override
            public CasualtyDetails selectCasualties(Collection<Unit> selectFrom, Map<Unit, Collection<Unit>> dependents, int count, String message, DiceRoll dice, PlayerID hit, List<Unit> defaultCasualties, GUID battleID) {
                return new CasualtyDetails(Arrays.asList(selectFrom.iterator().next()), Collections.<Unit>emptyList(), false);
            }
        });
        ScriptedRandomSource randomSource = new ScriptedRandomSource(0, 0, 0, 0, ScriptedRandomSource.ERROR);
        bridge.setRandomSource(randomSource);
        battle.fight(bridge);
        assertEquals(4, randomSource.getTotalRolled());
        assertEquals(0, attacked.getUnits().size());
    }

    public void testLimitBombardtoNumberOfUnloaded() {
        MoveDelegate move = moveDelegate(m_data);
        ITestDelegateBridge bridge = getDelegateBridge(italians(m_data));
        bridge.setRemote(new DummyTripleAPlayer() {

            @Override
            public boolean selectShoreBombard(Territory unitTerritory) {
                return true;
            }
        });
        bridge.setStepName("CombatMove");
        move.start(bridge, m_data);
        Territory sz14 = territory("14 Sea Zone", m_data);
        Territory sz15 = territory("15 Sea Zone", m_data);
        Territory eg = territory("Egypt", m_data);
        Territory li = territory("Libya", m_data);
        Territory balkans = territory("Balkans", m_data);
        load(balkans.getUnits().getMatches(Matches.UnitIsInfantry), new Route(balkans, sz14));
        move(sz14.getUnits().getUnits(), new Route(sz14, sz15));
        move(li.getUnits().getMatches(Matches.unitOwnedBy(italians(m_data))), new Route(li, eg));
        move(sz15.getUnits().getMatches(Matches.UnitIsLand), new Route(sz15, eg));
        move.end();
        battleDelegate(m_data).start(bridge, m_data);
        MustFightBattle mfb = (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(eg, false);
        assertEquals(2, mfb.getBombardingUnits().size());
    }

    public void testAmphAttackUndoAndAttackAgainBombard() {
        MoveDelegate move = moveDelegate(m_data);
        ITestDelegateBridge bridge = getDelegateBridge(italians(m_data));
        bridge.setRemote(new DummyTripleAPlayer() {

            @Override
            public boolean selectShoreBombard(Territory unitTerritory) {
                return true;
            }
        });
        bridge.setStepName("CombatMove");
        move.start(bridge, m_data);
        Territory sz14 = territory("14 Sea Zone", m_data);
        Territory sz15 = territory("15 Sea Zone", m_data);
        Territory eg = territory("Egypt", m_data);
        Territory li = territory("Libya", m_data);
        Territory balkans = territory("Balkans", m_data);
        load(balkans.getUnits().getMatches(Matches.UnitIsInfantry), new Route(balkans, sz14));
        move(sz14.getUnits().getUnits(), new Route(sz14, sz15));
        move(li.getUnits().getMatches(Matches.unitOwnedBy(italians(m_data))), new Route(li, eg));
        move(sz15.getUnits().getMatches(Matches.UnitIsLand), new Route(sz15, eg));
        move.undoMove(move.getMovesMade().size() - 1);
        move(sz15.getUnits().getMatches(Matches.UnitIsLand), new Route(sz15, eg));
        move.end();
        battleDelegate(m_data).start(bridge, m_data);
        MustFightBattle mfb = (MustFightBattle) MoveDelegate.getBattleTracker(m_data).getPendingBattle(eg, false);
        assertEquals(2, mfb.getBombardingUnits().size());
    }

    private Collection<Unit> getUnits(IntegerMap<UnitType> units, PlayerID from) {
        Iterator<UnitType> iter = units.keySet().iterator();
        Collection rVal = new ArrayList(units.totalValues());
        while (iter.hasNext()) {
            UnitType type = iter.next();
            rVal.addAll(from.getUnits().getUnits(type, units.getInt(type)));
        }
        return rVal;
    }

    public void assertValid(String string) {
        assertNull(string, string);
    }

    public void assertError(String string) {
        assertNotNull(string, string);
    }
}
