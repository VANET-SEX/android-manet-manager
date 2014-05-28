package org.span.service.vanetsex;

import java.util.List;
import java.util.Map;

public interface VANETObserver {

    public void onBeaconStateChanged(boolean started);
    
    public void onNeighborListChanged(Map<String, VANETNode> neighborMap);
    
    public void onStatisticData(VANETStatisticsData statisticsData);
    
    public void onMessageHistoryInit(List<VANETMessage> history);
    
    public void onMessageHistoryDiffUpdate(List<VANETMessage> diffHistory);
    
    public void onEventListChanged(List<VANETEvent> events);
}
