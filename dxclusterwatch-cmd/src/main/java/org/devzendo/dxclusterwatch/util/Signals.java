package org.devzendo.dxclusterwatch.util;

import sun.misc.Signal;
import sun.misc.SignalHandler;
@SuppressWarnings("restriction")

public class Signals {
	public static enum SignalName { CONT, INT };
	public static SignalHandler withHandler(final Runnable handler, final SignalName signalname) {
		final Signal sig = new Signal(signalname.name());
		final SignalHandler newHandler = new SignalHandler() {

			@Override
			public void handle(final Signal sig) {
				handler.run();
			}};
		final SignalHandler oldHandler = Signal.handle(sig, newHandler);
		return oldHandler;
	}
}
