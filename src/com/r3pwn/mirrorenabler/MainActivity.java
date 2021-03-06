package com.r3pwn.mirrorenabler;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {
    private static final int ENABLED = 1;
    private static final int DISABLED = 0;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Quick, thrown together, don't expect it to look pretty.
        // gms:cast:mirroring_enabled
        // gms:cast:remote_display_enabled
        // com.google.android.gms
        // com.google.android.apps.chromecast.app
        
        final ToggleButton mirror = (ToggleButton)findViewById(R.id.mirrorButton);
        final ToggleButton fix = (ToggleButton)findViewById(R.id.fixtb);
        final ToggleButton qsmirror = (ToggleButton)findViewById(R.id.qsmirrortb);
        
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Our Preferences
        final int mirror_status = preferences.getInt("mirror_status", DISABLED);
        final int fix_status = preferences.getInt("fix_status", DISABLED);
        final int qsmirror_status = preferences.getInt("qsmirror_status", DISABLED);
        final int incompatible_status = preferences.getInt("incompatible_status", DISABLED);
        final int unsupported_shown = preferences.getInt("unsupported_shown", DISABLED);
        
        final File backup = new File("/system/etc/audio_policy.conf.backup");
        final File audiosubmix = new File("/system/lib/hw/audio.r_submix.default.so");

        if (mirror_status == ENABLED) {
            mirror.setChecked(true);
        }

        if(backup.exists()) {
            fix.setChecked(true);
        }

        if (fix_status == ENABLED) {
            fix.setChecked(true);
        }

        if (qsmirror_status == ENABLED) {
            qsmirror.setChecked(true);
        }
        
        final SharedPreferences.Editor prefs_edit = preferences.edit();

        prefs_edit.putInt("incompatible_status", DISABLED);
        prefs_edit.apply();

        if (Build.VERSION.SDK_INT >= 20) {
            prefs_edit.putInt("incompatible_status", ENABLED);
            prefs_edit.apply();
            if (unsupported_shown == DISABLED) {
                final AlertDialog unsupported_version = new AlertDialog.Builder(MainActivity.this).create();
                unsupported_version.setTitle("Unsupported Android Version");
                unsupported_version.setMessage("The version of android you are running is not fully supported by this app. You will need to reboot after a preference change.");
                prefs_edit.putInt("unsupported_shown", ENABLED);
                prefs_edit.putInt("incompatible_status", ENABLED);
                prefs_edit.apply();
                unsupported_version.setButton(AlertDialog.BUTTON_NEUTRAL, "Okay", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog2, int which2) {
                            unsupported_version.dismiss();
                        }
                    });
                unsupported_version.show();
            }
        }

        if (Build.VERSION.SDK_INT <= 18) {
            prefs_edit.putInt("incompatible_status", ENABLED);
            prefs_edit.apply();
            if (unsupported_shown == DISABLED) {
                final AlertDialog unsupported_version = new AlertDialog.Builder(MainActivity.this).create();
                unsupported_version.setTitle("Unsupported Android Version");
                unsupported_version.setMessage("You must be running android KitKat to use this the mirroring feature.");
                prefs_edit.putInt("unsupported_shown", ENABLED);
                prefs_edit.putInt("incompatible_status", ENABLED);
                prefs_edit.apply();
                unsupported_version.setButton(AlertDialog.BUTTON_NEUTRAL, "Okay", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog2, int which2) {
                            unsupported_version.dismiss();
                        }
                    });
                unsupported_version.show();
                mirror.setEnabled(false);
                fix.setEnabled(false);
                qsmirror.setEnabled(false);
            }
        }

        File subin = new File("/system/bin/su");
        File suxbin = new File("/system/xbin/su");
        if(!(subin.exists() || suxbin.exists())) {
            final AlertDialog sualertDialog = new AlertDialog.Builder(MainActivity.this).create();
            sualertDialog.setTitle("You aren't rooted");
            sualertDialog.setMessage("It looks like you aren't rooted. Most features will not work. There is nothing I can do to help you.");

            sualertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Alright, thanks anyways.", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    sualertDialog.dismiss();
                }
            });

            sualertDialog.show();  
            mirror.setEnabled(false);
            fix.setEnabled(false);
            qsmirror.setEnabled(false);
        }
        
        // SQLite3 binary check
        File mysqlfile = new File("/data/data/com.r3pwn.mirrorenabler/lib/libhackyworkaround.so");
        if (!mysqlfile.exists()) {
            File file = new File("/system/bin/sqlite3");
            if (!file.exists()) {
                File file2 = new File("/system/xbin/sqlite3");
                if (!file2.exists()) {
                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                    alertDialog.setTitle("No SQLite3 binary found.");
                    alertDialog.setMessage("It looks like the SQLite3 binary is not installed. Would you like to install it now?");
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Yes, take me there!", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                                Intent dialogIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=ptSoft.util.sqlite3forroot"));
                                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(dialogIntent);
                        }
                    });

                    alertDialog.show();
                    mirror.setEnabled(false);
                    fix.setEnabled(false);
                    qsmirror.setEnabled(false);
                }
            }
        }
        if (!audiosubmix.exists()) {
            final AlertDialog asalertDialog = new AlertDialog.Builder(MainActivity.this).create();
            asalertDialog.setTitle("Your ROM is incompatible");
            asalertDialog.setMessage("It seems that your ROM is lacking the audio_submix file. You must contact your device maintainer and ask them NICELY to implement it. Please refer to the red part of the original post on XDA-Developers.");
            asalertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Okay, will do.",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            asalertDialog.dismiss();
                        }
                    });
            asalertDialog.show();  
            fix.setEnabled(false);
        }
        
        mirror.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mirror.isChecked()) {
                    try {
                        java.lang.Process su = Runtime.getRuntime().exec("su");
                        DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
                        prefs_edit.putInt("mirror_status", ENABLED);
                        prefs_edit.apply();
                        File mysqlfile = new File("/data/data/com.r3pwn.mirrorenabler/lib/libhackyworkaround.so");
                        if(mysqlfile.exists()) {
                            outputStream.writeBytes("/data/data/com.r3pwn.mirrorenabler/lib/libhackyworkaround.so /data/data/com.google.android.gsf/databases/gservices.db \"INSERT INTO overrides (name, value) VALUES ('gms:cast:mirroring_enabled', 'true');\"\n");
                            outputStream.writeBytes("/data/data/com.r3pwn.mirrorenabler/lib/libhackyworkaround.so /data/data/com.google.android.gsf/databases/gservices.db \"UPDATE overrides SET value='true' WHERE name='gms:cast:mirroring_enabled';\"\n");
                        } else if(!mysqlfile.exists()) {
                            outputStream.writeBytes("sqlite3 /data/data/com.google.android.gsf/databases/gservices.db \"INSERT INTO overrides (name, value) VALUES ('gms:cast:mirroring_enabled', 'true');\"\n");
                            outputStream.writeBytes("sqlite3 /data/data/com.google.android.gsf/databases/gservices.db \"UPDATE overrides SET value='true' WHERE name='gms:cast:mirroring_enabled';\"\n");
                        }
                        outputStream.writeBytes("am force-stop com.google.android.gsf\n");
                        outputStream.writeBytes("am force-stop com.google.android.gms\n");
                        outputStream.writeBytes("am force-stop com.google.android.apps.chromecast.app\n");
                        outputStream.writeBytes("exit\n");
                        outputStream.flush();
                        su.waitFor();

                    } catch(IOException e) {
                        // We do this to keep the compiler happy.
                    } catch (InterruptedException e) {
                        // We do this to keep the compiler happy.
                    }

                    if (incompatible_status == ENABLED) {
                        Toast.makeText(getApplicationContext(), "Please reboot for changes to take effect.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(),"Changes applied.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    try {
                        java.lang.Process su = Runtime.getRuntime().exec("su");
                        DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
                        prefs_edit.putInt("mirror_status", DISABLED);
                        prefs_edit.apply();
                        File mysqlfile = new File("/data/data/com.r3pwn.mirrorenabler/lib/libhackyworkaround.so");
                        if(mysqlfile.exists()) {
                            outputStream.writeBytes("/data/data/com.r3pwn.mirrorenabler/lib/libhackyworkaround.so /data/data/com.google.android.gsf/databases/gservices.db \"UPDATE overrides SET value='false' WHERE name='gms:cast:mirroring_enabled';\"\n");
                        } else if(!mysqlfile.exists()) {
                            outputStream.writeBytes("sqlite3 /data/data/com.google.android.gsf/databases/gservices.db \"UPDATE overrides SET value='false' WHERE name='gms:cast:mirroring_enabled';\"\n");
                        }
                        outputStream.writeBytes("am force-stop com.google.android.gsf\n");
                        outputStream.writeBytes("am force-stop com.google.android.gms\n");
                        outputStream.writeBytes("am force-stop com.google.android.apps.chromecast.app\n");
                        outputStream.writeBytes("exit\n");
                        outputStream.flush();
                        su.waitFor();
                    } catch(IOException e) {
                        // We do this to keep the compiler happy.
                    } catch (InterruptedException e) {
                        // We do this to keep the compiler happy.
                    }

                    if (incompatible_status == ENABLED) {
                        Toast.makeText(getApplicationContext(),"Please reboot for changes to take effect.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(),"Changes applied.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
            
        fix.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (fix.isChecked()) {
                    try {
                        java.lang.Process su = Runtime.getRuntime().exec("su");
                        DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
                        prefs_edit.putInt("fix_status", ENABLED);
                        prefs_edit.apply();
                        outputStream.writeBytes("mount -o rw,remount /system\n");
                        outputStream.writeBytes("mv /system/etc/audio_policy.conf /system/etc/audio_policy.conf.backup\n");
                        outputStream.writeBytes("cp /data/data/com.r3pwn.mirrorenabler/lib/libaudiopolicyconf.so /system/etc/audio_policy.conf\n");
                        outputStream.writeBytes("chmod 0644 /system/etc/audio_policy.conf\n");
                        outputStream.writeBytes("mount -o ro,remount /system\n");
                        outputStream.writeBytes("exit\n");
                        outputStream.flush();
                        su.waitFor();

                    } catch(IOException e) {
                        // We do this to keep the compiler happy.
                    } catch (InterruptedException e) {
                        // We do this to keep the compiler happy.
                    }
                    Toast.makeText(getApplicationContext(),"Please reboot for changes to take effect.", Toast.LENGTH_LONG).show();
                } else {
                    try {
                        java.lang.Process su = Runtime.getRuntime().exec("su");
                        DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
                        if (backup.exists()) {
                            prefs_edit.putInt("fix_status", DISABLED);
                            prefs_edit.apply();
                            outputStream.writeBytes("mount -o rw,remount /system\n");
                            outputStream.writeBytes("rm /system/etc/audio_policy.conf\n");
                            outputStream.writeBytes("mv /system/etc/audio_policy.conf.backup /system/etc/audio_policy.conf\n");
                            outputStream.writeBytes("chmod 0644 /system/etc/audio_policy.conf\n");
                            outputStream.writeBytes("mount -o ro,remount /system\n");
                            outputStream.writeBytes("exit\n");
                            outputStream.flush();
                            su.waitFor();
                            Toast.makeText(getApplicationContext(),"Please reboot for changes to take effect.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(),"Backup not found.", Toast.LENGTH_LONG).show();
                            fix.setChecked(true);
                        }
                    } catch(IOException e) {
                        // We do this to keep the compiler happy.
                    } catch (InterruptedException e) {
                        // We do this to keep the compiler happy.
                    }
                }
            }
        });
            
        qsmirror.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (qsmirror.isChecked()) {
                    try {
                        java.lang.Process su = Runtime.getRuntime().exec("su");
                        DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
                        prefs_edit.putInt("qsmirror_status", ENABLED);
                        prefs_edit.apply();
                        File mysqlfile = new File("/data/data/com.r3pwn.mirrorenabler/lib/libhackyworkaround.so");
                        if(mysqlfile.exists())
                        {
                            outputStream.writeBytes("/data/data/com.r3pwn.mirrorenabler/lib/libhackyworkaround.so /data/data/com.google.android.gsf/databases/gservices.db \"INSERT INTO overrides (name, value) VALUES ('gms:cast:remote_display_enabled', 'true');\"\n");
                            outputStream.writeBytes("/data/data/com.r3pwn.mirrorenabler/lib/libhackyworkaround.so /data/data/com.google.android.gsf/databases/gservices.db \"UPDATE overrides SET value='true' WHERE name='gms:cast:remote_display_enabled';\"\n");
                        } else
                        if(!mysqlfile.exists())
                        {
                            outputStream.writeBytes("sqlite3 /data/data/com.google.android.gsf/databases/gservices.db \"INSERT INTO overrides (name, value) VALUES ('gms:cast:remote_display_enabled', 'true');\"\n");
                            outputStream.writeBytes("sqlite3 /data/data/com.google.android.gsf/databases/gservices.db \"UPDATE overrides SET value='true' WHERE name='gms:cast:remote_display_enabled';\"\n");
                        }
                        outputStream.writeBytes("am force-stop com.google.android.gsf\n");
                        outputStream.writeBytes("am force-stop com.google.android.gms\n");
                        outputStream.writeBytes("am force-stop com.google.android.apps.chromecast.app\n");
                        outputStream.writeBytes("exit\n");
                        outputStream.flush();
                        su.waitFor();

                    } catch(IOException e) {
                        // We do this to keep the compiler happy.
                    } catch (InterruptedException e) {
                        // We do this to keep the compiler happy.
                    }
                    if (incompatible_status == ENABLED)
                    {
                        Toast.makeText(getApplicationContext(),"Please reboot for changes to take effect.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(),"Changes applied.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    try {
                        java.lang.Process su = Runtime.getRuntime().exec("su");
                        DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());
                        prefs_edit.putInt("qsmirror_status", DISABLED);
                        prefs_edit.apply();
                        outputStream.writeBytes("/data/data/com.r3pwn.mirrorenabler/lib/libhackyworkaround.so /data/data/com.google.android.gsf/databases/gservices.db \"UPDATE overrides SET value='false' WHERE name='gms:cast:remote_display_enabled';\"\n");
                        outputStream.writeBytes("am force-stop com.google.android.gsf\n");
                        outputStream.writeBytes("am force-stop com.google.android.gms\n");
                        outputStream.writeBytes("am force-stop com.google.android.apps.chromecast.app\n");
                        outputStream.writeBytes("exit\n");
                        outputStream.flush();
                        su.waitFor();
                    } catch(IOException e) {
                        // We do this to keep the compiler happy.
                    } catch (InterruptedException e) {
                        // We do this to keep the compiler happy.
                    }

                    if (incompatible_status == ENABLED) {
                        Toast.makeText(getApplicationContext(), "Please reboot for changes to take effect.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Changes applied.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }
}
