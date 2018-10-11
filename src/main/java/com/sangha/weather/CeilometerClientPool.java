package com.sangha.weather;

import com.sangha.weather.client.CeilometerClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CeilometerClientPool {
    private static final List<CeilometerClient> instances = new ArrayList<>();

    /**
     *
     * @param client
     * @return
     */
    public CeilometerClient add(CeilometerClient client) {
        if(client == null) return null;

        instances.add(client);

        return client;
    }

    /**
     *
     * @param deviceName
     * @return
     */
    public CeilometerClient getByLabel(String deviceName) {
        for(CeilometerClient c : instances) {
            if(c.getDevice().getLabel().equals(deviceName)) {
                return c;
            }
        }

        return null;
    }

    public CeilometerClient removeByLabel(String deviceName) {
        CeilometerClient cc = null;
        for(CeilometerClient c : instances) {
            if(c.getDevice().getLabel().equals(deviceName)) {
                cc =  c;
            }
        }

        if(cc != null) {
            instances.remove(cc);
            return cc;
        }

        return null;
    }

    /**
     *
     * @return
     */
    public int size() {
        return instances.size();
    }

    /**
     *
     * @return
     */
    public Collection<CeilometerClient> getAll() {
        return Collections.unmodifiableCollection(instances);
    }
}
