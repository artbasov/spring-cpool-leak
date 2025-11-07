# About
A simple reproducer of the r2dbc-pool connection leak.

# Prerequisites
1. A project created with [Spring Initializer](https://start.spring.io/) with [this config](https://start.spring.io/#!type=gradle-project-kotlin&language=kotlin&platformVersion=3.5.7&packaging=jar&configurationFileFormat=properties&jvmVersion=21&groupId=com.example&artifactId=demo&name=demo&description=Demo%20project%20for%20Spring%20Boot&packageName=com.example.demo&dependencies=webflux,data-r2dbc,postgresql)
2. Create Controller with `@RequestMapping` that calls to a Service transactional method that:
   1. Accepts `delay` to imitate latency of PG query processing (done via [pg_sleep](https://www.postgresql.org/docs/current/functions-datetime.html#FUNCTIONS-DATETIME-DELAY))
   2. Handles exceptions from repository call (that have that pg_sleep delay) by creating new transaction via `TransactionalOperator` with `PROPAGATION_REQUIRES_NEW` and `ISOLATION_READ_COMMITTED`
and inserts another entity (`AuditLogEntry` in the example)

# Steps to reproduce
1. Do 10 (`spring.r2dbc.pool.max-size`) times:
   1. call `curl --request GET --url 'http://localhost:8080/entities'` (default `delay` is `2000ms`, use `delay_ms` query param if needed)
   2. cancel request with `Ctrl+C` signal. Has to be done before query start returning results
2. Do 11th request without cancellation. Observe denial of the request service (infinite processing if no timeouts)
3. Verify all connections leaked via querying
```sql
select NOW() - query_start as est,* from pg_stat_activity
where application_name = 'r2dbc-postgresql'
```
Should be something like this:

| est | state | wait\_event | query | datid | datname | pid | leader\_pid | usesysid | usename | application\_name | client\_addr | client\_hostname | client\_port | backend\_start | xact\_start | query\_start | state\_change | wait\_event\_type | backend\_xid | backend\_xmin | query\_id | backend\_type |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| 0 years 0 mons 0 days 0 hours 0 mins 31.606435 secs | idle in transaction | ClientRead | SELECT pg\_sleep\($1\), id, name FROM entity | 16384 | cpool-leak | 3320 | null | 10 | postgres | r2dbc-postgresql | 192.168.65.1 | null | 57447 | 2025-11-07 12:31:54.677392 +00:00 | 2025-11-07 12:32:01.250153 +00:00 | 2025-11-07 12:32:01.284584 +00:00 | 2025-11-07 12:32:03.292572 +00:00 | Client | null | null | null | client backend |
| 0 years 0 mons 0 days 0 hours 0 mins 25.444702 secs | idle in transaction | ClientRead | SELECT pg\_sleep\($1\), id, name FROM entity | 16384 | cpool-leak | 3319 | null | 10 | postgres | r2dbc-postgresql | 192.168.65.1 | null | 36365 | 2025-11-07 12:31:54.657841 +00:00 | 2025-11-07 12:32:07.440278 +00:00 | 2025-11-07 12:32:07.446317 +00:00 | 2025-11-07 12:32:09.454568 +00:00 | Client | null | null | null | client backend |
| 0 years 0 mons 0 days 0 hours 0 mins 24.234579 secs | idle in transaction | ClientRead | SELECT pg\_sleep\($1\), id, name FROM entity | 16384 | cpool-leak | 3318 | null | 10 | postgres | r2dbc-postgresql | 192.168.65.1 | null | 18114 | 2025-11-07 12:31:54.636463 +00:00 | 2025-11-07 12:32:08.647755 +00:00 | 2025-11-07 12:32:08.656440 +00:00 | 2025-11-07 12:32:10.661039 +00:00 | Client | null | null | null | client backend |
| 0 years 0 mons 0 days 0 hours 0 mins 23.01125 secs | idle in transaction | ClientRead | SELECT pg\_sleep\($1\), id, name FROM entity | 16384 | cpool-leak | 3317 | null | 10 | postgres | r2dbc-postgresql | 192.168.65.1 | null | 22292 | 2025-11-07 12:31:54.614972 +00:00 | 2025-11-07 12:32:09.872444 +00:00 | 2025-11-07 12:32:09.879769 +00:00 | 2025-11-07 12:32:11.887494 +00:00 | Client | null | null | null | client backend |
| 0 years 0 mons 0 days 0 hours 0 mins 21.733991 secs | idle in transaction | ClientRead | SELECT pg\_sleep\($1\), id, name FROM entity | 16384 | cpool-leak | 3316 | null | 10 | postgres | r2dbc-postgresql | 192.168.65.1 | null | 41623 | 2025-11-07 12:31:54.593598 +00:00 | 2025-11-07 12:32:11.152034 +00:00 | 2025-11-07 12:32:11.157028 +00:00 | 2025-11-07 12:32:13.159517 +00:00 | Client | null | null | null | client backend |
| 0 years 0 mons 0 days 0 hours 0 mins 20.312086 secs | idle in transaction | ClientRead | SELECT pg\_sleep\($1\), id, name FROM entity | 16384 | cpool-leak | 3315 | null | 10 | postgres | r2dbc-postgresql | 192.168.65.1 | null | 35925 | 2025-11-07 12:31:54.566679 +00:00 | 2025-11-07 12:32:12.573245 +00:00 | 2025-11-07 12:32:12.578933 +00:00 | 2025-11-07 12:32:14.581566 +00:00 | Client | null | null | null | client backend |
| 0 years 0 mons 0 days 0 hours 0 mins 13.644517 secs | idle in transaction | ClientRead | SELECT pg\_sleep\($1\), id, name FROM entity | 16384 | cpool-leak | 3314 | null | 10 | postgres | r2dbc-postgresql | 192.168.65.1 | null | 20198 | 2025-11-07 12:31:54.544181 +00:00 | 2025-11-07 12:32:19.237828 +00:00 | 2025-11-07 12:32:19.246502 +00:00 | 2025-11-07 12:32:21.250023 +00:00 | Client | null | null | null | client backend |
| 0 years 0 mons 0 days 0 hours 0 mins 12.498835 secs | idle in transaction | ClientRead | SELECT pg\_sleep\($1\), id, name FROM entity | 16384 | cpool-leak | 3313 | null | 10 | postgres | r2dbc-postgresql | 192.168.65.1 | null | 16711 | 2025-11-07 12:31:54.518108 +00:00 | 2025-11-07 12:32:20.386066 +00:00 | 2025-11-07 12:32:20.392184 +00:00 | 2025-11-07 12:32:22.400643 +00:00 | Client | null | null | null | client backend |
| 0 years 0 mons 0 days 0 hours 0 mins 11.430473 secs | idle in transaction | ClientRead | SELECT pg\_sleep\($1\), id, name FROM entity | 16384 | cpool-leak | 3312 | null | 10 | postgres | r2dbc-postgresql | 192.168.65.1 | null | 21791 | 2025-11-07 12:31:54.481916 +00:00 | 2025-11-07 12:32:21.452915 +00:00 | 2025-11-07 12:32:21.460546 +00:00 | 2025-11-07 12:32:23.464249 +00:00 | Client | null | null | null | client backend |
| 0 years 0 mons 0 days 0 hours 0 mins 10.42177 secs | idle in transaction | ClientRead | SELECT pg\_sleep\($1\), id, name FROM entity | 16384 | cpool-leak | 3311 | null | 10 | postgres | r2dbc-postgresql | 192.168.65.1 | null | 25588 | 2025-11-07 12:31:54.356524 +00:00 | 2025-11-07 12:32:22.463041 +00:00 | 2025-11-07 12:32:22.469249 +00:00 | 2025-11-07 12:32:24.477659 +00:00 | Client | null | null | null | client backend |

Note all sessions are `idle in transaction` and in `ClientRead` that suggests that cancellation happened before `i.r.p.client.ReactorNettyClient          : Response: ParseComplete{}`
and no attempts were made to cancel the running query. Possibly due to lost `TransactionSynchronization` ?
```java
2025-11-07T15:38:30.257+03:00  WARN 49567 --- [spring-cpool-leak] [ctor-http-nio-4] reactor.core.publisher.FluxUsingWhen     : Async resource cleanup failed after cancel

java.lang.IllegalStateException: Transaction synchronization is not active
	at org.springframework.transaction.reactive.TransactionSynchronizationManager.getSynchronizations(TransactionSynchronizationManager.java:233) ~[spring-tx-6.2.12.jar:6.2.12]
	at org.springframework.transaction.reactive.AbstractReactiveTransactionManager.triggerBeforeCompletion(AbstractReactiveTransactionManager.java:654) ~[spring-tx-6.2.12.jar:6.2.12]
	at org.springframework.transaction.reactive.AbstractReactiveTransactionManager.processRollback(AbstractReactiveTransactionManager.java:548) ~[spring-tx-6.2.12.jar:6.2.12]
	at org.springframework.transaction.reactive.AbstractReactiveTransactionManager.lambda$rollback$43(AbstractReactiveTransactionManager.java:535) ~[spring-tx-6.2.12.jar:6.2.12]
	at reactor.core.publisher.MonoFlatMap$FlatMapMain.onNext(MonoFlatMap.java:132) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.FluxMap$MapSubscriber.onNext(FluxMap.java:122) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$ScalarSubscription.request(Operators.java:2570) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.FluxMap$MapSubscriber.request(FluxMap.java:164) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.MonoFlatMap$FlatMapMain.request(MonoFlatMap.java:194) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.FluxUsingWhen$CancelInner.onSubscribe(FluxUsingWhen.java:565) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.MonoFlatMap$FlatMapMain.onSubscribe(MonoFlatMap.java:117) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.FluxMap$MapSubscriber.onSubscribe(FluxMap.java:92) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.MonoJust.subscribe(MonoJust.java:55) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.MonoDeferContextual.subscribe(MonoDeferContextual.java:55) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.FluxFromMonoOperator.subscribe(FluxFromMonoOperator.java:85) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.FluxUsingWhen$UsingWhenSubscriber.cancel(FluxUsingWhen.java:333) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$DeferredSubscription.cancel(Operators.java:1695) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.MonoUsingWhen$ResourceSubscriber.cancel(MonoUsingWhen.java:231) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.drainLoop(Operators.java:2424) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.drain(Operators.java:2392) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.cancel(Operators.java:2204) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.MonoFlatMap$FlatMapMain.cancel(MonoFlatMap.java:207) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.cancel(FluxContextWrite.java:141) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.cancel(FluxContextWrite.java:141) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.cancel(FluxContextWrite.java:141) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators.terminate(Operators.java:1329) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.StrictSubscriber.cancel(StrictSubscriber.java:155) ~[reactor-core-3.7.12.jar:3.7.12]
	at kotlinx.coroutines.reactor.MonoKt$awaitSingleOrNull$2$1$onSubscribe$1.invoke(Mono.kt:47) ~[kotlinx-coroutines-reactor-1.8.1.jar:na]
	at kotlinx.coroutines.reactor.MonoKt$awaitSingleOrNull$2$1$onSubscribe$1.invoke(Mono.kt:47) ~[kotlinx-coroutines-reactor-1.8.1.jar:na]
	at kotlinx.coroutines.CancelHandler$UserSupplied.invoke(CancellableContinuationImpl.kt:660) ~[kotlinx-coroutines-core-jvm-1.8.1.jar:na]
	at kotlinx.coroutines.CancellableContinuationImpl.callCancelHandler(CancellableContinuationImpl.kt:245) ~[kotlinx-coroutines-core-jvm-1.8.1.jar:na]
	at kotlinx.coroutines.CancellableContinuationImpl.cancel(CancellableContinuationImpl.kt:208) ~[kotlinx-coroutines-core-jvm-1.8.1.jar:na]
	at kotlinx.coroutines.CancellableContinuationImpl.parentCancelled$kotlinx_coroutines_core(CancellableContinuationImpl.kt:220) ~[kotlinx-coroutines-core-jvm-1.8.1.jar:na]
	at kotlinx.coroutines.ChildContinuation.invoke(JobSupport.kt:1457) ~[kotlinx-coroutines-core-jvm-1.8.1.jar:na]
	at kotlinx.coroutines.JobSupport.notifyCancelling(JobSupport.kt:1483) ~[kotlinx-coroutines-core-jvm-1.8.1.jar:na]
	at kotlinx.coroutines.JobSupport.tryMakeCancelling(JobSupport.kt:806) ~[kotlinx-coroutines-core-jvm-1.8.1.jar:na]
	at kotlinx.coroutines.JobSupport.makeCancelling(JobSupport.kt:766) ~[kotlinx-coroutines-core-jvm-1.8.1.jar:na]
	at kotlinx.coroutines.JobSupport.cancelImpl$kotlinx_coroutines_core(JobSupport.kt:682) ~[kotlinx-coroutines-core-jvm-1.8.1.jar:na]
	at kotlinx.coroutines.JobSupport.cancelInternal(JobSupport.kt:643) ~[kotlinx-coroutines-core-jvm-1.8.1.jar:na]
	at kotlinx.coroutines.JobSupport.cancel(JobSupport.kt:628) ~[kotlinx-coroutines-core-jvm-1.8.1.jar:na]
	at kotlinx.coroutines.Job$DefaultImpls.cancel$default(Job.kt:195) ~[kotlinx-coroutines-core-jvm-1.8.1.jar:na]
	at kotlinx.coroutines.reactor.MonoCoroutine.dispose(Mono.kt:120) ~[kotlinx-coroutines-reactor-1.8.1.jar:na]
	at reactor.core.publisher.FluxCreate$SinkDisposable.dispose(FluxCreate.java:1120) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.MonoCreate$DefaultMonoSink.disposeResource(MonoCreate.java:329) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.MonoCreate$DefaultMonoSink.cancel(MonoCreate.java:316) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.FluxFilter$FilterSubscriber.cancel(FluxFilter.java:191) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.drainLoop(Operators.java:2424) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.drain(Operators.java:2392) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.cancel(Operators.java:2204) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.FluxHandle$HandleSubscriber.cancel(FluxHandle.java:277) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.FluxMap$MapSubscriber.cancel(FluxMap.java:169) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.MonoSingle$SingleSubscriber.doOnCancel(MonoSingle.java:108) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$MonoInnerProducerBase.cancel(Operators.java:2984) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.drainLoop(Operators.java:2424) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.drain(Operators.java:2392) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.cancel(Operators.java:2204) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.MonoFlatMap$FlatMapMain.cancel(MonoFlatMap.java:199) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.cancel(FluxContextWrite.java:141) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.FluxOnAssembly$OnAssemblySubscriber.cancel(FluxOnAssembly.java:654) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.drainLoop(Operators.java:2424) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.drain(Operators.java:2392) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.cancel(Operators.java:2204) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.MonoFlatMap$FlatMapMain.cancel(MonoFlatMap.java:207) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.MonoFlatMap$FlatMapMain.cancel(MonoFlatMap.java:207) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.MonoPeekTerminal$MonoTerminalPeekSubscriber.cancel(MonoPeekTerminal.java:144) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.drainLoop(Operators.java:2424) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.drain(Operators.java:2392) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.cancel(Operators.java:2204) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.MonoPeekTerminal$MonoTerminalPeekSubscriber.cancel(MonoPeekTerminal.java:144) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.drainLoop(Operators.java:2424) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.drain(Operators.java:2392) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.cancel(Operators.java:2204) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.MonoPeekTerminal$MonoTerminalPeekSubscriber.cancel(MonoPeekTerminal.java:144) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.drainLoop(Operators.java:2424) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.drain(Operators.java:2392) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.cancel(Operators.java:2204) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.MonoPeekTerminal$MonoTerminalPeekSubscriber.cancel(MonoPeekTerminal.java:144) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.drainLoop(Operators.java:2424) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.drain(Operators.java:2392) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators$MultiSubscriptionSubscriber.cancel(Operators.java:2204) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.FluxTap$TapSubscriber.cancel(FluxTap.java:322) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.MonoIgnoreThen$ThenIgnoreMain.cancel(MonoIgnoreThen.java:144) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.MonoPeekTerminal$MonoTerminalPeekSubscriber.cancel(MonoPeekTerminal.java:144) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.MonoPeekTerminal$MonoTerminalPeekSubscriber.cancel(MonoPeekTerminal.java:144) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.core.publisher.Operators.terminate(Operators.java:1329) ~[reactor-core-3.7.12.jar:3.7.12]
	at reactor.netty.channel.ChannelOperations.terminate(ChannelOperations.java:514) ~[reactor-netty-core-1.2.11.jar:1.2.11]
	at reactor.netty.http.server.HttpServerOperations.onInboundClose(HttpServerOperations.java:933) ~[reactor-netty-http-1.2.11.jar:1.2.11]
	at reactor.netty.channel.ChannelOperationsHandler.channelInactive(ChannelOperationsHandler.java:73) ~[reactor-netty-core-1.2.11.jar:1.2.11]
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelInactive(AbstractChannelHandlerContext.java:303) ~[netty-transport-4.1.128.Final.jar:4.1.128.Final]
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelInactive(AbstractChannelHandlerContext.java:281) ~[netty-transport-4.1.128.Final.jar:4.1.128.Final]
	at io.netty.channel.AbstractChannelHandlerContext.fireChannelInactive(AbstractChannelHandlerContext.java:274) ~[netty-transport-4.1.128.Final.jar:4.1.128.Final]
	at io.netty.channel.CombinedChannelDuplexHandler$DelegatingChannelHandlerContext.fireChannelInactive(CombinedChannelDuplexHandler.java:418) ~[netty-transport-4.1.128.Final.jar:4.1.128.Final]
	at io.netty.handler.codec.ByteToMessageDecoder.channelInputClosed(ByteToMessageDecoder.java:412) ~[netty-codec-4.1.128.Final.jar:4.1.128.Final]
	at io.netty.handler.codec.ByteToMessageDecoder.channelInactive(ByteToMessageDecoder.java:377) ~[netty-codec-4.1.128.Final.jar:4.1.128.Final]
	at io.netty.channel.CombinedChannelDuplexHandler.channelInactive(CombinedChannelDuplexHandler.java:221) ~[netty-transport-4.1.128.Final.jar:4.1.128.Final]
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelInactive(AbstractChannelHandlerContext.java:303) ~[netty-transport-4.1.128.Final.jar:4.1.128.Final]
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelInactive(AbstractChannelHandlerContext.java:281) ~[netty-transport-4.1.128.Final.jar:4.1.128.Final]
	at io.netty.channel.AbstractChannelHandlerContext.fireChannelInactive(AbstractChannelHandlerContext.java:274) ~[netty-transport-4.1.128.Final.jar:4.1.128.Final]
	at io.netty.channel.DefaultChannelPipeline$HeadContext.channelInactive(DefaultChannelPipeline.java:1352) ~[netty-transport-4.1.128.Final.jar:4.1.128.Final]
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelInactive(AbstractChannelHandlerContext.java:301) ~[netty-transport-4.1.128.Final.jar:4.1.128.Final]
	at io.netty.channel.AbstractChannelHandlerContext.invokeChannelInactive(AbstractChannelHandlerContext.java:281) ~[netty-transport-4.1.128.Final.jar:4.1.128.Final]
	at io.netty.channel.DefaultChannelPipeline.fireChannelInactive(DefaultChannelPipeline.java:850) ~[netty-transport-4.1.128.Final.jar:4.1.128.Final]
	at io.netty.channel.AbstractChannel$AbstractUnsafe$7.run(AbstractChannel.java:811) ~[netty-transport-4.1.128.Final.jar:4.1.128.Final]
	at io.netty.util.concurrent.AbstractEventExecutor.runTask$$$capture(AbstractEventExecutor.java:173) ~[netty-common-4.1.128.Final.jar:4.1.128.Final]
	at io.netty.util.concurrent.AbstractEventExecutor.runTask(AbstractEventExecutor.java) ~[netty-common-4.1.128.Final.jar:4.1.128.Final]
	at io.netty.util.concurrent.AbstractEventExecutor.safeExecute$$$capture(AbstractEventExecutor.java:166) ~[netty-common-4.1.128.Final.jar:4.1.128.Final]
	at io.netty.util.concurrent.AbstractEventExecutor.safeExecute(AbstractEventExecutor.java) ~[netty-common-4.1.128.Final.jar:4.1.128.Final]
	at io.netty.util.concurrent.SingleThreadEventExecutor.runAllTasks(SingleThreadEventExecutor.java:472) ~[netty-common-4.1.128.Final.jar:4.1.128.Final]
	at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:566) ~[netty-transport-4.1.128.Final.jar:4.1.128.Final]
	at io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:998) ~[netty-common-4.1.128.Final.jar:4.1.128.Final]
	at io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74) ~[netty-common-4.1.128.Final.jar:4.1.128.Final]
	at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30) ~[netty-common-4.1.128.Final.jar:4.1.128.Final]
	at java.base/java.lang.Thread.run(Thread.java:1583) ~[na:na]
```