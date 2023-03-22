package com.starklosch.invernadero.extensions

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
fun MultiplePermissionsState.allRequiredPermissionsGranted(requiredPermissions: Collection<String>): Boolean {
    return revokedPermissions.none { permissionState -> permissionState.permission in requiredPermissions }
}