package com.example.novacircle.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.novacircle.domain.model.NovaProfile

@Composable
fun ProfileSidebar(
    profile: NovaProfile?,
    onGlobalSharingChanged: (Boolean) -> Unit,
    onFullNameChanged: (String) -> Unit,
    onLogout: () -> Unit
) {
    var editingName by remember { mutableStateOf(false) }
    var nameDraft by remember(profile?.fullName) { mutableStateOf(profile?.fullName.orEmpty()) }

    ModalDrawerSheet(modifier = Modifier.widthIn(max = 300.dp)) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (profile?.avatarUrl != null) {
                    AsyncImage(
                        model = profile.avatarUrl,
                        contentDescription = "Profile photo",
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("@${profile?.username ?: ""}", style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(16.dp))

            if (editingName) {
                NovaTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    placeholder = "Full name"
                )
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    TextButton(onClick = {
                        onFullNameChanged(nameDraft)
                        editingName = false
                    }) { Text("Save") }
                    TextButton(onClick = { editingName = false }) { Text("Cancel") }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = profile?.fullName?.takeIf { it.isNotBlank() } ?: "Add full name",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (profile?.fullName.isNullOrBlank())
                            MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { editingName = true }) { Text("Edit") }
                }
            }

            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { /* TODO: image picker + upload to Supabase Storage */ }) {
                Text("Change profile photo")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Location sharing", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Turns off sharing across all circles",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Switch(
                    checked = profile?.locationSharingEnabled ?: true,
                    onCheckedChange = onGlobalSharingChanged
                )
            }

            Spacer(Modifier.weight(1f))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Text("Log out", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}