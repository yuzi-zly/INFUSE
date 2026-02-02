package com.CC.Util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;

public interface Loggable {
    Logger logger = (Logger) LogManager.getLogger(Loggable.class);
}
