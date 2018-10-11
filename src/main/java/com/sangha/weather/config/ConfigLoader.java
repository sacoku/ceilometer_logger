package com.sangha.weather.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class ConfigLoader {
    public static Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
    private final List<Device> devices = new ArrayList<>();
    private ConfigRoot deviceRoot = null;
    static boolean isThrowable = false;
    static ConfigLoader instance = null;
    enum deviceTokenValue { LABEL, DEVICE_TYPE, IP, PORT, PATH };

    public static ConfigLoader getInstance() {
        if(instance == null) instance = new ConfigLoader();
        return instance;
    }

    public static ConfigLoader setInstance(ConfigLoader inst) {
        instance = inst;

        return inst;
    }

    public void recursiveDirs(File path) throws Exception {
        File[] files = path.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                recursiveDirs(file);
            } else {
                if(FileSystems.getDefault().getPathMatcher("glob:**.yml").matches(file.toPath())) {
                    try {
                        logger.info("load " + file.toString() + " ....");
                        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                        Device device = mapper.readValue(file, Device.class);
                        if(device.getLabel().toLowerCase().equals(file.getName().substring(0, file.getName().lastIndexOf('.')).toLowerCase()))
                            devices.add(device);
                        else
                            throw new Exception("File Name과 Device Label이 틀립니다.");
                        logger.info("load " + file + " complete");
                    } catch(Exception e) {
                        logger.error("Load error " + file, e);
                        isThrowable = true;
                    }
                }

            }
        }
    }

    /**
     * path의 yml파일들을 load해서 devices arrayList에 저장
     * @param path : config파일의 Path
     * @return
     * @throws Exception
     */
    public ConfigLoader load(String path) throws Exception {
        String file = path + File.separator + "config.yml";

        logger.info("load " + file + " ....");
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        try {
            deviceRoot = mapper.readValue(new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF8")), ConfigRoot.class);

            for (String s : deviceRoot.getDevices()) {
                int i = 0;
                Device d = null;
                String[] tokens = s.split(",");
                for (String t : tokens) {
                    switch (i) {
                        case 0 :
                            d = new Device();
                            d.setLabel(t.trim());
                            break;
                        case 1:
                            d.setDevicetype(getDeviceTypeByNum(Integer.parseInt(t.trim())));
                            break;
                        case 2:
                            d.setIp(t.trim());
                            break;
                        case 3:
                            d.setPort(Integer.parseInt(t.trim()));
                            break;
                        case 4:
                            d.setPath(t.trim());
                            break;
                    }
                    i++;
                }

                if (i > 0) {
                    String hashVal = d.getLabel() + d.getType() + d.getIp() + String.valueOf(d.getPort()) + d.getPath();
                    logger.info(d.getLabel() + " Device를 로드 했습니다.");
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    digest.reset();
                    digest.update(hashVal.getBytes("utf8"));
                    hashVal = String.format("%040x", new BigInteger(1, digest.digest()));
                    d.setSha256(hashVal);
                    devices.add(d);
                }
            }
        } catch(Exception e) {
            logger.error("Config파일 로드가 실패 되었습니다.", e);
            return null;
        }

        return this;
    }

    public String getDeviceTypeByNum(int n) {
        switch(n) {
            case 1:
                return "CL31Device";
            case 2:
                return "CBME80Device";
            case 3:
                return "MC20Device";
            default:
                return null;
        }
    }

    /**
     *
     * @param file
     * @return
     * @throws Exception
     */
    public Device loadConfigFile(String file) throws Exception {
        File f = new File(file);
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Device device=mapper.readValue(new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF8")), Device.class);
        if(device.getLabel().toLowerCase().equals(f.getName().substring(0, f.getName().lastIndexOf('.')).toLowerCase())) {
            devices.add(device);
            return device;
        } else
            return null;
    }

    /**
     * device ArrayList를 리턴
     * @return
     */
    public List<Device> getDeviceList() {
        /* For Java 8 version */
        return devices;
    }

    public String getSiteName() {
        return deviceRoot.getSiteName();
    }

    public String getDefaultPath() {
        return deviceRoot.getDefaultPath();
    }

    public static void main(String [] args) throws Exception {
        ConfigLoader loader = new ConfigLoader();

        loader.load("D:\\Shared\\Tools");
    }
}
