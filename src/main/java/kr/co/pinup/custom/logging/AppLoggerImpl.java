package kr.co.pinup.custom.logging;

import kr.co.pinup.custom.logging.model.dto.BaseLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppLoggerImpl implements AppLogger {

    private final StructuredLogger structuredLogger;

    @Override
    public void info(BaseLog payload) {
        structuredLogger.log(payload);
    }

    @Override
    public void warn(BaseLog payload) {
        structuredLogger.log(payload);
    }

    @Override
    public void error(BaseLog payload) {
        structuredLogger.log(payload);
    }

// 가변인자 방식
//    @Override
//    public void info(String message, Object... kvPairs) {
//        structuredLogger.info(message, toMap(kvPairs));
//    }
//
//    @Override
//    public void warn(String message, Object... kvPairs) {
//        structuredLogger.warn(message, toMap(kvPairs));
//    }
//
//    @Override
//    public void error(String message, Object... kvPairs) {
//        structuredLogger.error(message, new RuntimeException("No exception provided"), toMap(kvPairs));
//    }
//
//    @Override
//    public void error(String message, Throwable t, Object... kvPairs) {
//        structuredLogger.error(message, t, toMap(kvPairs));
//    }
//
//    private Map<String, Object> toMap(Object... kvPairs) {
//        Map<String, Object> map = new HashMap<>();
//        for (int i = 0; i < kvPairs.length - 1; i += 2) {
//            map.put(String.valueOf(kvPairs[i]), kvPairs[i + 1]);
//        }
//        return map;
//    }

}