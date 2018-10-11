package com.sangha.weather.client;

import com.sangha.weather.device.inf.CeilometerDeviceInterface;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CeilometerClientDeviceFactory {
    public static Logger logger = LoggerFactory.getLogger(CeilometerClientDeviceFactory.class);
    private static final List<CeilometerDeviceInterface> instances = new ArrayList<>();

    /**
     *
     * @param deviceName
     * @return
     * @throws Exception
     */
    public static CeilometerDeviceInterface createClient(String deviceName) throws Exception {
        CeilometerDeviceInterface obj = null;

        for(Class clazz : new Reflections("com.sangha.weather.device").getSubTypesOf(CeilometerDeviceInterface.class)) {
            try {
                if (clazz.getName().indexOf(deviceName) > 0)
                    obj = (CeilometerDeviceInterface)clazz.newInstance();
            } catch(Exception e) {
                logger.error(e.toString());
            }
        }

        return obj;
    }

    /**
     *
     * @param clazz
     * @return
     */
    public CeilometerDeviceInterface getByClass(Class<? extends CeilometerDeviceInterface> clazz) {
        /* Java 8 Version */
        //return instances.stream().filter(instance -> instance.getClass().equals(clazz)).findFirst().orElse(null);

        /* Java 7 Version */
        for(CeilometerDeviceInterface c : instances) {
            if(c.getClass().equals(clazz)) {
                return c;
            }
        }

        return null;
    }

    /**
     *
     * @return
     */
    public Collection<CeilometerDeviceInterface> getAll() {
        return Collections.unmodifiableCollection(instances);
    }

    public List<CeilometerDeviceInterface> getList() { return instances; }
}
