package com.vp.infra.reactor;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.json.JsonObject;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

import com.lmax.disruptor.EventHandler;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

public class EventConsumer implements EventHandler<Buffer> {

	JedisPool pool = new JedisPool("localhost");
	Jedis jedis = null;
	Pipeline pipe = null;
	MongoClient mongoClient = null;
	DBCollection collection = null;

	List<DBObject> checkins = new ArrayList<DBObject>();

	public EventConsumer() {
		try {
			mongoClient = new MongoClient( "localhost" );
			collection = mongoClient.getDB("test").getCollection("checkins");
		} catch (final UnknownHostException e) {
			e.printStackTrace();
		}

		jedis = pool.getResource();
		pipe = jedis.pipelined();

	}

	@Override
	public void onEvent(Buffer buffer, long seq, boolean eob) throws Exception {
		try {
			final JsonObject json = new JsonObject(buffer.toString());
			final String userId = json.getString("userId");
			final Double latitude = (Double) json.getNumber("latitude");
			final Double longitude = (Double) json.getNumber("longitude");

			final long slot5 = System.currentTimeMillis() / 5000;

			final int lat10 = (int)(latitude * 10);
			final int long10 = (int)(longitude * 10);

			final String pointsLong10 = "ci:10:" + slot5 + ":long";
			final String pointsLat10 = "ci:10:" + slot5 + ":lat";
			final String pointsCounter10 = "ci:10:" + slot5 + ":" + lat10 + ":" + long10;


			jedis.zadd(pointsLat10, lat10, lat10 + ":" + long10);
			jedis.zadd(pointsLong10, long10, lat10 + ":" + long10);
			jedis.incr(pointsCounter10);

			final BasicDBObject basicDBObject = new BasicDBObject("user_id", userId);
			basicDBObject.put("loc", new double[]{latitude, longitude});
			checkins.add(basicDBObject);

			if (checkins.size() > 1000) {
				collection.insert(checkins);
				checkins.clear();
			}
		} catch (final Exception e) {
			//System.out.println(e.getMessage());
		}
	}
}
