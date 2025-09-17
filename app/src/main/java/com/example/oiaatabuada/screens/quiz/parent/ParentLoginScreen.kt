package com.example.oiaatabuada.screens.quiz.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import com.example.oiaatabuada.managers.HistoryManager

@Composable
fun ParentLoginScreen(
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val historyManager = remember { HistoryManager(context) }

    var password by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }

    // Campos para mudança de senha
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var changePasswordError by remember { mutableStateOf("") }

    var showExitConfirmation by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1E1E1E)
        ) {
            if (showChangePassword) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Alterar Senha",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = {
                            oldPassword = it
                            changePasswordError = ""
                        },
                        label = { Text("Senha atual", color = Color.White) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = {
                            newPassword = it
                            changePasswordError = ""
                        },
                        label = { Text("Nova senha", color = Color.White) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            changePasswordError = ""
                        },
                        label = { Text("Confirmar nova senha", color = Color.White) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (changePasswordError.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            changePasswordError,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Start)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { showChangePassword = false }) {
                            Text("Cancelar", color = Color.White)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                when {
                                    oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty() -> {
                                        changePasswordError = "Preencha todos os campos"
                                    }
                                    newPassword != confirmPassword -> {
                                        changePasswordError = "As senhas não coincidem"
                                    }
                                    newPassword.length < 4 -> {
                                        changePasswordError = "Nova senha deve ter pelo menos 4 caracteres"
                                    }
                                    !historyManager.checkParentPassword(oldPassword) -> {
                                        changePasswordError = "Senha atual incorreta"
                                    }
                                    else -> {
                                        historyManager.setParentPassword(newPassword)
                                        showChangePassword = false
                                        onSuccess()
                                    }
                                }
                            }
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Acesso dos Pais",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            showError = false
                        },
                        label = { Text("Senha de acesso", color = Color.White) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = showError,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (showError) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Senha incorreta!",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Start)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = { showChangePassword = true },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text("Alterar senha", color = Color.Cyan)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = onCancel) {
                            Text("Cancelar", color = Color.White)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (historyManager.checkParentPassword(password)) {
                                    onSuccess()
                                } else {
                                    showError = true
                                }
                            }
                        ) {
                            Text("Acessar")
                        }
                    }
                }
            }
        }
    }
}