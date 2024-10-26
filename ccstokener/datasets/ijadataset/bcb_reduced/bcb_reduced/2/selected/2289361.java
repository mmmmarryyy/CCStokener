package uk.ac.ebi.rhea.find;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.log4j.Logger;
import uk.ac.ebi.rhea.domain.Direction;
import uk.ac.ebi.rhea.domain.Reaction;
import uk.ac.ebi.rhea.domain.ReactionChange;
import uk.ac.ebi.rhea.domain.Status;
import uk.ac.ebi.rhea.mapper.IRheaHistoryReader;
import uk.ac.ebi.rhea.mapper.IRheaReader;
import uk.ac.ebi.rhea.mapper.util.IChebiHelper;
import uk.ac.ebi.rhea.util.CTFileWriter;
import uk.ac.ebi.rhea.util.ReactionUtil;

/**
 * Finder class to get information related to a reaction ID.
 * Two bean properties have to be set before calling any of the
 * <code>find...</code> methods:
 * <ul>
 * 	<li><code>id</code>: the reaction id</li>
 * 	<li><code>rheaReader</code>: the object which retrieves the information,
 *      actually.</li>
 * </ul>
 * @author rafalcan
 *
 */
public class ReactionFinder {

    private static Logger LOGGER = Logger.getLogger(ReactionFinder.class);

    private Long reactionId;

    /**
	 * <code>null</code> by default after setting the <code>reactionId</code>.
	 */
    private Direction reactionDir;

    private Long participantId;

    private IRheaReader rheaReader;

    private IRheaHistoryReader rheaHistoryReader;

    /**
     * Either this or <code>rxnUrl</code> is required in order to call the
     * @{@link #findRxnFile()} method.
     */
    private CTFileWriter ctFileWriter;

    /**
     * String representing the format of a URL where a RXN file can be found.
     * Either this or <code>ctFileWriter</code> is required in order to call the
     * {@link #findRxnFile()} method.
     */
    private String rxnUrl;

    private Reaction reaction;

    private Long unId;

    private Collection<Reaction> parentReactions;

    private Collection<Long> participatedReactions;

    private Map<Long, String> relatedEnzymes;

    private EnumMap<Direction, Long> relatedReactions;

    private Collection<Reaction> otherDirections;

    private String rxnFile;

    private Date lastModified;

    public IRheaReader getRheaReader() {
        return rheaReader;
    }

    /**
     * @deprecated ReactionUtil is not used any more for building RXN files.
     *      CTFileWriter is used instead.
     * @return
     */
    public ReactionUtil getReactionUtil() {
        return null;
    }

    public void setReactionId(Long id) {
        if (id.equals(this.reactionId)) return;
        this.reactionId = id;
        this.reactionDir = null;
        reset();
    }

    /**
	 * Sets the reaction ID as a String.
	 * @param id a reaction ID, which can be a directional one (see
	 * 		{@link Reaction#getDirId()}).
	 * @throws IllegalArgumentException if the ID is not valid.
	 */
    public void setReactionId(String id) {
        Pattern p = Pattern.compile("^(\\d+)(_(BI|RL|LR))?$");
        Matcher m = p.matcher(id);
        if (m.find()) {
            final Long rId = Long.valueOf(m.group(1));
            Direction rDir = m.group(3) == null ? null : Direction.valueOf(m.group(3));
            setReactionId(rId);
            setReactionDirection(rDir);
            reset();
        } else {
            throw new IllegalArgumentException("Illegal reaction ID: " + id);
        }
    }

    /**
	 * Resets previous findings.
	 */
    private void reset() {
        reaction = null;
        unId = null;
        parentReactions = null;
        relatedEnzymes = null;
        relatedReactions = null;
        otherDirections = null;
        rxnFile = null;
        lastModified = null;
    }

    public void setReactionDirection(Direction dir) {
        this.reactionDir = dir;
    }

    public void setParticipantId(Long participantId) {
        this.participantId = participantId;
        participatedReactions = null;
    }

    /**
     * Typically called just once, at initialisation time.
     * @param rheaReader
     */
    public void setRheaReader(IRheaReader rheaReader) {
        this.rheaReader = rheaReader;
    }

    /**
     * Sets a <code>ReactionUtil</code> required to get RXN files. This method
     * also sets the IRheaReader encapsulated in ReactionUtil.<br/>
     * Typically called just once, at initialisation time.<br/>
     * Either this method or {@link #setRxnUrl} must be called in order to call
     * {@link #getRxnFile()} successfully.
     * @param reactionUtil
     * @deprecated use ctFileWriter instead!
     */
    public void setReactionUtil(ReactionUtil reactionUtil) {
    }

    /**
     * Use either this or #setChebiHelper if you need an RXN file.
     * @param ctFileWriter a writer of RXN files.
     */
    public void setCtFileWriter(CTFileWriter ctFileWriter) {
        this.ctFileWriter = ctFileWriter;
    }

    /**
     * Use either this or #setCtFileWriter if you need an RXN file.
     * @param chebiHelper a helper to retrieve MOL files from ChEBI, used to
     *      build RXN files.
     */
    public void setChebiHelper(IChebiHelper chebiHelper) {
        this.ctFileWriter = new CTFileWriter(chebiHelper, true, false, false);
    }

    /**
     * Sets the URL format used to retrieve RXN files.<br/>
     * Typically called just once, at initialisation time.<br/>
     * Either this method or {@link #setReactionUtil} must be called in order to
     * call {@link #getRxnFile()} successfully.
     * @param rxnUrl A URL as string. It can contain the placeholder {0} which
     *      will be substituted by the value of <code>reactionId</code>.
     */
    public void setRxnUrl(String rxnUrl) {
        this.rxnUrl = rxnUrl;
    }

    public void setRheaHistoryReader(IRheaHistoryReader rheaHistoryReader) {
        this.rheaHistoryReader = rheaHistoryReader;
    }

    public Reaction find() throws Exception {
        LOGGER.debug("Getting information for reaction ID " + reactionId);
        StringBuilder id = new StringBuilder(reactionId.toString());
        Reaction r = null;
        if (reactionDir == null) {
            r = rheaReader.findByReactionId(reactionId);
        } else {
            id.append('_').append(reactionDir.getCode());
            r = rheaReader.findByReactionId(id.toString());
        }
        if (r == null) {
            LOGGER.warn("The reaction with ID " + reactionId + " does not exist in the database");
        }
        this.reaction = r;
        return r;
    }

    public Long findUnId() throws Exception {
        EnumMap<Direction, Long> related = rheaReader.findAllRelatedReactions(reactionId);
        Long masterId = related.get(Direction.UN);
        if (reactionId.equals(masterId)) {
            LOGGER.info("UN reaction: ID " + reactionId);
        } else {
            LOGGER.warn("Not a UN reaction: ID " + reactionId);
            LOGGER.warn("Getting its UN: ID " + masterId);
        }
        this.unId = masterId;
        return masterId;
    }

    public Collection<Reaction> findParentReactions() throws Exception {
        LOGGER.debug("Getting parent reactions for reaction ID " + reactionId);
        Collection<Reaction> parents = rheaReader.findParentReactions(reactionId);
        LOGGER.debug("Got them!");
        this.parentReactions = parents;
        return parents;
    }

    public Collection<Long> findParticipatedReactions() throws Exception {
        Collection<Long> participated = rheaReader.findParticipatedReactions(participantId);
        this.participatedReactions = participated;
        return participated;
    }

    public Map<Long, String> findRelatedEnzymes() throws Exception {
        LOGGER.debug("Getting enzymes related to reaction ID " + reactionId);
        Map<Long, String> enzymes = rheaReader.findRelatedEnzymes(reactionId);
        LOGGER.debug("Got them!");
        this.relatedEnzymes = enzymes;
        return enzymes;
    }

    public EnumMap<Direction, Long> findRelatedReactions() throws Exception {
        return findRelatedReactions(false);
    }

    public EnumMap<Direction, Long> findAllRelatedReactions() throws Exception {
        return findRelatedReactions(true);
    }

    public EnumMap<Direction, Long> findRelatedReactions(boolean all) throws Exception {
        LOGGER.debug("Getting reactions related to reaction ID " + reactionId);
        EnumMap<Direction, Long> related = all ? rheaReader.findAllRelatedReactions(reactionId) : rheaReader.findRelatedReactions(reactionId);
        LOGGER.debug("Got them!");
        return this.relatedReactions = related;
    }

    /**
	 * Find other related reactions with any of the given statuses.
     * @param sts searched statuses. If <code>null</code>, every status is accepted.
     * @return
     * @throws Exception
	 */
    public Collection<Reaction> findOtherDirections(EnumSet<Status> sts) throws Exception {
        EnumMap<Direction, Long> relatedMap = findRelatedReactions();
        if (relatedMap == null) return null;
        Collection<Reaction> related = null;
        for (Long id : relatedMap.values()) {
            Reaction r = rheaReader.findByReactionId(id, false);
            if (sts != null && !sts.contains(r.getStatus())) continue;
            if (related == null) related = new ArrayList<Reaction>();
            related.add(r);
        }
        return otherDirections = related;
    }

    /**
     * Retrieves a RXN file corresponding to the found reaction.
     * The strategy used follows this preference order:
     * <ol>
     *  <li>get existing file by URL. This requires <code>rxnUrl</code> to be
     *      set.</li>
     *  <li>get by building it from scratch. Requires <code>reactionUtil</code>
     *      to be set. This provides a fresh representation of the reaction (in
     *      case the RXN file does not exist yet, or the reaction has changed).
     * @return A RXN file as String.
     * @throws IllegalStateException If the ReactionFinder is not configured
     *      with a CTFileWriter or RXN URL.
     * @throws MalformedURLException If the configured URL is wrong.
     * @throws IOException If there is a problem accessing the file at URL.
     * @throws Exception If there is a problem building the RXN file.
     */
    public String findRxnFile() throws IllegalStateException, MalformedURLException, IOException, Exception {
        rxnFile = null;
        if (rxnUrl != null) {
            findRxnFileByUrl();
        } else if (ctFileWriter != null) {
            buildRxnFile();
        } else {
            LOGGER.error("ReactionFinder not properly configured");
            throw new IllegalStateException("ReactionFinder not properly configured");
        }
        return rxnFile;
    }

    private void findRxnFileByUrl() throws MalformedURLException, IOException {
        URL url = new URL(MessageFormat.format(rxnUrl, reactionId.toString()));
        LOGGER.debug("Retrieving RXN file by URL " + url);
        URLConnection con = url.openConnection(java.net.Proxy.NO_PROXY);
        con.connect();
        InputStream is = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        try {
            is = con.getInputStream();
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);
            String line = null;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            rxnFile = sb.toString();
        } catch (IOException e) {
            LOGGER.warn("Unable to retrieve RXN", e);
        } finally {
            if (br != null) {
                br.close();
            }
            if (isr != null) {
                isr.close();
            }
            if (is != null) {
                is.close();
            }
        }
    }

    private void buildRxnFile() throws Exception {
        if (reaction == null || reaction.getId() != reactionId) {
            find();
        }
        LOGGER.debug("Building RXN file from scratch");
        rxnFile = ctFileWriter.getRxnFile(reaction, null);
    }

    public Date findLastModified() throws Exception {
        if (rheaHistoryReader == null) {
            lastModified = null;
        } else {
            lastModified = rheaHistoryReader.getLastModified(reactionId);
        }
        return lastModified;
    }

    public Long getReactionId() {
        return reactionId;
    }

    public Direction getReactionDir() {
        return reactionDir;
    }

    public Reaction getReaction() {
        return reaction;
    }

    public Long getUnId() {
        return unId;
    }

    public Collection<Reaction> getParentReactions() {
        return parentReactions;
    }

    public Collection<Long> getParticipatedReactions() {
        return participatedReactions;
    }

    public Map<Long, String> getRelatedEnzymes() {
        return relatedEnzymes;
    }

    public EnumMap<Direction, Long> getRelatedReactions() {
        return relatedReactions;
    }

    public Collection<Reaction> getOtherDirections() {
        return otherDirections;
    }

    public String getRxnFile() {
        return rxnFile;
    }

    public Date getLastModified() {
        return lastModified;
    }
}
