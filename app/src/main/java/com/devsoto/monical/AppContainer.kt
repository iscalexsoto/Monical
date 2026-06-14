package com.devsoto.monical

import android.content.Context
import com.devsoto.monical.data.auth.AuthManager
import com.devsoto.monical.data.ocr.MlKitTextRecognizer
import com.devsoto.monical.data.parse.ReceiptParserRouter
import com.devsoto.monical.data.repository.FirestoreReceiptRepository
import com.devsoto.monical.data.repository.ReceiptRepository

/**
 * Manual dependency container (the project doesn't use Hilt). Holds the singletons the UI
 * layer needs and wires the data layer together. Created once in [MonicalApplication].
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val textRecognizer: MlKitTextRecognizer by lazy { MlKitTextRecognizer(appContext) }
    val parser: ReceiptParserRouter by lazy { ReceiptParserRouter() }
    val authManager: AuthManager by lazy { AuthManager() }
    val receiptRepository: ReceiptRepository by lazy { FirestoreReceiptRepository(authManager) }
}
