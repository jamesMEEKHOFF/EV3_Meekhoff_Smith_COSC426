

package com.example.a000_bt;

//import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import java.io.InputStream;
import java.io.OutputStream;

// ver01 -- sending Direct Command

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {
    // BT Variables
    private final String CV_ROBOTNAME = "SHOOSH_MON86";
    private BluetoothAdapter cv_btInterface = null;
    private Set<BluetoothDevice> cv_pairedDevices = null;
    private BluetoothDevice cv_btDevice = null;
    private BluetoothSocket cv_btSocket = null;

    // Data stream to/from NXT bluetooth
    private InputStream cv_is = null;
    private OutputStream cv_os = null;

    TextView cv_label01;
    TextView cv_label02;


    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch(motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // PRESSED
                cv_label02.setText("Button PRESSED");
                return true; // if you want to handle the touch event
            case MotionEvent.ACTION_UP:
                // RELEASED
                cv_label02.setText("Button Released");
                return true; // if you want to handle the touch event
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button cv_button = (Button) findViewById(R.id.vv_button);
        cv_label01 = (TextView) findViewById(R.id.vv_tvOut1);
        cv_label02 = (TextView) findViewById(R.id.vv_tvOut2);
        cv_button.setOnTouchListener(this);

        // Need grant permission once per install
        cpf_checkBTPermissions();
        cv_button.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Context ctx =  MainActivity.this;
                        cpf_EV3Test();
                        cpf_moveForward();
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
    /*public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == R.id.menu_first) {
            cpf_requestBTPermissions();
            return true;
        } else if (id == R.id.menu_second) {
            cv_btDevice = cpf_locateInPairedBTList(CV_ROBOTNAME);
            return true;
        } else if (id == R.id.menu_third) {
            cpf_connectToEV3(cv_btDevice);
            return true;
        } else if (id == R.id.menu_fourth) {
            cpf_EV3MoveMotor();
            return true;
        } else if (id == R.id.menu_fifth) {
            cpf_EV3PlayTone();
            return true;
        } else if (id == R.id.menu_sixth) {
            cpf_disconnFromEV3(cv_btDevice);
            return true;
        } else {
            return super.onOptionsItemSelected(menuItem);
        }*/
        switch (menuItem.getItemId()) {
            case R.id.menu_first: cpf_requestBTPermissions();
                return true;
            case R.id.menu_second: cv_btDevice = cpf_locateInPairedBTList(CV_ROBOTNAME);
                return true;
            case R.id.menu_third: cpf_connectToEV3(cv_btDevice);
                return true;
            case R.id.menu_fourth: cpf_EV3MoveMotor();
                return true;
            case R.id.menu_fifth: cpf_EV3PlayTone();
                return true;
            case R.id.menu_sixth: cpf_disconnFromEV3(cv_btDevice);
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    private void cpf_checkBTPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            cv_label01.setText("BLUETOOTH_SCAN already granted.\n");
        } else {
            cv_label01.setText("BLUETOOTH_SCAN NOT granted.\n");
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            cv_label02.setText("BLUETOOTH_CONNECT NOT granted.\n");
        } else {
            cv_label02.setText("BLUETOOTH_CONNECT already granted.\n");
        }
    }

    // https://www.geeksforgeeks.org/android-how-to-request-permissions-in-android-application/
    // https://stackoverflow.com/questions/67722950/android-12-new-bluetooth-permissions
    private void cpf_requestBTPermissions() {
        // We can give any value but unique for each permission.
        final int BLUETOOTH_SCAN_CODE = 100;
        final int BLUETOOTH_CONNECT_CODE = 101;

        // Android version < 12, "android.permission.BLUETOOTH" just fine
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Toast.makeText(MainActivity.this,
                    "BLUETOOTH granted for earlier Android", Toast.LENGTH_SHORT).show();
            return;
        }

        // Android 12+ has to go through the process
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.BLUETOOTH_SCAN},
                    BLUETOOTH_SCAN_CODE);
        } else {
            Toast.makeText(MainActivity.this,
                    "BLUETOOTH_SCAN already granted", Toast.LENGTH_SHORT).show();
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    BLUETOOTH_CONNECT_CODE);
        } else {
            Toast.makeText(MainActivity.this,
                    "BLUETOOTH_CONNECT already granted", Toast.LENGTH_SHORT).show();
        }
    }

    // Modify from chap14, pp390 findRobot()
    private BluetoothDevice cpf_locateInPairedBTList(String name) {
        BluetoothDevice lv_bd = null;
        try {
            cv_btInterface = BluetoothAdapter.getDefaultAdapter();
            /*if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return null;
            }*/
            cv_pairedDevices = cv_btInterface.getBondedDevices();
            Iterator<BluetoothDevice> lv_it = cv_pairedDevices.iterator();
            while (lv_it.hasNext()) {
                lv_bd = lv_it.next();
                if (lv_bd.getName().equalsIgnoreCase(name)) {
                    cv_label01.setText(name + " is in paired list");
                    return lv_bd;
                }
            }
            cv_label01.setText(name + " is NOT in paired list");
        } catch (Exception e) {
            cv_label01.setText("Failed in findRobot() " + e.getMessage());
        }
        return null;
    }

    // Modify frmo chap14, pp391 connectToRobot()
    private void cpf_connectToEV3(BluetoothDevice bd) {
        try {
            /*if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }*/
            cv_btSocket = bd.createRfcommSocketToServiceRecord
                    (UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            cv_btSocket.connect();

            //// HERE
            cv_is = cv_btSocket.getInputStream();
            cv_os = cv_btSocket.getOutputStream();
            cv_label02.setText("Connect to " + bd.getName() + " at " + bd.getAddress());
        } catch (Exception e) {
            cv_label02.setText("Error interacting with remote device [" +
                    e.getMessage() + "]");
        }
    }


    private void cpf_disconnFromEV3(BluetoothDevice bd) {
        try {
            cv_btSocket.close();
            cv_is.close();
            cv_os.close();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            cv_label02.setText(bd.getName() + " is disconnect ");
        } catch (Exception e) {
            cv_label02.setText("Error in disconnect -> " + e.getMessage());
        }
    }


    private void cpf_moveForward() {
        try {
            byte[] buffer = new byte[8];	// message length + 2
            buffer[0] = (byte) (8-2);	// message length
            buffer[1] = 0;

            buffer[2] = 34;			// id = 12 34
            buffer[3] = 12;

            buffer[4] = (byte) 0x00;	// reply

            buffer[5] = 0;
            buffer[6] = 0;

            buffer[7] = (byte) 0x01;
        }
        catch (Exception e) {
            cv_label01.setText("Error in MoveForward(" + e.getMessage() + ")");
        }
    }

    // Communication Developer Kit Page 27
    // 4.2.2 Start motor B & C forward at power 50 for 3 rotation and braking at destination
    private void cpf_EV3MoveMotor() {
        try {
            byte[] buffer = new byte[20];       // 0x12 command length

            buffer[0] = (byte) (20-2);
            buffer[1] = 0;

            buffer[2] = 34;
            buffer[3] = 12;

            buffer[4] = (byte) 0x80;

            buffer[5] = 0;
            buffer[6] = 0;

            buffer[7] = (byte) 0xae;
            buffer[8] = 0;

            buffer[9] = (byte) 0x06;

            buffer[10] = (byte) 0x81;
            buffer[11] = (byte) 0x32;

            buffer[12] = 0;

            buffer[13] = (byte) 0x82;
            buffer[14] = (byte) 0x84;
            buffer[15] = (byte) 0x03;

            buffer[16] = (byte) 0x82;
            buffer[17] = (byte) 0xB4;
            buffer[18] = (byte) 0x00;

            buffer[19] = 1;

            cv_os.write(buffer);
            cv_os.flush();
        }
        catch (Exception e) {
            cv_label01.setText("Error in MoveForward(" + e.getMessage() + ")");
        }
    }

    // 4.2.5 Play a 1Kz tone at level 2 for 1 sec.
    private void cpf_EV3PlayTone() {
        try {
            byte[] buffer = new byte[17];       // 0x0f command length

            buffer[0] = (byte) (17-2);
            buffer[1] = 0;

            buffer[2] = 34;
            buffer[3] = 12;

            buffer[4] = (byte) 0x80;

            buffer[5] = 0;
            buffer[6] = 0;

            buffer[7] = (byte) 0x94;
            buffer[8] = 1;

            buffer[9] = (byte) 0x81;
            buffer[10] = (byte) 0x02;

            buffer[11] = (byte) 0x82;
            buffer[12] = (byte) 0xd0;
            buffer[13] = (byte) 0x07;

            buffer[14] = (byte) 0x82;
            buffer[15] = (byte) 0xe8;
            buffer[16] = (byte) 0x03;

            cv_os.write(buffer);
            cv_os.flush();
        }
        catch (Exception e) {
            cv_label02.setText("Error in MoveForward(" + e.getMessage() + ")");
        }
    }
    // http://ev3directcommands.blogspot.com/2016/01/ev3-direct-commands-lesson-02-pre.html
    // Playing Sound Files
    // Sent 0x|1E:00|2A:00|80|00:00|94:02:81:64:84:2E:2F:75:69:2F:44:6F:77:6E:6C:6F:61:64:53:75:63:63:65:73:00|
    private void cpf_EV3Test() {
        try {
            byte[] buffer = new byte[32];

            buffer[0] = (byte) (32-2);
            buffer[1] = 0;

            buffer[2] = 34;
            buffer[3] = 12;

            buffer[4] = (byte) 0x80;

            buffer[5] = 0;
            buffer[6] = 0;

            buffer[7] = (byte) 0x94;
            buffer[8] = 2;

            buffer[9] = (byte) 0x81;
            buffer[10] = (byte) 0x64;

            buffer[11] = (byte) 0x84;
            buffer[12] = (byte) 0x2E;
            buffer[13] = (byte) 0x2F;
            buffer[14] = (byte) 0x75;
            buffer[15] = (byte) 0x69;
            buffer[16] = (byte) 0x2F;
            buffer[17] = (byte) 0x44;
            buffer[18] = (byte) 0x6F;
            buffer[19] = (byte) 0x77;
            buffer[20] = (byte) 0x6E;
            buffer[21] = (byte) 0x6C;
            buffer[22] = (byte) 0x6F;
            buffer[23] = (byte) 0x61;
            buffer[24] = (byte) 0x64;
            buffer[25] = (byte) 0x53;
            buffer[26] = (byte) 0x75;
            buffer[27] = (byte) 0x63;
            buffer[28] = (byte) 0x63;
            buffer[29] = (byte) 0x65;
            buffer[30] = (byte) 0x73;
            buffer[31] = (byte) 0x00;

            cv_os.write(buffer);
            cv_os.flush();
        }
        catch (Exception e) {
            cv_label02.setText("Error in MoveForward(" + e.getMessage() + ")");
        }
    }
}





















/*

package com.example.a000_bt;


//import androidx.annotation.NonNull;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import java.io.InputStream;
import java.io.OutputStream;

// ver01 -- sending Direct Command

public class MainActivity extends AppCompatActivity {
    // BT Variables
    private final String CV_ROBOTNAME = "SHOOSH_MON86";
    private BluetoothAdapter cv_btInterface = null;
    private Set<BluetoothDevice> cv_pairedDevices = null;
    private BluetoothDevice cv_btDevice = null;
    private BluetoothSocket cv_btSocket = null;

    // Data stream to/from NXT bluetooth
    private InputStream cv_is = null;
    private OutputStream cv_os = null;

    TextView cv_label01;
    TextView cv_label02;

    int speed;

    private boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button backwardButton = findViewById(R.id.backwardButton);
        Button forwardButton = findViewById(R.id.forwardButton);

        backwardButton.setOnTouchListener(buttonListener(true));
        forwardButton.setOnTouchListener(buttonListener(false));

        TextView speedLabel = findViewById(R.id.speedLabel);
        SeekBar speedSlider = findViewById(R.id.speedSlider);
        speedSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                speed = progress;
                speedLabel.setText(Integer.toString(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        speedLabel.setText(Integer.toString(speedSlider.getProgress()));

        cv_label01 = (TextView) findViewById(R.id.vv_tvOut1);
        cv_label02 = (TextView) findViewById(R.id.vv_tvOut2);

        // Need grant permission once per install
        cpf_checkBTPermissions();
    }

    private View.OnTouchListener buttonListener(boolean backwards) {
        return (view, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    stopMoving();
                    break;
                case MotionEvent.ACTION_DOWN:
                    startMoving(backwards);
                    break;
            }
            return false;
        };
    }

    private void startMoving(boolean backwards) {
        if (!isRunning) {
            Thread t = new Thread(() -> {
                try {
                    while (isRunning) {
                        cpf_EV3MoveMotor(backwards ? (int) speed * -1 : (int) speed);
                        Thread.sleep(10);
                    }

                    cpf_EV3MoveMotor(0);

                } catch (InterruptedException ex) {
                }
            });

            isRunning = true;
            t.setDaemon(true);
            t.start();
        }
    }

    private void stopMoving() {
        isRunning = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == R.id.menu_first) {
            cpf_requestBTPermissions();
            return true;
        } else if (id == R.id.menu_second) {
            cv_btDevice = cpf_locateInPairedBTList(CV_ROBOTNAME);
            return true;
        } else if (id == R.id.menu_third) {
            cpf_connectToEV3(cv_btDevice);
            return true;
        } else if (id == R.id.menu_fourth) {
            cpf_EV3MoveMotor(50);
            return true;
        } else if (id == R.id.menu_fifth) {
            cpf_EV3PlayTone();
            return true;
        } else if (id == R.id.menu_sixth) {
            cpf_disconnFromEV3(cv_btDevice);
            return true;
        } else {
            return super.onOptionsItemSelected(menuItem);
        }
        */
/*switch (menuItem.getItemId()) {
            case R.id.menu_first: cpf_requestBTPermissions();
                return true;
            case R.id.menu_second: cv_btDevice = cpf_locateInPairedBTList(CV_ROBOTNAME);
                return true;
            case R.id.menu_third: cpf_connectToEV3(cv_btDevice);
                return true;
            case R.id.menu_fourth: cpf_EV3MoveMotor();
                return true;
            case R.id.menu_fifth: cpf_EV3PlayTone();
                return true;
            case R.id.menu_sixth: cpf_disconnFromEV3(cv_btDevice);
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }*//*


    }

    private void cpf_checkBTPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            cv_label01.setText("BLUETOOTH_SCAN already granted.\n");
        } else {
            cv_label01.setText("BLUETOOTH_SCAN NOT granted.\n");
        }
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            cv_label02.setText("BLUETOOTH_CONNECT NOT granted.\n");
        } else {
            cv_label02.setText("BLUETOOTH_CONNECT already granted.\n");
        }
    }

    // https://www.geeksforgeeks.org/android-how-to-request-permissions-in-android-application/
    // https://stackoverflow.com/questions/67722950/android-12-new-bluetooth-permissions
    private void cpf_requestBTPermissions() {
        // We can give any value but unique for each permission.
        final int BLUETOOTH_SCAN_CODE = 100;
        final int BLUETOOTH_CONNECT_CODE = 101;

        // Android version < 12, "android.permission.BLUETOOTH" just fine
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Toast.makeText(MainActivity.this,
                    "BLUETOOTH granted for earlier Android", Toast.LENGTH_SHORT).show();
            return;
        }

        // Android 12+ has to go through the process
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.BLUETOOTH_SCAN},
                    BLUETOOTH_SCAN_CODE);
        } else {
            Toast.makeText(MainActivity.this,
                    "BLUETOOTH_SCAN already granted", Toast.LENGTH_SHORT).show();
        }

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    BLUETOOTH_CONNECT_CODE);
        } else {
            Toast.makeText(MainActivity.this,
                    "BLUETOOTH_CONNECT already granted", Toast.LENGTH_SHORT).show();
        }
    }

    // Modify from chap14, pp390 findRobot()
    private BluetoothDevice cpf_locateInPairedBTList(String name) {
        BluetoothDevice lv_bd = null;
        try {
            cv_btInterface = BluetoothAdapter.getDefaultAdapter();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return null;
            }
            cv_pairedDevices = cv_btInterface.getBondedDevices();
            Iterator<BluetoothDevice> lv_it = cv_pairedDevices.iterator();
            while (lv_it.hasNext()) {
                lv_bd = lv_it.next();
                if (lv_bd.getName().equalsIgnoreCase(name)) {
                    cv_label01.setText(name + " is in paired list");
                    return lv_bd;
                }
            }
            cv_label01.setText(name + " is NOT in paired list");
        } catch (Exception e) {
            cv_label01.setText("Failed in findRobot() " + e.getMessage());
        }
        return null;
    }

    // Modify frmo chap14, pp391 connectToRobot()
    private void cpf_connectToEV3(BluetoothDevice bd) {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            cv_btSocket = bd.createRfcommSocketToServiceRecord
                    (UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            cv_btSocket.connect();

            //// HERE
            cv_is = cv_btSocket.getInputStream();
            cv_os = cv_btSocket.getOutputStream();
            cv_label02.setText("Connect to " + bd.getName() + " at " + bd.getAddress());
        } catch (Exception e) {
            cv_label02.setText("Error interacting with remote device [" +
                    e.getMessage() + "]");
        }
    }

    private void cpf_disconnFromEV3(BluetoothDevice bd) {
        try {
            cv_btSocket.close();
            cv_is.close();
            cv_os.close();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            cv_label02.setText(bd.getName() + " is disconnect ");
        } catch (Exception e) {
            cv_label02.setText("Error in disconnect -> " + e.getMessage());
        }
    }

    // Communication Developer Kit Page 27
    // 4.2.2 Start motor B & C forward at power 50 for 3 rotation and braking at destination
    private void cpf_EV3MoveMotor(int power) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] buffer = new byte[20];       // 0x12 command length

                    buffer[0] = (byte) (20 - 2);
                    buffer[1] = 0;

                    buffer[2] = 34;
                    buffer[3] = 12;

                    buffer[4] = (byte) 0x80;

                    buffer[5] = 0;
                    buffer[6] = 0;

                    buffer[7] = (byte) 0xad; // OP code
                    buffer[8] = 0; //

                    buffer[9] = (byte) 0x06; // Output

                    buffer[10] = (byte) 0x81;
                    buffer[11] = (byte) power;

                    buffer[12] = 0;

                    buffer[13] = (byte) 0x82;
                    buffer[14] = (byte) 0x84;
                    buffer[15] = -1;

                    buffer[16] = (byte) 0x82;
                    buffer[17] = (byte) 0xB4;
                    buffer[18] = (byte) 0x03;

                    buffer[19] = 1;

                    cv_os.write(buffer);
                    cv_os.flush();
                } catch (Exception e) {
                    cv_label01.setText("Error in MoveForward(" + e.getMessage() + ")");
                }
            }
        });
    }


    // 4.2.5 Play a 1Kz tone at level 2 for 1 sec.
    private void cpf_EV3PlayTone() {
        try {
            byte[] buffer = new byte[17];       // 0x0f command length

            buffer[0] = (byte) (17 - 2);
            buffer[1] = 0;

            buffer[2] = 34;
            buffer[3] = 12;

            buffer[4] = (byte) 0x80;

            buffer[5] = 0;
            buffer[6] = 0;

            buffer[7] = (byte) 0x94;
            buffer[8] = 1;

            buffer[9] = (byte) 0x81;
            buffer[10] = (byte) 0x02;

            buffer[11] = (byte) 0x82;
            buffer[12] = (byte) 0xe8;
            buffer[13] = (byte) 0x03;

            buffer[14] = (byte) 0x82;
            buffer[15] = (byte) 0xe8;
            buffer[16] = (byte) 0x03;

            cv_os.write(buffer);
            cv_os.flush();
        } catch (Exception e) {
            cv_label02.setText("Error in MoveForward(" + e.getMessage() + ")");
        }
    }
}
*/
