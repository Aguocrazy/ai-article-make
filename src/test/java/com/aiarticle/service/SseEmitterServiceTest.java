package com.aiarticle.service;

import com.aiarticle.enums.SseMessageTypeEnum;
import com.aiarticle.model.vo.SseEventVO;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Consumer;

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
    void removesTaskStateAfterReplayingTerminalEvent() throws Exception {
        EmitterFactory factory = new EmitterFactory();
        SseEmitterService service = new SseEmitterService(factory);

        service.complete("task-1", SseMessageTypeEnum.ALL_COMPLETE, "done");
        service.subscribe("task-1");

        assertThat(taskStates(service)).doesNotContainKey("task-1");
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
    void buffersImmediateSendFailureAndCompletesFailedEmitterWithError() throws IOException {
        EmitterFactory factory = new EmitterFactory();
        SseEmitterService service = new SseEmitterService(factory);
        RecordingEmitter failedEmitter = (RecordingEmitter) service.subscribe("task-1");
        IOException failure = new IOException("send failed");
        failedEmitter.failNextSend(failure);

        service.send("task-1", SseMessageTypeEnum.AGENT3_STREAMING, "retry-me");
        RecordingEmitter replacement = (RecordingEmitter) service.subscribe("task-1");

        assertThat(failedEmitter.completedWithError).isSameAs(failure);
        assertThat(replacement.events)
                .extracting(SseEventVO::getData)
                .containsExactly("retry-me");
    }

    @Test
    void retainsPendingEventWhenReplayFailsAndRetriesOnNextSubscription() throws IOException {
        EmitterFactory factory = new EmitterFactory();
        SseEmitterService service = new SseEmitterService(factory);
        IOException failure = new IOException("replay failed");
        factory.failNextEmitter(failure);
        service.send("task-1", SseMessageTypeEnum.AGENT2_STREAMING, "pending");

        RecordingEmitter failedEmitter = (RecordingEmitter) service.subscribe("task-1");
        RecordingEmitter replacement = (RecordingEmitter) service.subscribe("task-1");

        assertThat(failedEmitter.completedWithError).isSameAs(failure);
        assertThat(replacement.events)
                .extracting(SseEventVO::getData)
                .containsExactly("pending");
    }

    @Test
    void timeoutAndErrorCallbacksRemoveEmptyTaskStates() throws Exception {
        EmitterFactory factory = new EmitterFactory();
        SseEmitterService service = new SseEmitterService(factory);
        RecordingEmitter timedOut = (RecordingEmitter) service.subscribe("timeout-task");
        RecordingEmitter errored = (RecordingEmitter) service.subscribe("error-task");

        timedOut.runTimeoutCallback();
        errored.runErrorCallback(new IOException("disconnected"));

        assertThat(taskStates(service))
                .doesNotContainKeys("timeout-task", "error-task");
    }

    @Test
    void concurrentPublishAndSubscriberReplacementPreserveEventOrder() throws Exception {
        List<Object> deliveryOrder = Collections.synchronizedList(new ArrayList<>());
        EmitterFactory factory = new EmitterFactory(deliveryOrder);
        SseEmitterService service = new SseEmitterService(factory);
        RecordingEmitter firstEmitter = (RecordingEmitter) service.subscribe("task-1");
        firstEmitter.blockNextSend();

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> publish = executor.submit(
                    () -> service.send("task-1", SseMessageTypeEnum.AGENT3_STREAMING, "first"));
            boolean sendBlocked = firstEmitter.awaitBlockedSend();
            if (!sendBlocked) {
                firstEmitter.releaseBlockedSend();
            }
            assertThat(sendBlocked).isTrue();

            Future<SseEmitter> subscribe = executor.submit(() -> service.subscribe("task-1"));
            boolean replacementCreated = factory.awaitSecondEmitterCreated();
            firstEmitter.releaseBlockedSend();
            assertThat(replacementCreated).isTrue();

            publish.get(5, TimeUnit.SECONDS);
            RecordingEmitter replacement = (RecordingEmitter) subscribe.get(5, TimeUnit.SECONDS);
            service.send("task-1", SseMessageTypeEnum.AGENT3_STREAMING, "second");

            assertThat(firstEmitter.events)
                    .extracting(SseEventVO::getData)
                    .containsExactly("first");
            assertThat(replacement.events)
                    .extracting(SseEventVO::getData)
                    .containsExactly("second");
            assertThat(deliveryOrder).containsExactly("first", "second");
        }
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

    @SuppressWarnings("unchecked")
    private static Map<String, ?> taskStates(SseEmitterService service) throws Exception {
        Field field = SseEmitterService.class.getDeclaredField("taskStates");
        field.setAccessible(true);
        return (Map<String, ?>) field.get(service);
    }

    private static final class EmitterFactory implements Function<Long, SseEmitter> {

        private final List<Long> timeouts = new ArrayList<>();
        private final List<Object> deliveryOrder;
        private final CountDownLatch secondEmitterCreated = new CountDownLatch(1);
        private IOException nextFailure;

        private EmitterFactory() {
            this(new ArrayList<>());
        }

        private EmitterFactory(List<Object> deliveryOrder) {
            this.deliveryOrder = deliveryOrder;
        }

        @Override
        public SseEmitter apply(Long timeout) {
            timeouts.add(timeout);
            RecordingEmitter emitter = new RecordingEmitter(timeout, deliveryOrder);
            if (nextFailure != null) {
                emitter.failNextSend(nextFailure);
                nextFailure = null;
            }
            if (timeouts.size() == 2) {
                secondEmitterCreated.countDown();
            }
            return emitter;
        }

        private void failNextEmitter(IOException failure) {
            nextFailure = failure;
        }

        private boolean awaitSecondEmitterCreated() throws InterruptedException {
            return secondEmitterCreated.await(5, TimeUnit.SECONDS);
        }
    }

    private static final class RecordingEmitter extends SseEmitter {

        private final List<SseEventVO> events = new ArrayList<>();
        private final List<Object> deliveryOrder;
        private boolean completed;
        private Runnable completionCallback;
        private Runnable timeoutCallback;
        private Consumer<Throwable> errorCallback;
        private IOException nextFailure;
        private Throwable completedWithError;
        private CountDownLatch sendBlocked;
        private CountDownLatch releaseSend;

        private RecordingEmitter(Long timeout, List<Object> deliveryOrder) {
            super(timeout);
            this.deliveryOrder = deliveryOrder;
        }

        @Override
        public synchronized void send(SseEventBuilder builder) throws IOException {
            if (sendBlocked != null) {
                sendBlocked.countDown();
                try {
                    releaseSend.await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IOException("interrupted", exception);
                }
            }
            if (nextFailure != null) {
                IOException failure = nextFailure;
                nextFailure = null;
                throw failure;
            }
            builder.build().stream()
                    .map(item -> item.getData())
                    .filter(SseEventVO.class::isInstance)
                    .map(SseEventVO.class::cast)
                    .findFirst()
                    .ifPresent(event -> {
                        events.add(event);
                        deliveryOrder.add(event.getData());
                    });
        }

        @Override
        public synchronized void complete() {
            completed = true;
        }

        @Override
        public synchronized void completeWithError(Throwable failure) {
            completedWithError = failure;
        }

        @Override
        public synchronized void onCompletion(Runnable callback) {
            completionCallback = callback;
        }

        @Override
        public synchronized void onTimeout(Runnable callback) {
            timeoutCallback = callback;
        }

        @Override
        public synchronized void onError(Consumer<Throwable> callback) {
            errorCallback = callback;
        }

        private synchronized void failNextSend(IOException failure) {
            nextFailure = failure;
        }

        private synchronized void blockNextSend() {
            sendBlocked = new CountDownLatch(1);
            releaseSend = new CountDownLatch(1);
        }

        private boolean awaitBlockedSend() throws InterruptedException {
            return sendBlocked.await(5, TimeUnit.SECONDS);
        }

        private void releaseBlockedSend() {
            releaseSend.countDown();
        }

        private void runCompletionCallback() {
            if (completionCallback != null) {
                completionCallback.run();
            }
        }

        private void runTimeoutCallback() {
            if (timeoutCallback != null) {
                timeoutCallback.run();
            }
        }

        private void runErrorCallback(Throwable failure) {
            if (errorCallback != null) {
                errorCallback.accept(failure);
            }
        }
    }
}
