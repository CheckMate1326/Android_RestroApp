package com.restaurant.mobile

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.restaurant.mobile.api.ApiClient
import com.restaurant.mobile.delivery.*
import kotlinx.coroutines.launch

/** Staff credentials are never persisted and are cleared when this screen leaves composition. */
class StaffActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var signedIn by remember { mutableStateOf(false) }
            var user by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var error by remember { mutableStateOf<String?>(null) }
            var busy by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            DisposableEffect(Unit) { onDispose { ApiClient.clearStaffSession() } }
            if (signedIn)
                RestaurantMobileApp { message ->
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            else
                RestroTheme {
                    androidx.compose.material3.Surface {
                        Page {
                            Heading("Restaurant staff", "Sign in with your operator credentials.")
                            Field("Staff username", user, { user = it })
                            Field("Password", password, { password = it }, true)
                            error?.let { Text(it) }
                            Action(
                                "Open staff tools",
                                !busy && user.isNotBlank() && password.isNotBlank(),
                            ) {
                                busy = true
                                error = null
                                scope.launch {
                                    signedIn = ApiClient.authenticateStaff(user, password)
                                    password = ""
                                    busy = false
                                    if (!signedIn)
                                        error =
                                            "Unable to sign in. Check your staff credentials and connection."
                                }
                            }
                        }
                    }
                }
        }
    }
}
