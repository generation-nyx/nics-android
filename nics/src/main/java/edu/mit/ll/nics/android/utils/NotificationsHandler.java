/*
 * Copyright (c) 2008-2021, Massachusetts Institute of Technology (MIT)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.mit.ll.nics.android.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.BigTextStyle;
import androidx.core.app.NotificationCompat.Builder;
import androidx.core.app.NotificationCompat.InboxStyle;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import edu.mit.ll.nics.android.R;
import edu.mit.ll.nics.android.database.entities.Alert;
import edu.mit.ll.nics.android.database.entities.Chat;
import edu.mit.ll.nics.android.database.entities.EODReport;
import edu.mit.ll.nics.android.database.entities.GeneralMessage;
import edu.mit.ll.nics.android.database.entities.Hazard;
import edu.mit.ll.nics.android.repository.ChatRepository;
import edu.mit.ll.nics.android.repository.EODReportRepository;
import edu.mit.ll.nics.android.repository.GeneralMessageRepository;
import edu.mit.ll.nics.android.utils.constants.Intents;

import static edu.mit.ll.nics.android.database.entities.Hazard.getHazardBounds;
import static edu.mit.ll.nics.android.utils.StringUtils.SPACED_DASH;
import static edu.mit.ll.nics.android.utils.constants.Intents.HAZARD_BOUNDS;
import static edu.mit.ll.nics.android.utils.constants.NICS.NICS_MAIN_ACTIVITY_PACKAGE_NAME;
import static edu.mit.ll.nics.android.utils.constants.NICS.NICS_PACKAGE_NAME;
import static edu.mit.ll.nics.android.utils.constants.Notifications.ALERTS_NOTIFICATION_ID;
import static edu.mit.ll.nics.android.utils.constants.Notifications.ALERT_BIG_CONTENT_TITLE;
import static edu.mit.ll.nics.android.utils.constants.Notifications.ALERT_CONTENT_TEXT;
import static edu.mit.ll.nics.android.utils.constants.Notifications.CHATS_GROUP;
import static edu.mit.ll.nics.android.utils.constants.Notifications.CHAT_BIG_CONTENT_TITLE;
import static edu.mit.ll.nics.android.utils.constants.Notifications.CHAT_CONTENT_TEXT;
import static edu.mit.ll.nics.android.utils.constants.Notifications.CHAT_NOTIFICATION_ID;
import static edu.mit.ll.nics.android.utils.constants.Notifications.EOD_REPORTS_GROUP;
import static edu.mit.ll.nics.android.utils.constants.Notifications.EOD_REPORT_CONTENT_TEXT;
import static edu.mit.ll.nics.android.utils.constants.Notifications.EOD_REPORT_NOTIFICATION_ID;
import static edu.mit.ll.nics.android.utils.constants.Notifications.GENERAL_MESSAGES_GROUP;
import static edu.mit.ll.nics.android.utils.constants.Notifications.GENERAL_MESSAGE_CONTENT_TEXT;
import static edu.mit.ll.nics.android.utils.constants.Notifications.GENERAL_MESSAGE_NOTIFICATION_ID;
import static edu.mit.ll.nics.android.utils.constants.Notifications.GEOFENCE_SERVICE_NOTIFICATION_CHANNEL_ID_SERVICE;
import static edu.mit.ll.nics.android.utils.constants.Notifications.GEOFENCE_SERVICE_NOTIFICATION_CHANNEL_NAME;
import static edu.mit.ll.nics.android.utils.constants.Notifications.HAZARD_BIG_CONTENT_TITLE;
import static edu.mit.ll.nics.android.utils.constants.Notifications.HAZARD_NOTIFICATION_CHANNEL_ID_SERVICE;
import static edu.mit.ll.nics.android.utils.constants.Notifications.HAZARD_NOTIFICATION_CHANNEL_NAME;
import static edu.mit.ll.nics.android.utils.constants.Notifications.HAZARD_NOTIFICATION_ID;
import static edu.mit.ll.nics.android.utils.constants.Notifications.LOCATION_NOTIFICATION_CHANNEL_ID_SERVICE;
import static edu.mit.ll.nics.android.utils.constants.Notifications.LOCATION_NOTIFICATION_CHANNEL_NAME;
import static edu.mit.ll.nics.android.utils.constants.Notifications.NICS_NOTIFICATION_CHANNEL_ID_SERVICE;
import static edu.mit.ll.nics.android.utils.constants.Notifications.NICS_NOTIFICATION_CHANNEL_NAME;
import static edu.mit.ll.nics.android.utils.constants.Notifications.NICS_TITLE;
import static edu.mit.ll.nics.android.utils.constants.Notifications.REPORT_BIG_CONTENT_TITLE;

public class NotificationsHandler {

    private static NotificationsHandler sInstance;

    private final Builder mBuilder;
    private final Builder mHazardsBuilder;
    private final NotificationManagerCompat mNotificationManager;

    private int numberOfNewGeneralMessages = 0;
    private int numberOfNewEODReports = 0;
    private int numberOfNewAlerts = 0;
    private int numberOfNewChatMessages = 0;

    private InboxStyle mGeneralMessageStyle;
    private InboxStyle mEODReportStyle;
    private InboxStyle mAlertsStyle;
    private InboxStyle mChatStyle;
    private BigTextStyle mHazardStyle;

    private static final long MIN_QUIET_PERIOD_MILLIS = TimeUnit.SECONDS.toMillis(2);
    private long lastChime = 0;

    public static NotificationsHandler getInstance(Context context) {
        if (sInstance == null) {
            synchronized (NotificationsHandler.class) {
                if (sInstance == null) {
                    sInstance = new NotificationsHandler(context);
                }
            }
        }
        return sInstance;
    }

    private NotificationsHandler(Context context) {
        mNotificationManager = NotificationManagerCompat.from(context);

        initStyles();
        createNotificationChannels();

        mBuilder = new NotificationCompat.Builder(context, NICS_NOTIFICATION_CHANNEL_ID_SERVICE);
        initBuilder(context);

        mHazardsBuilder = new NotificationCompat.Builder(context, HAZARD_NOTIFICATION_CHANNEL_ID_SERVICE);
        initHazardBuilder(context);
    }

    /**
     * Initialize the {@link Builder} for the normal notification channel.
     */
    private void initBuilder(Context context) {
        mBuilder.setColor(ContextCompat.getColor(context, R.color.notification))
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setContentTitle(NICS_TITLE)
                .setAutoCancel(true);

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mBuilder.setChannelId(NICS_NOTIFICATION_CHANNEL_ID_SERVICE); // Channel ID
        }
    }

    /**
     * Initialize the {@link Builder} for the hazard notification channel.
     */
    private void initHazardBuilder(Context context) {
        // Define the notification settings.
        mHazardsBuilder.setSmallIcon(R.drawable.alert)
                .setColor(Color.RED)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.alert_yellow))
                .setPriority(Notification.PRIORITY_HIGH)
                .setLights(Color.RED, 1000, 1000)
                .setAutoCancel(true)
                .setColorized(true);

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mHazardsBuilder.setChannelId(HAZARD_NOTIFICATION_CHANNEL_ID_SERVICE); // Channel ID
        }
    }

    /**
     * Initialize the {@link InboxStyle} inbox styles for each type of notification.
     */
    private void initStyles() {
        mGeneralMessageStyle = new InboxStyle();
        mGeneralMessageStyle.setBigContentTitle(REPORT_BIG_CONTENT_TITLE);

        mEODReportStyle = new InboxStyle();
        mEODReportStyle.setBigContentTitle(REPORT_BIG_CONTENT_TITLE);

        mAlertsStyle = new InboxStyle();
        mAlertsStyle.setBigContentTitle(ALERT_BIG_CONTENT_TITLE);

        mChatStyle = new InboxStyle();
        mChatStyle.setBigContentTitle(CHAT_BIG_CONTENT_TITLE);

        mHazardStyle = new NotificationCompat.BigTextStyle();
        mHazardStyle.setBigContentTitle(HAZARD_BIG_CONTENT_TITLE);
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mNotificationManager != null) {
            mNotificationManager.createNotificationChannel(new NotificationChannel(NICS_NOTIFICATION_CHANNEL_ID_SERVICE, NICS_NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH));
            mNotificationManager.createNotificationChannel(new NotificationChannel(HAZARD_NOTIFICATION_CHANNEL_ID_SERVICE, HAZARD_NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH));
            mNotificationManager.createNotificationChannel(new NotificationChannel(GEOFENCE_SERVICE_NOTIFICATION_CHANNEL_ID_SERVICE, GEOFENCE_SERVICE_NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW));
            mNotificationManager.createNotificationChannel(new NotificationChannel(LOCATION_NOTIFICATION_CHANNEL_ID_SERVICE, LOCATION_NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW));
        }
    }

    private void rateLimitChime(Builder mBuilder) {
        boolean silent = (System.currentTimeMillis() - lastChime) < MIN_QUIET_PERIOD_MILLIS;
        if (!silent) {
            lastChime = System.currentTimeMillis();
        }
        mBuilder.setSilent(silent);
    }

    public void notification(int id, Notification notification) {
        mNotificationManager.notify(id, notification);
    }

    public void cancelNotification(int id) {
        mNotificationManager.cancel(id);
    }

    public void createGeneralMessagesNotification(List<GeneralMessage> reports,
                                                  Context context,
                                                  GeneralMessageRepository generalMessageRepository) {
        // Add all new general message reports to the notification group.
        for (GeneralMessage report : reports) {
            // Update the general message report in the database so that it doesn't get added again.
            report.setNew(false);
            generalMessageRepository.addGeneralMessageToDatabase(report);

            // Telling the app to open the MainActivity with the provided Intent when the notification is clicked.
            // This will call onNewIntent, which is where we can tell the app to navigate.
            Intent intent = new Intent(Intents.NICS_VIEW_GENERAL_MESSAGES_LIST);
            intent.setClassName(NICS_PACKAGE_NAME, NICS_MAIN_ACTIVITY_PACKAGE_NAME);

            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            // Customizing the notification's content.
            mBuilder.setContentText(context.getString(R.string.description_colon).concat(report.getDescription()));
            mBuilder.setSubText(GENERAL_MESSAGE_CONTENT_TEXT);
            mBuilder.setContentTitle(context.getString(R.string.from).concat(report.getUser()));
            mBuilder.setGroup(GENERAL_MESSAGES_GROUP);
            mBuilder.setSmallIcon(R.drawable.report);
            mBuilder.setDefaults(NotificationCompat.DEFAULT_ALL);
            mBuilder.setColor(ContextCompat.getColor(context, R.color.holo_blue));
            mBuilder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.nics_logo));
            mBuilder.setContentIntent(contentIntent);
            rateLimitChime(mBuilder);

            // Broadcast the notification.
            mNotificationManager.notify(GENERAL_MESSAGE_NOTIFICATION_ID, mBuilder.build());
        }
    }

    public void createEODReportNotification(List<EODReport> reports,
                                            Context context,
                                            EODReportRepository repository) {
        // Add all new eod reports to the notification group.
        for (EODReport report : reports) {
            // Update the eod report in the database so that it doesn't get added again.
            report.setNew(false);
            repository.addEODReportToDatabase(report);

            // Telling the app to open the MainActivity with the provided Intent when the notification is clicked.
            // This will call onNewIntent, which is where we can tell the app to navigate.
            Intent intent = new Intent(Intents.NICS_VIEW_EOD_REPORTS_LIST);
            intent.setClassName(NICS_PACKAGE_NAME, NICS_MAIN_ACTIVITY_PACKAGE_NAME);

            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            // Customizing the notification's content.
            mBuilder.setContentText(report.getUser()
                    .concat(SPACED_DASH)
                    .concat(context.getString(R.string.team_colon))
                    .concat(report.getTeam() != null ? report.getTeam() : "")
                    .concat(context.getString(R.string.with_task_type))
                    .concat(context.getString(R.string.with_task_colon))
                    .concat(report.getTaskType() != null ? report.getTaskType() : ""));
            mBuilder.setSubText(EOD_REPORT_CONTENT_TEXT);
            mBuilder.setContentTitle(context.getString(R.string.from).concat(report.getUser()));
            mBuilder.setGroup(EOD_REPORTS_GROUP);
            mBuilder.setSmallIcon(R.drawable.report);
            mBuilder.setDefaults(NotificationCompat.DEFAULT_ALL);
            mBuilder.setColor(ContextCompat.getColor(context, R.color.holo_blue));
            mBuilder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.nics_logo));
            mBuilder.setContentIntent(contentIntent);
            rateLimitChime(mBuilder);

            // Broadcast the notification.
            mNotificationManager.notify(EOD_REPORT_NOTIFICATION_ID, mBuilder.build());
        }
    }

    public void createAlertsNotification(ArrayList<Alert> alerts) {
        mBuilder.setContentText(ALERT_CONTENT_TEXT);
        mBuilder.setSmallIcon(R.drawable.alert);

        for (Alert alert : alerts) {
            mAlertsStyle.addLine(alert.getUserName().concat(SPACED_DASH).concat(alert.getMessage()));
            numberOfNewAlerts++;
        }

        mBuilder.setNumber(numberOfNewAlerts);
        mBuilder.setStyle(mAlertsStyle);
        mBuilder.setDefaults(Notification.DEFAULT_ALL);
        rateLimitChime(mBuilder);

        mNotificationManager.notify(ALERTS_NOTIFICATION_ID, mBuilder.build());
    }

    public void createNewChatNotification(List<Chat> chats,
                                          Context context,
                                          ChatRepository repository) {
        // Add all new chat reports to the notification group.
        for (Chat chat : chats) {
            // Update the chat in the database so that it doesn't get added again.
            chat.setNew(false);
            repository.addChatToDatabase(chat);

            // Telling the app to open the MainActivity with the provided Intent when the notification is clicked.
            // This will call onNewIntent, which is where we can tell the app to navigate.
            Intent intent = new Intent(Intents.NICS_VIEW_CHAT_LIST);
            intent.setClassName(NICS_PACKAGE_NAME, NICS_MAIN_ACTIVITY_PACKAGE_NAME);

            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            // Customizing the notification's content.
            mBuilder.setContentText(chat.getNickName().concat(SPACED_DASH).concat(chat.getMessage()));
            mBuilder.setSubText(CHAT_CONTENT_TEXT);
            mBuilder.setContentTitle(context.getString(R.string.from).concat(chat.getNickName()));
            mBuilder.setGroup(CHATS_GROUP);
            mBuilder.setSmallIcon(R.drawable.baseline_message_white);
            mBuilder.setDefaults(NotificationCompat.DEFAULT_ALL);
            mBuilder.setColor(ContextCompat.getColor(context, R.color.holo_blue));
            mBuilder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.nics_logo));
            mBuilder.setContentIntent(contentIntent);
            rateLimitChime(mBuilder);

            // Broadcast the notification.
            mNotificationManager.notify(CHAT_NOTIFICATION_ID, mBuilder.build());
        }
    }

    public void createHazardsNotification(ArrayList<String> details,
                                          ArrayList<Hazard> hazards,
                                          Context context) {
        if (hazards.size() > 0) {
            Intent intent = new Intent(Intents.NICS_VIEW_HAZARDS);
            intent.setClassName(NICS_PACKAGE_NAME, NICS_MAIN_ACTIVITY_PACKAGE_NAME);
            intent.putExtra(HAZARD_BOUNDS, getHazardBounds(hazards));

            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            mHazardsBuilder.setSubText(hazards.size() + " hazard zone(s)");
            mHazardsBuilder.setContentIntent(contentIntent);

            StringBuilder sb = new StringBuilder();
            int count = 1;
            for (String detail : details) {
                sb.append(count);
                sb.append(": ");
                sb.append(detail);
                sb.append("\n");
                count++;
            }
            mHazardStyle.bigText(sb.toString());
            mHazardsBuilder.setStyle(mHazardStyle);
            mNotificationManager.notify(HAZARD_NOTIFICATION_ID, mHazardsBuilder.build());
        }
    }

    /**
     * Cancel all alert notifications.
     */
    public void cancelAlertNotifications() {
        numberOfNewAlerts = 0;
        mNotificationManager.cancel(ALERTS_NOTIFICATION_ID);
    }

    /**
     * Cancel all general message notifications.
     */
    public void cancelGeneralMessageNotifications() {
        numberOfNewGeneralMessages = 0;
        mNotificationManager.cancel(GENERAL_MESSAGE_NOTIFICATION_ID);
    }

    /**
     * Cancel all EOD report notifications.
     */
    public void cancelEODReportNotifications() {
        numberOfNewEODReports = 0;
        mNotificationManager.cancel(EOD_REPORT_NOTIFICATION_ID);
    }

    /**
     * Cancel all chat notifications.
     */
    public void cancelChatNotifications() {
        numberOfNewChatMessages = 0;
        mNotificationManager.cancel(CHAT_NOTIFICATION_ID);
    }

    /**
     * Cancel all hazard notifications.
     */
    public void cancelHazardNotifications() {
        mNotificationManager.cancel(HAZARD_NOTIFICATION_ID);
    }

    /**
     * Cancel all notifications.
     */
    public void cancelAllNotifications() {
        cancelAlertNotifications();
        cancelGeneralMessageNotifications();
        cancelEODReportNotifications();
        cancelChatNotifications();
        cancelHazardNotifications();
    }
}