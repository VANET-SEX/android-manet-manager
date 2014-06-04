package org.span.service.vanetsex;

import java.util.List;
import java.util.Map;

public interface VANETServiceObserver {
    
    // Callback methods called from VANETService
    
    public void onEventListChanged(List<VANETEvent> events);

}
