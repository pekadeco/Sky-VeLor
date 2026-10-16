package org.apache.logging.log4j;

public class LogManager {
    public static Logger getLogger(String name) { return new Logger(name); }
    public static Logger getLogger(Class<?> type) { return new Logger(type.getName()); }
    public static Logger getRootLogger() { return new Logger("root"); }
}
