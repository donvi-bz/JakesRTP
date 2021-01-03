package biz.donvi.jakesRTP;

/**
 * An exception for organizational purposes.
 */
class JrtpBaseException extends Exception {

    public JrtpBaseException() { super(); }

    public JrtpBaseException(String message) { super(message); }

    public JrtpBaseException(String message, Throwable cause) { super(message, cause); }

    public JrtpBaseException(Throwable cause) { super(cause); }

    static class PluginDisabledException extends JrtpBaseException {}

    static class NotPermittedException extends JrtpBaseException {
        public NotPermittedException(String message) { super(message); }
    }

    static class ConfigurationException extends JrtpBaseException {
        public ConfigurationException(String message) {super(message);}
    }
}
