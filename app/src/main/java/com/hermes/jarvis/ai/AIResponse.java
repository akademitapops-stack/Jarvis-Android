package com.hermes.jarvis.ai;

import com.hermes.jarvis.automation.SmartAutomation;

import java.util.ArrayList;
import java.util.List;

public class AIResponse {
    public String message = "";
    public String speakText = null;
    public List<String> commands = new ArrayList<>();
    public List<String> deviceActions = new ArrayList<>();
    public List<String> webSearches = new ArrayList<>();
    public String weatherLocation = null;
    public SmartAutomation.Rule autoCreate = null;
    public String autoDeleteName = null;
    public String memoryKey = null, memoryValue = null;
    public String scheduleMessage = null;
    public int scheduleMinutes = -1;
    public String replyPackage = null, replyMessage = null;
    public String callContact = null;
    public String contactSearch = null;
    public int reportHour = -1;
    public int reportMinute = -1;
    public boolean reportDisable = false;
    public boolean needsConfirmation = false;
    public String raw = "";
    public String model = "";
    public long timeMs = 0;
}
