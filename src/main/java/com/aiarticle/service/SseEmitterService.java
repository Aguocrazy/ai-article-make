package com.aiarticle.service;

import com.aiarticle.enums.SseMessageTypeEnum;
import com.aiarticle.model.vo.SseEventVO;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 管理 SSE 连接，并为尚未订阅或暂时发送失败的任务缓存事件。
 */
@Service
public class SseEmitterService {

    private static final long SSE_TIMEOUT_MILLIS = 30L * 60L * 1000L;
    private static final int MAX_PENDING_EVENTS = 1_000;

    private final Map<String, TaskState> taskStates = new ConcurrentHashMap<>();
    private final Function<Long, SseEmitter> emitterFactory;

    /**
     * 创建使用标准 SSE 发射器的服务。
     */
    public SseEmitterService() {
        this(SseEmitter::new);
    }

    /**
     * 创建可替换发射器工厂的服务，便于包内测试验证传输行为。
     *
     * @param emitterFactory 发射器工厂
     */
    SseEmitterService(Function<Long, SseEmitter> emitterFactory) {
        this.emitterFactory = emitterFactory;
    }

    /**
     * 订阅指定任务，并按写入顺序发送订阅前缓存的事件。
     *
     * @param taskId 任务 ID
     * @return 新 SSE 连接
     */
    public SseEmitter subscribe(String taskId) {
        SseEmitter emitter = emitterFactory.apply(SSE_TIMEOUT_MILLIS);

        while (true) {
            TaskState state = taskStates.computeIfAbsent(taskId, ignored -> new TaskState());
            SseEmitter oldEmitter;
            FlushResult flushResult;
            synchronized (state) {
                if (taskStates.get(taskId) != state) {
                    continue;
                }
                oldEmitter = state.emitter;
                state.emitter = emitter;
                registerCleanup(taskId, state, emitter);
                flushResult = flushPending(taskId, state, emitter);
            }
            if (oldEmitter != null) {
                oldEmitter.complete();
            }
            if (flushResult.failure() != null) {
                emitter.completeWithError(flushResult.failure());
            } else if (flushResult.terminal()) {
                emitter.complete();
            }
            return emitter;
        }
    }

    /**
     * 发送非终态事件。
     *
     * @param taskId 任务 ID
     * @param type   消息类型
     * @param data   事件数据
     */
    public void send(String taskId, SseMessageTypeEnum type, Object data) {
        emit(taskId, type, data, false);
    }

    /**
     * 发送终态事件并结束当前 SSE 连接。
     *
     * @param taskId 任务 ID
     * @param type   消息类型
     * @param data   事件数据
     */
    public void complete(String taskId, SseMessageTypeEnum type, Object data) {
        emit(taskId, type, data, true);
    }

    private void emit(String taskId, SseMessageTypeEnum type, Object data, boolean terminal) {
        PendingEvent event = new PendingEvent(SseEventVO.of(taskId, type, data), terminal);

        while (true) {
            TaskState state = taskStates.computeIfAbsent(taskId, ignored -> new TaskState());
            SseEmitter emitterToComplete = null;
            IOException sendFailure = null;
            synchronized (state) {
                if (taskStates.get(taskId) != state) {
                    continue;
                }
                if (state.emitter == null) {
                    buffer(state, event);
                    return;
                }
                try {
                    sendEvent(state.emitter, event.event());
                    if (terminal) {
                        emitterToComplete = state.emitter;
                        state.emitter = null;
                        taskStates.remove(taskId, state);
                    }
                } catch (IOException exception) {
                    emitterToComplete = state.emitter;
                    state.emitter = null;
                    buffer(state, event);
                    sendFailure = exception;
                }
            }
            if (sendFailure != null) {
                emitterToComplete.completeWithError(sendFailure);
            } else if (emitterToComplete != null) {
                emitterToComplete.complete();
            }
            return;
        }
    }

    private FlushResult flushPending(String taskId, TaskState state, SseEmitter emitter) {
        while (!state.pendingEvents.isEmpty() && state.emitter == emitter) {
            PendingEvent event = state.pendingEvents.peekFirst();
            try {
                sendEvent(emitter, event.event());
            } catch (IOException exception) {
                state.emitter = null;
                return new FlushResult(exception, false);
            }

            state.pendingEvents.removeFirst();
            if (event.terminal()) {
                state.pendingEvents.clear();
                state.emitter = null;
                taskStates.remove(taskId, state);
                return new FlushResult(null, true);
            }
        }
        return new FlushResult(null, false);
    }

    private void sendEvent(SseEmitter emitter, SseEventVO event) throws IOException {
        emitter.send(SseEmitter.event()
                .name(event.getType())
                .data(event));
    }

    private void registerCleanup(String taskId, TaskState state, SseEmitter emitter) {
        Runnable cleanup = () -> {
            synchronized (state) {
                if (state.emitter == emitter) {
                    state.emitter = null;
                    if (state.pendingEvents.isEmpty()) {
                        taskStates.remove(taskId, state);
                    }
                }
            }
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ignored -> cleanup.run());
    }

    private void buffer(TaskState state, PendingEvent event) {
        if (event.terminal()) {
            state.pendingEvents.removeIf(PendingEvent::terminal);
        }
        state.pendingEvents.addLast(event);
        while (state.pendingEvents.size() > MAX_PENDING_EVENTS) {
            PendingEvent oldestNonterminal = state.pendingEvents.stream()
                    .filter(candidate -> !candidate.terminal())
                    .findFirst()
                    .orElseThrow();
            state.pendingEvents.removeFirstOccurrence(oldestNonterminal);
        }
    }

    private static final class TaskState {

        private SseEmitter emitter;
        private final Deque<PendingEvent> pendingEvents = new ArrayDeque<>();
    }

    private record PendingEvent(SseEventVO event, boolean terminal) {
    }

    private record FlushResult(IOException failure, boolean terminal) {
    }
}
