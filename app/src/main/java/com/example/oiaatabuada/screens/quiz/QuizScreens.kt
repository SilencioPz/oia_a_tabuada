package com.example.oiaatabuada.screens.quiz

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.oiaatabuada.managers.MathQuestion
import androidx.compose.ui.res.painterResource
import com.example.oiaatabuada.R

@Composable
fun LogoHeader() {
    Image(
        painter = painterResource(id = R.drawable.silenciopz_logo2),
        contentDescription = "Logo SilencioPZ",
        modifier = Modifier
            .size(120.dp)
            .padding(8.dp),
        alignment = Alignment.Center
    )
}
@Composable
fun StartMessageScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            LogoHeader()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "🧮 Quiz de Matemática Iniciado!",
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Responda 3 perguntas corretamente!",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = Color.White
            )
        }
    }
}

@Composable
fun EndMessageScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            LogoHeader()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "🎉 Quiz Finalizado!",
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Até daqui 10 minutos!",
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = Color.White
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MathQuestionScreen(
    question: MathQuestion,
    questionNumber: Int,
    userAnswer: String,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Scaffold(
        containerColor = Color.Black,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Pergunta $questionNumber/3",
                    color = Color.White
                )
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Black
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(24.dp)
                .fillMaxSize()
                .background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            LogoHeader()

            Text(
                text = question.question,
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
                color = Color.White,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = userAnswer,
                onValueChange = onAnswerChange,
                label = {
                    Text(
                        "Sua resposta",
                        color = Color.White
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = LocalTextStyle.current.copy(color = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = userAnswer.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text(if (questionNumber < 3) "Próxima" else "Finalizar")
            }
        }
    }
}

@Composable
fun WaitingScreen(secondsRemaining: Int) {
    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            LogoHeader()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "⏰ Próximo desafio em:",
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                fontSize = 36.sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun PasswordScreen(
    onSuccess: () -> Unit,
    onCancel: () -> Unit = {}
) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        LogoHeader()

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Digite a senha",
            fontSize = 20.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                error = ""
            },
            label = { Text("Senha",  color = Color.White) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            textStyle = LocalTextStyle.current.copy(color = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        if (error.isNotEmpty()) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Gray,
                    contentColor = Color.White
                )
            ) {
                Text("Cancelar")
            }

            Button(
                onClick = {
                    if (password == "1234") {
                        onSuccess()
                    } else {
                        error = "Senha incorreta!"
                    }
                },
                enabled = password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text("Confirmar")
            }
        }
    }
}