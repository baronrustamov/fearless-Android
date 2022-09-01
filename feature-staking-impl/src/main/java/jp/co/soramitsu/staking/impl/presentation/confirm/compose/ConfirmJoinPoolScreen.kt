package jp.co.soramitsu.staking.impl.presentation.confirm.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import jp.co.soramitsu.common.compose.component.AccentButton
import jp.co.soramitsu.common.compose.component.GradientIcon
import jp.co.soramitsu.common.compose.component.H1
import jp.co.soramitsu.common.compose.component.H2
import jp.co.soramitsu.common.compose.component.InfoTable
import jp.co.soramitsu.common.compose.component.MarginVertical
import jp.co.soramitsu.common.compose.component.TitleValueViewState
import jp.co.soramitsu.common.compose.component.Toolbar
import jp.co.soramitsu.common.compose.component.ToolbarViewState
import jp.co.soramitsu.common.compose.theme.FearlessTheme
import jp.co.soramitsu.common.compose.theme.backgroundBlack
import jp.co.soramitsu.common.compose.theme.black2
import jp.co.soramitsu.common.compose.theme.colorAccentDark
import jp.co.soramitsu.feature_staking_impl.R

data class ConfirmJoinPoolScreenViewState(
    val toolbarViewState: ToolbarViewState,
    val amount: String,
    val address: TitleValueViewState,
    val selectedPool: TitleValueViewState,
    val networkFee: TitleValueViewState,
)

@Composable
fun ConfirmJoinPoolScreen(state: ConfirmJoinPoolScreenViewState, onNavigationClick: () -> Unit, onConfirm: () -> Unit) {
    Column(
        modifier = Modifier
            .background(backgroundBlack)
            .fillMaxSize()
    ) {
        MarginVertical(margin = 12.dp)
        Toolbar(state = state.toolbarViewState, onNavigationClick = onNavigationClick)
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize()
        ) {
            MarginVertical(margin = 24.dp)
            GradientIcon(iconRes = R.drawable.ic_vector, color = colorAccentDark, modifier = Modifier.align(CenterHorizontally))
            H2(
                text = stringResource(id = R.string.pool_staking_join_title),
                modifier = Modifier.align(CenterHorizontally),
                color = black2
            )
            MarginVertical(margin = 8.dp)
            H1(text = state.amount, modifier = Modifier.align(CenterHorizontally))
            MarginVertical(margin = 24.dp)
            InfoTable(listOf(state.address, state.selectedPool, state.networkFee))
            Spacer(modifier = Modifier.weight(1f))
            AccentButton(
                text = stringResource(id = R.string.common_confirm),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                onClick = onConfirm
            )
            MarginVertical(margin = 16.dp)
        }
    }
}

@Preview
@Composable
private fun ConfirmJoinPoolScreenPreview() {
    val state = ConfirmJoinPoolScreenViewState(
        toolbarViewState = ToolbarViewState("Confirm", R.drawable.ic_arrow_back_24dp),
        amount = "10 KSM",
        address = TitleValueViewState("From", "Account for join", "0x3784348729384923849223423"),
        selectedPool = TitleValueViewState("Selected Pool", "Pool #1", "id: 1"),
        networkFee = TitleValueViewState("Network Fee", "0.0051 KSM", "$0.32"),
    )
    FearlessTheme {
        ConfirmJoinPoolScreen(state, {}, {})
    }
}
