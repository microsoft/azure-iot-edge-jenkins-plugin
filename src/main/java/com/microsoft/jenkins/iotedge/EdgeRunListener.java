package com.microsoft.jenkins.iotedge;

import hudson.Extension;
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.tasks.BuildStep;

import java.util.HashMap;
import java.util.Map;

@Extension
public class EdgeRunListener extends RunListener<Run<?, ?>> {
    Map<Run, Long> timeMap;

    public EdgeRunListener() {
        timeMap = new HashMap<>();
    }
    @Override
    public void onStarted(Run r, TaskListener listener) {
        timeMap.put(r, System.currentTimeMillis());
    }

    @Override
    public void onCompleted(Run<?, ?> r, TaskListener listener) {
        String x = r.getDescription();
        long start = timeMap.get(r);
        long span = (System.currentTimeMillis() - start)/1000;
        boolean success = r.getResult() == Result.SUCCESS;
        AzureIoTEdgePlugin.sendEvent(success, span, r.getId(), r.getId(), r.getClass().getName());
    }
}
