import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.JApplet;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.sgmiller.formicidae.AntMetadata;
import org.sgmiller.formicidae.IO;
import org.sgmiller.formicidae.Simulator;
import org.sgmiller.formicidae.TickListener;
import org.sgmiller.formicidae.World;
import org.sgmiller.formicidae.gui.*;

/**
 * <P>Applet for running matches on the ant-wars website, for demonstration purposes,
 *  and for persistance of official ladder match results.  Also serves to make available
 *  casual viewing of previous ladder matches through a browser and the website links,
 *  which pass in to the applet the parameters of previous ladder matches for convenient
 *  viewing and replay.</P><BR>
 * 
 * <P>When running matches locally, particularly when developing an ant, note that one 
 *  generally instead uses the formicidae.bat / formicidae.sh scripts, which in turn
 *  make use of either Simulator or ControlPanel.  While you could adapt it or use it,
 *  if for some reason you really wanted or needed to run as an applet, this applet is 
 *  intended for the antwars website's use, not local use.</P><BR>
 * 
 * @author scgmille
 * @since 1.3, Aug 7 2004
 */
public class Formicidae extends JApplet implements TickListener, ChangeListener {

    /**
   * Users can control and manipulate the visual display of the match via a
   * control panel of widgets that allow a range of viewing and information activities.
   */
    ControlPanel controlPanel;

    /**
   * Each match is assigned a unique match identifier for later results tracking and
   * possible ladder ranking purposes.  Also referred to as the 'ladder seed'.
   */
    int matchId;

    /**
   * Each world has a unique hill identifier.
   */
    int hillId;

    /**
   * Visually tracks the progress of time through the match, as ticks go by towards
   * the eventual end of the match at a predetermined final tick round, typically
   * 100000 for a classical match, but strictly speaking, dynamically configurable.
   */
    JProgressBar progressBar;

    /**
   * Labels for the scores for the respecitive teams playing the match.
   */
    JLabel scoreLabels[];

    /**
   * A match employs a variety of models (speed model, zoom model, etcetera) during
   * play for various viewing and control uses.
   */
    Models models;

    /**
   * The panel housing the visual representation of the unfolding match.
   */
    JPanel panel;

    /**
   * Standard JApplet initialization method - loads up the relevant world and ant
   * genomes, and kicks off the simulation with suitable match parameters.
   */
    public void init() {
        try {
            int rngSeed = 1;
            try {
                this.matchId = Integer.parseInt(getParameter("ladderSeed"));
                rngSeed = this.matchId;
            } catch (NumberFormatException nf) {
                nf.printStackTrace();
            }
            URL worldURL = new URL(getParameter("worldMap"));
            World world = IO.readMap(worldURL.openStream(), new AntMetadata[] { new AntMetadata(getParameter("redName"), getParameter("redGenome")), new AntMetadata(getParameter("blackName"), getParameter("blackGenome")) }, rngSeed);
            if ("yes".equalsIgnoreCase(getParameter("visualizer"))) {
                ControlPanel.appletMode = true;
                Images.loadFromApplet(this);
                this.controlPanel = new ControlPanel(new String[0]);
                this.controlPanel.models.speed.setTicksPerSecond(100);
                this.controlPanel.newSimulation(world);
                this.controlPanel.models.simulator.addTickListener(this);
                this.models = this.controlPanel.models;
            } else {
                this.models = new Models();
                this.models.init(world);
                this.panel = new JPanel();
                this.progressBar = new JProgressBar();
                this.progressBar.setMinimum(0);
                this.progressBar.setMaximum(100000);
                this.progressBar.setStringPainted(true);
                this.panel.add(this.progressBar);
                this.scoreLabels = new JLabel[this.models.world.getPlayerCount()];
                for (int i = 0; i < this.models.world.getPlayerCount(); i++) {
                    this.scoreLabels[i] = new JLabel();
                    this.panel.add(this.scoreLabels[i]);
                }
                this.models.score.addChangeListener(this);
                this.models.simulator.addTickListener(this);
                this.models.simulator.start();
            }
            this.hillId = Integer.parseInt(getParameter("hillid"));
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return;
    }

    /**
   * Standard JApplet API signature - sets up the graphical resources.
   */
    public void start() {
        if (this.controlPanel != null) {
            getContentPane().add(this.controlPanel.simPanel.getContentPane());
        } else {
            getContentPane().add(this.panel);
        }
        return;
    }

    /**
   * WorldEventListener callback.
   * 
   * @param simulation
   * @param tick
   * @param world
   */
    public void tickOccured(Simulator simulation, int tick, World world) {
        if ((this.progressBar != null) && (tick % 100 == 0)) {
            this.progressBar.setValue(tick);
        }
        return;
    }

    /**
   * <P>If this is a ladder match, transmit the final score back to the server.
   *  If this is a scrimmage match, the outcome is merely for viewing interest
   *  and does not affect the rankings.</P><BR>
   * 
   * <P>We do not currently go to particular lengths to encrypt or obsfuscate
   *  results trasmission, as hacking of results is rather easily detectable,
   *  both by observation of the matches and community feedback.  We could upgrade
   *  to the use of source code hashes and one-time encryption nonces and suchlike,
   *  and of course we could limit results by ip filtering, as we execute the ladder
   *  matches on our own server, rather than user boxes, which is already a quite 
   *  significant deterrent to casual mischief on it's own.  Relatedly, we could also
   *  simply penalize and-or ban clearly confirmed cheaters as observed, of course.
   *  It seems doubtful however, that such will arise - as it cuts against the entire 
   *  spirit of antwars play and the fundamentally motivating challenge of coding ants.
   *  Hence we'll cross that bridge as needed should we ever come to it.
   * </P><BR>
   * 
   * <P>simulationEnded is a WorldEventListener callback.</P><BR>
   */
    public void simulationEnded() {
        if (getParameter("ladderMatch") != null) {
            int[] scores = models.world.getScores();
            if (models.simulator.getTick() < 100000) {
                for (int i = 0; i < scores.length; i++) {
                    scores[i] = -1;
                }
            }
            StringBuffer args = new StringBuffer("ladder_result.php?matchid=");
            args.append(this.matchId);
            args.append("&hillid=").append(this.hillId);
            for (int i = 0; i < scores.length; i++) {
                args.append("&p").append(i).append('=').append(scores[i]);
            }
            try {
                URL url = new URL(getCodeBase(), args.toString());
                URLConnection connection = url.openConnection();
                System.err.println(((HttpURLConnection) connection).getResponseCode());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return;
    }

    /**
   * Listener callback.
   * 
   * @param event Standard ChangeEvent instance
   * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
   */
    public void stateChanged(ChangeEvent event) {
        for (int i = 0; i < this.scoreLabels.length; i++) {
            this.scoreLabels[i].setText(this.models.world.getPlayerName(i) + ": " + this.models.score.getScore(i));
        }
        return;
    }
}
