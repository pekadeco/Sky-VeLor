package org.apache.logging.log4j;

import java.util.ArrayList;
import java.util.List;

public class Logger {
    public static final List<String> LINES = new ArrayList<String>();
    private final String name;

    public Logger(String name) { this.name = name; }

    public void info(String message) { LINES.add("INFO " + message); }
    public void warn(String message) { LINES.add("WARN " + message); }
    public void error(String message) { LINES.add("ERROR " + message); }
    public void info(String message, Object... args) { LINES.add("INFO " + message); }
}
