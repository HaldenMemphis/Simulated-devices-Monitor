package com.msc.devices;

import com.msc.devices.utils.Mac;
import org.kaaproject.kaa.client.configuration.base.ConfigurationListener;
import org.kaaproject.kaa.client.configuration.base.SimpleConfigurationStorage;
import org.kaaproject.kaa.client.DesktopKaaPlatformContext;
import org.kaaproject.kaa.client.Kaa;
import org.kaaproject.kaa.client.KaaClient;
import org.kaaproject.kaa.client.SimpleKaaClientStateListener;
import org.kaaproject.kaa.client.logging.strategies.RecordCountLogUploadStrategy;
import org.kaaproject.kaa.schema.Monitor.Configuration;
import org.kaaproject.kaa.scheme.Monitor.BloodSugarMonitorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @program: Simulated-devices-Monitor
 * @description:
 * @author: yfliu
 * @create: 2023-07-04 19:58
 **/
public class BloodSugarMonitor {

    private static final long START_DELAY = 1000L;

    private static final Logger LOG = LoggerFactory.getLogger(BloodSugarMonitor.class);
    private static KaaClient kaaClient;

    private static ScheduledFuture<?> scheduledFuture;
    private static ScheduledExecutorService scheduledExecutorService;


    public static void main(String[] args) {
        LOG.info(BloodSugarMonitor.class.getSimpleName() + " app starting!");

        scheduledExecutorService = Executors.newScheduledThreadPool(1);

        //Create the Kaa desktop context for the application.
        DesktopKaaPlatformContext desktopKaaPlatformContext = new DesktopKaaPlatformContext();

        /*
         * Create a Kaa client and add a listener which displays the Kaa client
         * configuration as soon as the Kaa client is started.
         */
        kaaClient = Kaa.newClient(desktopKaaPlatformContext, new KaaClientStateListener(), true);

        /*
         *  Used by log collector on each adding of the new log record in order to check whether to send logs to server.
         *  Start log upload when there is at least one record in storage.
         */
        RecordCountLogUploadStrategy strategy = new RecordCountLogUploadStrategy(1);
        strategy.setMaxParallelUploads(1);
        kaaClient.setLogUploadStrategy(strategy);

        /*
         * Persist configuration in a local storage to avoid downloading it each
         * time the Kaa client is started.
         */
        kaaClient.setConfigurationStorage(new SimpleConfigurationStorage(desktopKaaPlatformContext, "saved_config.cfg"));

        kaaClient.addConfigurationListener(new ConfigurationListener() {
            @Override
            public void onConfigurationUpdate(Configuration configuration) {
                LOG.info("Received configuration data. New sample period: {}", configuration.getMessageSendTimeMins());
                onChangedConfiguration(TimeUnit.MINUTES.toMillis(configuration.getMessageSendTimeMins()));
            }
        });

        //Start the Kaa client and connect it to the Kaa server.
        kaaClient.start();

        LOG.info("--= Press any key to exit =--");
        try {
            System.in.read();
        } catch (IOException e) {
            LOG.error("IOException has occurred: {}", e.getMessage());
        }
        LOG.info("Stopping...");
        scheduledExecutorService.shutdown();
        kaaClient.stop();
        //TODO
    }

    private static class KaaClientStateListener extends SimpleKaaClientStateListener {

        @Override
        public void onStarted() {
            super.onStarted();
            LOG.info("Kaa client started");
            Configuration configuration = kaaClient.getConfiguration();
            LOG.info("Default sample period: {}", configuration.getMessageSendTimeMins());
            onKaaStarted(TimeUnit.MINUTES.toMillis(configuration.getMessageSendTimeMins()));
        }

        private static void onKaaStarted(long time) {
            if (time <= 0) {
                LOG.error("Wrong time is used. Please, check your configuration!");
                kaaClient.stop();
                System.exit(0);
            }

            scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            BloodSugarMonitorMessage bloodSugarMonitorMessageBody = getBloodSugarMonitorBody();
                            kaaClient.addLogRecord(bloodSugarMonitorMessageBody);
                            LOG.info("Sampled Blood Sugar Data: {}", bloodSugarMonitorMessageBody.getBloodSugar());
                        }
                    }, 0, time, TimeUnit.MILLISECONDS);
        }


        @Override
        public void onStopped() {
            super.onStopped();
            LOG.info("Kaa client stopped");
        }
    }

    private static void onChangedConfiguration(long time) {
        if (time == 0) {
            time = START_DELAY;
        }
        scheduledFuture.cancel(false);

        scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(
                new Runnable() {
                    @Override
                    public void run() {
                        BloodSugarMonitorMessage bloodSugarMonitorBody = getBloodSugarMonitorBody();
                        kaaClient.addLogRecord(bloodSugarMonitorBody);
                        LOG.info("Sampled Blood Sugar Data: {}", bloodSugarMonitorBody.getBloodSugar());
                    }
                }, 0, time, TimeUnit.MILLISECONDS);
    }


    private static BloodSugarMonitorMessage getBloodSugarMonitorBody() {
        BloodSugarMonitorMessage bloodSugarMonitorMessageBody = new BloodSugarMonitorMessage(Mac.generateRandomMacAddress(), getRandomBloodSugarData(), System.currentTimeMillis());
        return bloodSugarMonitorMessageBody;
    }

    private static float getRandomBloodSugarData() {
        Random random = new Random();
        double min = 3.9;
        double max = 11.0;
        double randomValue = min + (max - min) * random.nextDouble();
        return (float) randomValue;
    }

    private float getHighBloodSugarData() {
        Random random = new Random();
        double min = 11.1;
        double max = 33.3;
        double randomValue = min + (max - min) * random.nextDouble();
        return (float) randomValue;
    }

    private float getLowBloodSugarData() {
        Random random = new Random();
        double min = 0.0;
        double max = 3.9;
        double randomValue = min + (max - min) * random.nextDouble();
        return (float) randomValue;
    }


}
