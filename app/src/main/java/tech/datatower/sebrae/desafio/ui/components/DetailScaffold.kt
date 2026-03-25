package tech.datatower.sebrae.desafio.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import tech.datatower.sebrae.desafio.R

/**
 * Estrutura base para telas internas com barra superior e ação de retorno.
 *
 * Encapsula um `TopAppBar` com comportamento de scroll e delega o conteúdo da tela para o bloco
 * `content`.
 *
 * @param title Título principal exibido no topo da tela.
 * @param onBack Ação executada ao tocar no ícone de voltar.
 * @param actions Conteúdo opcional para ações no topo.
 * @param content Conteúdo principal da tela; recebe os `PaddingValues` do `Scaffold` e o
 *   `TopAppBarScrollBehavior` para integração de scroll.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
    content: @Composable (PaddingValues, TopAppBarScrollBehavior) -> Unit,
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
  Scaffold(
      modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      topBar = {
        TopAppBar(
            title = {
              Text(
                  text = title,
                  style = MaterialTheme.typography.titleMedium,
              )
            },
            navigationIcon = {
              IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
              }
            },
            actions = { actions() },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            scrollBehavior = scrollBehavior,
        )
      },
      containerColor = MaterialTheme.colorScheme.background,
  ) { innerPadding ->
    content(innerPadding, scrollBehavior)
  }
}
