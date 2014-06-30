package org.span.service.vanetsex;

import java.util.List;
import java.util.Map;

import org.span.service.vanetsex.pingpong.VANETPingPongState;

public interface VANETServiceObserver {
    
    // Callback methods called from VANETService
    
    public void onEventListChanged(List<VANETEvent> events);
    
}
