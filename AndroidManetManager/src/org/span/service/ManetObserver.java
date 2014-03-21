/**
 *  SPAN - Smart Phone Ad-Hoc Networking project
 *  Copyright (c) 2012 The MITRE Corporation.
 */
package org.span.service;

import org.span.service.core.ManetService.AdhocStateEnum;
import org.span.service.system.ManetConfig;



public interface ManetObserver {

	// callback methods
	
	public void onServiceConnected();
		
	public void onServiceDisconnected();
	
	public void onServiceStarted();
	
	public void onServiceStopped();
	
	public void onAdhocStateUpdated(AdhocStateEnum state, String info);
		
	public void onConfigUpdated(ManetConfig manetcfg);
	
	public void onError(String error);
	
}
