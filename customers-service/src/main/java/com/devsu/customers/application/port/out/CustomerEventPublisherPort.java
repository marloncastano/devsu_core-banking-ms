package com.devsu.customers.application.port.out;

import com.devsu.customers.application.event.CustomerEvent;

public interface CustomerEventPublisherPort {

	/**
	 * Publishes the event once the current transaction commits, so rolled-back changes are never announced.
	 */
	void publish(CustomerEvent event);
}
