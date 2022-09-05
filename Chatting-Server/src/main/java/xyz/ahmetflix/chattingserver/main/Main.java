package xyz.ahmetflix.chattingserver.main;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.apache.commons.lang3.JavaVersion;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.fusesource.jansi.AnsiConsole;
import xyz.ahmetflix.chattingserver.Server;
import xyz.ahmetflix.chattingserver.dedicated.DedicatedServer;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class Main {

    public static boolean useJline = true;
    public static boolean useConsole = true;

    public static void main(String[] args) {
        System.setProperty("log4j2.formatMsgNoLookups", "true");
        try {
            if (!SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_17)) {
                System.err.println("It seems like you are not using Java 17!");
                System.out.println("The use of Java 17 is strongly recommended.");
            }
        } catch (Exception ignored) {
            System.err.println("Failed to get Java version! Continuing either way..");
        }

        OptionParser parser = new OptionParser() {
            {
                acceptsAll(asList("?", "help"), "Show the help");

                acceptsAll(asList("c", "config"), "Properties file to use").withRequiredArg().ofType(File.class)
                        .defaultsTo(new File("server.properties")).describedAs("Properties file");

                acceptsAll(asList("h", "host", "server-ip"), "Host to listen on").withRequiredArg().ofType(String.class)
                        .describedAs("Hostname or IP");

                acceptsAll(asList("p", "port", "server-port"), "Port to listen on").withRequiredArg()
                        .ofType(Integer.class).describedAs("Port");

                acceptsAll(asList("s", "size", "max-players"), "Maximum amount of players").withRequiredArg()
                        .ofType(Integer.class).describedAs("Server size");

                acceptsAll(asList("d", "date-format"), "Format of the date to display in the console (for log entries)")
                        .withRequiredArg().ofType(SimpleDateFormat.class).describedAs("Log date format");

                acceptsAll(asList("log-pattern"), "Specfies the log filename pattern").withRequiredArg()
                        .ofType(String.class).defaultsTo("server.log").describedAs("Log filename");

                acceptsAll(asList("log-limit"), "Limits the maximum size of the log file (0 = unlimited)")
                        .withRequiredArg().ofType(Integer.class).defaultsTo(0).describedAs("Max log size");

                acceptsAll(asList("log-count"), "Specified how many log files to cycle through").withRequiredArg()
                        .ofType(Integer.class).defaultsTo(1).describedAs("Log count");

                acceptsAll(asList("log-append"), "Whether to append to the log file").withRequiredArg()
                        .ofType(Boolean.class).defaultsTo(true).describedAs("Log append");

                acceptsAll(asList("C", "commands-settings"), "File for command settings").withRequiredArg()
                        .ofType(File.class).defaultsTo(new File("commands.yml")).describedAs("Yml file");

                acceptsAll(asList("nojline"), "Disables jline and emulates the vanilla console");

                acceptsAll(asList("noconsole"), "Disables the console");
            }
        };

        OptionSet options = null;

        try {
            options = parser.parse(args);
        } catch (joptsimple.OptionException ex) {
            Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, ex.getLocalizedMessage());
        }

        if ((options == null) || (options.has("?"))) {
            try {
                parser.printHelpOn(System.out);
            } catch (IOException ex) {
                Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            }
        } else {
            String path = new File(".").getAbsolutePath();
            if (path.contains("!") || path.contains("+")) {
                System.err.println(
                        "Cannot run server in a directory with ! or + in the pathname. Please rename the affected folders and try again.");
                return;
            }

            try {
                String jline_UnsupportedTerminal = new String(new char[] { 'j', 'l', 'i', 'n', 'e', '.', 'U', 'n', 's',
                        'u', 'p', 'p', 'o', 'r', 't', 'e', 'd', 'T', 'e', 'r', 'm', 'i', 'n', 'a', 'l' });
                String jline_terminal = new String(
                        new char[] { 'j', 'l', 'i', 'n', 'e', '.', 't', 'e', 'r', 'm', 'i', 'n', 'a', 'l' });

                useJline = !(jline_UnsupportedTerminal).equals(System.getProperty(jline_terminal));

                if (options.has("nojline")) {
                    System.setProperty("user.language", "en");
                    useJline = false;
                }

                if (useJline) {
                    System.setProperty("library.jansi.version", "Server");
                    AnsiConsole.systemInstall();
                } else {
                    System.setProperty(jline.TerminalFactory.JLINE_TERMINAL, jline.UnsupportedTerminal.class.getName());
                }

                if (options.has("noconsole")) {
                    useConsole = false;
                }

                int maxPermGen = 0;
                for (String s : java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments()) {
                    if (s.startsWith("-XX:MaxPermSize")) {
                        maxPermGen = Integer.parseInt(s.replaceAll("[^\\d]", ""));
                        maxPermGen <<= 10 * ("kmg".indexOf(Character.toLowerCase(s.charAt(s.length() - 1))));
                    }
                }
                if (Float.parseFloat(System.getProperty("java.class.version")) < 52 && maxPermGen < (128 << 10))
                {
                    System.out.println("Warning, your max perm gen size is not set or less than 128mb. It is recommended you restart Java with the following argument: -XX:MaxPermSize=128M");
                }

                System.out.println("Loading libraries, please wait...");

                OptionSet finalOptions = options;

                DedicatedServer server = Server.spin(thread -> {
                    DedicatedServer dedicatedserver = new DedicatedServer(finalOptions, thread);

                    if (finalOptions.has("port")) {
                        int port = (Integer) finalOptions.valueOf("port");
                        if (port > 0) {
                            dedicatedserver.setPort(port);
                        }
                    }

                    return dedicatedserver;
                });
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

    }

    private static List<String> asList(String... params) {
        return Arrays.asList(params);
    }

}
