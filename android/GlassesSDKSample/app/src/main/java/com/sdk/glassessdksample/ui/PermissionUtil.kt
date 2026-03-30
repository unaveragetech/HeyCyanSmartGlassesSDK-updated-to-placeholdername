package com.sdk.glassessdksample.ui
import android.content.Context
import android.os.Build
import androidx.fragment.app.FragmentActivity
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions

/**
 * @author hzy ,
 * @date  2020/12/22
 **/
fun requestCallPhonePermission(
    activity: FragmentActivity,
    requestCallback: OnPermissionCallback,
) {
    XXPermissions.with(activity)
        .permission(Permission.READ_PHONE_STATE)
        .permission(Permission.READ_CALL_LOG)
        .permission(Permission.CALL_PHONE)
        .permission(Permission.READ_CONTACTS)
        .permission(Permission.ANSWER_PHONE_CALLS)
        .request(requestCallback)
}

fun hasCallPhonePermission(
    activity: FragmentActivity,
): Boolean {
    val permissions = mutableListOf<String>()
    permissions.add(Permission.READ_PHONE_STATE)
    permissions.add(Permission.READ_CALL_LOG)
    permissions.add(Permission.CALL_PHONE)
    permissions.add(Permission.READ_CONTACTS)
    permissions.add(Permission.ANSWER_PHONE_CALLS)
    return XXPermissions.isGranted(activity, permissions)
}

fun hasCameraPermission(
    context: Context,
): Boolean {
    return XXPermissions.isGranted(context, Permission.CAMERA)
}

fun hasSMSPermission(
    activity: FragmentActivity,
): Boolean {
    val permissions = mutableListOf<String>()
    permissions.add(Permission.READ_SMS)
    permissions.add(Permission.RECEIVE_SMS)
    return XXPermissions.isGranted(activity, permissions)
}

fun hasContactPermission(activity: FragmentActivity): Boolean {
    return XXPermissions.isGranted(activity, Permission.READ_CONTACTS)
}

fun hasLocationPermission(activity: FragmentActivity): Boolean {
    return XXPermissions.isGranted(activity, Permission.ACCESS_FINE_LOCATION)
}

fun hasBgLocationPermission(activity: FragmentActivity): Boolean {
    return XXPermissions.isGranted(activity, Permission.ACCESS_BACKGROUND_LOCATION)
}

fun hasCallPermission(activity: FragmentActivity): Boolean {
    val permissions = mutableListOf<String>()
    permissions.add(Permission.READ_PHONE_STATE)
    permissions.add(Permission.READ_CALL_LOG)
    permissions.add(Permission.CALL_PHONE)
    permissions.add(Permission.ANSWER_PHONE_CALLS)
    return XXPermissions.isGranted(activity, permissions)
}

fun hasBluetooth(activity: FragmentActivity): Boolean {
    val permissions = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissions.add(Permission.BLUETOOTH_SCAN)
        permissions.add(Permission.BLUETOOTH_CONNECT)
        permissions.add(Permission.BLUETOOTH_ADVERTISE)
    } else {
        permissions.add(Permission.BLUETOOTH_SCAN) // XXPermissions handles legacy mapping
        permissions.add(Permission.ACCESS_FINE_LOCATION)
    }
    return XXPermissions.isGranted(activity, permissions)
}


fun requestSMSPermission(
    activity: FragmentActivity,
    requestCallback: OnPermissionCallback,
) {
    XXPermissions.with(activity)
        .permission(Permission.READ_SMS)
        .permission(Permission.RECEIVE_SMS)
        .request(requestCallback)
}

fun requestLocationPermission(
    activity: FragmentActivity,
    requestCallback: OnPermissionCallback,
) {
    XXPermissions.with(activity)
        .permission(Permission.ACCESS_COARSE_LOCATION)
        .permission(Permission.ACCESS_FINE_LOCATION)
        .request(requestCallback)
}

fun requestBluetoothPermission(
    activity: FragmentActivity,
    requestCallback: OnPermissionCallback
) {
    val request = XXPermissions.with(activity)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        request.permission(Permission.BLUETOOTH_SCAN)
        request.permission(Permission.BLUETOOTH_CONNECT)
        request.permission(Permission.BLUETOOTH_ADVERTISE)
    }
    // Always request location for scanning as a fallback/requirement for BLE on many versions
    request.permission(Permission.ACCESS_FINE_LOCATION)
    request.request(requestCallback)
}

fun requestCallPermission(
    activity: FragmentActivity,
    requestCallback: OnPermissionCallback,
) {
    XXPermissions.with(activity)
        .permission(Permission.READ_PHONE_STATE)
        .permission(Permission.READ_CALL_LOG)
        .permission(Permission.CALL_PHONE)
        .permission(Permission.ANSWER_PHONE_CALLS)
        .request(requestCallback)
}

fun requestContactPermission(
    activity: FragmentActivity,
    requestCallback: OnPermissionCallback,
) {
    XXPermissions.with(activity)
        .permission(Permission.READ_CONTACTS)
        .request(requestCallback)
}

fun requestBgLocation(activity: FragmentActivity, requestCallback: OnPermissionCallback) {
    XXPermissions.with(activity)
        .permission(Permission.ACCESS_COARSE_LOCATION)
        .permission(Permission.ACCESS_FINE_LOCATION)
        .permission(Permission.ACCESS_BACKGROUND_LOCATION)
        .request(requestCallback)
}

fun requestAlertWindowPermission(activity: FragmentActivity) {
    XXPermissions.with(activity).permission(Permission.SYSTEM_ALERT_WINDOW)
}

fun requestNearbyWifiDevicesPermission(
    activity: FragmentActivity,
    requestCallback: OnPermissionCallback
) {
    XXPermissions.with(activity)
        .permission(Permission.NEARBY_WIFI_DEVICES)
        .request(requestCallback)
}

fun hasNearbyWifiDevicesPermission(
    activity: FragmentActivity
): Boolean {
    return XXPermissions.isGranted(activity, Permission.NEARBY_WIFI_DEVICES)
}


fun requestAllPermission(
    activity: FragmentActivity,
    callback: OnPermissionCallback
) {
    val request = XXPermissions.with(activity)
    
    // Bluetooth & Location
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        request.permission(Permission.BLUETOOTH_SCAN)
        request.permission(Permission.BLUETOOTH_CONNECT)
        request.permission(Permission.BLUETOOTH_ADVERTISE)
    }
    request.permission(Permission.ACCESS_FINE_LOCATION)
    
    // Storage
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        request.permission(Permission.MANAGE_EXTERNAL_STORAGE)
    } else {
        request.permission(Permission.READ_EXTERNAL_STORAGE)
        request.permission(Permission.WRITE_EXTERNAL_STORAGE)
    }

    // Nearby Wi-Fi (Required for the P2P transfer feature on Pixel 6/Android 13+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        request.permission(Permission.NEARBY_WIFI_DEVICES)
    }
    
    request.request(callback)
}


fun requestCameraPermission(
    activity: FragmentActivity,
    requestCallback: OnPermissionCallback,
) {
    XXPermissions.with(activity).permission(
        Permission.CAMERA
    ).request(requestCallback)
}