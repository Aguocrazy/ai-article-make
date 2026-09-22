package com.aiarticle.service;

import com.aiarticle.enums.SseMessageTypeEnum;
import com.aiarticle.model.vo.SseEventVO;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class SseEmitterServiceTest {

    private static final long THIRTY_MINUTES_MILLIS = 30L * 60L * 1000L;

    @Test
    void replaysEventsSentBeforeSubscriptionInOrder() throws IOException {
        EmitterFactory factory = new EmitterFactory();
        SseEmitterService service = new SseEmitterService(factory);

        service.send("task-1", SseMessageTypeEnum.AGENT2_STREAMING, "first");
        service.send("task-1", SseMessageTypeEnum.AGENT2_STREAMING, "second");
        RecordingEmitter emitter = (RecordingEmitter) service.subscribe("task-1");

        assertThat(factory.timeouts).containsExactly(THIRTY_MINUTES_MILLIS);
        assertThat(emitter.events)
                .extracting(event -> event.getData())
                .containsExactly("first", "second");
    }

    @Test
    void deliversEventImmediatelyToExistingSubscriber() throws IOException {
        EmitterFactory factory = new EmitterFactory();
        SseEmitterService service = new SseEmitterService(factory);
        RecordingEmitter emitter = (RecordingEmitter) service.subscribe("task-1");

        service.send("task-1", SseMessageTypeEnum.AGENT3_STREAMING, "chunk");

        assertThat(emitter.events).singleElement().satisfies(event -> {
            assertThat(event.getTaskId()).isEqualTo("task-1");
            assertThat(event.getType()).isEqualTo(SseMessageTypeEnum.AGENT3_STREAMING.getValue());
            assertThat(event.getData()).isEqualTo("chunk");
            assertThat(event.getTimestamp()).isPositive();
        });
        assertThat(emitter.completed).isFalse();
    }

    @Test
    void replaysTerminalEventThenCompletesSubscriber() throws IOException {
        EmitterFactory factory = new EmitterFactory();
        SseEmitterService service = new SseEmitterService(factory);

        service.complete("task-1", SseMessageTypeEnum.ALL_COMPLETE, "done");
        RecordingEmitter first = (RecordingEmitter) service.subscribe("task-1");
        RecordingEmitter second = (RecordingEmitter) service.subscribe("task-1");

        assertThat(first.events)
                .extracting(SseEventVO::getData)
                .containsExactly("done");
        assertThat(first.completed).isTrue();
        assertThat(second.events).isEmpty();
    }

    @Test
    void replacingSubscriberCompletesOldEmitterWithoutRemovingReplacement() throws IOException {
        EmitterFactory factory = new EmitterFactory();
        SseEmitterService service = new SseEmitterService(factory);
        RecordingEmitter oldEmitter = (RecordingEmitter) service.subscribe("task-1");

        RecordingEmitter replacement = (RecordingEmitter) service.subscribe("task-1");
        oldEmitter.runCompletionCallback();
        service.send("task-1", SseMessageTypeEnum.AGENT2_STREAMING, "new");

        assertThat(oldEmitter.completed).isTrue();
        assertThat(replacement.events)
                .extracting(SseEventVO::getData)
                .containsExactly("new");
    }

    @Test
    void boundsPendingEventsAndEvictsOldestNonterminalFirst() throws IOException {
        EmitterFactory factory = new EmitterFactory();
        SseEmitterService service = new SseEmitterService(factory);

        for (int i = 0; i < 1_001; i++) {
            service.send("task-1", SseMessageTypeEnum.AGENT3_STREAMING, i);
        }
        RecordingEmitter emitter = (RecordingEmitter) service.subscribe("task-1");

        assertThat(emitter.events).hasSize(1_000);
        assertThat(emitter.events.getFirst().getData()).isEqualTo(1);
        assertThat(emitter.events.getLast().getData()).isEqualTo(1_000);
    }

    @Test
    void terminalPendingEventSurvivesBufferPressure() throws IOException {
        EmitterFactory factory = new EmitterFactory();
        SseEmitterService service = new SseEmitterService(factory);

        for (int i = 0; i < 1_000; i++) {
            service.send("task-1", SseMessageTypeEnum.AGENT3_STREAMING, i);
        }
        service.complete("task-1", SseMessageTypeEnum.ERROR, "terminal");
        RecordingEmitter emitter = (RecordingEmitter) service.subscribe("task-1");

        assertThat(emitter.events).hasSize(1_000);
        assertThat(emitter.events.getFirst().getData()).isEqualTo(1);
        assertThat(emitter.events.getLast().getData()).isEqualTo("terminal");
        assertThat(emitter.completed).isTrue();
    }

    @Test
    void retainsLatestTerminalEventWhenTerminalCompletionsOverflowBuffer() throws IOException {
        EmitterFactory factory = new EmitterFactory();
        SseEmitterService service = new SseEmitterService(factory);

        for (int i = 0; i < 1_001; i++) {
            service.complete("task-1", SseMessageTypeEnum.ERROR, "terminal-" + i);
        }
        RecordingEmitter emitter = (RecordingEmitter) service.subscribe("task-1");

        assertThat(emitter.events)
                .extracting(SseEventVO::getData)
                .containsExactly("terminal-1000");
        assertThat(emitter.completed).isTrue();
    }

    private static final class EmitterFactory implements Function<Long, SseEmitter> {

        private final List<Long> timeouts = new ArrayList<>();

        @Override
        public SseEmitter apply(Long timeout) {
            timeouts.add(timeout);
            return new RecordingEmitter(timeout);
        }
    }

    private static final class RecordingEmitter extends SseEmitter {

        private final List<SseEventVO> events = new ArrayList<>();
        private boolean completed;
        private Runnable completionCallback;

        private RecordingEmitter(Long timeout) {
            super(timeout);
        }

        @Override
        public synchronized void send(SseEventBuilder builder) throws IOException {
            builder.build().stream()
                    .map(item -> item.getData())
                    .filter(SseEventVO.class::isInstance)
                    .map(SseEventVO.class::cast)
                    .findFirst()
                    .ifPresent(events::add);
        }

        @Override
        public synchronized void complete() {
            completed = true;
        }

        @Override
        public synchronized void onCompletion(Runnable callback) {
            completionCallback = callback;
        }

        private void runCompletionCallback() {
            if (completionCallback != null) {
                completionCallback.run();
            }
        }
    }
}
