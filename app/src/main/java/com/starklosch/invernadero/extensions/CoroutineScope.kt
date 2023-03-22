// Source: https://github.com/JuulLabs/sensortag/blob/55288ed1e79fe0ed286ea2950a2f9a45b1f8706b/app/src/commonMain/kotlin/CoroutineScope.kt
package com.starklosch.invernadero.extensions

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren

fun CoroutineScope.childScope() =
    CoroutineScope(coroutineContext + Job(coroutineContext[Job]))

fun CoroutineScope.cancelChildren(
    cause: CancellationException? = null
) = coroutineContext[Job]?.cancelChildren(cause)