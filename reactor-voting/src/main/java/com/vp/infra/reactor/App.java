package com.vp.infra.reactor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.lmax.disruptor.EventHandler;

/**
 * Hello world!
 *
 */
public class App extends Verticle {

	JedisPool pool = new JedisPool("localhost");

	@Override
	public void start() {
		final AtomicLong reqs = new AtomicLong(0);

		final RouteMatcher rm = new RouteMatcher();
		rm.options("/ci", new Handler<HttpServerRequest>() {

			@Override
			public void handle(HttpServerRequest req) {
				req.response().headers()
				.add("Access-Control-Allow-Origin", "*")
				.add("Access-Control-Allow-Headers", "*")
				.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");

				req.response().setStatusCode(204).end();
			}
		});

		final EventHandler<Buffer> c = new EventConsumer();
		rm.post("/checkin", new Handler<HttpServerRequest>() {

			@Override
			public void handle(HttpServerRequest req) {

				req.response().setStatusCode(200).end();
				reqs.incrementAndGet();

				req.bodyHandler(new Handler<Buffer>() {

					@Override
					public void handle(Buffer buffer) {
						try {
							c.onEvent(buffer, 0, false);
						} catch (final Exception e) {
							e.printStackTrace();
						}
					}
				});
			}
		});

		rm.get("/ci", new Handler<HttpServerRequest>() {

			@Override
			public void handle(HttpServerRequest req) {
				final long ts = Long.valueOf(req.params().get("ts"));
				final int lat1 = (int)(Double.valueOf(req.params().get("lat1")) * 10);
				final int lat2 = (int)(Double.valueOf(req.params().get("lat2")) * 10);
				final int long1 =(int)( Double.valueOf(req.params().get("long1")) * 10);
				final int long2 = (int)(Double.valueOf(req.params().get("long2")) * 10);

				final Map<String, Object> points = new HashMap<String, Object>();
				try (Jedis jedis = pool.getResource()) {
					//ZRANGEBYSCORE ci:10:43480537:long 342 345
					final Set<String> latPoints = jedis.zrangeByScore("ci:10:" + ts + ":lat", lat1, lat2);
					final Set<String> longPoints = jedis.zrangeByScore("ci:10:" + ts + ":long", long1, long2);

					latPoints.retainAll(longPoints);

					for (final String key : latPoints) {
						//"ci:10:43480537:323:344"
						points.put("\"" + key + "\"", jedis.get("ci:10:" + ts + ":" + key));
					}
				}

				final String msg = new JsonObject(points).toString();
				req.response().headers()
				.add("Access-Control-Allow-Origin", "*")
				.add("Access-Control-Allow-Headers", "*")
				.add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
				req.response().setStatusCode(200)
				.putHeader("Content-Type", "application/json")
				.putHeader("Content-Length", String.valueOf(msg.getBytes().length))
				.end(msg);
			}
		});

		vertx.setPeriodic(5000, new Handler<Long>() {

			@Override
			public void handle(Long event) {
				final long count = reqs.getAndSet(0);
				if (count > 0) {
					System.out.println("Avg req/s is: " + count/5);
				}
			}
		});

		vertx.createHttpServer().requestHandler(rm).setAcceptBacklog(200000).setTCPKeepAlive(false).listen(8080);
	}

	@Override
	public void stop() {
		super.stop();
	}

}
