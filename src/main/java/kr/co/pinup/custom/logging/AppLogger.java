package kr.co.pinup.custom.logging;

import kr.co.pinup.custom.logging.model.dto.BaseLog;

public interface AppLogger {
    void info(BaseLog payload);
    void warn(BaseLog payload);
    void error(BaseLog payload);

// 가변인자 방식
//    void info(String message, Object... kvPairs);
//    void warn(String message, Object... kvPairs);
//    void error(String message, Object... kvPairs);
//    void error(String message, Throwable t, Object... kvPairs);

}