package lk.sure.dream.compose.home

import android.app.Activity
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp

@Composable
fun PrivacyPolicyDialog(
    onAccept: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var acceptChecked by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            Button(
                onClick = onAccept,
                enabled = acceptChecked,
            ) {
                Text("Accept & Continue")
            }
        },
        dismissButton = {
            TextButton(
                onClick = (context as Activity)::finishAffinity,
            ) {
                Text("Decline")
            }
        },
        title = { Text("Privacy Policy") },
        text = {
            Column {
                Text("Before you begin, please review our Privacy Policy")

                PrivacyPolicyText()

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = acceptChecked,
                        onCheckedChange = { acceptChecked = it }
                    )

                    Text("I have read and agree to the Privacy Policy.")
                }
            }
        },
        modifier = modifier
    )
}

@Composable
fun PrivacyPolicyInfoDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {},
        dismissButton = { TextButton(onDismissRequest) { Text("Cancel") } },
        title = { Text("Privacy Policy") },
        text = {
            Column {
                PrivacyPolicyText()
            }
        },
        modifier = modifier,
    )
}

@Composable
fun ColumnScope.PrivacyPolicyText() {
    val state = rememberLazyListState()

    val canScrollMore by remember {
        derivedStateOf {
            state.canScrollForward
        }
    }

    Box(
        modifier = Modifier.weight(1F),
        contentAlignment = Alignment.BottomCenter
    ) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceDim,
                ) {
                    Text(
                        text = AnnotatedString.fromHtml(policy),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    )
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = canScrollMore,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                AlertDialogDefaults.containerColor
                            )
                        )
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    null,
                    tint = AlertDialogDefaults.iconContentColor
                )
            }
        }
    }
}

const val PRIVACY_POLICY = """
<p><b>Effective from:</b> 2025-04-01</p>
<br/>
<p>
    Dream (hereinafter referred to as "us, "we" or "our) respects
    your privacy and is committed to protecting the information
    you share with us. This Privacy Policy explains how we
    collect, use, and protect your data when you use our drawing
    app (referred to as “App”)
</p>
<br/>
<h3>1. Information We Do Not Collect</h3>
<p>
    We <b>do not collect</b> any <b>personally identifiable information</b>
    (PII) from users. We do not require or store any personal
    data such as your name, email address, or location.
</p>
<br/>
<h3>2. Use of Storage</h3>
<p>
    Our App requests <b>permission to access your device's
    storage</b>. This permission allows us to:
</p>
<ul>
    <li>Save your drawings or any content you create within the app.</li>
    <li>Load and modify images or files from your device's storage.</li>
    <li>Store temporary files for the app's operation.</li>
</ul>
<p>
    <b>Please note</b> that the app does not share, sell, or use the
    data stored in your device's storage for any purpose other
    than the functioning of the App itself.
</p>
<br/>
<h3>3. Data Loss Disclaimer</h3>
<p>
    While we make every effort to ensure a <b>smooth and reliable
    experience</b>, App cannot guarantee that your data
    (including saved drawings or files) will always be preserved.
    <b>Data</b> stored on your device <b>may be lost</b> due to:
</p>
<ul>
    <li><b>Uninstalling the app</b></li>
    <li><b>Device malfunction or damage</b></li>
    <li><b>Operating system updates or resets</b></li>
    <li><b>App crashes or bugs</b></li>
    <li><b>User error</b></li>
</ul>
<p>
    We are <b>not responsible for any loss of data</b> that
    may occur during your use of the App.
</p>
<br/>
<h3>4. Changes to This Privacy Policy</h3>
<p>
    We may <b>update</b> this <b>Privacy Policy</b> from time to time. Any
    changes will be posted in the app, and the updated policy
    will be effective as of the date it is posted.
</p>
<br/>
<h3>5. Contact Us</h3>
<p>
    If you have any questions or concerns about this Privacy
    Policy or how we handle your data, please contact us at:
</p>
<ul>
    <li>
        <b>email:</b> dreamartpixel@gmail.com
    </li>
</ul>
"""

private val policy = PRIVACY_POLICY
    .trimIndent()
    .replace("<ul>", "")
    .replace("</ul>", "")
    .replace("<li>", "<br/>&nbsp;&nbsp;&nbsp;&nbsp;• ")
    .replace("</li>>", "")