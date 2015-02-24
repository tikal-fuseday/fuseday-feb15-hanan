package com.vp.infra.reactor;

import org.vertx.java.core.buffer.Buffer;

import com.lmax.disruptor.EventFactory;

public class BufferEventFactory implements EventFactory<Buffer> {

	public Buffer newInstance() {
		return new Buffer();
	}

}
