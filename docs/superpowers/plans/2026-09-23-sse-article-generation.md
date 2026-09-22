# SSE Async Article Generation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a five-worker, no-queue article generation pipeline that returns a task ID immediately and streams buffered progress to the Vue workbench through SSE.

**Architecture:** `ArticleGenerationService` owns task creation and authorization, `ArticleGenerationTask` runs the existing agents and persists state, and `SseEmitterService` owns connections plus bounded pre-subscription buffering. A named Spring executor has five threads, zero queue capacity, and aborts the sixth concurrent submission.

**Tech Stack:** Java 21, Spring Boot 3.5, Spring MVC `SseEmitter`, MyBatis-Flex, JUnit 5/Mockito, Vue 3, TypeScript, Axios, browser `EventSource`

---

## File map

- Create `src/main/java/com/aiarticle/config/ArticleGenerationExecutorConfig.java`: named five-thread executor.
- Create `src/main/java/com/aiarticle/model/dto/article/ArticleCreateRequest.java`: create request.
- Create `src/main/java/com/aiarticle/model/vo/ArticleTaskVO.java`: task ID response.
- Create `src/main/java/com/aiarticle/model/vo/SseEventVO.java`: stable SSE data envelope.
- Create `src/main/java/com/aiarticle/service/SseEmitterService.java`: emitter registry and bounded event buffer.
- Create `src/main/java/com/aiarticle/service/ArticleGenerationService.java`: task-facing service contract.
- Create `src/main/java/com/aiarticle/service/impl/ArticleGenerationServiceImpl.java`: creation, ownership check, submission.
- Create `src/main/java/com/aiarticle/task/ArticleGenerationTask.java`: sequential agent orchestration and persistence.
- Create `src/main/java/com/aiarticle/controller/ArticleController.java`: `/article/create` and `/article/stream/{taskId}`.
- Create matching tests under `src/test/java/com/aiarticle/`.
- Create `fronted/src/api/article.ts`: create API, event types, SSE URL.
- Modify `fronted/src/views/WriteView.vue`: replace local fake generation with real task/SSE flow.
- Modify `README.md`: document implemented endpoints and concurrency behavior.

### Task 1: Five-worker no-queue executor

**Files:**
- Create: `src/main/java/com/aiarticle/config/ArticleGenerationExecutorConfig.java`
- Test: `src/test/java/com/aiarticle/config/ArticleGenerationExecutorConfigTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void rejectsSixthConcurrentTaskWithoutQueueing() throws Exception {
    ThreadPoolTaskExecutor executor = new ArticleGenerationExecutorConfig()
            .articleGenerationExecutor();
    CountDownLatch running = new CountDownLatch(5);
    CountDownLatch release = new CountDownLatch(1);
    for (int i = 0; i < 5; i++) {
        executor.execute(() -> {
            running.countDown();
            await(release);
        });
    }
    assertTrue(running.await(2, TimeUnit.SECONDS));
    assertThrows(TaskRejectedException.class, () -> executor.execute(() -> {}));
    release.countDown();
    executor.shutdown();
}
```

- [ ] **Step 2: Verify the test is red**

Run: `JAVA_HOME=/Users/guoshao/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home ./mvnw -Dtest=ArticleGenerationExecutorConfigTest test`

Expected: compilation failure because `ArticleGenerationExecutorConfig` does not exist.

- [ ] **Step 3: Implement the executor**

```java
@Configuration
public class ArticleGenerationExecutorConfig {
    public static final String EXECUTOR_NAME = "articleGenerationExecutor";

    @Bean(name = EXECUTOR_NAME)
    public ThreadPoolTaskExecutor articleGenerationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("article-generation-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
```

- [ ] **Step 4: Verify green**

Run the command from Step 2. Expected: one passing test.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/aiarticle/config/ArticleGenerationExecutorConfig.java src/test/java/com/aiarticle/config/ArticleGenerationExecutorConfigTest.java
git commit -m "feat: add bounded article generation executor"
```

### Task 2: Buffered SSE event transport

**Files:**
- Create: `src/main/java/com/aiarticle/model/vo/SseEventVO.java`
- Create: `src/main/java/com/aiarticle/service/SseEmitterService.java`
- Test: `src/test/java/com/aiarticle/service/SseEmitterServiceTest.java`

- [ ] **Step 1: Write failing tests for buffering and terminal replay**

```java
@Test
void replaysEventsPublishedBeforeSubscriptionInOrder() throws Exception {
    service.send("task-1", SseMessageTypeEnum.AGENT1_COMPLETE, "title");
    service.send("task-1", SseMessageTypeEnum.AGENT2_STREAMING, "chunk");

    SseEmitter emitter = service.subscribe("task-1");

    assertSame(emitter, service.getEmitterForTest("task-1"));
    verify(emitterFactory).create(SseEmitterService.TIMEOUT_MILLIS);
}

@Test
void terminalEventIsReplayedAndThenCompletesEmitter() {
    service.complete("task-1", SseMessageTypeEnum.ALL_COMPLETE, "done");
    SseEmitter emitter = service.subscribe("task-1");
    assertNotNull(emitter);
    assertFalse(service.hasPendingEvents("task-1"));
}
```

Use a package-private `EmitterFactory` constructor dependency so tests can supply a recording `SseEmitter`; do not expose production state with public test-only methods.

- [ ] **Step 2: Verify red**

Run: `JAVA_HOME=/Users/guoshao/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home ./mvnw -Dtest=SseEmitterServiceTest test`

Expected: compilation failure because the service and event envelope do not exist.

- [ ] **Step 3: Implement bounded buffering**

`SseEventVO`:

```java
public record SseEventVO(String taskId, String type, Object data, long timestamp) {
    public static SseEventVO of(String taskId, SseMessageTypeEnum type, Object data) {
        return new SseEventVO(taskId, type.getValue(), data, System.currentTimeMillis());
    }
}
```

`SseEmitterService` behavior:

```java
public static final long TIMEOUT_MILLIS = Duration.ofMinutes(30).toMillis();
private static final int MAX_PENDING_EVENTS = 1000;
private final ConcurrentMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();
private final ConcurrentMap<String, Deque<PendingEvent>> pending = new ConcurrentHashMap<>();

public SseEmitter subscribe(String taskId) {
    SseEmitter emitter = emitterFactory.create(TIMEOUT_MILLIS);
    SseEmitter old = emitters.put(taskId, emitter);
    if (old != null) old.complete();
    registerCleanup(taskId, emitter);
    flush(taskId, emitter);
    return emitter;
}

public void send(String taskId, SseMessageTypeEnum type, Object data) {
    publish(taskId, new PendingEvent(type, SseEventVO.of(taskId, type, data), false));
}

public void complete(String taskId, SseMessageTypeEnum type, Object data) {
    publish(taskId, new PendingEvent(type, SseEventVO.of(taskId, type, data), true));
}
```

Synchronize per-task queue mutation and flushing. If no emitter exists or `send` throws `IOException`, append to the deque and evict the oldest non-terminal event when it exceeds 1000. A terminal pending event must survive until the first subscription, be sent, complete the emitter, and remove the pending queue.

- [ ] **Step 4: Verify green**

Run the command from Step 2. Expected: all `SseEmitterServiceTest` tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/aiarticle/model/vo/SseEventVO.java src/main/java/com/aiarticle/service/SseEmitterService.java src/test/java/com/aiarticle/service/SseEmitterServiceTest.java
git commit -m "feat: add buffered SSE event service"
```

### Task 3: Sequential article generation worker

**Files:**
- Create: `src/main/java/com/aiarticle/task/ArticleGenerationTask.java`
- Test: `src/test/java/com/aiarticle/task/ArticleGenerationTaskTest.java`

- [ ] **Step 1: Write the failing success-path test**

```java
@Test
void runsAgentsInOrderPersistsCompletedArticleAndClosesStream() {
    Article article = Article.builder().taskId("t1").userId(7L).topic("AI").build();
    when(articleMapper.selectOneByQuery(any())).thenReturn(article);

    task.run("t1");

    InOrder order = inOrder(titleAgent, outlineAgent, contentAgent,
            imageRequirementAgent, imageAgent, articleMergeAgent);
    order.verify(titleAgent).generate(any());
    order.verify(outlineAgent).generate(any(), any());
    order.verify(contentAgent).generate(any(), any());
    order.verify(imageRequirementAgent).generate(any());
    order.verify(imageAgent).generate(any(), any());
    order.verify(articleMergeAgent).merge(any());
    assertEquals(ArticleConstant.STATUS_COMPLETED, article.getStatus());
    verify(sseEmitterService).complete(eq("t1"),
            eq(SseMessageTypeEnum.ALL_COMPLETE), any());
}
```

Add a second test where `titleAgent.generate` throws and assert `FAILED`, a nonblank `errorMessage`, mapper update, and terminal `ERROR`.

- [ ] **Step 2: Verify red**

Run: `JAVA_HOME=/Users/guoshao/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home ./mvnw -Dtest=ArticleGenerationTaskTest test`

Expected: compilation failure because `ArticleGenerationTask` does not exist.

- [ ] **Step 3: Implement orchestration**

The worker must:

```java
public void run(String taskId) {
    Article article = findByTaskId(taskId);
    ArticleState state = new ArticleState();
    state.setTaskId(taskId);
    state.setTopic(article.getTopic());
    try {
        article.setStatus(ArticleConstant.STATUS_PROCESSING);
        articleMapper.update(article);

        titleAgent.generate(state);
        send(taskId, AGENT1_COMPLETE, state.getTitle());
        outlineAgent.generate(state, message -> forwardPrefixed(taskId, message));
        send(taskId, AGENT2_COMPLETE, state.getOutline());
        contentAgent.generate(state, message -> forwardPrefixed(taskId, message));
        send(taskId, AGENT3_COMPLETE, state.getContent());
        imageRequirementAgent.generate(state);
        send(taskId, AGENT4_COMPLETE, state.getImageRequirements());
        imageAgent.generate(state, message -> forwardPrefixed(taskId, message));
        send(taskId, AGENT5_COMPLETE, state.getImages());
        articleMergeAgent.merge(state);
        send(taskId, MERGE_COMPLETE, state.getFullContent());

        copyStateToArticle(state, article);
        article.setStatus(ArticleConstant.STATUS_COMPLETED);
        article.setCompletedTime(LocalDateTime.now());
        article.setErrorMessage(null);
        articleMapper.update(article);
        sseEmitterService.complete(taskId, ALL_COMPLETE, state);
    } catch (RuntimeException exception) {
        article.setStatus(ArticleConstant.STATUS_FAILED);
        article.setErrorMessage(truncate(exception.getMessage(), 2000));
        articleMapper.update(article);
        sseEmitterService.complete(taskId, ERROR, article.getErrorMessage());
    }
}
```

Parse `TYPE:data` callbacks using only the first colon, map the prefix through `SseMessageTypeEnum`, and send the suffix as data. Serialize `outline` and `images` with `GsonUtils.toJson` when copying to the entity.

- [ ] **Step 4: Verify green**

Run the command from Step 2. Expected: success and failure tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/aiarticle/task/ArticleGenerationTask.java src/test/java/com/aiarticle/task/ArticleGenerationTaskTest.java
git commit -m "feat: orchestrate article generation pipeline"
```

### Task 4: Create and subscribe backend APIs

**Files:**
- Create: `src/main/java/com/aiarticle/model/dto/article/ArticleCreateRequest.java`
- Create: `src/main/java/com/aiarticle/model/vo/ArticleTaskVO.java`
- Create: `src/main/java/com/aiarticle/service/ArticleGenerationService.java`
- Create: `src/main/java/com/aiarticle/service/impl/ArticleGenerationServiceImpl.java`
- Create: `src/main/java/com/aiarticle/controller/ArticleController.java`
- Test: `src/test/java/com/aiarticle/service/impl/ArticleGenerationServiceImplTest.java`
- Test: `src/test/java/com/aiarticle/controller/ArticleControllerTest.java`

- [ ] **Step 1: Write failing service tests**

```java
@Test
void createsPendingArticleForCurrentUserAndSubmitsTask() {
    ArticleTaskVO result = service.create(" topic ", 9L);
    assertNotNull(result.taskId());
    verify(articleMapper).insert(argThat(article ->
            article.getUserId().equals(9L)
                    && article.getTopic().equals("topic")
                    && article.getStatus().equals(ArticleConstant.STATUS_PENDING)));
    verify(executor).execute(any());
}

@Test
void marksArticleFailedAndReturnsBusyErrorWhenExecutorRejects() {
    doThrow(new TaskRejectedException("full")).when(executor).execute(any());
    BusinessException error = assertThrows(BusinessException.class,
            () -> service.create("topic", 9L));
    assertEquals(ErrorCode.OPERATION_ERROR.getCode(), error.getCode());
    verify(articleMapper).update(argThat(article ->
            ArticleConstant.STATUS_FAILED.equals(article.getStatus())));
}

@Test
void refusesSubscriptionToAnotherUsersTask() {
    when(articleMapper.selectOneByQuery(any())).thenReturn(
            Article.builder().taskId("t1").userId(10L).build());
    assertThrows(BusinessException.class, () -> service.subscribe("t1", 9L));
}
```

- [ ] **Step 2: Verify service tests are red**

Run: `JAVA_HOME=/Users/guoshao/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home ./mvnw -Dtest=ArticleGenerationServiceImplTest test`

Expected: compilation failure because request/service/VO classes do not exist.

- [ ] **Step 3: Implement service and DTOs**

```java
public record ArticleCreateRequest(String topic) {}
public record ArticleTaskVO(String taskId) {}

public interface ArticleGenerationService {
    ArticleTaskVO create(String topic, long userId);
    SseEmitter subscribe(String taskId, long userId);
}
```

`create` trims and validates topic length 1–500, generates `IdUtil.fastSimpleUUID()`, inserts `PENDING`, then calls the named `AsyncTaskExecutor.execute(() -> articleGenerationTask.run(taskId))`. Catch `TaskRejectedException`, mark the inserted row `FAILED`, and throw `BusinessException(OPERATION_ERROR, "生成任务已满，请稍后重试")`. `subscribe` queries by `taskId`, returns not-found for no row and no-auth when `article.userId != userId`, then delegates to `SseEmitterService`.

- [ ] **Step 4: Write and verify controller tests**

```java
@Test
void createUsesLoggedInUser() {
    User user = User.builder().id(9L).build();
    when(userService.getLoginUser(request)).thenReturn(user);
    when(articleGenerationService.create("topic", 9L))
            .thenReturn(new ArticleTaskVO("t1"));
    assertEquals("t1",
            controller.create(new ArticleCreateRequest("topic"), request).getData().taskId());
}

@Test
void streamReturnsEmitterForOwnedTask() {
    User user = User.builder().id(9L).build();
    when(userService.getLoginUser(request)).thenReturn(user);
    when(articleGenerationService.subscribe("t1", 9L)).thenReturn(emitter);
    assertSame(emitter, controller.stream("t1", request));
}
```

Controller signatures:

```java
@PostMapping("/create")
public BaseResponse<ArticleTaskVO> create(
        @RequestBody ArticleCreateRequest body, HttpServletRequest request)

@GetMapping(value = "/stream/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter stream(
        @PathVariable String taskId, HttpServletRequest request)
```

Run: `JAVA_HOME=/Users/guoshao/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home ./mvnw -Dtest=ArticleControllerTest,ArticleGenerationServiceImplTest test`

Expected: all service and controller tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/aiarticle/model/dto/article src/main/java/com/aiarticle/model/vo/ArticleTaskVO.java src/main/java/com/aiarticle/service/ArticleGenerationService.java src/main/java/com/aiarticle/service/impl/ArticleGenerationServiceImpl.java src/main/java/com/aiarticle/controller/ArticleController.java src/test/java/com/aiarticle/service/impl/ArticleGenerationServiceImplTest.java src/test/java/com/aiarticle/controller/ArticleControllerTest.java
git commit -m "feat: expose async article creation and SSE APIs"
```

### Task 5: Connect the Vue workbench

**Files:**
- Create: `fronted/src/api/article.ts`
- Modify: `fronted/src/views/WriteView.vue`

- [ ] **Step 1: Add the typed article API**

```ts
import { post } from '@/request'

export interface ArticleTask {
  taskId: string
}

export interface ArticleSseEvent<T = unknown> {
  taskId: string
  type: string
  data: T
  timestamp: number
}

export function createArticle(topic: string) {
  return post<ArticleTask>('/article/create', { topic })
}

export function articleStreamUrl(taskId: string) {
  return `/article/stream/${encodeURIComponent(taskId)}`
}
```

- [ ] **Step 2: Replace fake generation with SSE**

In `WriteView.vue`, add `errorMessage`, `progressMessage`, and a module-scoped `EventSource | null`. `generate()` must call `createArticle(form.topic.trim())`, open `new EventSource(articleStreamUrl(taskId), { withCredentials: true })`, and register listeners for each enum value.

Behavior:

```ts
source.addEventListener('AGENT3_STREAMING', (event) => {
  const payload = JSON.parse((event as MessageEvent).data) as ArticleSseEvent<string>
  preview.value += String(payload.data)
})
source.addEventListener('MERGE_COMPLETE', (event) => {
  const payload = JSON.parse((event as MessageEvent).data) as ArticleSseEvent<string>
  preview.value = String(payload.data)
})
source.addEventListener('ALL_COMPLETE', finishSuccessfully)
source.addEventListener('ERROR', finishWithError)
source.onerror = () => finishWithError('生成连接已断开，请稍后查看文章状态')
```

Close an existing source before reset/new generation and in `onBeforeUnmount`. Disable the generation button while running, show the current stage near the button, preserve the existing local draft save when `ALL_COMPLETE` arrives, and display backend/SSE errors.

- [ ] **Step 3: Verify the frontend build**

Run: `npm run build` from `fronted/`.

Expected: type-check and Vite build both exit 0.

- [ ] **Step 4: Commit**

```bash
git add fronted/src/api/article.ts fronted/src/views/WriteView.vue
git commit -m "feat: stream article generation in workbench"
```

### Task 6: Documentation and full verification

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Update documentation**

Replace “后续” and “尚未实现” statements with the implemented contract:

```text
POST /article/create                 登录；返回 taskId
GET  /article/stream/{taskId}        登录且仅限任务所属用户；SSE
```

Document the five-thread, zero-queue executor, immediate busy response for the sixth task, 30-minute SSE timeout, event buffering before subscription, and disconnect-does-not-cancel semantics.

- [ ] **Step 2: Run all backend tests**

Run: `JAVA_HOME=/Users/guoshao/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home ./mvnw test`

Expected: exit 0 with zero test failures.

- [ ] **Step 3: Run the frontend production build**

Run: `npm run build` from `fronted/`.

Expected: exit 0.

- [ ] **Step 4: Inspect the final diff**

Run: `git diff --check && git status --short`

Expected: no whitespace errors; only planned files remain uncommitted.

- [ ] **Step 5: Commit documentation**

```bash
git add README.md
git commit -m "docs: document asynchronous SSE generation"
```
