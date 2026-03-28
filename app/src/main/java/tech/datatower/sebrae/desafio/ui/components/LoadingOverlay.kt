package tech.datatower.sebrae.desafio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * Overlay de carregamento animado.
 *
 * Exibe um indicador de progresso circular sobre o conteúdo existente, bloqueando interações com a
 * tela enquanto a operação estiver em andamento. A visibilidade é animada com transições de fade.
 *
 * @param isVisible Controla se o overlay está visível.
 * @param message Mensagem opcional exibida abaixo do indicador de progresso.
 * @param modifier Modificador aplicado ao container externo.
 */
@Composable
fun LoadingOverlay(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    message: String? = null,
) {
  AnimatedVisibility(
      visible = isVisible,
      enter = fadeIn(),
      exit = fadeOut(),
  ) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.38f))
                .zIndex(10f),
        contentAlignment = Alignment.Center,
    ) {
      Card(
          shape = RoundedCornerShape(16.dp),
          colors =
              CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
              ),
          elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
      ) {
        Column(
            modifier = Modifier.padding(horizontal = 36.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
          if (message != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
          }
        }
      }
    }
  }
}

