package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.Formatters

@Composable
fun CalculatorDialog(
    initialValue: String = "",
    onDismiss: () -> Unit,
    onApplyResult: (Double) -> Unit
) {
    var expr by remember { mutableStateOf(initialValue.ifBlank { "0" }) }

    val evaluatedResult = remember(expr) {
        evaluateMathExpression(expr)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Calculate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Kalkulator Pembayaran", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Calculator Screen Display
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = expr.ifBlank { "0" },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = Formatters.formatRupiah(evaluatedResult ?: 0.0),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.End
                        )
                    }
                }

                // Calculator Buttons Grid
                val buttonRows = listOf(
                    listOf("C", "⌫", "÷", "×"),
                    listOf("7", "8", "9", "-"),
                    listOf("4", "5", "6", "+"),
                    listOf("1", "2", "3", "="),
                    listOf("0", "00", "000", ".")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    buttonRows.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            row.forEach { btnText ->
                                val isOperator = btnText in listOf("+", "-", "×", "÷", "=")
                                val isClear = btnText == "C"
                                val isBackspace = btnText == "⌫"

                                val bgColor = when {
                                    isClear -> Color(0xFFFFEBEE)
                                    isOperator -> MaterialTheme.colorScheme.primaryContainer
                                    isBackspace -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.surface
                                }

                                val textColor = when {
                                    isClear -> Color(0xFFD32F2F)
                                    isOperator -> MaterialTheme.colorScheme.onPrimaryContainer
                                    isBackspace -> MaterialTheme.colorScheme.onSecondaryContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bgColor)
                                        .clickable {
                                            when (btnText) {
                                                "C" -> expr = "0"
                                                "⌫" -> {
                                                    expr = if (expr.length > 1) expr.dropLast(1) else "0"
                                                }
                                                "=" -> {
                                                    val res = evaluateMathExpression(expr)
                                                    if (res != null) {
                                                        expr = if (res % 1.0 == 0.0) res.toLong().toString() else res.toString()
                                                    }
                                                }
                                                "+", "-", "×", "÷" -> {
                                                    if (expr == "0") {
                                                        if (btnText == "-") expr = "-"
                                                    } else {
                                                        val lastChar = expr.lastOrNull()
                                                        if (lastChar in listOf('+', '-', '×', '÷')) {
                                                            expr = expr.dropLast(1) + btnText
                                                        } else {
                                                            expr += btnText
                                                        }
                                                    }
                                                }
                                                else -> { // Numbers and dots
                                                    if (expr == "0" && btnText != ".") {
                                                        expr = btnText
                                                    } else {
                                                        expr += btnText
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isBackspace) {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Hapus",
                                            tint = textColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Text(
                                            text = btnText,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalVal = evaluateMathExpression(expr) ?: 0.0
                    onApplyResult(finalVal)
                    onDismiss()
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text("Gunakan Hasil", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Batal")
            }
        }
    )
}

/**
 * Safely evaluates standard math expressions like "25000 + 15000 * 2"
 */
fun evaluateMathExpression(expression: String): Double? {
    try {
        val sanitized = expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace(" ", "")

        if (sanitized.isBlank()) return 0.0

        // Parse simple tokens: numbers and operators
        val tokens = mutableListOf<String>()
        var currentNum = StringBuilder()

        for (i in sanitized.indices) {
            val char = sanitized[i]
            if (char.isDigit() || char == '.') {
                currentNum.append(char)
            } else if (char in listOf('+', '-', '*', '/')) {
                if (currentNum.isNotEmpty()) {
                    tokens.add(currentNum.toString())
                    currentNum = StringBuilder()
                } else if (char == '-' && (tokens.isEmpty() || tokens.last() in listOf("+", "-", "*", "/"))) {
                    // Negative number
                    currentNum.append(char)
                    continue
                }
                tokens.add(char.toString())
            }
        }
        if (currentNum.isNotEmpty()) {
            tokens.add(currentNum.toString())
        }

        if (tokens.isEmpty()) return 0.0

        // Perform multiplication and division first
        val step1 = mutableListOf<String>()
        var idx = 0
        while (idx < tokens.size) {
            val token = tokens[idx]
            if (token == "*" || token == "/") {
                val prevNum = step1.removeAt(step1.size - 1).toDoubleOrNull() ?: 0.0
                val nextNum = tokens.getOrNull(idx + 1)?.toDoubleOrNull() ?: 0.0
                val res = if (token == "*") prevNum * nextNum else if (nextNum != 0.0) prevNum / nextNum else 0.0
                step1.add(res.toString())
                idx += 2
            } else {
                step1.add(token)
                idx++
            }
        }

        // Perform addition and subtraction next
        var result = step1.getOrNull(0)?.toDoubleOrNull() ?: 0.0
        idx = 1
        while (idx < step1.size) {
            val op = step1[idx]
            val nextNum = step1.getOrNull(idx + 1)?.toDoubleOrNull() ?: 0.0
            if (op == "+") {
                result += nextNum
            } else if (op == "-") {
                result -= nextNum
            }
            idx += 2
        }

        return result
    } catch (e: Exception) {
        return null
    }
}
