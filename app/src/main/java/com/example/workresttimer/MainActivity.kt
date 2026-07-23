package com.example.workresttimer

import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class TimerState { IDLE, WORK, REST, PAUSED, FINISHED }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WorkRestTimerApp(onTriggerAlert = { playBeepAndVibrate() })
                }
            }
        }
    }

    private fun playBeepAndVibrate() {
        try {
            val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 500)
            val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun WorkRestTimerApp(onTriggerAlert: () -> Unit) {
    var workSecInput by remember { mutableStateOf("30") }
    var restSecInput by remember { mutableStateOf("10") }
    var roundsInput by remember { mutableStateOf("5") }

    var timerState by remember { mutableStateOf(TimerState.IDLE) }
    var previousState by remember { mutableStateOf(TimerState.WORK) }
    
    var currentRound by remember { mutableIntStateOf(1) }
    var secondsLeft by remember { mutableIntStateOf(30) }

    val totalRounds = roundsInput.toIntOrNull() ?: 1
    val workSec = workSecInput.toIntOrNull() ?: 30
    val restSec = restSecInput.toIntOrNull() ?: 10

    LaunchedEffect(timerState, secondsLeft) {
        if (timerState == TimerState.WORK || timerState == TimerState.REST) {
            if (secondsLeft > 0) {
                delay(1000L)
                secondsLeft -= 1
            } else {
                onTriggerAlert()
                if (timerState == TimerState.WORK) {
                    if (restSec > 0) {
                        timerState = TimerState.REST
                        secondsLeft = restSec
                    } else if (currentRound < totalRounds) {
                        currentRound++
                        timerState = TimerState.WORK
                        secondsLeft = workSec
                    } else {
                        timerState = TimerState.FINISHED
                    }
                } else if (timerState == TimerState.REST) {
                    if (currentRound < totalRounds) {
                        currentRound++
                        timerState = TimerState.WORK
                        secondsLeft = workSec
                    } else {
                        timerState = TimerState.FINISHED
                    }
                }
            }
        }
    }

    val backgroundColor = when (timerState) {
        TimerState.WORK -> Color(0xFF2E7D32) // Green
        TimerState.REST -> Color(0xFFE65100) // Orange
        TimerState.FINISHED -> Color(0xFF1565C0) // Blue
        else -> MaterialTheme.colorScheme.background
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Work:Rest Timer",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 24.dp)
        )

        if (timerState == TimerState.IDLE) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = workSecInput,
                    onValueChange = { workSecInput = it },
                    label = { Text("Work Duration (seconds)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = restSecInput,
                    onValueChange = { restSecInput = it },
                    label = { Text("Rest Duration (seconds)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = roundsInput,
                    onValueChange = { roundsInput = it },
                    label = { Text("Total Rounds") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = when (timerState) {
                        TimerState.WORK -> "WORK"
                        TimerState.REST -> "REST"
                        TimerState.PAUSED -> "PAUSED"
                        TimerState.FINISHED -> "DONE!"
                        else -> ""
                    },
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = String.format("%02d:%02d", secondsLeft / 60, secondsLeft % 60),
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (timerState != TimerState.FINISHED) {
                    Text(
                        text = "Round $currentRound of $totalRounds",
                        fontSize = 22.sp,
                        color = Color.White
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            when (timerState) {
                TimerState.IDLE -> {
                    Button(
                        onClick = {
                            currentRound = 1
                            secondsLeft = workSec
                            timerState = TimerState.WORK
                        },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text("START", fontSize = 20.sp)
                    }
                }
                TimerState.WORK, TimerState.REST -> {
                    Button(
                        onClick = {
                            previousState = timerState
                            timerState = TimerState.PAUSED
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("PAUSE")
                    }
                }
                TimerState.PAUSED -> {
                    Button(
                        onClick = { timerState = previousState },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                    ) {
                        Text("RESUME")
                    }
                    Button(
                        onClick = { timerState = TimerState.IDLE },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("RESET")
                    }
                }
                TimerState.FINISHED -> {
                    Button(
                        onClick = { timerState = TimerState.IDLE },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text("RESET", color = Color.Black)
                    }
                }
            }
        }
    }
}
