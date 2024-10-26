package collab.fm.server.stats.reporter;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import org.apache.log4j.Logger;
import collab.fm.server.bean.entity.*;
import collab.fm.server.stats.StatsUtil;
import collab.fm.server.util.DaoUtil;
import collab.fm.server.util.Resources;
import collab.fm.server.util.exception.BeanPersistenceException;
import collab.fm.server.util.exception.StaleDataException;

/**
 * Report feature model details. (See TEMPLATEs.)
 * @author mark
 *
 */
public class ModelReporter implements Reporter {

    private static Logger logger = Logger.getLogger(ModelReporter.class);

    private static final String TEMPLATE_INTRO = "[Feature Model (ID = $id)]" + NL + "Model name: $name" + NL + "=== Elements Overview ===" + NL + "Number of features: $nf" + NL + "Number of relationships: total $nrt refine $nrf require $nrq exclude $nre" + NL + "Number of feature names: total $fnt avg(#/feature) $fna lowest $fnlo highest $fnhi" + NL + "Number of feature descriptions: total $fdt avg(#/feature) $fda lowest $fdlo highest $fdhi" + NL + "=== Contributions Overview ===" + NL + "Number of contributors: $nc" + NL + "Number of creations: total $ct avg(#/person) $ca lowest $clo highest $chi" + NL + "Number of YES votes: total $yt avg(#/person) $ya lowest $ylo highest $yhi" + NL + "Number of NO votes: total $nvt avg(#/person) $nva lowest $nvlo highest $nvhi" + NL + "=== Support/Oppose Overview ===" + NL + "Supporters of a feature: avg $sfa lowest $sflo highest $sfhi" + NL + "Opponents of a feature: avg $ofa lowest $oflo highest $ofhi" + NL + "Supporters of a relationship: avg $sra lowest $srlo highest $srhi" + NL + "Opponents of a relationship: avg $ora lowest $orlo highest $orhi" + NL;

    private static final String TEMPLATE_MODEL_NAME = "$value ($yes:$no)";

    private static final String TEMPLATE_USER_STATS = "%-5s %-12s %-4d %-4d %-6.2f %-6.2f %-6.2f %-6.2f %-6.2f %-6.2f";

    private static final String USER_STATS_TITLE = String.format("%-28s %-20s %-20s %n" + "%-5s %-12s %-4s %-4s %-6s %-6s %-6s %-6s %-6s %-6s %n" + "%-54s", " ", "Support(Create)", "Support(Vote)", "U_ID", "User Name", "C#", "V#", "avg", "min", "max", "avg", "min", "max", "---------------------------------------------------------------------------");

    private static final String TEMPLATE_FEATURE_DETAILS = "%-5s %-12s %-30s %-5s %-5d %-6.2f";

    private static final String FEATURE_DETAILS_HEADER = String.format("%-5s %-12s %-30s %-5s %-5s %-6s %n" + "%-70s", "F_ID", "Attr", "Value", "V_ID", "CrtBy", "Suppport", "-----------------------------------------------------------------------");

    private static final String TEMPLATE_REL_DETAILS = "%-5s %-10s %-15s %-15s %-5s %-6.2f";

    private static final String REL_DETAILS_HEADER = String.format("%-5s %-10s %-15s %-15s %-5s %-6s %n" + "%-70s", "R_ID", "Type", "Left F", "Right F", "CrtBy", "Support", "-----------------------------------------------------------------------");

    private String intro;

    private StringBuilder featureDetails = new StringBuilder();

    private StringBuilder relationDetails = new StringBuilder();

    public static final String CFG_FILE_NAME = "reporter.properties";

    public static final String KEY_SHOW_LAST = "ModelReporter.showlast";

    public static final String KEY_TARGETS = "ModelReporter.targets";

    public void report() {
        List<String> targets = getReportedModelIDs();
        try {
            if (targets == null) {
                List<Model> models = DaoUtil.getModelDao().getAll();
                if (models != null) {
                    for (Model m : models) {
                        reportModel(m);
                        logger.info(NL);
                    }
                }
            } else {
                for (String s : targets) {
                    Model m = DaoUtil.getModelDao().getById(Long.valueOf(s), false);
                    if (m != null) {
                        reportModel(m);
                        logger.info(NL);
                    }
                }
            }
        } catch (BeanPersistenceException e) {
            logger.error("Reporter error.", e);
        } catch (StaleDataException e) {
            logger.error("Stale data error.", e);
        }
    }

    protected List<String> getReportedModelIDs() {
        List<String> targets = null;
        Properties cfg = new Properties();
        try {
            URL url = this.getClass().getClassLoader().getResource(CFG_FILE_NAME);
            cfg.load(url.openStream());
            String ts = cfg.getProperty(KEY_TARGETS);
            if (ts != null) {
                targets = Arrays.asList(cfg.getProperty(KEY_TARGETS).split(","));
            } else {
                String last = cfg.getProperty(KEY_SHOW_LAST);
                if (last == null) {
                    return null;
                }
                int n = Integer.valueOf(last).intValue();
                try {
                    List<Model> models = DaoUtil.getModelDao().getAll();
                    int sz = (models == null ? 0 : models.size());
                    if (sz > 0) {
                        targets = new ArrayList<String>(sz);
                        for (int i = 0; i < n; i++) {
                            targets.add(0, String.valueOf(sz - i));
                        }
                    }
                } catch (BeanPersistenceException e) {
                    e.printStackTrace();
                } catch (StaleDataException e) {
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return targets;
    }

    @SuppressWarnings("unchecked")
    protected void reportModel(Model m) throws BeanPersistenceException, StaleDataException {
        String rslt = TEMPLATE_INTRO.replaceFirst("\\$id", m.getId().toString());
        Set<ModelName> names = (Set<ModelName>) m.getNames();
        StringBuilder strNames = new StringBuilder();
        for (ModelName name : names) {
            strNames.append(TEMPLATE_MODEL_NAME.replaceFirst("\\$value", name.getName()).replaceFirst("\\$yes", String.valueOf(name.getSupporterNum())).replaceFirst("\\$no", String.valueOf(name.getOpponentNum())) + "  ");
        }
        rslt = rslt.replaceFirst("\\$name", strNames.toString());
        Map<Long, UserStats> userStatsMap = new HashMap<Long, UserStats>();
        IntCounter fYes = new IntCounter(), fNo = new IntCounter();
        IntCounter rYes = new IntCounter(), rNo = new IntCounter();
        List<Feature> features = DaoUtil.getFeatureDao().getAll(m.getId());
        rslt = rslt.replaceFirst("\\$nf", StatsUtil.nullSafeSize(features));
        List<Relationship> relations = DaoUtil.getRelationshipDao().getAll(m.getId());
        rslt = rslt.replaceFirst("\\$nrt", StatsUtil.nullSafeSize(relations));
        List<BinaryRelationship> refines = null, requires = null, excludes = null;
        if (relations != null) {
            refines = new ArrayList<BinaryRelationship>();
            requires = new ArrayList<BinaryRelationship>();
            excludes = new ArrayList<BinaryRelationship>();
            for (Relationship r : relations) {
                addUserStats(userStatsMap, r);
                rYes.count(r.getSupporterNum());
                rNo.count(r.getOpponentNum());
                if (Resources.BIN_REL_REFINES.equals(r.getType())) {
                    refines.add((BinaryRelationship) r);
                } else if (Resources.BIN_REL_REQUIRES.equals(r.getType())) {
                    requires.add((BinaryRelationship) r);
                } else if (Resources.BIN_REL_EXCLUDES.equals(r.getType())) {
                    excludes.add((BinaryRelationship) r);
                }
            }
        }
        rslt = rslt.replaceFirst("\\$nrf", StatsUtil.nullSafeSize(refines)).replaceFirst("\\$nrq", StatsUtil.nullSafeSize(requires)).replaceFirst("\\$nre", StatsUtil.nullSafeSize(excludes));
        IntCounter fnameCounter = new IntCounter(), fdCounter = new IntCounter();
        if (features != null) {
            for (Feature f : features) {
                addUserStats(userStatsMap, f);
                fYes.count(f.getSupporterNum());
                fNo.count(f.getOpponentNum());
                fnameCounter.count(f.getNames().size());
                fdCounter.count(f.getDescriptions().size());
            }
        }
        rslt = rslt.replaceFirst("\\$fnt", String.valueOf(fnameCounter.sum)).replaceFirst("\\$fna", fnameCounter.toAvg(StatsUtil.nullSafeIntSize(features))).replaceFirst("\\$fnlo", String.valueOf(fnameCounter.min)).replaceFirst("\\$fnhi", String.valueOf(fnameCounter.max)).replaceFirst("\\$fdt", String.valueOf(fdCounter.sum)).replaceFirst("\\$fda", fdCounter.toAvg(StatsUtil.nullSafeIntSize(features))).replaceFirst("\\$fdlo", String.valueOf(fdCounter.min)).replaceFirst("\\$fdhi", String.valueOf(fdCounter.max));
        List<User> users = DaoUtil.getUserDao().getAll(m.getId());
        rslt = rslt.replaceFirst("\\$nc", StatsUtil.nullSafeSize(users));
        if (features != null) {
            for (Feature f : features) {
                for (Votable v : f.getNames()) {
                    FeatureName fn = (FeatureName) v;
                    addUserStats(userStatsMap, fn);
                }
                for (Votable v : f.getDescriptions()) {
                    FeatureDescription fd = (FeatureDescription) v;
                    addUserStats(userStatsMap, fd);
                }
                OptionalityAdapter oa = new OptionalityAdapter(f.getOptionality());
                addUserStats(userStatsMap, oa);
            }
        }
        IntCounter cc = new IntCounter(), yesc = new IntCounter(), noc = new IntCounter();
        for (Map.Entry<Long, UserStats> entry : userStatsMap.entrySet()) {
            cc.count(entry.getValue().creation);
            yesc.count(entry.getValue().yes);
            noc.count(entry.getValue().no);
        }
        int uNum = StatsUtil.nullSafeIntSize(users);
        rslt = rslt.replaceFirst("\\$ct", String.valueOf(cc.sum)).replaceFirst("\\$ca", cc.toAvg(uNum)).replaceFirst("\\$clo", String.valueOf(cc.min)).replaceFirst("\\$chi", String.valueOf(cc.max)).replaceFirst("\\$yt", String.valueOf(yesc.sum)).replaceFirst("\\$ya", yesc.toAvg(uNum)).replaceFirst("\\$ylo", String.valueOf(yesc.min)).replaceFirst("\\$yhi", String.valueOf(yesc.max)).replaceFirst("\\$nvt", String.valueOf(noc.sum)).replaceFirst("\\$nva", noc.toAvg(uNum)).replaceFirst("\\$nvlo", String.valueOf(noc.min)).replaceFirst("\\$nvhi", String.valueOf(noc.max));
        rslt = rslt.replaceFirst("\\$sfa", fYes.toAvg(StatsUtil.nullSafeIntSize(features))).replaceFirst("\\$sflo", String.valueOf(fYes.min)).replaceFirst("\\$sfhi", String.valueOf(fYes.max)).replaceFirst("\\$ofa", fNo.toAvg(StatsUtil.nullSafeIntSize(features))).replaceFirst("\\$oflo", String.valueOf(fNo.min)).replaceFirst("\\$ofhi", String.valueOf(fNo.max)).replaceFirst("\\$sra", rYes.toAvg(StatsUtil.nullSafeIntSize(relations))).replaceFirst("\\$srlo", String.valueOf(rYes.min)).replaceFirst("\\$srhi", String.valueOf(rYes.max)).replaceFirst("\\$ora", rNo.toAvg(StatsUtil.nullSafeIntSize(relations))).replaceFirst("\\$orlo", String.valueOf(rNo.min)).replaceFirst("\\$orhi", String.valueOf(rNo.max));
        logger.info(rslt);
        Map<Long, String> us = new HashMap<Long, String>();
        for (User u : users) {
            us.put(u.getId(), u.getName());
        }
        reportUserStats(userStatsMap, us);
        reportFeatureDetails(features);
        reportRelationDetails(relations);
    }

    protected void reportRelationDetails(List<Relationship> list) {
        logger.info(NL + "=== Relationship details ===");
        logger.info(REL_DETAILS_HEADER);
        if (list == null) {
            return;
        }
        for (Relationship r : list) {
            reportRelation(r);
        }
    }

    protected void reportRelation(Relationship r) {
        if (Resources.BIN_REL_EXCLUDES.equals(r.getType()) || Resources.BIN_REL_REFINES.equals(r.getType()) || Resources.BIN_REL_REQUIRES.equals(r.getType())) {
            BinaryRelationship br = (BinaryRelationship) r;
            logger.info(String.format(TEMPLATE_REL_DETAILS, r.getId(), r.getType(), br.getLeftFeatureId(), br.getRightFeatureId(), r.getCreator(), StatsUtil.toPercentage(r.getSupporterNum(), r.getOpponentNum())));
        }
    }

    protected void reportFeatureDetails(List<Feature> list) {
        logger.info(NL + "=== Feature/Attribute/Value details ===");
        logger.info(FEATURE_DETAILS_HEADER);
        if (list == null) {
            return;
        }
        for (Feature f : list) {
            reportFeature(f);
        }
    }

    protected void reportFeature(Feature f) {
        logger.info(String.format(TEMPLATE_FEATURE_DETAILS, f.getId().toString(), "/", "/", "/", f.getCreator(), StatsUtil.toPercentage(f.getSupporterNum(), f.getOpponentNum())));
        logger.info(String.format(TEMPLATE_FEATURE_DETAILS, "/", "Optionality", "Mandotary", "/", f.getCreator(), StatsUtil.toPercentage(f.getOptionality().getSupporters().size(), f.getOptionality().getOpponents().size())));
        reportVotableSet(f.getNames(), "Name");
        reportVotableSet(f.getDescriptions(), "Description");
    }

    protected void reportVotableSet(Set<? extends Votable> set, String attr) {
        for (Votable v : set) {
            logger.info(String.format(TEMPLATE_FEATURE_DETAILS, "/", attr, v.toValueString(), v.getId().toString(), v.getCreator(), StatsUtil.toPercentage(v.getSupporterNum(), v.getOpponentNum())));
        }
    }

    protected void reportUserStats(Map<Long, UserStats> map, Map<Long, String> users) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== User contribution/rating details ===" + NL + USER_STATS_TITLE + NL);
        List<Map.Entry<Long, UserStats>> list = new LinkedList<Map.Entry<Long, UserStats>>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<Long, UserStats>>() {

            public int compare(Map.Entry<Long, UserStats> o1, Map.Entry<Long, UserStats> o2) {
                return -(o1.getValue().compareTo(o2.getValue()));
            }
        });
        for (Map.Entry<Long, UserStats> e : list) {
            e.getValue().calculateSupportRate();
            sb.append(String.format(TEMPLATE_USER_STATS, e.getKey(), safeGetName(users, e.getKey()), e.getValue().creation, e.getValue().yes + e.getValue().no, e.getValue().avgCreationSupport, e.getValue().minCreationSupport, e.getValue().maxCreationSupport, e.getValue().avgVoteSupport, e.getValue().minVoteSupport, e.getValue().maxVoteSupport));
            sb.append(NL);
        }
        logger.info(sb.toString());
    }

    protected String safeGetName(Map<Long, String> users, Long id) {
        String name = users.get(id);
        if (name == null) {
            return "<unknown>";
        }
        return name;
    }

    protected UserStats safeGet(Map<Long, UserStats> map, Long id) {
        UserStats c = map.get(id);
        if (c == null) {
            c = new UserStats();
            map.put(id, c);
        }
        return c;
    }

    protected void addUserStats(Map<Long, UserStats> target, Votable element) {
        addCreationStats(target, element);
        addVoteStats(target, element);
    }

    protected void addVoteStats(Map<Long, UserStats> target, Votable element) {
        for (Long id : element.getVote().getSupporters()) {
            Support vs = new Support();
            UserStats c = safeGet(target, id);
            c.yes++;
            vs.yes = element.getSupporterNum();
            vs.no = element.getOpponentNum();
            c.voteSupport.add(vs);
        }
        for (Long id : element.getVote().getOpponents()) {
            Support vs = new Support();
            UserStats c = safeGet(target, id);
            c.no++;
            vs.yes = element.getOpponentNum();
            vs.no = element.getSupporterNum();
            c.voteSupport.add(vs);
        }
    }

    protected void addCreationStats(Map<Long, UserStats> target, Votable element) {
        if (!element.hasCreator()) {
            return;
        }
        Support cs = new Support();
        UserStats con = safeGet(target, element.getCreator());
        con.creation++;
        cs.yes = element.getSupporterNum();
        cs.no = element.getOpponentNum();
        con.creationSupport.add(cs);
    }

    protected static class UserStats implements Comparable<UserStats> {

        public int creation = 0;

        public int yes = 0;

        public int no = 0;

        public List<Support> creationSupport = new ArrayList<Support>();

        public List<Support> voteSupport = new ArrayList<Support>();

        public float avgCreationSupport;

        public float minCreationSupport;

        public float maxCreationSupport;

        public float avgVoteSupport;

        public float minVoteSupport;

        public float maxVoteSupport;

        public void calculateSupportRate() {
            SupportCounter csc = calculateRate(creationSupport);
            avgCreationSupport = csc.toAvg();
            minCreationSupport = csc.min;
            maxCreationSupport = csc.max;
            SupportCounter vsc = calculateRate(voteSupport);
            avgVoteSupport = vsc.toAvg();
            minVoteSupport = vsc.min;
            maxVoteSupport = vsc.max;
        }

        private SupportCounter calculateRate(List<Support> ss) {
            SupportCounter c = new SupportCounter();
            for (Support s : ss) {
                c.count(s);
            }
            return c;
        }

        public int compareTo(UserStats o) {
            if (this.creation < o.creation) {
                return -1;
            }
            if (this.creation > o.creation) {
                return 1;
            }
            int v = this.yes + this.no - o.yes - o.no;
            if (v < 0) {
                return -1;
            }
            if (v > 0) {
                return 1;
            }
            return 0;
        }
    }

    protected static class Support {

        public int yes = 0;

        public int no = 0;

        public float rate() {
            return StatsUtil.toPercentage(yes, no);
        }
    }

    protected static class SupportCounter {

        public float min = 0.0f;

        public float max = 0.0f;

        public int yes = 0;

        public int no = 0;

        private boolean hasMeetFirst = false;

        public void count(Support s) {
            yes += s.yes;
            no += s.no;
            float rate = s.rate();
            if (!hasMeetFirst) {
                min = rate;
                max = rate;
                hasMeetFirst = true;
                return;
            }
            if (rate < min) {
                min = rate;
            } else if (rate > max) {
                max = rate;
            }
        }

        public float toAvg() {
            return StatsUtil.toPercentage(yes, no);
        }
    }

    protected static class IntCounter {

        public int min = 0;

        public int max = 0;

        public int sum = 0;

        private boolean hasMeetFirstNumber = false;

        public void count(int number) {
            sum += number;
            if (!hasMeetFirstNumber) {
                min = number;
                max = number;
                hasMeetFirstNumber = true;
                return;
            }
            if (number < min) {
                min = number;
            } else if (number > max) {
                max = number;
            }
        }

        public String toAvg(int divider) {
            return StatsUtil.zeroSafeAvg(sum, divider);
        }
    }

    protected static class OptionalityAdapter implements Votable {

        private Vote opt;

        public OptionalityAdapter(Vote opt) {
            this.opt = opt;
        }

        public Long getCreator() {
            return Votable.VOID_CREATOR;
        }

        public int getOpponentNum() {
            return opt.getOpponents().size();
        }

        public int getSupporterNum() {
            return opt.getSupporters().size();
        }

        public Vote getVote() {
            return opt;
        }

        public boolean hasCreator() {
            return false;
        }

        public void vote(boolean yes, Long userid) {
            opt.vote(yes, userid);
        }

        public void setCreator(Long id) {
        }

        public Long getId() {
            return -1L;
        }

        public String toValueString() {
            return "";
        }
    }
}
