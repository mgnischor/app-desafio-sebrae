/*
    Desafio SEBRAE - Gestão Educacional Transformadora

    Arquivo: /app/src/main/java/tech/datatower/sebrae/desafio/ui/components/ListSearchHeader.kt
    Descrição: Componente de cabeçalho com campo de busca para telas de listagem.
    Autor: Miguel Nischor <miguel@nischor.com.br>

    AVISO DE LICENÇA – USO DEMONSTRATIVO

    Este software é propriedade exclusiva de seu(s) autor(es) e está protegido pelas leis de
    direitos autorais e demais legislações aplicáveis.

    Sua utilização está estritamente limitada para fins demonstrativos no contexto do evento
    “Prêmio Educador Transformador” do SEBRAE. Qualquer uso fora desse escopo, incluindo, mas
    não se limitando a, reprodução, distribuição, modificação, engenharia reversa,
    sublicenciamento, comercialização ou qualquer outra forma de exploração, é expressamente
    proibido sem autorização prévia e por escrito do(s) detentor(es) dos direitos.

    Este licenciamento não concede quaisquer direitos de propriedade intelectual ao usuário,
    sendo permitido apenas o acesso e uso temporário para apresentação e avaliação durante o
    referido evento.

    O descumprimento destes termos poderá resultar em medidas legais cabíveis.

    Todos os direitos reservados.
*/
package tech.datatower.sebrae.desafio.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Componente padrão de busca e contador de resultados para telas de listagem.
 *
 * @param query Valor atual digitado no campo de busca.
 * @param onQueryChange Callback acionado a cada alteração de texto.
 * @param placeholder Texto de ajuda exibido quando o campo está vazio.
 * @param resultCount Quantidade de itens retornados após o filtro.
 * @param resultLabel Rótulo textual complementar do contador.
 * @param modifier Modificador opcional para customização externa.
 */
@Composable
fun ListSearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    resultCount: Int,
    resultLabel: String,
    modifier: Modifier = Modifier,
) {
  OutlinedTextField(
      value = query,
      onValueChange = onQueryChange,
      placeholder = { Text(placeholder) },
      leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
      singleLine = true,
      shape = RoundedCornerShape(16.dp),
      colors =
          OutlinedTextFieldDefaults.colors(
              unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
              focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
      modifier = modifier.fillMaxWidth(),
  )
  Spacer(modifier = Modifier.height(4.dp))
  Text(
      text = "$resultCount $resultLabel",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(start = 4.dp, top = 4.dp),
  )
}
