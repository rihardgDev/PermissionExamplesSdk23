package com.example.rgdev.permissionexamplessdk23;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import static android.Manifest.permission.WRITE_CONTACTS;


import com.canelmas.let.AskPermission;
import com.canelmas.let.DeniedPermission;
import com.canelmas.let.Let;
import com.canelmas.let.RuntimePermissionListener;
import com.canelmas.let.RuntimePermissionRequest;

import java.util.ArrayList;
import java.util.List;



/*
*  Permissions example, first button shows crash message if permision is denied, second button
*  uses google sdk for handling permissions, third button uses Let library for handling permissions
* */
public class MainActivity extends AppCompatActivity  implements RuntimePermissionListener {
    private static final String TAG = "Contacts";
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


    }
    //method for inserting dummy contact, without permission handler,
    private void insertDummyContact() {
        // Two operations are needed to insert a new contact.
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>(2);

        // First, set up a new raw contact.
        ContentProviderOperation.Builder op =
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null);
        operations.add(op.build());

        // Next, set the name for the contact.
        op = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                        "__DUMMY CONTACT from runtime permissions sample");
        operations.add(op.build());

        // Apply the operations.
        ContentResolver resolver = getContentResolver();
        try {
            resolver.applyBatch(ContactsContract.AUTHORITY, operations);
        } catch (RemoteException e) {
            Log.d(TAG, "Could not add a new contact: " + e.getMessage());
        } catch (OperationApplicationException e) {
            Log.d(TAG, "Could not add a new contact: " + e.getMessage());
        }
    }

    // Method withoud permission handler, it crashes first time the
    // application is run and if the permission has been revoked and it displays the crash msg
    public void insertDummyContactWithoutPermissions(View v)
    {
        try {
            insertDummyContact();
        }
        catch (Exception e)
        {
            new AlertDialog.Builder(MainActivity.this)
                    .setMessage(e.toString())
                    .setNegativeButton("Cancel", null)
                    .create()
                    .show();
        }

    }

    //the method requests Android permission handler
    // https://developer.android.com/training/permissions/requesting.html
    public void insertDummyContactWrapper(View v) {
        int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.WRITE_CONTACTS);
        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_CONTACTS)) {
                showMessageOKCancel("You need to allow access to Contacts",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                requestPermissions(new String[] {Manifest.permission.WRITE_CONTACTS},
                                        REQUEST_CODE_ASK_PERMISSIONS);
                            }
                        });
                return;
            }
            requestPermissions(new String[] {Manifest.permission.WRITE_CONTACTS},
                    REQUEST_CODE_ASK_PERMISSIONS);
            return;
        }
        insertDummyContact();
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    //Handles the  permissions request response, if you're using permissions in fragment
    // use the fragment method, otherwise respons will be posted to parent Activity
    //https://stackoverflow.com/questions/32714787/android-m-permissions-onrequestpermissionsresult-not-being-called
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // Let library eases the handling
        Let.handle(this, requestCode, permissions, grantResults);
        //Android standard handling code
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "WRITE_CONTACTS Sucessfull!", Toast.LENGTH_SHORT)
                            .show();
                    // Permission Granted
                    insertDummyContact();
                } else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "WRITE_CONTACTS Denied!", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

    @AskPermission(WRITE_CONTACTS) //Let library
    public void PermissionHelperDemo(View v)
    {

        insertDummyContact();

    }

    @Override //Let library
    public void onShowPermissionRationale(List<String> permissions, final RuntimePermissionRequest request) {
        /**
         * show permission rationales in a dialog, wait for user confirmation and retry the permission
         * request by calling request.retry()
         */

        //  tell user why you need these permissions
        final StringBuilder sb = new StringBuilder();

        for (String permission : permissions) {
            sb.append("Contacts");
            sb.append("\n");
        }

        new AlertDialog.Builder(this).setTitle("Permission Required!")
                .setMessage(sb.toString())
                .setCancelable(true)
                .setNegativeButton("No Thanks", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.retry();
                    }
                })
                .show();

    }


    @Override //Let library
    public void onPermissionDenied(List<DeniedPermission> deniedPermissionList) {

        /**
         * Let's just do nothing if permission is denied without
         * 'Never ask Again' checked.
         *
         * If it's the case show more informative message and prompt user
         * to the app settings screen.
         */

        final StringBuilder sb = new StringBuilder();

        for (DeniedPermission result : deniedPermissionList) {

            if (result.isNeverAskAgainChecked()) {
                sb.append("onNeverShowAgain for " + result.getPermission());
                sb.append("\n");
            }

        }



            new AlertDialog.Builder(this).setTitle("Go Settings and Grant Permission")
                    .setMessage(sb.toString())
                    .setCancelable(true)
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivityForResult(intent, 1);

                            dialog.dismiss();
                        }
                    }).show();


    }
}

