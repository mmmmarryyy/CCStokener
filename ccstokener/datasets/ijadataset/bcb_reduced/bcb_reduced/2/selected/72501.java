package org.eyrene.javaj.io;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Title: Args Manager</p>
 * <p>Description: manager con controllo degli argomenti e stampa dell'usage</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: eyrene</p>
 * @author Francesco Vadicamo
 * @version 2.2
 */
public class ArgsMgr {

    /**Proprieta' dell'applicazione*/
    private Properties properties;

    /**
	 * Costruttore con passaggio della path del file delle proprieta'
	 *
	 * @param properties_file path del file .properties contenente le proprieta'
	 */
    public ArgsMgr(String properties_file) throws IOException {
        if (properties_file == null) throw new NullPointerException("PRE-CONDIZIONE VIOLATA!");
        this.properties = new Properties();
        this.properties.load(ResourceMgr.loadResourceAsStream(properties_file));
    }

    /**
	 * Costruttore con passaggio dell'URL del file delle proprieta'
	 *
	 * @param properties_url url del file .properties contenente le proprieta'
	 */
    public ArgsMgr(URL properties_url) throws IOException {
        if (properties_url == null) throw new NullPointerException("PRE-CONDIZIONE VIOLATA!");
        this.properties = new Properties();
        this.properties.load(properties_url.openStream());
    }

    /**
	 * Restituisce l'utilizzo corretto dell'applicazione specificato nel file .properties
	 *
	 * @return una stringa rappresentante l'usage corretto per l'applicazione
	 */
    public String getUsage() {
        StringBuilder usage = new StringBuilder("Usage: ");
        String app_name = properties.getProperty("APP_NAME");
        if (app_name != null) usage.append(app_name); else usage.append("app_name");
        usage.append(" [-(");
        for (int i = 0; i < getNumOptions(); i++) {
            String opt_value = null;
            if ((opt_value = properties.getProperty("OPT_VALUE_" + i)) != null && opt_value.trim().length() != 0) {
                usage.append("|" + opt_value);
                String opt_param = "";
                if ((opt_param = properties.getProperty("OPT_PARAM_" + i)) != null && opt_value.trim().length() != 0) usage.append(":").append(opt_param);
            }
        }
        usage.append(")] ");
        for (int i = 0; i < getNumArguments(); i++) {
            String arg_opt = null;
            if ((arg_opt = properties.getProperty("ARG_OPT_VALUE_" + i)) != null && arg_opt.trim().length() != 0) usage.append("[-(").append(arg_opt).append("):]");
            usage.append(properties.getProperty("ARG_VALUE_" + i) + " ");
        }
        return usage.toString();
    }

    /**
	 * Restituisce il numero di argomenti (indispensabili per l'applicazione)
	 *
	 * @return il numero di argomenti minimi necessari per l'avvio dell'applicazione
	 */
    public int getNumArguments() {
        int num = 0;
        String curr = null;
        while ((curr = properties.getProperty("ARG_VALUE_" + num)) != null && curr.trim().length() != 0) num++;
        return num;
    }

    /**
	 * Restituisce il numero di opzioni disponibili per l'applicazione
	 *
	 * @return il numero di opzioni disponibili per l'applicazione
	 */
    public int getNumOptions() {
        int num = 0;
        String curr = null;
        while ((curr = properties.getProperty("OPT_VALUE_" + num)) != null && curr.trim().length() != 0) num++;
        return num;
    }

    private String buildRegex() {
        String STATUS = properties.getProperty("REGEX_STATUS");
        if (STATUS != null && STATUS.trim().equalsIgnoreCase("on")) {
            String ONLY = properties.getProperty("REGEX_ONLY");
            if (ONLY == null || ONLY.trim().equalsIgnoreCase("no")) {
                String OPT = "-(?:";
                for (int i = 0; i < getNumOptions(); i++) {
                    String opt_value = null;
                    if ((opt_value = properties.getProperty("OPT_VALUE_" + i)) != null && opt_value.trim().length() != 0) {
                        if (i != 0) OPT += "|";
                        if (opt_value.equals("?")) OPT += "(\\?)"; else {
                            OPT += "(" + opt_value;
                            String opt_param = "";
                            if ((opt_param = properties.getProperty("OPT_PARAM_" + i)) != null && opt_param.trim().length() != 0) OPT += ":(?:\\S)+";
                            OPT += ")";
                        }
                    }
                }
                OPT += ")";
                String PAR = "[\\S&&[^-]](?:\\S)*";
                String OPTs = "(?:(?:" + OPT + ")(?:\\s)+)+";
                String PARs = "";
                for (int i = 0; i < getNumArguments(); i++) PARs += "(?:(" + PAR + ")(?:\\s)+)?";
                String ARG = "(?:\\s)*(?:" + OPTs + ")?(?:" + PARs + ")(?:\\s)*";
                return ARG;
            } else {
                String tmp;
                String OPT = "-(?:";
                for (int i = 0; i < getNumOptions(); i++) {
                    String opt_value = null;
                    if ((opt_value = properties.getProperty("OPT_VALUE_" + i)) != null && opt_value.trim().length() != 0) {
                        if (i != 0) OPT += "|";
                        if (opt_value.equals("?")) OPT += "(\\?)"; else {
                            OPT += "(" + opt_value;
                            String opt_param = "";
                            if ((opt_param = properties.getProperty("OPT_PARAM_" + i)) != null && opt_param.trim().length() != 0) OPT += ":(?:\\S)+" + opt_param;
                            OPT += ")";
                        }
                    }
                }
                OPT += ")";
                String PAR = "[\\S&&[^-]](?:\\S)*";
                if ((tmp = properties.getProperty("REGEX_PAR")) != null && !tmp.trim().equals(PAR)) {
                    PAR = tmp;
                    System.out.println("ArgsMgr >> REGEX_PAR loaded!");
                }
                String OvP = "(?:" + PAR + ")|(?:" + OPT + ")";
                if ((tmp = properties.getProperty("REGEX_OvP")) != null && !tmp.trim().equals(OvP)) {
                    OvP = tmp;
                    System.out.println("ArgsMgr >> REGEX_OvP loaded!");
                }
                String OPTs = "(?:(?:" + OPT + ")(?:\\s)+)+";
                if ((tmp = properties.getProperty("REGEX_OPTs")) != null && !tmp.trim().equals(OPTs)) {
                    OPTs = tmp;
                    System.out.println("ArgsMgr >> REGEX_OPTs loaded!");
                }
                String PARs = "(?:" + PAR + ")|(?:" + OPT + ")";
                if ((tmp = properties.getProperty("REGEX_PARs")) != null && !tmp.trim().equals(PARs)) {
                    PARs = tmp;
                    System.out.println("ArgsMgr >> REGEX_PARs loaded!");
                }
                throw new UnsupportedOperationException("Operation not yet implemented!");
            }
        } else return null;
    }

    /**
	 * Controlla il corretto passaggio dei parametri specificati
     *
     * @param args array di argomenti passati alla funzione main
     * @return una stringa contenente gli eventuali errori riscontrati o null altrimenti
     */
    public String checkUsage(String[] args) {
        if (args == null) throw new NullPointerException("PRE-CONDIZIONE VIOLATA!");
        StringBuilder usage = null;
        String argv = "";
        for (int i = 0; i < args.length; i++) argv += args[i] + " ";
        String regex = buildRegex();
        if (regex != null) {
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(argv);
            if ((args.length == 0 || !args[0].equals("-?")) && m.matches()) return null; else {
                usage = new StringBuilder(getUsage()).append("\n");
                if (args.length > 0 && args[0].equals("-?")) usage.append("\nOPTIONs:\n").append(getFullList()); else usage.append("> type '").append(properties.getProperty("APP_NAME")).append(" -?' to view all options");
            }
        } else if (args.length < getNumArguments()) {
            usage = new StringBuilder(getUsage()).append("\n");
            if (args.length > 0 && args[0].equals("-?")) usage.append("\nOPTIONs:\n").append(getFullList()); else usage.append("> type '").append(properties.getProperty("APP_NAME")).append(" -?' to view all options");
        }
        try {
            return usage.toString();
        } catch (NullPointerException e) {
            return null;
        }
    }

    /**
	 * Convalida l'array di argomenti passati secondo le specifiche del file .properties
	 * 
	 * @param args array degli argomenti da convalidare
	 * @return l'array ordinato secondo le specifiche dichiarate nel file .properties
	 *          o null se gli argomenti passati non sono validi
	 */
    public String[] validateArgs(String[] args) {
        if (args == null) throw new NullPointerException("PRE-CONDIZIONE VIOLATA!");
        String args_txt = "";
        for (int i = 0; i < args.length; i++) args_txt += args[i] + " ";
        String regex = buildRegex();
        if (regex != null) {
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(args_txt);
            if (m.matches()) {
                String[] argv = new String[m.groupCount()];
                for (int i = 0; i < argv.length; i++) {
                    argv[i] = m.group(i + 1);
                }
                return argv;
            } else return null;
        } else return null;
    }

    /**
	 * Restituisce il valore dell'argomento richiesto (come da file .properties)
	 * 
	 * @param args array degli argomenti passati
	 * @param arg_number il numero dell'argomento richiesto (parte da 0)
	 * @return il valore corrispondente all'argomento specificato
	 */
    public String getArgument(String[] args, int arg_number) {
        if (args == null) throw new NullPointerException("PRE-CONDIZIONE VIOLATA!");
        if (arg_number < 0 || arg_number >= getNumArguments()) throw new IllegalArgumentException("PRE-CONDIZIONE VIOLATA!");
        String[] argv = validateArgs(args);
        return (argv != null && arg_number < getNumOptions() + argv.length && argv[getNumOptions() + arg_number] != null) ? argv[getNumOptions() + arg_number] : "";
    }

    /**
	 * Restituisce il valore dell'opzione richiesta
	 * 
	 * @param args array degli argomenti passati
	 * @param opt_number il numero dell'opzione richiesta (parte da 0)
	 * @return il valore corrispondente all'opzione specificata
	 */
    public String getOption(String[] args, int opt_number) {
        if (args == null) throw new NullPointerException("PRE-CONDIZIONE VIOLATA!");
        if (opt_number < 0 || opt_number >= getNumOptions()) throw new IllegalArgumentException("PRE-CONDIZIONE VIOLATA!");
        String[] argv = validateArgs(args);
        return (argv != null && opt_number < argv.length && argv[opt_number] != null) ? argv[opt_number] : "";
    }

    /**
	 * Restituisce una linea contenente l'argomento specificato e la sua descrizione
	 *
	 * @param arg_number il numero dell'argomento rihiesto
	 * @return una stringa specificante l'usage dell'argomento o null se non esiste
	 */
    public String getArgumentLine(int arg_number) {
        if (arg_number < 0) throw new IllegalArgumentException("PRE-CONDIZIONE VIOLATA!");
        String line = "", curr;
        if ((curr = properties.getProperty("ARG_VALUE_" + arg_number)) != null && curr.trim().length() != 0) {
            line += "\"" + curr + "\"\t";
            if ((curr = properties.getProperty("ARG_DESCR_" + arg_number)) != null && curr.trim().length() != 0) line += "\"" + curr + "\"\t";
        } else return null;
        if ((curr = properties.getProperty("ARG_OPT_VALUE_" + arg_number)) != null && curr.trim().length() != 0) {
            line += "\n -> arg options: \t" + curr;
            if ((curr = properties.getProperty("ARG_OPT_DESCR_" + arg_number)) != null && curr.trim().length() != 0) line += "\t" + curr;
        }
        return line;
    }

    /**
	 * Restituisce una linea contenente il corretto usage dell'opzione specificata
	 *
	 * @param opt_number il numero di opzione richiesto
	 * @return una stringa specificante l'usage dell'opzione o null se non esiste
	 */
    public String getOptionLine(int opt_number) {
        if (opt_number < 0) throw new IllegalArgumentException("PRE-CONDIZIONE VIOLATA!");
        String line = "-", curr;
        curr = properties.getProperty("OPT_VALUE_" + opt_number);
        if (curr == null || curr.trim().length() == 0) return null;
        line += curr + "";
        curr = properties.getProperty("OPT_PARAM_" + opt_number);
        if (curr == null) return null;
        if (curr.equals("")) curr = "\t"; else curr = ":" + curr;
        line += curr + "\t";
        curr = properties.getProperty("OPT_DESCR_" + opt_number);
        if (curr == null) return null;
        line += curr;
        return line;
    }

    /**
	 * Restituisce un listato degli argomenti e delle opzioni disponibili
	 *
	 * @return una stringa contenente una lista degli argomenti e delle opzioni
	 */
    public String getFullList() {
        String line = "", curr;
        boolean end = false;
        for (int i = 0; !end; i++) {
            curr = getArgumentLine(i);
            if (curr == null) end = true; else line += curr + "\n";
        }
        line += "\n";
        end = false;
        for (int i = 0; !end; i++) {
            curr = getOptionLine(i);
            if (curr == null) end = true; else line += curr + "\n";
        }
        return line;
    }
}
