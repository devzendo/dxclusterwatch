package org.devzendo.dxclusterwatch.util;

import sun.misc.Signal;
import sun.misc.SignalHandler;
@SuppressWarnings("restriction")

public class Signals {
	public static enum SignalName { CONT, INT };
	public static SignalHandler withHandler(final Runnable runnable, final SignalName signalName) {
		final Signal sig = new Signal(signalName.name());
		final SignalHandler newHandler = new SignalHandler() {

			@Override
			public void handle(final Signal sig) {
				runnable.run();
			}};
		final SignalHandler oldHandler = Signal.handle(sig, newHandler);
		return oldHandler;
	}
	
	public static SignalHandler handle(final SignalHandler handler, final SignalName signalName) {
		return Signal.handle(new Signal(signalName.name()), handler);
	}
}
