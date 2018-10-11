/*****************************************************************************
 * History:
 *     1.1 : 최초 생성
 *     1.2 : 작달일사계(MC20) 추가
 *     1.3 :
 *        - logback config 파일 외부에서 읽어 드리도록 변경
 *        - file delete event 감지 및 해당 device 삭제
 *     1.4 :
 *        - Java 8 -> Java 7 지원으로 변경
 *        - Config File(Multiple -> Single)
 *     1.5 :
 *        - Config 파일 단순화 작업에 따른 소스 수정
 *        - Config 파일 한글 지원
 *        - 파일생성시 Prefix, Postfix 이름에 의거해서 생성
 *     1.6 :
 *        - Config파일에서 DeivceType을 번호로....(점점 허접해짐...ㅠㅜ)
 *     1.7 :
 *        - 단일 Config파일에서 Hotplug 기능 추가
 *     1.7.4 :
 *        - WatchService 버그 수정
 *     1.7.5 :
 *        - EventLoop(Max File Open bug 수정)
 *****************************************************************************/

package com.sangha.weather;

import com.sangha.weather.client.CeilometerClient;
import com.sangha.weather.config.ConfigLoader;
import com.sangha.weather.config.Device;
import com.sangha.weather.utils.CommonUtils;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.nio.file.*;

/**
 *
 */
public class CeilometerLogger {
    public static Logger logger = LoggerFactory.getLogger(CeilometerLogger.class);
    public static ConfigLoader loader = null;
    public static enum RunStatus { START, STOP };
    public static EventLoopGroup eventLoop = new NioEventLoopGroup();

    /**
     *
     * @param pool
     * @throws Exception
     */
    public static void start(CeilometerClientPool pool) throws Exception {
        for(CeilometerClient c : pool.getAll()) {
            c.start();
        }
    }

    /**
     *
     * @param pool
     * @throws Exception
     */
    public static void stop(CeilometerClientPool pool) throws Exception {
        for(CeilometerClient c : pool.getAll()) {
            c.stopClient();
        }
    }

    /**
     *
     * @param pool
     * @param l1
     * @param l2
     * @param mode
     * @throws Exception
     */
    public static void reload(CeilometerClientPool pool, ConfigLoader l1, ConfigLoader l2, RunStatus mode) throws Exception {
        if(l1 != null && l2 != null) {
            for (Device d : l1.getDeviceList()) {
                boolean isFind = false;
                for (Device d2 : l2.getDeviceList()) {
                    if (d.getSha256().equals(d2.getSha256())) {
                        isFind = true;
                    }
                }

                if (!isFind) {
                    if (mode == RunStatus.START) {
                        logger.info("[" + d.getLabel() + "] 를 시작합니다.");
                        pool.add(new CeilometerClient(d, eventLoop)).start();
                    } else if (mode == RunStatus.STOP) {
                        logger.info("[" + d.getLabel() + "] 를 중지합니다.");
                        CeilometerClient c = pool.removeByLabel(d.getLabel());
                        if (c != null) c.stopClient();
                    }
                }
            }
        }
    }

    public static String getVersion() {
        String version = null;

        // try to load from maven properties first
        /*
        try {
            Properties p = new Properties();
            InputStream is = CeilometerLogger.class.getResourceAsStream("/META-INF/maven/com.my.group/my-artefact/pom.properties");
            if (is != null) {
                p.load(is);
                version = p.getProperty("version", "");
            }
        } catch (Exception e) {
            // ignore
        }
        */

        // fallback to using Java API
        if (version == null) {
            Package aPackage = CeilometerLogger.class.getPackage();
            if (aPackage != null) {
                version = aPackage.getImplementationVersion();
                if (version == null) {
                    version = aPackage.getSpecificationVersion();
                }
            }
        }

        if (version == null) {
            // we could not compute the version so use a blank
            version = "";
        }

        return version;
    }

    public static void main(String[] args) throws Exception {
        try {
            if(args.length < 1) {
                System.out.println("CeilometerLogger [config-path]");
                return ;
            }

            logger.info("*************************************************************");
            logger.info("                    System Information");
            logger.info("  Version : " + getVersion());
            logger.info("  Compile Date : " + CommonUtils.getClassBuildTime());
            logger.info("*************************************************************");

            CeilometerClientPool pool = new CeilometerClientPool();
            loader = ConfigLoader.getInstance();
            if(loader.load(args[0]) != null) {

                for (Device d : loader.getDeviceList()) {
                    pool.add(new CeilometerClient(d, eventLoop));
                }

                start(pool);
                logger.info("*********** [ " + loader.getSiteName() + " ] Site 를 시작합니다. *************");
            }

            logger.info("파일 시스템[" + args[0] + "] 모니터링이 시작되었습니다.");

            WatchService ws = FileSystems.getDefault().newWatchService();
            Paths.get(args[0]).register(ws, StandardWatchEventKinds.ENTRY_MODIFY);

            while(true) {
                WatchKey wk = ws.take();
                for (WatchEvent event : wk.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    Thread.sleep(1000);
                    if(kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        if("config.yml".equals(event.context().toString())) {
                            logger.info("Hotplug Modify Event Detected => " + args[0] + File.separator + event.context());
                            ConfigLoader l = new ConfigLoader();
                            if(l.load(args[0]) != null) {
                                if(loader == null) loader = l;
                                reload(pool, loader, l, RunStatus.STOP);
                                Thread.sleep(2000);
                                reload(pool, l, loader, RunStatus.START);
                                loader  = l; ConfigLoader.setInstance(l);

                            }
                        }
                    }
                    wk.reset();
                }
            }
        } catch(Exception e) {
            logger.error("Aborted.", e);
        }
    }
}
