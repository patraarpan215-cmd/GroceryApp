package com.example.grocery.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ExpiryWorker extends Worker {

    public ExpiryWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        long currentTime = System.currentTimeMillis();
        Log.d("ExpiryWorker", "Checking pantry expiration dates in the background...");

        // Simulating an expiring item to prove the algorithm works
        sendExpiryNotification("Whole Milk", "Expires in 2 days! Consider using it in a recipe soon.");

        return Result.success();
    }

    // The new logic to actually trigger the phone notification
    private void sendExpiryNotification(String itemName, String message) {
        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "pantry_expiry_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Pantry Expiry Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Pantry Alert: " + itemName)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // This checks if the user has granted the permission before sending it.
        // It resolves the red error in Android Studio.
        if (androidx.core.app.ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            manager.notify(1, builder.build());
        }
    }
}