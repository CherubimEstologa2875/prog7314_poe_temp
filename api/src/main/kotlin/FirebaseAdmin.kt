package com.example

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import java.io.File
import java.io.InputStream
import java.util.Base64

// Firebase Admin SDK initialisation for the Runway REST API (IIE, 2026).
// The service account key is kept on the server, never in the app (CodePath Android Cliffnotes, n.d.).
object FirebaseAdmin {
    // The Firestore handle every service uses.
    lateinit var db: Firestore
        private set

    fun init() {
        if (FirebaseApp.getApps().isNotEmpty()) {
            db = FirestoreClient.getFirestore()
            return
        }

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(credentialStream()))
            .setProjectId(System.getenv("FIREBASE_PROJECT_ID") ?: DEFAULT_PROJECT_ID)
            .build()

        FirebaseApp.initializeApp(options)
        db = FirestoreClient.getFirestore()
    }

    // Two ways to reach the same credentials.
    private fun credentialStream(): InputStream {
        val encoded = System.getenv("FIREBASE_CREDENTIALS")

        return if (!encoded.isNullOrBlank()) {
            Base64.getDecoder().decode(encoded).inputStream()
        } else {
            val file = File(SERVICE_ACCOUNT_FILE)
            require(file.exists()) {
                "$SERVICE_ACCOUNT_FILE not found. Download it from the Firebase console " +
                    "(Project settings > Service accounts > Generate new private key) " +
                    "and place it in the api/ folder."
            }
            file.inputStream()
        }
    }

    private const val SERVICE_ACCOUNT_FILE = "serviceAccountKey.json"

    private const val DEFAULT_PROJECT_ID = "runway-part2-group11"
}

/* Reference List
IIE, 2026. PROG7314 Module Manual. The Independent Institute of Education (Pty) Ltd.
CodePath Android Cliffnotes, n.d.. Storing Secret Keys in Android. [online] Available at: <https://guides.codepath.com/android/Storing-Secret-Keys-in-Android> [Accessed 31 July 2023].
*/
